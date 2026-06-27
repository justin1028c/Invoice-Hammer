package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.dto.AiInvoiceResultDto
import com.fordham.toolbelt.data.remote.AiReceiptResponse
import com.fordham.toolbelt.data.remote.GeminiSchema
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.util.*
import kotlinx.serialization.json.Json

import com.fordham.toolbelt.data.DatabaseProvider

class GeminiTaskProcessor(
    private val repository: KtorGeminiRepository,
    private val databaseProvider: DatabaseProvider,
    private val localLlmEngine: com.fordham.toolbelt.data.local.LocalLlmEngine
) {

    private suspend fun jobNoteDao() = databaseProvider.getDatabase().jobNoteDao()

    suspend fun processTask(type: TaskType, data: String): GeminiOutcome {
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
                    schemaObj = GeminiPromptSchemas.summarizeSchema,
                    outputMode = LlmLocalePolicy.OutputMode.UserFacingProse
                )
                TaskType.GENERATE -> TaskPromptParts(
                    instruction = "You are a professional billing reminder composer for a contractor app.",
                    outputSchema = "Return a JSON object with the subject and body.",
                    schemaObj = GeminiPromptSchemas.generateSchema,
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
                    schemaObj = GeminiPromptSchemas.voiceFragmentSchema,
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
                    schemaObj = GeminiPromptSchemas.changeOrderResponseSchema,
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
                when (localOutcome) {
                    is GeminiOutcome.Success -> {
                        AppLogger.d("KtorGeminiRepository", "Using local LLM outcome successfully.")
                        return GeminiOutcome.Success(AiUtil.cleanJson(localOutcome.text))
                    }
                    is GeminiOutcome.Failure -> {
                        AppLogger.d("KtorGeminiRepository", "Local LLM returned failure: ${localOutcome.error.value}. Falling back to cloud.")
                    }
                }
            }

            val response = repository.callGemini(
                prompt = prompt,
                model = repository.taskModelName,
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

    suspend fun processInvoiceText(text: String, categories: List<String>): InvoiceTextOutcome {
        return try {
            val currentDate = DateTimeUtil.getNowFormatted()
            val prompt = LlmLocalePolicy.wrapPrompt(
                """
                Extract structured invoice data from the following text and return a single flat JSON object matching the requested schema.
                
                ### 📅 Current Context (Use for Relative Dates)
                - Current Date Reference: $currentDate (Use this to resolve phrases like "yesterday," "last week," or "due in 15 days").
    
                ### 🛡️ Domain Constraints (Never Violate)
                - Monetary amounts must be non-negative (>= 0.0)
                - Client name cannot be blank
                - Tax rate: 0.0–100.0 (default 7.0 if unspecified)
                - Math Verification: For every item, "amount" MUST exactly equal "quantity * unitPrice".
    
                ### Schema Requirements
                Your output must be a valid JSON object matching this schema exactly:
                {
                  "clientName": "string (name of the client, cannot be blank)",
                  "clientAddress": "string (client billing address)",
                  "items": [
                    {
                      "description": "string (robust, professional description; capitalize properly, expand abbreviations/shorthand like 'rep' to 'Repair/Replacement of', do not output cryptic codes or single words without context)",
                      "quantity": number (default 1.0),
                      "unitPrice": number (default to amount if quantity is 1.0),
                      "amount": number (total item cost, MUST equal quantity * unitPrice),
                      "category": "string (either 'Labor', 'Materials', or 'Service')"
                    }
                  ],
                  "laborHours": number (optional),
                  "laborRate": number (optional),
                  "depositAmount": number (default 0.0),
                  "taxRatePercent": number (default 7.0),
                  "discountPercent": number (default 0.0),
                  "notes": "string (optional)",
                  "confidenceScore": number (0.0 to 1.0),
                  "userSummary": "string (friendly verbal summary of parsed items and totals)",
                  "validationIssues": ["string (validation warnings/codes if applicable, e.g. 'MISSING_CLIENT_NAME', 'MISSING_CLIENT_ADDRESS', 'ZERO_AMOUNT', 'MATH_MISMATCH')"]
                }
    
                [INPUT TEXT OR TRANSCRIPT]
                $text
    
                CRITICAL: Return ONLY raw JSON. No explanation. No markdown formatting. No code fences.
                """.trimIndent(),
                LlmLocalePolicy.OutputMode.StructuredJson
            )
    
            if (localLlmEngine.isSupported()) {
                val localOutcome = localLlmEngine.generateText(com.fordham.toolbelt.domain.model.LlmPrompt(prompt))
                if (localOutcome is GeminiOutcome.Success) {
                    AppLogger.d("KtorGeminiRepository", "Using local LLM for processInvoiceText successfully.")
                    val cleaned = AiUtil.cleanJson(localOutcome.text)
                    try {
                        val result = repository.json.decodeFromString<AiInvoiceResultDto>(cleaned).toDomain()
                        return InvoiceTextOutcome.Success(result)
                    } catch (e: Exception) {
                        AppLogger.e("KtorGeminiRepository", "Failed to parse local LLM invoice JSON. Raw: ${localOutcome.text}", e)
                    }
                } else {
                    AppLogger.d("KtorGeminiRepository", "Local LLM returned failure in processInvoiceText. Falling back to cloud.")
                }
            }
    
            val response = repository.callGemini(
                prompt = prompt,
                model = repository.agentModelName,
                responseMimeType = "application/json",
                temperature = 0.0f,
                responseSchema = GeminiPromptSchemas.invoiceResponseSchema
            )
            val resText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleaned = AiUtil.cleanJson(resText)
            try {
                val result = repository.json.decodeFromString<AiInvoiceResultDto>(cleaned).toDomain()
                InvoiceTextOutcome.Success(result)
            } catch (e: Exception) {
                AppLogger.e("KtorGeminiRepository", "Failed to parse invoice text JSON. Raw: $resText, Cleaned: $cleaned", e)
                throw e
            }
        } catch (e: Exception) {
            InvoiceTextOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process invoice text"))
        }
    }

    suspend fun processReceiptImage(imageBytes: ByteArray): ReceiptImageOutcome = try {
        val prompt = LlmLocalePolicy.wrapPrompt(
            """
            You are an expert receipt extraction assistant. Analyze this receipt image and extract the line items.
            Classify each item into one of the following construction categories: 'Drywall', 'Flooring', 'Roofing', 'Plumbing', 'Electrical', 'Painting', 'Carpentry', 'General Repair'.
            Return a JSON object with the following exact schema:
            {
              "items": [
                {
                  "description": "string (robust, professional description; capitalize properly, expand receipt abbreviations like 'plywd' to 'Plywood', do not output raw SKUs/codes or single words without context)",
                  "quantity": 1.0 (double, quantity of item purchased),
                  "unitPrice": 12.34 (double, individual unit price),
                  "totalPrice": 12.34 (double, item total price/cost),
                  "category": "string (one of: 'Drywall', 'Flooring', 'Roofing', 'Plumbing', 'Electrical', 'Painting', 'Carpentry', 'General Repair')"
                }
              ]
            }
            CRITICAL: Return ONLY raw JSON matching this schema. No explanation or code fences.
            """.trimIndent(),
            LlmLocalePolicy.OutputMode.StructuredJson
        )
        val response = repository.callGemini(
            prompt = prompt,
            imageBytes = imageBytes,
            model = repository.agentModelName,
            responseMimeType = "application/json",
            temperature = 0.0f,
            responseSchema = GeminiPromptSchemas.receiptResponseSchema
        )
        val resText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        val cleaned = AiUtil.cleanJson(resText)
        try {
            val aiResponse = repository.json.decodeFromString<AiReceiptResponse>(cleaned)
            val items = aiResponse.items.map { 
                val qty = it.quantity ?: 1.0
                val price = it.unitPrice ?: it.totalPrice
                ReceiptItem(
                    id = ReceiptId(randomUUID()),
                    description = it.description,
                    quantity = qty,
                    unitPrice = price,
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

    suspend fun processOcrImage(imageBytes: ByteArray): GeminiOutcome = try {
        val prompt = "Extract all text from this image exactly as written. Return only the raw text. Do not summarize."
        val response = repository.callGemini(prompt, imageBytes = imageBytes, model = repository.taskModelName, responseMimeType = "text/plain")
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        GeminiOutcome.Success(text)
    } catch (e: Exception) {
        GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to extract text from image"))
    }

    suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String): GeminiOutcome = try {
        val prompt = UserFacingCopy.Llm.transcribeInstruction()
        val response = repository.callGemini(
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
