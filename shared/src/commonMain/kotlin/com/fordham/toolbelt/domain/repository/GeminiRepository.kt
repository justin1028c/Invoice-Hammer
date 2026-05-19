package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.*

interface GeminiRepository {
    suspend fun processTask(type: TaskType, data: String): GeminiOutcome
    suspend fun processAgentCommand(input: String): AgentCommandOutcome
    
    // New Agentic Brain Contract
    suspend fun generateToolCall(input: String, context: String): ToolCallOutcome

    // Migrated from GeminiParser
    suspend fun processInvoiceText(text: String, categories: List<String>): InvoiceTextOutcome
    suspend fun processReceiptImage(imageBytes: ByteArray): ReceiptImageOutcome
    
    // OCR capability for Raw Text Extraction
    suspend fun processOcrImage(imageBytes: ByteArray): GeminiOutcome
}
