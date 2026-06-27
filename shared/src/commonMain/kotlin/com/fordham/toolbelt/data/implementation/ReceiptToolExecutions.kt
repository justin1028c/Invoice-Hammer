package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.ProcessReceiptRequest
import com.fordham.toolbelt.domain.usecase.ProcessReceiptUseCase
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong

class ReceiptToolExecutions(
    private val clientToolExecutions: ClientToolExecutions,
    private val receiptRepository: ReceiptRepository,
    private val draftRepository: DraftRepository,
    private val supplierRepository: SupplierRepository,
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase,
    private val processReceiptUseCase: ProcessReceiptUseCase
) {
    suspend fun executeGetUnbilledReceipts(arguments: GetUnbilledReceiptsArgs): ToolExecutionResult {
        val client = clientToolExecutions.resolveClient(arguments.clientId)
            ?: return ToolExecutionResult.Failure(ToolName.GetUnbilledReceipts, FailureMessage("Client not found."))
        return when (val receipts = receiptRepository.getItemsByClient(client.name.value).first()) {
            is ReceiptListOutcome.Failure -> ToolExecutionResult.Failure(
                ToolName.GetUnbilledReceipts,
                receipts.error
            )
            is ReceiptListOutcome.Success -> ToolExecutionResult.UnbilledReceiptsFound(
                clientId = arguments.clientId,
                receipts = receipts.receipts.filterNot { it.isBilled }.map { receipt ->
                    UnbilledReceiptSummary(
                        receiptId = receipt.id,
                        supplierName = NaturalLanguage(receipt.supplierName.ifBlank { "General" }),
                        amount = CurrencyAmountCents((receipt.totalPrice * 100.0).roundToLong())
                    )
                },
                uiEffects = listOf(
                    AgentUiEffect.NavigateToTab(AppTab.Receipts),
                    AgentUiEffect.SelectClient(client.id)
                )
            )
        }
    }

    suspend fun executeScanLastReceipt(arguments: ScanLastReceiptArgs): ToolExecutionResult {
        val bytes = ForemanRuntimeBinding.current().pendingReceiptImageBytes
            ?: return ToolExecutionResult.Failure(
                ToolName.ScanLastReceipt,
                FailureMessage("No receipt photo ready. Open Receipts and capture an image first.")
            )
        if (!hasSubscriptionFeature(SubscriptionFeature.ReceiptOcr)) {
            return ToolExecutionResult.Failure(
                ToolName.ScanLastReceipt,
                FailureMessage("Pro subscription required for receipt scan.")
            )
        }
        val result = processReceiptUseCase(
            ProcessReceiptRequest(
                imageBytes = ReceiptImagePayload(bytes),
                clientName = arguments.clientName
            )
        )
        return when (result) {
            is ProcessReceiptOutcome.Success -> {
                val draft = draftRepository.getDraft().first()
                if (draft.clientName.isNotBlank()) {
                    val lineItems = result.items.map { item ->
                        LineItem(
                            description = ItemsSummary(if (item.supplierName.isNotBlank()) "${item.description} (${item.supplierName})" else item.description),
                            amount = MoneyAmount(item.totalPrice),
                            category = "Materials",
                            quantity = item.quantity,
                            unitPrice = item.unitPrice?.let { MoneyAmount(it) }
                        )
                    }
                    draftRepository.saveDraft(
                        draft.copy(
                            lineItems = draft.lineItems + lineItems,
                            linkedReceiptIds = draft.linkedReceiptIds + result.items.map { it.id.value }
                        )
                    )
                    result.items.forEach { item ->
                        receiptRepository.updateItem(
                            item.copy(
                                isBilled = true,
                                linkedInvoiceId = InvoiceId("current_draft")
                            )
                        )
                    }
                    ToolExecutionResult.ReceiptScanned(
                        itemCount = result.items.size,
                        uiEffects = listOf(
                            AgentUiEffect.NavigateToTab(AppTab.NewInvoice)
                        )
                    )
                } else {
                    ToolExecutionResult.ReceiptScanned(
                        itemCount = result.items.size,
                        uiEffects = listOf(AgentUiEffect.NavigateToTab(AppTab.Receipts))
                    )
                }
            }
            is ProcessReceiptOutcome.PremiumRequired -> ToolExecutionResult.Failure(
                ToolName.ScanLastReceipt,
                FailureMessage("Pro subscription required for receipt scan.")
            )
            is ProcessReceiptOutcome.Failure -> ToolExecutionResult.Failure(ToolName.ScanLastReceipt, result.error)
        }
    }

    suspend fun executeOpenSupplier(arguments: OpenSupplierArgs): ToolExecutionResult {
        val suppliers = when (val outcome = supplierRepository.getVisibleSuppliers().first()) {
            is SupplierListOutcome.Success -> outcome.suppliers
            is SupplierListOutcome.Failure -> return ToolExecutionResult.Failure(
                ToolName.OpenSupplier,
                FailureMessage("Failed to load suppliers list: ${outcome.error.value}")
            )
        }

        val matchedSupplier = when {
            arguments.supplierId != null -> {
                suppliers.firstOrNull { it.id == arguments.supplierId }
            }
            arguments.supplierName != null -> {
                val queryName = arguments.supplierName.value.trim()
                suppliers.firstOrNull { it.name.equals(queryName, ignoreCase = true) }
                    ?: suppliers.firstOrNull { it.name.contains(queryName, ignoreCase = true) }
            }
            else -> null
        } ?: return ToolExecutionResult.Failure(
            ToolName.OpenSupplier,
            FailureMessage("Supplier not found.")
        )

        return ToolExecutionResult.OpenSupplierCompleted(
            supplierId = matchedSupplier.id.value,
            name = matchedSupplier.name,
            packageName = matchedSupplier.packageName,
            webUrl = matchedSupplier.webUrl,
            uiEffects = listOf(
                AgentUiEffect.NavigateToTab(AppTab.Suppliers),
                AgentUiEffect.OpenSupplierStore(
                    supplierId = matchedSupplier.id.value,
                    name = matchedSupplier.name,
                    packageName = matchedSupplier.packageName,
                    webUrl = matchedSupplier.webUrl
                )
            )
        )
    }
}
