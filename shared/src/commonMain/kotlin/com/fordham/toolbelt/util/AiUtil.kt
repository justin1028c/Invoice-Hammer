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

        // Find the first '{' and last '}' to isolate the JSON object
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        
        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1)
        }
        
        return cleaned
    }
}
