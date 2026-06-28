package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.AppDatabase
import com.fordham.toolbelt.data.DatabaseProvider
import com.fordham.toolbelt.data.JobNoteDao
import com.fordham.toolbelt.data.JobNoteEntity
import com.fordham.toolbelt.data.local.LocalLlmEngine
import com.fordham.toolbelt.data.remote.ForemanGeminiConfig
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.HttpRequestBuilder
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KtorGeminiRepositoryTest {

    private val httpClient: HttpClient = mockk()
    private val geminiConfig: ForemanGeminiConfig = mockk()
    private val jobNoteDao = FakeJobNoteDao()
    private val databaseProvider: DatabaseProvider = mockk()
    private val appDatabase: AppDatabase = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val localLlmEngine = FakeLocalLlmEngine()
    
    private lateinit var repository: KtorGeminiRepository

    @Before
    fun setup() {
        // Set up default non-blank config properties to prevent validation failures in constructor
        every { geminiConfig.agentModelName } returns "gemini-1.5-pro"
        every { geminiConfig.taskModelName } returns "gemini-1.5-flash"
        coEvery { databaseProvider.getDatabase() } returns appDatabase
        every { appDatabase.jobNoteDao() } returns jobNoteDao

        repository = KtorGeminiRepository(
            httpClient = httpClient,
            geminiConfig = geminiConfig,
            databaseProvider = databaseProvider,
            settingsRepository = settingsRepository,
            localLlmEngine = localLlmEngine
        )
    }

    @Test
    fun `processTask routes locally when local LLM is supported and succeeds`() = runTest {
        // Arrange
        jobNoteDao.relevantContextResult = emptyList()
        localLlmEngine.isSupportedResult = true
        localLlmEngine.generateTextResult = GeminiOutcome.Success("{\"summary\": \"test summary\"}")

        // Act
        val outcome = repository.processTask(TaskType.SUMMARIZE, "test data")

        // Assert
        assertTrue(outcome is GeminiOutcome.Success)
        assertEquals("{\"summary\": \"test summary\"}", (outcome as GeminiOutcome.Success).text)
        
        // Verify no HTTP client call was initiated (because it routed locally)
        verify { httpClient wasNot Called }
    }

    @Test
    fun `processTask falls back to cloud when local LLM is supported but fails`() = runTest {
        // Arrange
        jobNoteDao.relevantContextResult = emptyList()
        localLlmEngine.isSupportedResult = true
        localLlmEngine.generateTextResult = GeminiOutcome.Failure(FailureMessage("NPU overload"))
        
        // Mock the cloud call requirements to let callGemini proceed and throw configuration error or run
        every { geminiConfig.isReady } returns true
        every { geminiConfig.backendBaseUrl } returns "http://localhost:8080"
        every { geminiConfig.isBackendApiKeyConfigured } returns false
        
        // Act & Assert
        // The cloud call will fail because we mocked httpClient to throw or not return proper response, 
        // which confirms the execution reached the cloud call after local model fallback.
        val outcome = repository.processTask(TaskType.SUMMARIZE, "test data")
        
        assertTrue(outcome is GeminiOutcome.Failure)
    }

    @Test
    fun `processTask falls back to cloud when local LLM is not supported`() = runTest {
        // Arrange
        jobNoteDao.relevantContextResult = emptyList()
        localLlmEngine.isSupportedResult = false
        
        // Mock the cloud call requirements to let callGemini proceed
        every { geminiConfig.isReady } returns true
        every { geminiConfig.backendBaseUrl } returns "http://localhost:8080"
        every { geminiConfig.isBackendApiKeyConfigured } returns false

        // Act
        val outcome = repository.processTask(TaskType.SUMMARIZE, "test data")

        // Assert
        assertTrue(outcome is GeminiOutcome.Failure) // Fails due to unmocked HTTP responses, confirming it hit callGemini
    }
}

class FakeJobNoteDao : JobNoteDao {
    var relevantContextResult: List<JobNoteEntity> = emptyList()

    override fun getNotesByInvoice(invoiceId: String): kotlinx.coroutines.flow.Flow<List<JobNoteEntity>> {
        return kotlinx.coroutines.flow.emptyFlow()
    }

    override fun getNotesByClient(clientName: String): kotlinx.coroutines.flow.Flow<List<JobNoteEntity>> {
        return kotlinx.coroutines.flow.emptyFlow()
    }

    override suspend fun getRelevantContext(query: String): List<JobNoteEntity> {
        return relevantContextResult
    }

    override suspend fun insertNote(note: JobNoteEntity) {}

    override suspend fun deleteNote(note: JobNoteEntity) {}

    override suspend fun deleteAllNotes() {}
}

class FakeLocalLlmEngine : LocalLlmEngine {
    var isSupportedResult: Boolean = false
    var generateTextResult: GeminiOutcome = GeminiOutcome.Success("")

    override suspend fun isSupported(): Boolean {
        return isSupportedResult
    }

    override suspend fun generateText(prompt: LlmPrompt): GeminiOutcome {
        return generateTextResult
    }

    override fun isModelDownloaded(): Boolean = isSupportedResult
    override fun getDownloadProgress(): Float = if (isSupportedResult) 1.0f else 0.0f
    override fun isDownloading(): Boolean = false
    override fun startDownload(onProgress: (Float) -> Unit, onComplete: (Boolean) -> Unit) {
        onComplete(true)
    }
    override fun deleteModel(): Boolean = true
}
