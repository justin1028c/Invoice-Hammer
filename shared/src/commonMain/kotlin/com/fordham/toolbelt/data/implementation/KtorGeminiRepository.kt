package com.fordham.toolbelt.data.implementation

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
    private val jobNoteDao: com.fordham.toolbelt.data.JobNoteDao,
    private val settingsRepository: com.fordham.toolbelt.domain.repository.SettingsRepository
) : GeminiRepository {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val agentModelName = geminiConfig.agentModelName.also { check(it.isNotBlank()) { "Gemini agent model name is blank" } }
    private val taskModelName  = geminiConfig.taskModelName.ifBlank { geminiConfig.agentModelName }

    override suspend fun processTask(type: TaskType, data: String): GeminiOutcome = try {
        val contextString = if (type == TaskType.PARSE_VOICE_FRAGMENT) {
            ""
        } else {
            val queryTerm = extractSearchKeyword(data)
            val context = jobNoteDao.getRelevantContext(queryTerm)
            context.joinToString("\n") { it.text }
        }

        val (taskInstruction, outputSchema) = when (type) {
            TaskType.SUMMARIZE -> Pair(
                "You are a concise job-summary writer for a contractor app.",
                """Return a JSON object: {"summary": "string"}
                   The summary is 1-2 sentences describing the work performed. Plain text only."""
            )
            TaskType.GENERATE -> Pair(
                "You are a professional billing reminder composer for a contractor app.",
                """Return a JSON object: {"subject": "string", "body": "string"}
                   The subject is the email/text subject line. The body is a short, professional
                   plain-text reminder (2-3 sentences max). No HTML. No markdown."""
            )
            TaskType.PARSE_VOICE_FRAGMENT -> Pair(
                "You are a field invoice data extraction engine for a contractor app. " +
                "Extract structured invoice fields from a spoken voice transcript. " +
                "You must be conservative: only populate a field if you are confident. " +
                "Never invent data that is not present in the transcript.",
                """
                Return ONLY a raw JSON object — no markdown, no explanation, no preamble.
                Schema:
                {
                  "customerName":        string | null,   // Full name of the client. null if not stated.
                  "operationalAddress":  string | null,   // Job site address. null if not stated.
                  "serviceScope":        string | null,   // Description of work performed. null if not stated.
                  "amountDollars":       number | null,   // Total charge in US dollars as a decimal (e.g. 150.0). null if not stated.
                  "confidence":          number           // Your overall extraction confidence 0.0–1.0
                }
                Rules:
                - customerName: extract only if preceded by "for", "invoice", "bill", "charge", or a clear name signal.
                - operationalAddress: only populate if the fragment contains a street number + street type OR city/state.
                - serviceScope: the work description verbatim from the transcript.
                - amountDollars: convert spoken numbers to decimal dollars. "sixty five fifty" = 65.50. "$800" = 800.0.
                  "a hundred and twenty" = 120.0. Do NOT scale dollar amounts down.
                - If you are not confident a field is present, return null for that field.
                """.trimIndent()
            )
            else -> Pair(
                "You are a helpful contractor assistant.",
                """Return a JSON object: {"result": "string"} with a concise plain-text answer."""
            )
        }

        val prompt = if (contextString.isNotBlank()) {
            """
                $taskInstruction
                
                CONTEXT (job notes):
                $contextString
                
                INPUT:
                $data
                
                OUTPUT CONTRACT — CRITICAL:
                $outputSchema
                Return ONLY raw JSON. No explanation. No markdown fences. No preamble.
            """.trimIndent()
        } else {
            """
                $taskInstruction
                
                INPUT:
                $data
                
                OUTPUT CONTRACT — CRITICAL:
                $outputSchema
                Return ONLY raw JSON. No explanation. No markdown fences. No preamble.
            """.trimIndent()
        }

        val response = callGemini(
            prompt = prompt,
            model = taskModelName,
            responseMimeType = "application/json"
        )
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?.let { AiUtil.cleanJson(it) }
            ?: ""
        GeminiOutcome.Success(text)
    } catch (e: Exception) {
        GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process task"))
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
                    systemInstruction = systemInstruction
                        ?: com.fordham.toolbelt.domain.model.agent.ForemanOperatingRules.core(),
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

    override suspend fun processInvoiceText(text: String, categories: List<String>): InvoiceTextOutcome = try {
        val prompt = """
            You are an expert invoice parsing assistant. Your task is to extract invoice details from the text below.
            You must return a JSON object with the following exact keys:
            - "clientName": string, the name of the client/customer (e.g. John Doe, Smith Enterprises). If not found, return empty string.
            - "clientAddress": string, the billing address of the client. If not found, return empty string.
            - "items": array of objects, representing line items or charges. Each object MUST contain:
                * "description": string, the service or product description (e.g. "Labor", "Plumbing repair", "Materials")
                * "amount": double, the cost/amount of this line item
                * "category": string, either "Labor", "Materials", or "Service" (default to "Service")

            [INVOICE TEXT]
            $text

            CRITICAL: Return ONLY raw JSON matching this schema. No explanation or code fences.
        """.trimIndent()
        val response = callGemini(prompt, model = agentModelName, responseMimeType = "application/json")
        val resText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        val result = json.decodeFromString<AiInvoiceResultDto>(AiUtil.cleanJson(resText)).toDomain()
        InvoiceTextOutcome.Success(result)
    } catch (e: Exception) {
        InvoiceTextOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process invoice text"))
    }

    override suspend fun processReceiptImage(imageBytes: ByteArray): ReceiptImageOutcome = try {
        val prompt = """
            You are an expert receipt extraction assistant. Analyze this receipt image and extract the line items.
            Return a JSON object with the following exact schema:
            {
              "items": [
                {
                  "description": "string (name/description of the item)",
                  "totalPrice": 12.34 (double, individual item cost or price)
                }
              ]
            }
            CRITICAL: Return ONLY raw JSON matching this schema. No explanation or code fences.
        """.trimIndent()
        val response = callGemini(prompt, imageBytes = imageBytes, model = agentModelName, responseMimeType = "application/json")
        val resText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        
        val aiResponse = json.decodeFromString<AiReceiptResponse>(AiUtil.cleanJson(resText))
        val items = aiResponse.items.map { 
            ReceiptItem(
                id = ReceiptId(randomUUID()),
                description = it.description,
                totalPrice = it.totalPrice,
                lastUpdated = DateTimeUtil.nowEpochMillis(),
                clientName = ""
            )
        }
        ReceiptImageOutcome.Success(items)
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
        val response = callGemini(
            prompt = userText,
            model = agentModelName,
            history = geminiHistory,
            imageBytes = imageBytes,
            systemInstruction = systemInstruction,
            tools = ForemanGeminiTools.buildTools(functions),
            toolConfig = GeminiToolConfig(
                functionCallingConfig = GeminiFunctionCallingConfig(mode = toolCallingMode.apiValue)
            )
        )
        val part = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
        val functionCall = part?.functionCall
        if (functionCall != null) {
            val mapped = ForemanGeminiFunctionMapper.map(functionCall)
            AppLogger.d("FOREMAN_DEBUG", "function_call=${functionCall.name} args=${functionCall.args}")
            if (mapped == null) {
                return ToolCallOutcome.Failure(FailureMessage("Foreman returned an unknown tool: ${functionCall.name}"))
            }
            return ToolCallOutcome.Success(toolCall = mapped, completionReasoning = "")
        }
        val text = part?.text?.trim().orEmpty()
        if (text.isNotBlank()) {
            return ToolCallOutcome.Success(toolCall = null, completionReasoning = text)
        }
        return ToolCallOutcome.Failure(FailureMessage("Foreman returned no tool call."))
    }

    private suspend fun callGemini(
        prompt: String,
        model: String = agentModelName,
        history: List<GeminiContent> = emptyList(),
        imageBytes: ByteArray? = null,
        responseMimeType: String? = null,
        systemInstruction: String? = null,
        tools: GeminiTools? = null,
        toolConfig: GeminiToolConfig? = null,
        maxOutputTokens: Int? = null
    ): GeminiResponse {
        val currentUserTurn = GeminiContent(
            role = "user",
            parts = listOfNotNull(
                GeminiPart(text = prompt),
                imageBytes?.let {
                    GeminiPart(inlineData = GeminiInlineData("image/jpeg", encodeBase64(it)))
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
            generationConfig = responseMimeType?.let { GeminiGenerationConfig(it, maxOutputTokens) }
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
                return response.body()
            } catch (e: Exception) {
                lastException = e
                val is503 = e.message?.contains("503") == true
                val isTransient = is503 || e.message?.contains("connection") == true || e.message?.contains("network") == true
                
                if (isTransient) {
                    if (is503 && currentModel == agentModelName && agentModelName != taskModelName) {
                        AppLogger.e("KtorGeminiRepository", "Primary agent model $agentModelName returned 503. Falling back to task model $taskModelName for retry.", e)
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
}
