package com.fordham.toolbelt.util

object AiUtil {
    fun cleanJson(raw: String): String {
        var cleaned = raw.trim()
        
        // Remove markdown code blocks if present
        if (cleaned.contains("```")) {
            // Try to find content between ```json and ``` or just ``` and ```
            val regex = "```(?:json)?\\s*([\\s\\S]*?)\\s*```".toRegex()
            val match = regex.find(cleaned)
            if (match != null) {
                cleaned = match.groupValues[1].trim()
            } else {
                // Fallback: just strip the markers
                cleaned = cleaned.replace("```json", "").replace("```", "").trim()
            }
        }

        // Handle pre-filled { prompt format: if output starts with double-quote or key, prepend {
        if (!cleaned.startsWith("{") && !cleaned.startsWith("[")) {
            val trimmed = cleaned.trimStart()
            if (trimmed.startsWith("\"") || trimmed.startsWith("clientName")) {
                cleaned = "{\n" + cleaned
            }
        }

        // Find the first '{' or '[' and match its closing partner
        val firstBrace = cleaned.indexOf('{')
        val firstBracket = cleaned.indexOf('[')
        
        val start = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            firstBracket != -1 -> firstBracket
            else -> -1
        }

        if (start != -1) {
            val openChar = cleaned[start]
            val closeChar = if (openChar == '{') '}' else ']'
            var depth = 0
            var matchedEnd = -1
            for (i in start until cleaned.length) {
                val c = cleaned[i]
                if (c == openChar) {
                    depth++
                } else if (c == closeChar) {
                    depth--
                    if (depth == 0) {
                        matchedEnd = i
                        break
                    }
                }
            }
            if (matchedEnd != -1) {
                cleaned = cleaned.substring(start, matchedEnd + 1)
            }
        }
        
        // Remove trailing commas before closing braces/brackets to avoid parsing exceptions
        cleaned = cleaned.replace(",\\s*([}\\u005D])".toRegex(), "$1")

        // Small local models sometimes escape dollar signs as "\$"; JSON only allows a
        // fixed set of escape characters, so normalize this common invoice amount case.
        cleaned = cleaned.replace("\\$", "$")
        
        return cleaned
    }
}
