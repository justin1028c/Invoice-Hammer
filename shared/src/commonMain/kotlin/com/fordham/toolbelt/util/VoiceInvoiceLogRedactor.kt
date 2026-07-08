package com.fordham.toolbelt.util

object VoiceInvoiceLogRedactor {
    fun transcriptMeta(text: String): String {
        val trimmed = text.trim()
        return "len=${trimmed.length} hash=${stableHash(trimmed)} preview='${preview(trimmed)}'"
    }

    fun preview(text: String, maxChars: Int = 32): String {
        if (text.isBlank()) return ""
        val compact = text
            .replace(EmailPattern, "[email]")
            .replace(PhonePattern, "[phone]")
            .replace(AddressNumberPattern, "[num]")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return if (compact.length <= maxChars) compact else compact.take(maxChars).trimEnd() + "..."
    }

    private fun stableHash(text: String): String {
        var hash = 1125899907L
        text.forEach { char -> hash = 31 * hash + char.code }
        return hash.toULong().toString(16).takeLast(8)
    }

    private val EmailPattern = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val PhonePattern = Regex("""(?x)(?:\+?1[\s.-]?)?(?:\(?\d{3}\)?[\s.-]?)\d{3}[\s.-]?\d{4}""")
    private val AddressNumberPattern = Regex("""\b\d{2,6}\b""")
}
