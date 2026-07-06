package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.GeminiSchema

object GeminiPromptSchemas {

    val summarizeSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "summary" to GeminiSchema(type = "STRING", description = "Concise 1-2 sentence description of work performed.")
        ),
        required = listOf("summary")
    )

    val generateSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "subject" to GeminiSchema(type = "STRING", description = "Subject line of reminder"),
            "body" to GeminiSchema(type = "STRING", description = "Message body of reminder (2-3 sentences max).")
        ),
        required = listOf("subject", "body")
    )

    val voiceFragmentSchema = GeminiSchema(
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

    val receiptItemSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "description" to GeminiSchema(type = "STRING", description = "Robust, professional description of the item. Capitalize first letter, expand shorthand e.g., 'plywd' -> 'Plywood materials', 'sch40' -> 'Schedule 40 PVC pipe', do not output raw SKU/codes or generic single words without context"),
            "quantity" to GeminiSchema(type = "NUMBER", description = "quantity of the item purchased (default 1.0)"),
            "unitPrice" to GeminiSchema(type = "NUMBER", description = "individual unit price (default to totalPrice)"),
            "totalPrice" to GeminiSchema(type = "NUMBER", description = "item total price/cost as a decimal"),
            "category" to GeminiSchema(type = "STRING", description = "the construction category, select from: 'Drywall', 'Flooring', 'Roofing', 'Plumbing', 'Electrical', 'Painting', 'Carpentry', 'General Repair'")
        ),
        required = listOf("description", "totalPrice", "category")
    )

    val receiptResponseSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "items" to GeminiSchema(type = "ARRAY", items = receiptItemSchema, description = "list of line items found on the receipt")
        ),
        required = listOf("items")
    )

    val lineItemSchema = GeminiSchema(
        type = "OBJECT",
        properties = mapOf(
            "description" to GeminiSchema(type = "STRING", description = "Robust, professional description of the service or product. Capitalize first letter, expand abbreviations e.g., 'rep sink' -> 'Repair/replace leaking sink', 'inst panel' -> 'Installation of new electrical panel', do not output generic single words without context"),
            "amount" to GeminiSchema(type = "NUMBER", description = "the cost/amount of this line item"),
            "category" to GeminiSchema(type = "STRING", description = "select from: 'Drywall', 'Flooring', 'Roofing', 'Plumbing', 'Electrical', 'Painting', 'Carpentry', 'General Repair', 'Labor', or 'Materials'")
        ),
        required = listOf("description", "amount", "category")
    )

    val invoiceResponseSchema = GeminiSchema(
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

    val changeOrderOpportunitySchema = GeminiSchema(
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

    val changeOrderResponseSchema = GeminiSchema(
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
}
