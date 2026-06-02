package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.GeminiFunctionDeclaration
import com.fordham.toolbelt.data.remote.GeminiSchema
import com.fordham.toolbelt.data.remote.GeminiTools
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.ParameterType
import com.fordham.toolbelt.domain.model.agent.toApiName

internal object ForemanGeminiTools {
    private val lineItemObjectSchema = GeminiSchema(
        type = "OBJECT",
        description = "Individual invoice line item detail",
        properties = mapOf(
            "description" to GeminiSchema(type = "STRING", description = "Job detail or material name verbatim"),
            "amount" to GeminiSchema(type = "NUMBER", description = "Total line item cost"),
            "category" to GeminiSchema(type = "STRING", description = "Trade category — MUST be one of: Drywall, Plumbing, Electrical, Painting, Carpentry, Flooring, Roofing, General Repair. Infer from description if not stated."),
            "quantity" to GeminiSchema(type = "NUMBER", description = "Optional quantity or hours"),
            "unitPrice" to GeminiSchema(type = "NUMBER", description = "Optional rate per hour or price per unit")
        ),
        required = listOf("description", "amount", "category")
    )

    private val lineItemsArraySchema = GeminiSchema(
        type = "ARRAY",
        description = "List of services, labor, and materials to bill on the invoice",
        items = lineItemObjectSchema
    )

    fun buildTools(functions: List<AgentFunction>): GeminiTools {
        val declarations = functions.map { fn ->
            GeminiFunctionDeclaration(
                name = fn.toolName.toApiName(),
                description = fn.description.value,
                parameters = GeminiSchema(
                    type = "OBJECT",
                    properties = fn.parameters.associate { param ->
                        val schemaType = when (param.name.value) {
                            "lineItems", "lineItemsJson" -> lineItemsArraySchema
                            "taxRate", "deposit" -> GeminiSchema(type = "NUMBER")
                            "isEstimate", "replaceLineItems", "createClientIfMissing" ->
                                GeminiSchema(type = "BOOLEAN")
                            else -> GeminiSchema(type = "STRING")
                        }
                        param.name.value to schemaType
                    },
                    required = fn.parameters.filter { it.required }.map { it.name.value }.ifEmpty { null }
                )
            )
        }
        return GeminiTools(functionDeclarations = declarations)
    }
}
