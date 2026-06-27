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



    private val clientExecutions = ClientToolExecutions(
        clientRepository = clientRepository,
        invoiceRepository = invoiceRepository,
        addJobNoteUseCase = addJobNoteUseCase
    )

    private val invoiceExecutions = InvoiceToolExecutions(
        clientToolExecutions = clientExecutions,
        clientRepository = clientRepository,
        receiptRepository = receiptRepository,
        draftRepository = draftRepository,
        invoiceRepository = invoiceRepository,
        settingsRepository = settingsRepository,
        generateAndSaveInvoiceUseCase = generateAndSaveInvoiceUseCase
    )

    private val receiptExecutions = ReceiptToolExecutions(
        clientToolExecutions = clientExecutions,
        receiptRepository = receiptRepository,
        draftRepository = draftRepository,
        supplierRepository = supplierRepository,
        hasSubscriptionFeature = hasSubscriptionFeature,
        processReceiptUseCase = processReceiptUseCase
    )

    private val communicationExecutions = CommunicationToolExecutions(
        invoiceRepository = invoiceRepository
    )

    private val helperExecutions = HelperToolExecutions(
        getProfitGuardianStatus = getProfitGuardianStatus,
        detectChangeOrders = detectChangeOrders,
        getDailyBriefing = getDailyBriefing
    )

    override suspend fun execute(toolName: ToolName, arguments: ToolArguments): ToolExecutionResult {
        return when (arguments) {
            is SearchClientsArgs -> clientExecutions.executeSearchClients(arguments)
            is SelectClientArgs -> clientExecutions.executeSelectClient(arguments)
            is GetClientDetailsArgs -> clientExecutions.executeGetClientDetails(arguments)
            is GetUnbilledReceiptsArgs -> receiptExecutions.executeGetUnbilledReceipts(arguments)
            is OpenTabArgs -> helperExecutions.executeOpenTab(arguments)
            is CreateClientArgs -> clientExecutions.executeCreateClient(arguments)
            is CreateDraftInvoiceArgs -> invoiceExecutions.executeCreateDraftInvoice(arguments)
            is UpdateDraftInvoiceArgs -> invoiceExecutions.executeUpdateDraftInvoice(arguments)
            is AddJobNoteArgs -> clientExecutions.executeAddJobNote(arguments)
            is SaveInvoiceFromDraftArgs -> invoiceExecutions.executeSaveInvoiceFromDraft(arguments)
            is SearchInvoiceHistoryArgs -> invoiceExecutions.executeSearchInvoiceHistory(arguments)
            is ScanLastReceiptArgs -> receiptExecutions.executeScanLastReceipt(arguments)
            is QuickInvoiceArgs -> invoiceExecutions.executeQuickInvoice(arguments)
            is QuickClientAndInvoiceArgs -> invoiceExecutions.executeQuickClientAndInvoice(arguments)
            is QuickClientLookupArgs -> clientExecutions.executeQuickClientLookup(arguments)
            is AppendDraftLinesArgs -> invoiceExecutions.executeAppendDraftLines(arguments)
            is DuplicateLastInvoiceArgs -> invoiceExecutions.executeDuplicateLastInvoice(arguments)
            is DuplicateAndEditArgs -> invoiceExecutions.executeDuplicateAndEdit(arguments)
            is QuickInvoiceFromUnbilledReceiptsArgs -> invoiceExecutions.executeQuickInvoiceFromUnbilledReceipts(arguments)
            is QuickSendInvoiceArgs -> communicationExecutions.executeQuickSendInvoice(arguments)
            is SendInvoiceEmailArgs -> communicationExecutions.executeSendInvoiceEmail(arguments)
            is SendInvoiceSmsArgs -> communicationExecutions.executeSendInvoiceSms(arguments)
            is DeleteInvoiceApprovalArgs -> invoiceExecutions.executeDeleteInvoice(arguments)
            is OpenLastInvoiceArgs -> invoiceExecutions.executeOpenLastInvoice(arguments)
            is OpenSupplierArgs -> receiptExecutions.executeOpenSupplier(arguments)
            is GetProfitGuardianStatusArgs -> helperExecutions.executeGetProfitGuardianStatus(arguments)
            is DetectChangeOrdersArgs -> helperExecutions.executeDetectChangeOrders(arguments)
            is GetDailyBriefingArgs -> helperExecutions.executeGetDailyBriefing(arguments)
            is CreateChangeOrderArgs -> invoiceExecutions.executeCreateChangeOrder(arguments)
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

