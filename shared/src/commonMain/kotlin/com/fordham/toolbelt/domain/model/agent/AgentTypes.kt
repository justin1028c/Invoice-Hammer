package com.fordham.toolbelt.domain.model.agent

import kotlin.jvm.JvmInline

@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SessionId cannot be blank." }
    }
}

@JvmInline
value class NaturalLanguage(val value: String)

@JvmInline
value class ToolCallId(val value: String) {
    init {
        require(value.isNotBlank()) { "ToolCallId cannot be blank." }
    }
}

@JvmInline
value class ParameterName(val value: String) {
    init {
        require(value.matches(IDENTIFIER_PATTERN)) {
            "ParameterName must be a stable identifier."
        }
    }

    private companion object {
        val IDENTIFIER_PATTERN = Regex("[A-Za-z][A-Za-z0-9_]*")
    }
}

@JvmInline
value class ToolDescription(val value: String) {
    init {
        require(value.isNotBlank()) { "ToolDescription cannot be blank." }
    }
}

@JvmInline
value class TimestampMillis(val value: Long) {
    init {
        require(value >= 0L) { "TimestampMillis cannot be negative." }
    }
}

@JvmInline
value class CurrencyAmountCents(val value: Long) {
    init {
        require(value >= 0L) { "CurrencyAmountCents cannot be negative." }
    }
}
