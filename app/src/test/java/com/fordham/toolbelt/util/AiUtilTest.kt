package com.fordham.toolbelt.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class AiUtilTest {
    @Test
    fun `cleanJson normalizes invalid escaped dollar signs from local model output`() {
        val raw = """
            Response:
            {
              "name": "quick_invoice",
              "args": {
                "lineItemsJson": "\"15 ft drywall mudded, skimmed \${'$'}400\""
              }
            }
        """.trimIndent()

        val cleaned = AiUtil.cleanJson(raw)
        val parsed = Json.parseToJsonElement(cleaned).jsonObject
        val lineItemsJson = parsed
            .getValue("args")
            .jsonObject
            .getValue("lineItemsJson")
            .jsonPrimitive
            .content

        assertEquals("\"15 ft drywall mudded, skimmed ${'$'}400\"", lineItemsJson)
    }
}
