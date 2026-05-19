package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.DraftInvoice
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.ClientSearchHit
import com.fordham.toolbelt.domain.model.agent.CreateDraftInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.CurrencyAmountCents
import com.fordham.toolbelt.domain.model.agent.DeleteInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.FunctionParameter
import com.fordham.toolbelt.domain.model.agent.GetUnbilledReceiptsArgs
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.ParameterName
import com.fordham.toolbelt.domain.model.agent.ParameterType
import com.fordham.toolbelt.domain.model.agent.SearchClientsArgs
import com.fordham.toolbelt.domain.model.agent.SendInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.ToolArguments
import com.fordham.toolbelt.domain.model.agent.ToolDescription
import com.fordham.toolbelt.domain.model.agent.ToolExecutionResult
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.model.agent.UnbilledReceiptSummary
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.repository.DraftRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.ToolRegistry
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong

class RepositoryToolRegistry(
    private val clientRepository: ClientRepository,
    private val receiptRepository: ReceiptRepository,
    private val draftRepository: DraftRepository,
    private val invoiceRepository: InvoiceRepository
) : ToolRegistry {
    override fun availableFunctions(): List<AgentFunction> {
        return listOf(
            AgentFunction(
                toolName = ToolName.SearchClients,
                description = ToolDescription("Search the client directory by name, address, phone, or notes."),
                parameters = listOf(FunctionParameter(ParameterName("query"), ParameterType.Text, required = true))
            ),
            AgentFunction(
                toolName = ToolName.GetUnbilledReceipts,
                description = ToolDescription("Find unbilled receipts for a resolved client."),
                parameters = listOf(FunctionParameter(ParameterName("clientId"), ParameterType.Text, required = true))
            ),
            AgentFunction(
                toolName = ToolName.CreateDraftInvoice,
                description = ToolDescription("Start a draft invoice for a resolved client."),
                parameters = listOf(FunctionParameter(ParameterName("clientId"), ParameterType.Text, required = true))
            ),
            AgentFunction(
                toolName = ToolName.SendInvoiceForApproval,
                description = ToolDescription("Queue an invoice send action for explicit user approval."),
                parameters = listOf(FunctionParameter(ParameterName("invoiceId"), ParameterType.Text, required = true))
            ),
            AgentFunction(
                toolName = ToolName.DeleteInvoiceForApproval,
                description = ToolDescription("Queue an invoice delete action for explicit user approval."),
                parameters = listOf(FunctionParameter(ParameterName("invoiceId"), ParameterType.Text, required = true))
            )
        )
    }

    override suspend fun execute(
        toolName: ToolName,
        arguments: ToolArguments
    ): ToolExecutionResult {
        return when (arguments) {
            is SearchClientsArgs -> executeSearchClients(arguments)
            is GetUnbilledReceiptsArgs -> executeGetUnbilledReceipts(arguments)
            is CreateDraftInvoiceArgs -> executeCreateDraftInvoice(arguments)
            is SendInvoiceApprovalArgs -> executeSendInvoiceApproval(arguments)
            is DeleteInvoiceApprovalArgs -> executeDeleteInvoiceApproval(arguments)
        }
    }

    private suspend fun executeSearchClients(arguments: SearchClientsArgs): ToolExecutionResult {
        val clients = clientRepository.searchClients(arguments.query.value)
        return ToolExecutionResult.ClientSearchCompleted(
            clients = clients.map { client ->
                ClientSearchHit(
                    clientId = client.id,
                    displayName = NaturalLanguage(client.name)
                )
            }
        )
    }

    private suspend fun executeGetUnbilledReceipts(arguments: GetUnbilledReceiptsArgs): ToolExecutionResult {
        val client = clientRepository.getAllClients().first().let { outcome ->
            when (outcome) {
                is com.fordham.toolbelt.domain.model.ClientListOutcome.Success ->
                    outcome.clients.firstOrNull { it.id == arguments.clientId }
                is com.fordham.toolbelt.domain.model.ClientListOutcome.Failure -> null
            }
        } ?: return ToolExecutionResult.Failure(
            toolName = ToolName.GetUnbilledReceipts,
            error = FailureMessage("Client was not found.")
        )

        return when (val receipts = receiptRepository.getItemsByClient(client.name).first()) {
            is ReceiptListOutcome.Failure -> ToolExecutionResult.Failure(
                toolName = ToolName.GetUnbilledReceipts,
                error = receipts.error
            )
            is ReceiptListOutcome.Success -> ToolExecutionResult.UnbilledReceiptsFound(
                clientId = arguments.clientId,
                receipts = receipts.receipts
                    .filterNot { it.isBilled }
                    .map { receipt ->
                        UnbilledReceiptSummary(
                            receiptId = receipt.id,
                            supplierName = NaturalLanguage(receipt.supplierName.ifBlank { "General" }),
                            amount = CurrencyAmountCents((receipt.totalPrice * 100.0).roundToLong())
                        )
                    }
            )
        }
    }

    private suspend fun executeCreateDraftInvoice(arguments: CreateDraftInvoiceArgs): ToolExecutionResult {
        val client = clientRepository.getAllClients().first().let { outcome ->
            when (outcome) {
                is com.fordham.toolbelt.domain.model.ClientListOutcome.Success ->
                    outcome.clients.firstOrNull { it.id == arguments.clientId }
                is com.fordham.toolbelt.domain.model.ClientListOutcome.Failure -> null
            }
        } ?: return ToolExecutionResult.Failure(
            toolName = ToolName.CreateDraftInvoice,
            error = FailureMessage("Client was not found.")
        )

        val invoiceId = com.fordham.toolbelt.domain.model.InvoiceId(randomUUID())
        draftRepository.saveDraft(
            DraftInvoice(
                clientName = client.name,
                clientAddress = client.address,
                saveToClientDirectory = false
            )
        )
        return ToolExecutionResult.DraftInvoiceCreated(
            invoiceId = invoiceId,
            clientId = client.id
        )
    }

    private suspend fun executeSendInvoiceApproval(arguments: SendInvoiceApprovalArgs): ToolExecutionResult {
        val invoice = invoiceRepository.getInvoiceById(arguments.invoiceId)
            ?: return ToolExecutionResult.Failure(
                toolName = ToolName.SendInvoiceForApproval,
                error = FailureMessage("Invoice was not found.")
            )
        return ToolExecutionResult.InvoiceApprovalQueued(invoice.id)
    }

    private suspend fun executeDeleteInvoiceApproval(arguments: DeleteInvoiceApprovalArgs): ToolExecutionResult {
        val invoice = invoiceRepository.getInvoiceById(arguments.invoiceId)
            ?: return ToolExecutionResult.Failure(
                toolName = ToolName.DeleteInvoiceForApproval,
                error = FailureMessage("Invoice was not found.")
            )
        return when (val outcome = invoiceRepository.deleteInvoice(invoice)) {
            is InvoiceOutcome.Success -> ToolExecutionResult.InvoiceDeletionQueued(invoice.id)
            is InvoiceOutcome.Failure -> ToolExecutionResult.Failure(
                toolName = ToolName.DeleteInvoiceForApproval,
                error = outcome.error
            )
        }
    }
}
