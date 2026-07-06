package com.fordham.toolbelt

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fordham.toolbelt.data.local.AndroidLocalLlmEngine
import com.fordham.toolbelt.data.implementation.LocalPromptProvider
import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.model.LlmPrompt
import com.fordham.toolbelt.util.SecretProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStreamReader

@RunWith(AndroidJUnit4::class)
class GemmaRegressionTest {

    private lateinit var context: Context
    private lateinit var localLlmEngine: AndroidLocalLlmEngine
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        val targetFile = java.io.File(context.filesDir, MODEL_FILE_NAME)
        println("TEST_DEBUG: targetFile path = ${targetFile.absolutePath}")
        println("TEST_DEBUG: targetFile exists = ${targetFile.exists()}")
        if (targetFile.exists()) {
            println("TEST_DEBUG: targetFile length = ${targetFile.length()}")
        }
        
        val localTmpSource = java.io.File("/data/local/tmp/$MODEL_FILE_NAME")
        println("TEST_DEBUG: localTmpSource path = ${localTmpSource.absolutePath}")
        println("TEST_DEBUG: localTmpSource exists = ${localTmpSource.exists()}")
        if (localTmpSource.exists()) {
            println("TEST_DEBUG: localTmpSource length = ${localTmpSource.length()}")
        }

        if (!targetFile.exists() || targetFile.length() < 100 * 1024 * 1024) {
            if (localTmpSource.exists() && localTmpSource.length() >= 100 * 1024 * 1024) {
                println("TEST_DEBUG: Copying from localTmpSource to filesDir in pure Kotlin...")
                targetFile.parentFile?.mkdirs()
                localTmpSource.copyTo(targetFile, overwrite = true)
                println("TEST_DEBUG: Copy finished. New size = ${targetFile.length()}")
            } else {
                println("TEST_DEBUG: Source file not found in /data/local/tmp. Falling back to shell copy...")
                val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
                uiAutomation.executeShellCommand("mkdir -p /data/data/com.fordham.toolbelt/files").close()
                val pfd = uiAutomation.executeShellCommand(
                    "cp /data/local/tmp/$MODEL_FILE_NAME /data/data/com.fordham.toolbelt/files/$MODEL_FILE_NAME"
                )
                java.io.FileInputStream(pfd.fileDescriptor).use { it.readBytes() }
                pfd.close()
                uiAutomation.executeShellCommand("chmod 660 /data/data/com.fordham.toolbelt/files/$MODEL_FILE_NAME").close()
            }
        }

        localLlmEngine = AndroidLocalLlmEngine(
            context = context,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
            ioDispatcher = Dispatchers.IO,
            secretProvider = object : SecretProvider {
                override fun getGoogleClientId(): String = ""
                override fun getSecret(key: String): String = ""
            }
        )
    }

    @Test
    fun runPromptRegressionSuite() = runBlocking {
        // Verify Gemma model is present on the device
        assertTrue(
            "Gemma 3n E2B model file is not present on device. Please launch the app, go to Settings, and download the local Gemma model first.",
            localLlmEngine.isSupported()
        )

        // 1. Load test cases
        val casesJson = readAssetFile("tests/voice_invoice_cases.json")
        val rootArray = json.parseToJsonElement(casesJson).jsonArray

        var passed = 0
        var total = 0

        println("=== STARTING GEMMA REGRESSION TESTS ===")

        for (caseElement in rootArray) {
            total++
            val case = caseElement.jsonObject
            val name = case["name"]?.jsonPrimitive?.content.orEmpty()
            val transcript = case["transcript"]?.jsonPrimitive?.content.orEmpty()
            val expected = case["expected"]?.jsonObject ?: throw IllegalArgumentException("Missing expected block")

            println("\n[$total/${rootArray.size}] Running Test Case: \"$name\"")
            println("Transcript: \"$transcript\"")

            val promptText = LocalPromptProvider.getVoiceInvoicePrompt(transcript, "2026-06-30")

            val startTime = System.currentTimeMillis()
            val outcome = localLlmEngine.generateText(LlmPrompt(promptText))
            val duration = System.currentTimeMillis() - startTime

            println("Inference time: ${duration}ms")

            when (outcome) {
                is GeminiOutcome.Success -> {
                    val rawResult = outcome.text
                    println("Raw AI Output:\n$rawResult")

                    // Validate output JSON has expected fields
                    try {
                        val cleanedJson = com.fordham.toolbelt.util.AiUtil.cleanJson(rawResult)
                        val resultJson = json.parseToJsonElement(cleanedJson).jsonObject
                        
                        if (expected.containsKey("clientName")) {
                            val expectedClient = expected["clientName"]?.jsonPrimitive?.content.orEmpty()
                            val actualClient = resultJson["clientName"]?.jsonPrimitive?.content.orEmpty()
                            assertEquals("Client name mismatch in case '$name'", expectedClient, actualClient)
                        }

                        if (expected.containsKey("taxRatePercent")) {
                            val expectedTax = expected["taxRatePercent"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 7.0
                            val actualTax = resultJson["taxRatePercent"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 7.0
                            assertEquals("Tax rate mismatch in case '$name'", expectedTax, actualTax, 0.01)
                        }

                        if (expected.containsKey("depositAmount")) {
                            val expectedDeposit = expected["depositAmount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                            val actualDeposit = resultJson["depositAmount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                            assertEquals("Deposit amount mismatch in case '$name'", expectedDeposit, actualDeposit, 0.01)
                        }

                        println("✅ PASS: \"$name\"")
                        passed++
                    } catch (e: Throwable) {
                        println("❌ FAIL: \"$name\" - Error: ${e.message}")
                        throw e
                    }
                }
                is GeminiOutcome.Failure -> {
                    println("❌ FAIL: \"$name\" - Inference failure: ${outcome.error.value}")
                    fail("Inference failed for case '$name'")
                }
            }
        }

        println("\n=== REGRESSION SUITE COMPLETED: $passed/$total PASSED ===")
        assertEquals("Some regression tests failed", total, passed)
    }

    private fun readAssetFile(path: String): String {
        val assetManager = InstrumentationRegistry.getInstrumentation().context.assets
        assetManager.open(path).use { inputStream ->
            InputStreamReader(inputStream).use { reader ->
                return reader.readText()
            }
        }
    }

    private companion object {
        const val MODEL_FILE_NAME = "gemma-3n-E2B-it-int4.litertlm"
    }
}
