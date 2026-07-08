package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.AiInvoiceResult
import com.fordham.toolbelt.domain.model.ItemsSummary
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.VoiceInvoiceEvidence
import kotlin.math.abs

class ValidateVoiceInvoiceResultUseCase {
    operator fun invoke(input: AiInvoiceResult, evidence: VoiceInvoiceEvidence? = null): AiInvoiceResult {
        val issues = input.validationIssues.toMutableList()
        val normalizedItems = input.items.mapNotNull { item ->
            val quantity = item.quantity?.takeIf { it > 0.0 } ?: 1.0
            val normalizedUnitPrice = item.unitPrice?.value?.coerceAtLeast(0.0)
                ?: (item.amount.value.coerceAtLeast(0.0) / quantity)
            val normalizedAmount = when {
                item.unitPrice != null -> normalizedUnitPrice * quantity
                else -> item.amount.value.coerceAtLeast(0.0)
            }
            if (normalizedAmount <= 0.0) {
                if ("ZERO_AMOUNT" !in issues) issues += "ZERO_AMOUNT"
                return@mapNotNull null
            }
            if (item.unitPrice != null && abs(normalizedAmount - item.amount.value) > 0.01) {
                if ("MATH_MISMATCH" !in issues) issues += "MATH_MISMATCH"
            }
            LineItem(
                description = ItemsSummary(item.description.value.trim()),
                amount = MoneyAmount(normalizedAmount),
                category = item.category.ifBlank { "Service" },
                quantity = quantity,
                unitPrice = normalizedUnitPrice?.let(::MoneyAmount)
            )
        }

        val splitClient = splitAddressFromClientName(input.clientName.trim(), input.clientAddress.trim())
        val normalizedClientName = chooseClientName(
            parsedName = splitClient.name,
            evidenceName = evidence?.clientNameCandidate.orEmpty()
        )
        val normalizedClientAddress = splitClient.address
        splitClient.issue?.let { issue ->
            if (issue !in issues) issues += issue
        }
        if (normalizedClientName.isBlank()) {
            if ("MISSING_CLIENT_NAME" !in issues) issues += "MISSING_CLIENT_NAME"
        } else {
            issues.removeAll { it == "MISSING_CLIENT_NAME" }
        }
        if (normalizedClientAddress.isBlank()) {
            if ("MISSING_CLIENT_ADDRESS" !in issues) issues += "MISSING_CLIENT_ADDRESS"
        } else {
            issues.removeAll { it == "MISSING_CLIENT_ADDRESS" }
        }
        if (normalizedItems.isEmpty() && "NO_VALID_LINE_ITEMS" !in issues) issues += "NO_VALID_LINE_ITEMS"

        val evidenceIssues = evidence?.let { validateAgainstEvidence(normalizedItems, input, it) }.orEmpty()
        evidenceIssues.forEach { issue ->
            if (issue !in issues) issues += issue
        }

        return input.copy(
            clientName = normalizedClientName,
            clientAddress = normalizedClientAddress,
            items = normalizedItems,
            depositAmount = input.depositAmount.coerceAtLeast(0.0),
            taxRatePercent = input.taxRatePercent.coerceIn(0.0, 100.0),
            discountPercent = input.discountPercent.coerceIn(0.0, 100.0),
            confidenceScore = input.confidenceScore.coerceIn(0.0, 1.0),
            notes = input.notes.trim(),
            userSummary = input.userSummary.trim(),
            validationIssues = issues.distinct()
        )
    }

    private fun splitAddressFromClientName(name: String, address: String): ClientFields {
        val match = addressInNamePattern.find(name) ?: return ClientFields(name, address, null)
        val cleanName = name.substring(0, match.range.first).trim().trim(',', '-', ':')
        val extractedAddress = name.substring(match.range.first).trim().trim(',', '-', ':')
        if (cleanName.isBlank() || extractedAddress.isBlank()) return ClientFields(name, address, null)

        val cleanAddress = when {
            address.isBlank() -> extractedAddress
            addressesLookRelated(address, extractedAddress) -> address
            else -> extractedAddress
        }
        return ClientFields(cleanName, cleanAddress, "CLIENT_NAME_CONTAINED_ADDRESS")
    }

    private fun chooseClientName(parsedName: String, evidenceName: String): String {
        val parsed = parsedName.trim()
        val evidence = evidenceName.trim()
        if (evidence.isBlank()) return parsed
        if (parsed.isBlank()) return evidence

        val parsedLower = parsed.lowercase()
        val evidenceLower = evidence.lowercase()
        if (parsedLower == evidenceLower) return parsed
        if (parsedLower.startsWith("$evidenceLower ")) {
            val remainder = parsedLower.removePrefix(evidenceLower).trim()
            if (remainder in ClientNameContaminationWords || ClientNameContaminationPattern.containsMatchIn(remainder)) {
                return evidence
            }
        }
        return parsed
    }

    private fun addressesLookRelated(left: String, right: String): Boolean {
        val leftTokens = addressTokens(left)
        val rightTokens = addressTokens(right)
        val sharedTokens = leftTokens.intersect(rightTokens)
        if (sharedTokens.size >= 2) return true

        val leftNumber = streetNumberPattern.find(left)?.value
        val rightNumber = streetNumberPattern.find(right)?.value
        return leftNumber != null && leftNumber == rightNumber && sharedTokens.isNotEmpty()
    }

    private fun addressTokens(value: String): Set<String> {
        return value.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it.any(Char::isLetter) && it !in addressStopWords }
            .toSet()
    }

    private fun validateAgainstEvidence(
        items: List<LineItem>,
        input: AiInvoiceResult,
        evidence: VoiceInvoiceEvidence
    ): List<String> {
        val issues = mutableListOf<String>()
        val accountedAmounts = buildList {
            items.forEach { item ->
                add(item.amount.value)
                item.unitPrice?.value?.let(::add)
            }
            if (input.depositAmount > 0.0) add(input.depositAmount)
            if (input.laborRate != null) add(input.laborRate)
        }

        val unmatchedMoney = evidence.moneyAmounts.filterNot { spoken ->
            accountedAmounts.any { accounted -> abs(spoken - accounted) <= MONEY_EPSILON }
        }
        if (unmatchedMoney.isNotEmpty()) issues += "UNMATCHED_MONEY_AMOUNT"

        val spokenTaxRate = evidence.percentages.firstOrNull()
        if (spokenTaxRate != null && abs(input.taxRatePercent - spokenTaxRate) > MONEY_EPSILON) {
            issues += "TAX_RATE_MISMATCH"
        }

        if (evidence.phoneNumbers.isNotEmpty()) issues += "CONTACT_PHONE_DETECTED"
        if (evidence.emails.isNotEmpty()) issues += "CONTACT_EMAIL_DETECTED"

        return issues
    }

    private companion object {
        const val MONEY_EPSILON = 0.01
        val streetNumberPattern = Regex("""\b\d{2,6}\b""")
        val addressInNamePattern = Regex(
            """\b\d{2,6}\s+[A-Za-z0-9.'-]+(?:\s+[A-Za-z0-9.'-]+){0,7}\s+(?:street|st|avenue|ave|road|rd|drive|dr|court|ct|lane|ln|way|place|pl|circle|cir|boulevard|blvd|terrace|ter|trail|trl|parkway|pkwy)\b.*""",
            RegexOption.IGNORE_CASE
        )
        val addressStopWords = setOf(
            "street", "avenue", "road", "drive", "court", "lane", "way", "place", "circle",
            "boulevard", "terrace", "trail", "parkway", "st", "ave", "rd", "dr", "ct",
            "ln", "pl", "cir", "blvd", "ter", "trl", "pkwy", "georgia"
        )
        val ClientNameContaminationWords = setOf("in", "at", "on", "for", "client", "customer")
        val ClientNameContaminationPattern = Regex(
            """(?i)\b(?:street|st|road|rd|avenue|ave|court|ct|drive|dr|lane|ln|boulevard|blvd|way|place|pl|circle|cir|georgia|ga|\d{3,})\b"""
        )
    }

    private data class ClientFields(
        val name: String,
        val address: String,
        val issue: String?
    )
}
