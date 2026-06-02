package com.fordham.toolbelt.util

import kotlin.math.max
import kotlin.math.min

object StringFuzzyMatcher {

    /**
     * Calculates the Levenshtein distance between two strings.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val str1 = s1.lowercase().trim()
        val str2 = s2.lowercase().trim()
        if (str1 == str2) return 0
        if (str1.isEmpty()) return str2.length
        if (str2.isEmpty()) return str1.length

        val dp = IntArray(str2.length + 1) { it }
        for (i in 1..str1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..str2.length) {
                val temp = dp[j]
                if (str1[i - 1] == str2[j - 1]) {
                    dp[j] = prev
                } else {
                    dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + 1)
                }
                prev = temp
            }
        }
        return dp[str2.length]
    }

    /**
     * Returns a similarity score between 0.0 (completely different) and 1.0 (exact match).
     */
    fun similarity(s1: String, s2: String): Double {
        val maxLen = max(s1.trim().length, s2.trim().length)
        if (maxLen == 0) return 1.0
        val dist = levenshteinDistance(s1, s2)
        return (maxLen - dist).toDouble() / maxLen.toDouble()
    }
}
