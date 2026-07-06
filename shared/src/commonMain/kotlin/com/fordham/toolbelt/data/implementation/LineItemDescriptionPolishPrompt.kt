package com.fordham.toolbelt.data.implementation

internal object LineItemDescriptionPolishPrompt {

    /**
     * Builds a single Gemma-format prompt that rewrites [rawDescriptions] into
     * professional contractor language.
     *
     * Output contract: a JSON array of strings, same count and order as input.
     * Example input:  ["Completed punch list repairs caught the bathroom vanity", "Adjusted 2 interior doors"]
     * Example output: ["Caulked bathroom vanity", "Adjusted 2 interior doors"]
     */
    fun build(rawDescriptions: List<String>): String {
        val numbered = rawDescriptions.mapIndexed { i, d -> "${i + 1}. $d" }.joinToString("\n")
        return """
<start_of_turn>user
You are a professional contractor invoice assistant. Rewrite each line item description below into clean, professional contractor language. 

Rules:
- Keep the same meaning and trade category.
- Fix obvious speech-to-text errors (e.g. "caught" → "caulked", "go wash" → "pressure washed").
- Do NOT add, remove, or combine items.
- Do NOT include any dollar amounts, prices, quantities, or numbers that were not in the original.
- Keep quantities that are already present (e.g. "2 doors" stays "2 doors").
- Return ONLY a JSON array of strings, one per input line, in the same order.
- No explanation, no markdown, no extra keys.

Descriptions to polish:
$numbered
<end_of_turn>
<start_of_turn>model
[
        """.trimIndent()
    }
}
