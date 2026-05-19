package com.fordham.toolbelt.util

/**
 * A simple but effective runtime string de-obfuscator to thwart static analysis.
 * Strings are XOR-ed with an internal key.
 */
object StringObfuscator {
    private const val INTERNAL_KEY = "F0RDH4M_T00LB3LT_PR0T0C0L"

    /**
     * Decodes an XOR-obfuscated Base64 string.
     */
    fun decode(obfuscated: String): String {
        return try {
            val decodedBytes = decodeBase64(obfuscated)
            val keyBytes = INTERNAL_KEY.encodeToByteArray()
            val result = ByteArray(decodedBytes.size)
            for (i in decodedBytes.indices) {
                result[i] = (decodedBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            result.decodeToString()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Helper to obfuscate strings during development.
     */
    fun obfuscate(input: String): String {
        val keyBytes = INTERNAL_KEY.encodeToByteArray()
        val inputBytes = input.encodeToByteArray()
        val result = ByteArray(inputBytes.size)
        for (i in inputBytes.indices) {
            result[i] = (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return encodeBase64(result)
    }
}
