from pathlib import Path

reg_path = Path("shared/src/commonMain/kotlin/com/fordham/toolbelt/data/implementation/RepositoryToolRegistry.kt")
text = reg_path.read_text(encoding="utf-8")
marker = "    private suspend fun resolveClient"
idx = text.find(marker)
if idx < 0:
    raise SystemExit("marker not found")
head = text[:idx].rstrip()
tail = text[idx:]
tail = tail.replace("    private suspend fun execute", "    suspend fun execute")
exec_file = """package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.ClientListOutcome
import com.fordham.toolbelt.domain.model.DraftInvoice
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.GenerateInvoiceOutcome
import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.model.JobNoteOutcome
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.AddJobNoteUseCase
import com.fordham.toolbelt.domain.usecase.GenerateAndSaveInvoiceUseCase
import com.fordham.toolbelt.domain.usecase.GenerateInvoiceRequest
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong

internal class RepositoryToolRegistryExecutions(
    private val clientRepository: ClientRepository,
    private val receiptRepository: ReceiptRepository,
    private val draftRepository: DraftRepository,
    private val invoiceRepository: InvoiceRepository,
    private val settingsRepository: SettingsRepository,
    private val addJobNoteUseCase: AddJobNoteUseCase,
    private val generateAndSaveInvoiceUseCase: GenerateAndSaveInvoiceUseCase
) {
""" + tail + "\n"
Path("shared/src/commonMain/kotlin/com/fordham/toolbelt/data/implementation/RepositoryToolRegistryExecutions.kt").write_text(
    exec_file, encoding="utf-8"
)
inject = """
    private val executions = RepositoryToolRegistryExecutions(
        clientRepository,
        receiptRepository,
        draftRepository,
        invoiceRepository,
        settingsRepository,
        addJobNoteUseCase,
        generateAndSaveInvoiceUseCase
    )

"""
head = head.replace(
    "    override suspend fun execute(toolName: ToolName, arguments: ToolArguments): ToolExecutionResult {",
    inject + "    override suspend fun execute(toolName: ToolName, arguments: ToolArguments): ToolExecutionResult {",
)
replacements = [
    "executeSearchClients",
    "executeSelectClient",
    "executeGetClientDetails",
    "executeGetUnbilledReceipts",
    "executeOpenTab",
    "executeCreateDraftInvoice",
    "executeUpdateDraftInvoice",
    "executeAddJobNote",
    "executeSaveInvoiceFromDraft",
    "executeSearchInvoiceHistory",
    "executeSendInvoiceEmail",
    "executeSendInvoiceSms",
    "executeDeleteInvoice",
]
for name in replacements:
    head = head.replace(f"-> {name}(", f"-> executions.{name}(")
reg_path.write_text(head + "\n}\n", encoding="utf-8")
print("done", len(head.splitlines()), len(exec_file.splitlines()))
