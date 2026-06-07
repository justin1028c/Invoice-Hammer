package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.ForemanToolCallingMode

interface GeminiRepository {
    suspend fun processTask(type: TaskType, data: String): GeminiOutcome
    
    suspend fun generateToolCall(
        input: String,
        context: String,
        session: com.fordham.toolbelt.domain.model.agent.ForemanSession,
        functions: List<AgentFunction>,
        imageBytes: ByteArray? = null,
        systemInstruction: String? = null,
        toolCallingMode: ForemanToolCallingMode = ForemanToolCallingMode.RequireTool
    ): ToolCallOutcome


    // Migrated from GeminiParser
    suspend fun processInvoiceText(text: String, categories: List<String>): InvoiceTextOutcome
    suspend fun processReceiptImage(imageBytes: ByteArray): ReceiptImageOutcome
    
    // OCR capability for Raw Text Extraction
    suspend fun processOcrImage(imageBytes: ByteArray): GeminiOutcome
    
    // Audio Transcription
    suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String): GeminiOutcome
}
