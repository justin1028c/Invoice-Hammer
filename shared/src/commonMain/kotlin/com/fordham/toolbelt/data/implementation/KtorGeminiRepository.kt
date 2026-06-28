package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.DatabaseProvider
import com.fordham.toolbelt.data.remote.*
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.data.dto.AiInvoiceResultDto
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.util.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


class KtorGeminiRepository(
    private val httpClient: HttpClient,
    private val geminiConfig: ForemanGeminiConfig,
    private val databaseProvider: DatabaseProvider,
    private val settingsRepository: com.fordham.toolbelt.domain.repository.SettingsRepository,
    private val localLlmEngine: com.fordham.toolbelt.data.local.LocalLlmEngine
) : GeminiRepository {

    private suspend fun jobNoteDao() = databaseProvider.getDatabase().jobNoteDao()

    internal val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    internal val agentModelName = geminiConfig.agentModelName.also { check(it.isNotBlank()) { "Gemini agent model name is blank" } }
    internal val taskModelName  = geminiConfig.taskModelName.ifBlank { geminiConfig.agentModelName }

    private val summarizeSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "summary" to GeminiSchema(type = "STRING", description = "Concise 1-2 sentence description of work performed.")
        ),
        required = listOf("summary")
    )

    private val generateSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "subject" to GeminiSchema(type = "STRING", description = "Subject line of reminder"),
            "body" to GeminiSchema(type = "STRING", description = "Message body of reminder (2-3 sentences max).")
        ),
        required = listOf("subject", "body")
    )

    private val voiceFragmentSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "customerName" to GeminiSchema(type = "STRING", description = "Full name of the client. null if not stated."),
            "operationalAddress" to GeminiSchema(type = "STRING", description = "Job site address. null if not stated."),
            "serviceScope" to GeminiSchema(type = "STRING", description = "Verbatim work description. null if not stated."),
            "amountDollars" to GeminiSchema(type = "NUMBER", description = "Total charge in USD as a decimal. null if not stated."),
            "confidence" to GeminiSchema(type = "NUMBER", description = "Overall extraction confidence 0.0-1.0")
        ),
        required = listOf("confidence")
    )

    private val receiptItemSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "description" to GeminiSchema(type = "STRING", description = "verbatim item name or description"),
            "totalPrice" to GeminiSchema(type = "NUMBER", description = "item total price/cost as a decimal"),
            "category" to GeminiSchema(type = "STRING", description = "the construction category, select from: 'Drywall', 'Flooring', 'Roofing', 'Plumbing', 'Electrical', 'Painting', 'Carpentry', 'General Repair'")
        ),
        required = listOf("description", "totalPrice", "category")
    )

    private val receiptResponseSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "items" to GeminiSchema(type = "ARRAY", items = receiptItemSchema, description = "list of line items found on the receipt")
        ),
        required = listOf("items")
    )

    private val lineItemSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "description" to GeminiSchema(type = "STRING", description = "the service or product description"),
            "amount" to GeminiSchema(type = "NUMBER", description = "the cost/amount of this line item"),
            "category" to GeminiSchema(type = "STRING", description = "either 'Labor', 'Materials', or 'Service'")
        ),
        required = listOf("description", "amount", "category")
    )

    private val invoiceResponseSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "clientName" to GeminiSchema(type = "STRING", description = "the name of the client/customer"),
            "clientAddress" to GeminiSchema(type = "STRING", description = "the billing address of the client"),
            "items" to GeminiSchema(type = "ARRAY", items = lineItemSchema, description = "list of line items or charges"),
            "laborHours" to GeminiSchema(type = "NUMBER", description = "hourly labor hours"),
            "laborRate" to GeminiSchema(type = "NUMBER", description = "hourly labor rate in dollars"),
            "depositAmount" to GeminiSchema(type = "NUMBER", description = "deposit amount in dollars"),
            "taxRatePercent" to GeminiSchema(type = "NUMBER", description = "tax rate percentage"),
            "discountPercent" to GeminiSchema(type = "NUMBER", description = "discount percentage"),
            "notes" to GeminiSchema(type = "STRING", description = "additional terms/notes"),
            "confidenceScore" to GeminiSchema(type = "NUMBER", description = "overall parsing confidence score 0.0-1.0"),
            "userSummary" to GeminiSchema(type = "STRING", description = "verbal confirmation summary"),
            "validationIssues" to GeminiSchema(
                type = "ARRAY",
                items = GeminiSchema(type = "STRING"),
                description = "validation warnings"
            )
        ),
        required = listOf("clientName", "clientAddress", "items")
    )

    private val changeOrderOpportunitySchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "detectedTask" to GeminiSchema(type = "STRING", description = "Verbatim name of extra work/task done"),
            "confidence" to GeminiSchema(type = "STRING", description = "Confidence level, select from: 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'"),
            "minPrice" to GeminiSchema(type = "NUMBER", description = "Minimum estimated cost in USD"),
            "maxPrice" to GeminiSchema(type = "NUMBER", description = "Maximum estimated cost in USD"),
            "recommendedItems" to GeminiSchema(
                type = "ARRAY",
                items = lineItemSchema,
                description = "Recommended line item(s) to add to the change order"
            )
        ),
        required = listOf("detectedTask", "confidence", "minPrice", "maxPrice", "recommendedItems")
    )

    private val changeOrderResponseSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "opportunities" to GeminiSchema(
                type = "ARRAY",
                items = changeOrderOpportunitySchema,
                description = "List of potential unbilled tasks detected in logs/transcripts"
            )
        ),
        required = listOf("opportunities")
    )

    override suspend fun processTask(type: TaskType, data: String): GeminiOutcome {
        return try {
            val contextString = if (type == TaskType.PARSE_VOICE_FRAGMENT) {
                ""
            } else {
                val queryTerm = extractSearchKeyword(data)
                val context = jobNoteDao().getRelevantContext(queryTerm)
                context.joinToString("\n") { it.text }
            }

            val taskPrompt = when (type) {
                TaskType.SUMMARIZE -> TaskPromptParts(
                    instruction = "You are a concise job-summary writer for a contractor app.",
                    outputSchema = "Return a JSON object with the summary description.",
                    schemaObj = summarizeSchema,
                    outputMode = LlmLocalePolicy.OutputMode.UserFacingProse
                )
                TaskType.GENERATE -> TaskPromptParts(
                    instruction = "You are a professional billing reminder composer for a contractor app.",
                    outputSchema = "Return a JSON object with the subject and body.",
                    schemaObj = generateSchema,
                    outputMode = LlmLocalePolicy.OutputMode.UserFacingProse
                )
                TaskType.PARSE_VOICE_FRAGMENT -> TaskPromptParts(
                    instruction = "You are a field invoice data extraction engine for a contractor app. " +
                        "Extract structured invoice fields from a spoken voice transcript. " +
                        "You must be conservative: only populate a field if you are confident. " +
                        "Never invent data that is not present in the transcript.\n\n" +
                        "EXAMPLE:\n" +
                        "Input: \"charge eighty five fifty to john doe at twelve main street for drywall repair\"\n" +
                        "Output: {\"customerName\": \"John Doe\", \"operationalAddress\": \"12 Main Street\", \"serviceScope\": \"Drywall repair\", \"amountDollars\": 85.50, \"confidence\": 1.0}\n\n",
                    outputSchema = "Extract customerName, operationalAddress, serviceScope, amountDollars, and confidence.",
                    schemaObj = voiceFragmentSchema,
                    outputMode = LlmLocalePolicy.OutputMode.StructuredJson
                )
                TaskType.DETECT_CHANGE_ORDERS -> TaskPromptParts(
                    instruction = "You are an expert change order analysis engine for a contractor app. " +
                        "Compare the original estimate (budgeted items) against the recent logs, transcripts, or extra customer requests. " +
                        "Identify any tasks described in the logs/transcripts that represent work NOT covered by the original estimate. " +
                        "For each identified task, return: " +
                        "1. detectedTask: a concise name for the extra work. " +
                        "2. confidence: your confidence level ('LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'). " +
                        "3. minPrice & maxPrice: estimated cost range for this task. " +
                        "4. recommendedItems: suggested line items to bill for this work.",
                    outputSchema = "Return a JSON object containing an array of opportunities.",
                    schemaObj = changeOrderResponseSchema,
                    outputMode = LlmLocalePolicy.OutputMode.StructuredJson
                )
                else -> TaskPromptParts(
                    instruction = "You are a helpful contractor assistant.",
                    outputSchema = "Return a concise plain-text answer in a JSON object.",
                    schemaObj = null,
                    outputMode = LlmLocalePolicy.OutputMode.UserFacingProse
                )
            }

            val localizedInstruction = LlmLocalePolicy.wrapPrompt(taskPrompt.instruction, taskPrompt.outputMode)

            val prompt = if (contextString.isNotBlank()) {
                """
                    $localizedInstruction
                    
                    CONTEXT (job notes):
                    $contextString
                    
                    INPUT:
                    $data
                    
                    OUTPUT CONTRACT — CRITICAL:
                    ${taskPrompt.outputSchema}
                    Return ONLY raw JSON matching the schema. No explanation. No markdown fences. No preamble.
                """.trimIndent()
            } else {
                """
                    $localizedInstruction
                    
                    INPUT:
                    $data
                    
                    OUTPUT CONTRACT — CRITICAL:
                    ${taskPrompt.outputSchema}
                    Return ONLY raw JSON matching the schema. No explanation. No markdown fences. No preamble.
                """.trimIndent()
            }

            if (localLlmEngine.isSupported()) {
                val localOutcome = localLlmEngine.generateText(com.fordham.toolbelt.domain.model.LlmPrompt(prompt))
                if (localOutcome is GeminiOutcome.Success) {
                    AppLogger.d("KtorGeminiRepository", "Using local LLM outcome successfully.")
                    return GeminiOutcome.Success(AiUtil.cleanJson(localOutcome.text))
                } else {
                    AppLogger.d("KtorGeminiRepository", "Local LLM returned failure: ${(localOutcome as GeminiOutcome.Failure).error.value}. Falling back to cloud.")
                }
            }

            val response = callGemini(
                prompt = prompt,
                model = taskModelName,
                responseMimeType = "application/json",
                temperature = 0.0f,
                responseSchema = taskPrompt.schemaObj
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?.let { AiUtil.cleanJson(it) }
                ?: ""
            GeminiOutcome.Success(text)
        } catch (e: Exception) {
            GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process task"))
        }
    }

    private fun extractSearchKeyword(data: String): String {
        val stopWords = setOf(
            "the", "and", "with", "this", "your", "that", "from", "have", "will",
            "please", "just", "about", "some", "been", "what", "were", "invoice", "client",
            "draft", "customer", "total", "amount", "unpaid", "friendly", "professional", "reminder"
        )
        val cleanTerms = data.lowercase()
            .split(Regex("[^a-zA-Z0-9]"))
            .map { it.trim() }
            .filter { it.length > 3 && it !in stopWords }
        return cleanTerms.firstOrNull() ?: data.take(20).trim()
    }

    override suspend fun generateToolCall(
        input: String,
        context: String,
        session: com.fordham.toolbelt.domain.model.agent.ForemanSession,
        functions: List<AgentFunction>,
        imageBytes: ByteArray?,
        systemInstruction: String?,
        toolCallingMode: com.fordham.toolbelt.domain.model.agent.ForemanToolCallingMode
    ): ToolCallOutcome {
        return try {
            if (functions.isEmpty()) {
                ToolCallOutcome.Failure(
                    FailureMessage("Foreman requires native function calling; no tools were registered.")
                )
            } else {
                generateToolCallWithFunctions(
                    input = input,
                    context = context,
                    session = session,
                    functions = functions,
                    imageBytes = imageBytes,
                    systemInstruction = LlmLocalePolicy.wrapSystemInstruction(
                        systemInstruction ?: com.fordham.toolbelt.domain.model.agent.ForemanOperatingRules.core(),
                        LlmLocalePolicy.OutputMode.UserFacingProse
                    ),
                    toolCallingMode = toolCallingMode
                )
            }
        } catch (e: Exception) {
            AppLogger.e("KtorGeminiRepository", "generateToolCall failed", e)
            ToolCallOutcome.Failure(mapForemanFailure(e))
        }
    }

    private fun mapForemanFailure(e: Exception): FailureMessage {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("not configured", ignoreCase = true) ->
                FailureMessage("Can't reach Foreman — add foreman.gemini.backend.url in local.properties.")
            msg.contains("401", ignoreCase = true) || msg.contains("Unauthorized", ignoreCase = true) ->
                FailureMessage("Foreman auth failed. Check foreman.backend.api.key matches Supabase.")
            msg.contains("503", ignoreCase = true) || msg.contains("Service Unavailable", ignoreCase = true) ->
                FailureMessage("Google Gemini is temporarily overloaded (503 Service Unavailable). Please try again in a few seconds.")
            msg.contains("429", ignoreCase = true) ->
                FailureMessage("Foreman is busy — try again in a minute.")
            msg.contains("connection", ignoreCase = true) || msg.contains("network", ignoreCase = true) ->
                FailureMessage("Can't reach Foreman — check your connection.")
            else -> FailureMessage(msg.ifBlank { "Foreman request failed." })
        }
    }

    override suspend fun processInvoiceText(text: String, categories: List<String>): InvoiceTextOutcome {
        return try {
        val currentDate = DateTimeUtil.getNowFormatted()
        val prompt = LlmLocalePolicy.wrapPrompt(
            """
            You are Foreman, the voice-first AI assistant inside Invoice Hammer for handymen and contractors (Bilingual English + Spanish).

            Goal: Turn spoken commands or noisy transcriptions into accurate, structured invoice drafts. Always prioritize user control and data integrity.

            ### 📅 Current Context (Use for Relative Dates)
            - Current Date Reference: $currentDate (Use this to resolve phrases like "yesterday," "last week," or "due in 15 days").

            ### 🛡️ Domain Constraints (Never Violate)
            - Monetary amounts must be non-negative (>= 0.0)
            - Client name cannot be blank
            - Tax rate: 0.0–100.0 (default 7.0 if unspecified)
            - Math Verification: For every item, "amount" MUST exactly equal "quantity * unitPrice". Double-check your math.

            ### 📤 Required Output Schema Description
            Extract and return the details from the text below as a single flat JSON object with the following schema:
            - clientName: string, name of the client. Cannot be blank.
            - clientAddress: string, client billing address.
            - items: array of objects representing line items. Each object MUST contain:
                * description: string, the service or product summary.
                * quantity: number, quantity of units (default 1.0 if not specified).
                * unitPrice: number, cost per unit (default to amount if quantity is 1.0).
                * amount: number, total item cost (MUST equal quantity * unitPrice).
                * category: string, select from 'Labor', 'Materials', or 'Service'.
            - laborHours: number, hourly labor hours if stated.
            - laborRate: number, hourly labor rate in dollars if stated.
            - depositAmount: number, deposit amount in dollars (default 0.0).
            - taxRatePercent: number, tax percentage rate (0.0 to 100.0, default 7.0).
            - discountPercent: number, discount percentage (default 0.0).
            - notes: string, any additional terms or notes.
            - confidenceScore: number, overall parsing confidence score (0.0 to 1.0).
            - userSummary: string, friendly verbal summary of parsed items and totals.
            - validationIssues: array of strings. Fill with these codes if applicable: "MISSING_CLIENT_NAME", "MISSING_CLIENT_ADDRESS", "ZERO_AMOUNT", "LOW_AUDIO_CONFIDENCE", "MATH_MISMATCH".

            ### 🔨 Contractor Domain Knowledge & Lingo
            * Drywall: "15 ft of drywall mudded taped and skimmed at $500" -> Qty: 15, Price: 500, Category: "Materials"
            * Plumbing: "Replaced toilet at $200" -> Qty: 1, Price: 200, Category: "Service"
            * Labor: "Add 10 hours at $50/hour" -> Qty: 10, Price: 50, Category: "Labor"
            * Bilingual matching: Map Spanish terms to standardized descriptions (e.g., "tablaroca" to "Sheetrock/Drywall", "masilla" to "Drywall Mud", "inodoro" to "Toilet replacement").

            #### 🌐 Few-Shot Bilingual / Spanglish Examples:
            * Input: "Cobra 5 horas de labor a $45 y ponle $150 de la masilla y la tablaroca a Justin"
                - clientName: "Justin"
                - laborHours: 5.0, laborRate: 45.0
                - items: [{"description": "Drywall mud and sheetrock (Masilla y tablaroca)", "quantity": 1.0, "unitPrice": 150.0, "amount": 150.0, "category": "Materials"}]
            * Input: "Factura a John Doe $200 por reparar el inodoro y ponle tax de siete por ciento"
                - clientName: "John Doe"
                - items: [{"description": "Toilet repair (Reparación de inodoro)", "quantity": 1.0, "unitPrice": 200.0, "amount": 200.0, "category": "Service"}]
                - taxRatePercent: 7.0

            ### 🎤 Handling Noisy Job-Site Inputs
            * Noisy Audio/Uncertainty: If input is noisy or unclear, drop confidenceScore < 0.6, output empty fields, append "LOW_AUDIO_CONFIDENCE" to validationIssues, and ask for clarification in userSummary.
            * Incremental Additions: If user says "Add 2 hours to that", append it as a new line item.

            [INVOICE TEXT OR TRANSCRIPT]
            $text

            CRITICAL: Return ONLY raw JSON matching this schema. No explanation or markdown code fences.
            """.trimIndent(),
            LlmLocalePolicy.OutputMode.StructuredJson
        )
        if (localLlmEngine.isSupported()) {
            val localOutcome = localLlmEngine.generateText(com.fordham.toolbelt.domain.model.LlmPrompt(prompt))
            if (localOutcome is GeminiOutcome.Success) {
                AppLogger.d("KtorGeminiRepository", "Using local LLM for processInvoiceText successfully.")
                val cleaned = AiUtil.cleanJson(localOutcome.text)
                try {
                    val result = json.decodeFromString<AiInvoiceResultDto>(cleaned).toDomain()
                    return InvoiceTextOutcome.Success(result)
                } catch (e: Exception) {
                    AppLogger.e("KtorGeminiRepository", "Failed to parse local LLM invoice JSON. Raw: ${localOutcome.text}", e)
                }
            } else {
                AppLogger.d("KtorGeminiRepository", "Local LLM returned failure in processInvoiceText. Falling back to cloud.")
            }
        }

        val response = callGemini(
            prompt = prompt,
            model = agentModelName,
            responseMimeType = "application/json",
            temperature = 0.0f,
            responseSchema = invoiceResponseSchema
        )
        val resText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        val cleaned = AiUtil.cleanJson(resText)
        try {
            val result = json.decodeFromString<AiInvoiceResultDto>(cleaned).toDomain()
            InvoiceTextOutcome.Success(result)
        } catch (e: Exception) {
            AppLogger.e("KtorGeminiRepository", "Failed to parse invoice text JSON. Raw: $resText, Cleaned: $cleaned", e)
            throw e
        }
    } catch (e: Exception) {
        InvoiceTextOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process invoice text"))
    }
}

    override suspend fun processReceiptImage(imageBytes: ByteArray): ReceiptImageOutcome = try {
        val prompt = LlmLocalePolicy.wrapPrompt(
            """
            You are an expert receipt extraction assistant. Analyze this receipt image and extract the line items.
            Classify each item into one of the following construction categories: 'Drywall', 'Flooring', 'Roofing', 'Plumbing', 'Electrical', 'Painting', 'Carpentry', 'General Repair'.
            Return a JSON object with the following exact schema:
            {
              "items": [
                {
                  "description": "string (name/description of the item)",
                  "totalPrice": 12.34 (double, individual item cost or price),
                  "category": "string (one of: 'Drywall', 'Flooring', 'Roofing', 'Plumbing', 'Electrical', 'Painting', 'Carpentry', 'General Repair')"
                }
              ]
            }
            CRITICAL: Return ONLY raw JSON matching this schema. No explanation or code fences.
            """.trimIndent(),
            LlmLocalePolicy.OutputMode.StructuredJson
        )
        val response = callGemini(
            prompt = prompt,
            imageBytes = imageBytes,
            model = agentModelName,
            responseMimeType = "application/json",
            temperature = 0.0f,
            responseSchema = receiptResponseSchema
        )
        val resText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        val cleaned = AiUtil.cleanJson(resText)
        try {
            val aiResponse = json.decodeFromString<AiReceiptResponse>(cleaned)
            val items = aiResponse.items.map { 
                ReceiptItem(
                    id = ReceiptId(randomUUID()),
                    description = it.description,
                    totalPrice = it.totalPrice,
                    category = it.category ?: "General Repair",
                    lastUpdated = DateTimeUtil.nowEpochMillis(),
                    clientName = ""
                )
            }
            ReceiptImageOutcome.Success(items)
        } catch (e: Exception) {
            AppLogger.e("KtorGeminiRepository", "Failed to parse receipt image JSON. Raw: $resText, Cleaned: $cleaned", e)
            throw e
        }
    } catch (e: Exception) {
        ReceiptImageOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process receipt image"))
    }

    override suspend fun processOcrImage(imageBytes: ByteArray): GeminiOutcome = try {
        val prompt = "Extract all text from this image exactly as written. Return only the raw text. Do not summarize."
        val response = callGemini(prompt, imageBytes = imageBytes, model = taskModelName, responseMimeType = "text/plain")
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        GeminiOutcome.Success(text)
    } catch (e: Exception) {
        GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to extract text from image"))
    }

    private fun buildGeminiHistory(session: com.fordham.toolbelt.domain.model.agent.ForemanSession): List<GeminiContent> {
        val lastUserIdx = session.history.indexOfLast { it.role == com.fordham.toolbelt.domain.model.agent.AgentRole.User }
        if (lastUserIdx <= 0) return emptyList()

        val priorTurns = session.history.subList(0, lastUserIdx)
        val result = mutableListOf<GeminiContent>()
        for (turn in priorTurns) {
            when (turn.role) {
                com.fordham.toolbelt.domain.model.agent.AgentRole.User -> {
                    result += GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = turn.content.value))
                    )
                }
                com.fordham.toolbelt.domain.model.agent.AgentRole.Foreman -> {
                    result += GeminiContent(
                        role = "model",
                        parts = listOf(GeminiPart(text = turn.content.value))
                    )
                }
                com.fordham.toolbelt.domain.model.agent.AgentRole.ToolSystem -> {
                    if (!turn.content.value.startsWith(com.fordham.toolbelt.domain.model.agent.ForemanOperatingRules.SUMMARY_MARKER)) {
                        result += GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(text = "[TOOL_RESULT] ${turn.content.value}"))
                        )
                    }
                }
            }
        }
        return result
    }

    private suspend fun generateToolCallWithFunctions(
        input: String,
        context: String,
        session: com.fordham.toolbelt.domain.model.agent.ForemanSession,
        functions: List<AgentFunction>,
        imageBytes: ByteArray?,
        systemInstruction: String,
        toolCallingMode: com.fordham.toolbelt.domain.model.agent.ForemanToolCallingMode
    ): ToolCallOutcome {
        val userText = buildString {
            append(context)
            append("\n\n[USER REQUEST]\n")
            append(input)
            if (imageBytes != null) {
                append("\n\n[RECEIPT_IMAGE_ATTACHED] A receipt photo is attached. ")
                append("Use SCAN_LAST_RECEIPT to OCR it into Receipts, or infer line items if the user wants to bill from the receipt.")
            }
        }
        val geminiHistory = buildGeminiHistory(session)
        AppLogger.d("FOREMAN_DEBUG", "PROMPT:\n$userText\nSYSTEM_INSTRUCTION:\n$systemInstruction")
        val response = callGemini(
            prompt = userText,
            model = agentModelName,
            history = geminiHistory,
            imageBytes = imageBytes,
            systemInstruction = systemInstruction,
            tools = ForemanGeminiTools.buildTools(functions),
            toolConfig = GeminiToolConfig(
                functionCallingConfig = GeminiFunctionCallingConfig(mode = toolCallingMode.apiValue)
            ),
            temperature = 0.0f
        )
        val part = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
        val functionCall = part?.functionCall
        val text = part?.text?.trim().orEmpty()
        AppLogger.d("FOREMAN_DEBUG", "RESPONSE_TEXT: $text\nFUNCTION_CALL: ${functionCall?.name} args=${functionCall?.args}")
        if (functionCall != null) {
            val mapped = ForemanGeminiFunctionMapper.map(functionCall)
            AppLogger.d("FOREMAN_DEBUG", "function_call=${functionCall.name} args=${functionCall.args}")
            if (mapped == null) {
                return ToolCallOutcome.Failure(FailureMessage("Foreman returned an unknown tool: ${functionCall.name}"))
            }
            return ToolCallOutcome.Success(toolCall = mapped, completionReasoning = "")
        }
        if (text.isNotBlank()) {
            return ToolCallOutcome.Success(toolCall = null, completionReasoning = text)
        }
        return ToolCallOutcome.Failure(FailureMessage("Foreman returned no tool call."))
    }

    internal suspend fun callGemini(
        prompt: String,
        model: String = agentModelName,
        history: List<GeminiContent> = emptyList(),
        imageBytes: ByteArray? = null,
        audioBytes: ByteArray? = null,
        audioMimeType: String? = null,
        responseMimeType: String? = null,
        systemInstruction: String? = null,
        tools: GeminiTools? = null,
        toolConfig: GeminiToolConfig? = null,
        maxOutputTokens: Int? = null,
        temperature: Float? = null,
        responseSchema: GeminiSchema? = null
    ): GeminiResponse {
        val currentUserTurn = GeminiContent(
            role = "user",
            parts = listOfNotNull(
                GeminiPart(text = prompt),
                imageBytes?.let {
                    GeminiPart(inlineData = GeminiInlineData("image/jpeg", encodeBase64(it)))
                },
                audioBytes?.let {
                    if (audioMimeType != null) {
                        GeminiPart(inlineData = GeminiInlineData(audioMimeType, encodeBase64(it)))
                    } else null
                }
            )
        )
        val request = GeminiRequest(
            contents = history + currentUserTurn,
            systemInstruction = systemInstruction?.let {
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = it)))
            },
            tools = tools?.let { listOf(it) },
            toolConfig = toolConfig,
            generationConfig = if (responseMimeType != null || maxOutputTokens != null || temperature != null || responseSchema != null) {
                GeminiGenerationConfig(
                    responseMimeType = responseMimeType,
                    maxOutputTokens = maxOutputTokens,
                    temperature = temperature,
                    responseSchema = responseSchema
                )
            } else null
        )

        check(geminiConfig.isReady) {
            "Foreman Gemini backend is not configured. Set foreman.gemini.backend.url and gemini.model.name in local.properties."
        }

        val proxyUrl = "${geminiConfig.backendBaseUrl.trimEnd('/')}/v1/generate"
        var lastException: Exception? = null
        var currentModel = model
        
        for (attempt in 1..3) {
            try {
                AppLogger.d("KtorGeminiRepository", "Sending request on attempt $attempt using model: $currentModel")
                val response = httpClient.post(proxyUrl) {
                    if (geminiConfig.isBackendApiKeyConfigured) {
                        header("x-foreman-backend-key", geminiConfig.backendApiKey)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(ForemanGeminiProxyRequest(model = currentModel, request = request))
                }
                
                if (response.status.value == 503) {
                    val errorBody = try { response.bodyAsText() } catch (e: Exception) { "503 Service Unavailable" }
                    throw Exception("Foreman Gemini proxy failed with status 503 (Service Unavailable): $errorBody")
                }
                
                if (!response.status.isSuccess()) {
                    val errorBody = try { response.bodyAsText() } catch (e: Exception) { "Could not read error body: ${e.message}" }
                    throw Exception(
                        "Foreman Gemini proxy failed with status ${response.status} (code ${response.status.value}). " +
                            "Response details: $errorBody"
                    )
                }
                val geminiRes: GeminiResponse = response.body()
                val usage = geminiRes.usageMetadata
                if (usage != null) {
                    val inputRate = if (currentModel.contains("lite", ignoreCase = true) || currentModel.contains("8b", ignoreCase = true)) 0.0375 else 0.075
                    val outputRate = if (currentModel.contains("lite", ignoreCase = true) || currentModel.contains("8b", ignoreCase = true)) 0.15 else 0.30
                    val inputCost = (usage.promptTokenCount / 1_000_000.0) * inputRate
                    val outputCost = (usage.candidatesTokenCount / 1_000_000.0) * outputRate
                    val totalCost = inputCost + outputCost
                    
                    AppLogger.d(
                        "KtorGeminiRepository",
                        "GEMINI_COST_TRACKER | Model: $currentModel | " +
                            "Input: ${usage.promptTokenCount} tokens | Output: ${usage.candidatesTokenCount} tokens | " +
                            "Cost: $$${DateTimeUtil.formatDecimal(totalCost, 6)}"
                    )
                    
                    try {
                        val currentSettings = settingsRepository.getBusinessSettings()
                        val updatedCost = currentSettings.cumulativeLlmCostUsd + totalCost
                        settingsRepository.saveBusinessSettings(currentSettings.copy(cumulativeLlmCostUsd = updatedCost))
                    } catch (se: Exception) {
                        AppLogger.e("KtorGeminiRepository", "Failed to update cumulative LLM cost in settings", se)
                    }
                }
                return geminiRes
            } catch (e: Exception) {
                lastException = e
                val is503 = e.message?.contains("503") == true
                val is429 = e.message?.contains("429") == true
                val isTransient = is503 || is429 || e.message?.contains("connection") == true || e.message?.contains("network") == true
                
                if (isTransient) {
                    if ((is503 || is429) && currentModel == agentModelName && agentModelName != taskModelName) {
                        AppLogger.e("KtorGeminiRepository", "Primary agent model $agentModelName returned ${if (is503) "503" else "429"}. Falling back to task model $taskModelName for retry.", e)
                        currentModel = taskModelName
                    }
                    if (attempt < 3) {
                        val backoffMs = attempt * 1500L
                        AppLogger.e("KtorGeminiRepository", "Transient failure on attempt $attempt, retrying in ${backoffMs}ms...", e)
                        delay(backoffMs)
                    } else {
                        throw e
                    }
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: Exception("Foreman Gemini request failed after retries.")
    }

    override suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String): GeminiOutcome = try {
        val prompt = UserFacingCopy.Llm.transcribeInstruction()
        val response = callGemini(
            prompt = prompt,
            audioBytes = audioBytes,
            audioMimeType = mimeType,
            temperature = 0.0f
        )
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        GeminiOutcome.Success(text.trim())
    } catch (e: Exception) {
        GeminiOutcome.Failure(FailureMessage(e.message ?: "Failed to transcribe audio"))
    }

    private data class TaskPromptParts(
        val instruction: String,
        val outputSchema: String,
        val schemaObj: GeminiSchema?,
        val outputMode: LlmLocalePolicy.OutputMode
    )
}
