package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.*
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.data.dto.AiInvoiceResultDto
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.util.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ToolCallDto(
    val toolName: String, 
    val params: Map<String, String> = emptyMap(), 
    val reasoning: String = ""
)

class KtorGeminiRepository(
    private val httpClient: HttpClient,
    private val secretProvider: SecretProvider,
    private val jobNoteDao: com.fordham.toolbelt.data.JobNoteDao,
    private val clientDao: com.fordham.toolbelt.data.ClientDao,
    private val settingsRepository: com.fordham.toolbelt.domain.repository.SettingsRepository
) : GeminiRepository {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val modelName = secretProvider.getGeminiModelName().also { check(it.isNotBlank()) { "Gemini model name is blank" } }
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    override suspend fun processTask(type: TaskType, data: String): GeminiOutcome = try {
        val context = jobNoteDao.getRelevantContext(data.take(20))
        val contextString = context.joinToString("\n") { it.text }

        val prompt = """
            Task: ${type.name}
            Context: $contextString
            Input: $data
        """.trimIndent()

        val response = callGemini(prompt)
        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        GeminiOutcome.Success(text)
    } catch (e: Exception) {
        GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process Gemini task"))
    }

    override suspend fun processAgentCommand(input: String): AgentCommandOutcome {
        return AgentCommandOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage("Use generateToolCall instead"))
    }

    override suspend fun generateToolCall(input: String, context: String): ToolCallOutcome = try {
        val toolPrompt = """
            You are the Foreman AI Brain, the central orchestrator for the Invoice Hammer KMP app.
            Your job is to translate user natural language commands into a single, highly accurate tool call.

            [CURRENT CONTEXT]
            $context
            
            [USER INPUT]
            "$input"

            [AVAILABLE TOOLS]
            1. SEARCH_CLIENTS
               - Description: Searches the client directory.
               - Parameters: { "query": "string" }
            2. GET_CLIENT_DETAILS
               - Description: Retrieves full profile and history for a specific client.
               - Parameters: { "clientName": "string" }
            3. CREATE_DRAFT_INVOICE
               - Description: Creates a new draft invoice.
               - Parameters: { "clientName": "string", "amount": "number", "items": "string description" }
            4. DELETE_INVOICE
               - Description: [DESTRUCTIVE] Deletes an existing invoice.
               - Parameters: { "invoiceId": "string" }
            5. ADD_JOB_NOTE
               - Description: Adds a progress note to a client profile.
               - Parameters: { "clientName": "string", "note": "string content" }
            6. OPEN_TAB
               - Description: Navigates to a specific app screen.
               - Parameters: { "tabName": "CLIENTS" | "HISTORY" | "RECEIPTS" | "SETTINGS" | "STATS" | "SUPPLIERS" }

            [CRITICAL SAFETY RULES]
            - Only call DELETE_INVOICE if the user explicitly requests deletion of a specific invoice.
            - If the user's intent is conversational or doesn't map to a tool, return toolName: "UNKNOWN" and explain why in the reasoning.

            [RESPONSE FORMAT]
            You must output valid JSON only. Do not wrap in markdown block.
            {
              "toolName": "SEARCH_CLIENTS" | "GET_CLIENT_DETAILS" | "CREATE_DRAFT_INVOICE" | "DELETE_INVOICE" | "ADD_JOB_NOTE" | "OPEN_TAB" | "UNKNOWN",
              "params": { "key": "value" },
              "reasoning": "Explain step-by-step why you chose this action based on context."
            }
        """.trimIndent()

        val response = callGemini(toolPrompt, responseMimeType = "application/json")
        val resText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        val cleanedJson = AiUtil.cleanJson(resText)
        
        val dto = json.decodeFromString<ToolCallDto>(cleanedJson)
        
        ToolCallOutcome.Success(mapDtoToToolCall(dto))
    } catch (e: Exception) {
        ToolCallOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to generate tool call"))
    }

    private fun mapDtoToToolCall(dto: ToolCallDto): ForemanToolCall? {
        val type = try { ToolType.valueOf(dto.toolName) } catch (e: Exception) { return null }
        if (type == ToolType.UNKNOWN) return null

        val parameters = when (type) {
            ToolType.SEARCH_CLIENTS -> ToolParameters.SearchClients(dto.params["query"] ?: "")
            ToolType.GET_CLIENT_DETAILS -> ToolParameters.GetClientDetails(dto.params["clientName"] ?: "")
            ToolType.CREATE_DRAFT_INVOICE -> ToolParameters.CreateDraftInvoice(
                dto.params["clientName"] ?: "",
                dto.params["amount"]?.toDoubleOrNull() ?: 0.0,
                dto.params["items"] ?: ""
            )
            ToolType.DELETE_INVOICE -> ToolParameters.DeleteInvoice(InvoiceId(dto.params["invoiceId"] ?: ""))
            ToolType.ADD_JOB_NOTE -> ToolParameters.AddJobNote(dto.params["clientName"] ?: "", dto.params["note"] ?: "")
            ToolType.OPEN_TAB -> ToolParameters.OpenTab(dto.params["tabName"] ?: "")
            else -> ToolParameters.None
        }

        return ForemanToolCall(
            id = randomUUID(),
            type = type,
            parameters = parameters,
            reasoning = dto.reasoning
        )
    }

    override suspend fun processInvoiceText(text: String, categories: List<String>): InvoiceTextOutcome = try {
        val prompt = "Extract invoice data: $text"
        val response = callGemini(prompt, responseMimeType = "application/json")
        val resText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        val result = json.decodeFromString<AiInvoiceResultDto>(AiUtil.cleanJson(resText)).toDomain()
        InvoiceTextOutcome.Success(result)
    } catch (e: Exception) {
        InvoiceTextOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process invoice text"))
    }

    override suspend fun processReceiptImage(imageBytes: ByteArray): ReceiptImageOutcome = try {
        val prompt = "Extract receipt items from this image. Return JSON with 'items' array containing 'description' and 'totalPrice' for each line item."
        val response = callGemini(prompt, imageBytes = imageBytes, responseMimeType = "application/json")
        val resText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        
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
        val response = callGemini(prompt, imageBytes = imageBytes, responseMimeType = "text/plain")
        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        GeminiOutcome.Success(text)
    } catch (e: Exception) {
        GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to extract text from image"))
    }

    private suspend fun callGemini(
        prompt: String,
        imageBytes: ByteArray? = null,
        responseMimeType: String? = null
    ): GeminiResponse {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOfNotNull(
                        GeminiPart(text = prompt),
                        imageBytes?.let { 
                            GeminiPart(inlineData = GeminiInlineData("image/jpeg", encodeBase64(it))) 
                        }
                    )
                )
            ),
            generationConfig = responseMimeType?.let { GeminiGenerationConfig(it) }
        )

        val apiKey = secretProvider.getGeminiApiKey().also { check(it.isNotBlank()) { "Gemini API key is blank" } }

        return httpClient.post(baseUrl) {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
