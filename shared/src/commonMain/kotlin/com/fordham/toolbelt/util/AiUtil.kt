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

        // Find the first '{' or '[' and last '}' or ']'
        val firstBrace = cleaned.indexOf('{')
        val firstBracket = cleaned.indexOf('[')
        val lastBrace = cleaned.lastIndexOf('}')
        val lastBracket = cleaned.lastIndexOf(']')

        val start = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            firstBracket != -1 -> firstBracket
            else -> -1
        }

        val end = when {
            lastBrace != -1 && lastBracket != -1 -> maxOf(lastBrace, lastBracket)
            lastBrace != -1 -> lastBrace
            lastBracket != -1 -> lastBracket
            else -> -1
        }

        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1)
        }
        
        return cleaned
    }
}
