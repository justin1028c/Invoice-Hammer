package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.agent.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.AddJobNoteUseCase
import com.fordham.toolbelt.domain.usecase.GenerateAndSaveInvoiceUseCase
import com.fordham.toolbelt.domain.usecase.ProcessReceiptUseCase
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import com.fordham.toolbelt.domain.usecase.agent.*

class RepositoryToolRegistry(
    private val clientRepository: ClientRepository,
    private val receiptRepository: ReceiptRepository,
    private val draftRepository: DraftRepository,
    private val invoiceRepository: InvoiceRepository,
    private val settingsRepository: SettingsRepository,
    private val supplierRepository: SupplierRepository,
    private val addJobNoteUseCase: AddJobNoteUseCase,
    private val generateAndSaveInvoiceUseCase: GenerateAndSaveInvoiceUseCase,
    private val processReceiptUseCase: ProcessReceiptUseCase,
    private val hasSubscriptionFeature: HasSubscriptionFeatureUseCase,
    private val getProfitGuardianStatus: GetProfitGuardianStatusUseCase,
    private val detectChangeOrders: DetectChangeOrdersUseCase,
    private val getDailyBriefing: GetDailyBriefingUseCase
) : ToolRegistry {

    override fun availableFunctions(): List<AgentFunction> = listOf(

        fn(ToolName.SearchClients, "Search clients by name, phone, email, or address.", "query"),

        fn(ToolName.OpenSupplier, "Open a specific store app or website by name or ID.", "supplierId", "supplierName"),

        fn(ToolName.SelectClient, "Open a client profile by id or name.", "clientId", "clientName"),

        fn(ToolName.GetClientDetails, "Summarize a client profile.", "clientId"),

        fn(ToolName.GetUnbilledReceipts, "List unbilled receipts for a client.", "clientId"),

        fn(
            ToolName.OpenTab,
            "Switch main tab. tabName MUST be one bottom-bar label: ${AppTab.NAV_LABELS}. " +
                "Use this tool alone for navigation — no search or other tools.",
            "tabName"
        ),

        fn(ToolName.CreateClient, "Create a new client record.", "clientName", "address", "phone", "email"),

        fn(ToolName.CreateDraftInvoice, "Start a new invoice draft for a client.", "clientId"),

        fn(

            ToolName.UpdateDraftInvoice,

            "Fill the New Invoice draft: client, address, tax, deposit, line items (JSON array).",

            "clientName",

            "clientAddress",

            "taxRate",

            "deposit",

            "lineItemsJson",

            "replaceLineItems"

        ),

        fn(ToolName.AddJobNote, "Add a job note to a client.", "clientName", "note"),

        fn(ToolName.SaveInvoiceFromDraft, "Save the current draft as invoice PDF + database record.", "isEstimate"),

        fn(ToolName.SearchInvoiceHistory, "Search invoices by client, id, or summary text.", "query"),

        fn(ToolName.ScanLastReceipt, "OCR the last captured receipt photo into Receipts.", "clientName"),

        fn(

            ToolName.QuickInvoice,

            "One-shot for an existing client: draft, line items, save PDF invoice. " +
                "Prefer flat fields jobDescription + category + totalAmount for a single line; " +
                "use lineItemsJson only for multiple lines or hour/rate math.",

            "clientName",

            "clientAddress",

            "jobDescription",

            "category",

            "totalAmount",

            "lineItemsJson",

            "isEstimate",

            "createClientIfMissing"

        ),

        fn(
            ToolName.QuickClientAndInvoice,
            "One-shot for a brand-new client: create client with address, fill job line + trade category, " +
                "set total, and save PDF invoice in ONE call. Never chain CREATE_CLIENT or UPDATE_DRAFT. " +
                "Prefer flat fields jobDescription + category + totalAmount.",
            "clientName",
            "clientAddress",
            "jobDescription",
            "category",
            "totalAmount",
            "lineItemsJson",
            "isEstimate"
        ),

        fn(ToolName.QuickClientLookup, "Search client and return profile summary.", "query"),

        fn(ToolName.AppendDraftLines, "Add line items to the current draft without replacing existing lines.", "lineItemsJson"),

        fn(ToolName.DuplicateLastInvoice, "Copy the client's most recent invoice into a new draft.", "clientName"),

        fn(
            ToolName.DuplicateAndEdit,
            "Copy the client's last invoice into a draft and append extra line items for editing.",
            "clientName",
            "lineItemsJson"
        ),

        fn(
            ToolName.QuickInvoiceFromUnbilledReceipts,
            "Bill all unbilled receipt expenses for a client into one invoice PDF.",
            "clientName",
            "isEstimate",
            "createClientIfMissing"
        ),

        fn(

            ToolName.QuickSendInvoice,

            "[APPROVAL] Find invoice and queue email or SMS send.",

            "invoiceId",

            "clientName",

            "channel",

            "recipientPhone",

            "recipientEmail",

            "message"

        ),

        fn(ToolName.SendInvoiceEmail, "[APPROVAL] Email an invoice PDF.", "invoiceId", "recipientEmail", "subject", "body"),

        fn(ToolName.SendInvoiceSms, "[APPROVAL] Text an invoice PDF.", "invoiceId", "recipientPhone", "message"),
        fn(ToolName.DeleteInvoiceForApproval, "[APPROVAL] Delete an invoice.", "invoiceId"),
        fn(ToolName.OpenLastInvoice, "Open the PDF of the most recently saved invoice instantly.", "invoiceId"),
        fn(ToolName.GetProfitGuardianStatus, "Check real-time material cost warnings and projected profit anomalies.", "invoiceId"),
        fn(ToolName.DetectChangeOrders, "Analyze job logs against estimates to identify unbilled work.", "invoiceId"),
        fn(ToolName.GetDailyBriefing, "Retrieve active contractor daily summaries, overdue invoices, and recommended recovery actions."),
        fn(ToolName.CreateChangeOrder, "[APPROVAL] Append a recommended change order to an invoice draft.", "invoiceId", "description", "amount")
    )



    private val executions = RepositoryToolRegistryExecutions(
        clientRepository = clientRepository,
        receiptRepository = receiptRepository,
        draftRepository = draftRepository,
        invoiceRepository = invoiceRepository,
        settingsRepository = settingsRepository,
        supplierRepository = supplierRepository,
        addJobNoteUseCase = addJobNoteUseCase,
        generateAndSaveInvoiceUseCase = generateAndSaveInvoiceUseCase,
        processReceiptUseCase = processReceiptUseCase,
        hasSubscriptionFeature = hasSubscriptionFeature,
        getProfitGuardianStatus = getProfitGuardianStatus,
        detectChangeOrders = detectChangeOrders,
        getDailyBriefing = getDailyBriefing
    )



    override suspend fun execute(toolName: ToolName, arguments: ToolArguments): ToolExecutionResult {

        return when (arguments) {

            is SearchClientsArgs -> executions.executeSearchClients(arguments)

            is SelectClientArgs -> executions.executeSelectClient(arguments)

            is GetClientDetailsArgs -> executions.executeGetClientDetails(arguments)

            is GetUnbilledReceiptsArgs -> executions.executeGetUnbilledReceipts(arguments)

            is OpenTabArgs -> executions.executeOpenTab(arguments)

            is CreateClientArgs -> executions.executeCreateClient(arguments)

            is CreateDraftInvoiceArgs -> executions.executeCreateDraftInvoice(arguments)

            is UpdateDraftInvoiceArgs -> executions.executeUpdateDraftInvoice(arguments)

            is AddJobNoteArgs -> executions.executeAddJobNote(arguments)

            is SaveInvoiceFromDraftArgs -> executions.executeSaveInvoiceFromDraft(arguments)

            is SearchInvoiceHistoryArgs -> executions.executeSearchInvoiceHistory(arguments)

            is ScanLastReceiptArgs -> executions.executeScanLastReceipt(arguments)

            is QuickInvoiceArgs -> executions.executeQuickInvoice(arguments)

            is QuickClientAndInvoiceArgs -> executions.executeQuickClientAndInvoice(arguments)

            is QuickClientLookupArgs -> executions.executeQuickClientLookup(arguments)

            is AppendDraftLinesArgs -> executions.executeAppendDraftLines(arguments)

            is DuplicateLastInvoiceArgs -> executions.executeDuplicateLastInvoice(arguments)

            is DuplicateAndEditArgs -> executions.executeDuplicateAndEdit(arguments)

            is QuickInvoiceFromUnbilledReceiptsArgs -> executions.executeQuickInvoiceFromUnbilledReceipts(arguments)

            is QuickSendInvoiceArgs -> executions.executeQuickSendInvoice(arguments)

            is SendInvoiceEmailArgs -> executions.executeSendInvoiceEmail(arguments)

            is SendInvoiceSmsArgs -> executions.executeSendInvoiceSms(arguments)

            is DeleteInvoiceApprovalArgs -> executions.executeDeleteInvoice(arguments)
            is OpenLastInvoiceArgs -> executions.executeOpenLastInvoice(arguments)
            is OpenSupplierArgs -> executions.executeOpenSupplier(arguments)
            is GetProfitGuardianStatusArgs -> executions.executeGetProfitGuardianStatus(arguments)
            is DetectChangeOrdersArgs -> executions.executeDetectChangeOrders(arguments)
            is GetDailyBriefingArgs -> executions.executeGetDailyBriefing(arguments)
            is CreateChangeOrderArgs -> executions.executeCreateChangeOrder(arguments)
        }

    }



    private fun fn(toolName: ToolName, description: String, vararg params: String): AgentFunction {
        return AgentFunction(
            toolName = toolName,
            description = ToolDescription(description),
            parameters = params.map {
                FunctionParameter(
                    com.fordham.toolbelt.domain.model.agent.ParameterName(it),
                    ParameterType.Text,
                    required = it !in OPTIONAL_FUNCTION_PARAMS
                )
            }
        )
    }

    private companion object {
        val OPTIONAL_FUNCTION_PARAMS = setOf(
            "replaceLineItems",
            "createClientIfMissing",
            "lineItemsJson",
            "jobDescription",
            "category",
            "totalAmount",
            "isEstimate"
        )
    }
}

