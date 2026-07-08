package com.fordham.toolbelt.domain.model

data class VoiceInvoiceEvidence(
    val normalizedTranscript: String,
    val clientNameCandidate: String = "",
    val moneyAmounts: List<Double> = emptyList(),
    val percentages: List<Double> = emptyList(),
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val zipCodes: List<String> = emptyList(),
    val streetAddressCandidates: List<String> = emptyList(),
    val measurements: List<String> = emptyList()
)
