package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.dto.ForemanSessionPersistenceDto
import com.fordham.toolbelt.data.dto.ForemanTurnDto
import com.fordham.toolbelt.data.dto.ResolvedEntityEntryDto
import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.ReceiptId
import com.fordham.toolbelt.domain.model.agent.AgentRole
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.ForemanTurn
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.ResolvedClient
import com.fordham.toolbelt.domain.model.agent.ResolvedEntities
import com.fordham.toolbelt.domain.model.agent.ResolvedEntityId
import com.fordham.toolbelt.domain.model.agent.ResolvedInvoice
import com.fordham.toolbelt.domain.model.agent.ResolvedReceipt
import com.fordham.toolbelt.domain.model.agent.SessionId
import com.fordham.toolbelt.domain.model.agent.TimestampMillis
import com.fordham.toolbelt.domain.model.agent.ToolCallId
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.repository.PersistedForemanState

object ForemanSessionPersistenceMapper {
    fun toDto(state: PersistedForemanState): ForemanSessionPersistenceDto {
        val session = state.session
        return ForemanSessionPersistenceDto(
            sessionId = session.sessionId.value,
            history = session.history.map(::turnToDto),
            activeClientId = session.activeClient?.value,
            activeDraftInvoiceId = session.activeDraftInvoice?.value,
            resolvedEntities = session.resolvedEntities.entries().map { entry ->
                val (type, id) = when (val entity = entry.entity) {
                    is ResolvedClient -> "client" to entity.id.value
                    is ResolvedInvoice -> "invoice" to entity.id.value
                    is ResolvedReceipt -> "receipt" to entity.id.value
                }
                ResolvedEntityEntryDto(
                    alias = entry.alias.value,
                    entityType = type,
                    entityId = id
                )
            },
            lastSystemPrompt = state.lastSystemPrompt
        )
    }

    fun fromDto(dto: ForemanSessionPersistenceDto): PersistedForemanState? {
        if (dto.sessionId.isBlank()) return null
        var resolved = ResolvedEntities.empty()
        dto.resolvedEntities.forEach { entry ->
            val entity: ResolvedEntityId = when (entry.entityType) {
                "client" -> ResolvedClient(ClientId(entry.entityId))
                "invoice" -> ResolvedInvoice(InvoiceId(entry.entityId))
                "receipt" -> ResolvedReceipt(ReceiptId(entry.entityId))
                else -> return null
            }
            resolved = resolved.remember(NaturalLanguage(entry.alias), entity)
        }
        val history = dto.history.mapNotNull(::turnFromDto)
        return PersistedForemanState(
            session = ForemanSession(
                sessionId = SessionId(dto.sessionId),
                history = history,
                activeClient = dto.activeClientId?.takeIf { it.isNotBlank() }?.let(::ClientId),
                activeDraftInvoice = dto.activeDraftInvoiceId?.takeIf { it.isNotBlank() }?.let(::InvoiceId),
                resolvedEntities = resolved
            ),
            lastSystemPrompt = dto.lastSystemPrompt
        )
    }

    private fun turnToDto(turn: ForemanTurn): ForemanTurnDto = ForemanTurnDto(
        role = when (turn.role) {
            AgentRole.User -> "user"
            AgentRole.Foreman -> "foreman"
            AgentRole.ToolSystem -> "tool"
        },
        content = turn.content.value,
        timestamp = turn.timestamp.value,
        toolCallId = turn.toolCallId?.value,
        toolName = turn.toolName?.let(::toolNameWire)
    )

    private fun toolNameWire(name: ToolName): String = when (name) {
        ToolName.SearchClients -> "SearchClients"
        ToolName.SelectClient -> "SelectClient"
        ToolName.GetClientDetails -> "GetClientDetails"
        ToolName.GetUnbilledReceipts -> "GetUnbilledReceipts"
        ToolName.OpenTab -> "OpenTab"
        ToolName.CreateDraftInvoice -> "CreateDraftInvoice"
        ToolName.UpdateDraftInvoice -> "UpdateDraftInvoice"
        ToolName.AddJobNote -> "AddJobNote"
        ToolName.SaveInvoiceFromDraft -> "SaveInvoiceFromDraft"
        ToolName.SearchInvoiceHistory -> "SearchInvoiceHistory"
        ToolName.CreateClient -> "CreateClient"
        ToolName.ScanLastReceipt -> "ScanLastReceipt"
        ToolName.QuickInvoice -> "QuickInvoice"
        ToolName.QuickClientAndInvoice -> "QuickClientAndInvoice"
        ToolName.QuickClientLookup -> "QuickClientLookup"
        ToolName.QuickSendInvoice -> "QuickSendInvoice"
        ToolName.AppendDraftLines -> "AppendDraftLines"
        ToolName.DuplicateLastInvoice -> "DuplicateLastInvoice"
        ToolName.DuplicateAndEdit -> "DuplicateAndEdit"
        ToolName.QuickInvoiceFromUnbilledReceipts -> "QuickInvoiceFromUnbilledReceipts"
        ToolName.SendInvoiceEmail -> "SendInvoiceEmail"
        ToolName.SendInvoiceSms -> "SendInvoiceSms"
        ToolName.DeleteInvoiceForApproval -> "DeleteInvoiceForApproval"
        ToolName.OpenLastInvoice -> "OpenLastInvoice"
        ToolName.OpenSupplier -> "OpenSupplier"
        ToolName.GetProfitGuardianStatus -> "GetProfitGuardianStatus"
        ToolName.DetectChangeOrders -> "DetectChangeOrders"
        ToolName.GetDailyBriefing -> "GetDailyBriefing"
        ToolName.CreateChangeOrder -> "CreateChangeOrder"
    }

    private fun toolNameFromWire(raw: String): ToolName? = when (raw) {
        "SearchClients" -> ToolName.SearchClients
        "SelectClient" -> ToolName.SelectClient
        "GetClientDetails" -> ToolName.GetClientDetails
        "GetUnbilledReceipts" -> ToolName.GetUnbilledReceipts
        "OpenTab" -> ToolName.OpenTab
        "CreateDraftInvoice" -> ToolName.CreateDraftInvoice
        "UpdateDraftInvoice" -> ToolName.UpdateDraftInvoice
        "AddJobNote" -> ToolName.AddJobNote
        "SaveInvoiceFromDraft" -> ToolName.SaveInvoiceFromDraft
        "SearchInvoiceHistory" -> ToolName.SearchInvoiceHistory
        "CreateClient" -> ToolName.CreateClient
        "ScanLastReceipt" -> ToolName.ScanLastReceipt
        "QuickInvoice" -> ToolName.QuickInvoice
        "QuickClientAndInvoice" -> ToolName.QuickClientAndInvoice
        "QuickClientLookup" -> ToolName.QuickClientLookup
        "QuickSendInvoice" -> ToolName.QuickSendInvoice
        "AppendDraftLines" -> ToolName.AppendDraftLines
        "DuplicateLastInvoice" -> ToolName.DuplicateLastInvoice
        "DuplicateAndEdit" -> ToolName.DuplicateAndEdit
        "QuickInvoiceFromUnbilledReceipts" -> ToolName.QuickInvoiceFromUnbilledReceipts
        "SendInvoiceEmail" -> ToolName.SendInvoiceEmail
        "SendInvoiceSms" -> ToolName.SendInvoiceSms
        "DeleteInvoiceForApproval" -> ToolName.DeleteInvoiceForApproval
        "OpenLastInvoice" -> ToolName.OpenLastInvoice
        "OpenSupplier" -> ToolName.OpenSupplier
        "GetProfitGuardianStatus" -> ToolName.GetProfitGuardianStatus
        "DetectChangeOrders" -> ToolName.DetectChangeOrders
        "GetDailyBriefing" -> ToolName.GetDailyBriefing
        "CreateChangeOrder" -> ToolName.CreateChangeOrder
        else -> null
    }

    private fun turnFromDto(dto: ForemanTurnDto): ForemanTurn? {
        val role = when (dto.role) {
            "user" -> AgentRole.User
            "foreman" -> AgentRole.Foreman
            "tool" -> AgentRole.ToolSystem
            else -> return null
        }
        return ForemanTurn(
            role = role,
            content = NaturalLanguage(dto.content),
            timestamp = TimestampMillis(dto.timestamp),
            toolCallId = dto.toolCallId?.takeIf { it.isNotBlank() }?.let(::ToolCallId),
            toolName = dto.toolName?.takeIf { it.isNotBlank() }?.let(::toolNameFromWire)
        )
    }
}
