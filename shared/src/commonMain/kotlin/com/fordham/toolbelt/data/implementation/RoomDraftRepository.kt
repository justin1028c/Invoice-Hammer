package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.DraftDao
import com.fordham.toolbelt.data.DraftInvoiceEntity
import com.fordham.toolbelt.data.dto.LineItemDto
import com.fordham.toolbelt.domain.model.DraftInvoice
import com.fordham.toolbelt.domain.repository.DraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomDraftRepository(
    private val draftDao: DraftDao
) : DraftRepository {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override fun getDraft(): Flow<DraftInvoice> {
        return draftDao.getDraft().map { entity ->
            val e = entity ?: DraftInvoiceEntity()
            DraftInvoice(
                clientName = e.clientName,
                clientAddress = e.clientAddress,
                taxRate = e.taxRate,
                deposit = e.deposit,
                hourlyRate = e.hourlyRate,
                logoUri = e.logoUri,
                selectedCategory = e.selectedCategory,
                itemDesc = e.itemDesc,
                itemAmt = e.itemAmt,
                elapsedSeconds = e.elapsedSeconds,
                startTime = e.startTime,
                timerRunning = e.timerRunning,
                saveToClientDirectory = e.saveToClientDirectory,
                lineItems = try {
                    json.decodeFromString<List<LineItemDto>>(e.lineItemsJson).map { it.toDomain() }
                } catch (t: Throwable) {
                    emptyList()
                },
                capturedPhotos = try {
                    json.decodeFromString<List<String>>(e.capturedPhotosJson)
                } catch (t: Throwable) {
                    emptyList()
                },
                linkedReceiptIds = try {
                    json.decodeFromString<List<String>>(e.linkedReceiptIdsJson)
                } catch (t: Throwable) {
                    emptyList()
                }
            )
        }
    }

    override suspend fun saveDraft(draft: DraftInvoice) {
        val entity = DraftInvoiceEntity(
            clientName = draft.clientName,
            clientAddress = draft.clientAddress,
            taxRate = draft.taxRate,
            deposit = draft.deposit,
            hourlyRate = draft.hourlyRate,
            logoUri = draft.logoUri,
            selectedCategory = draft.selectedCategory,
            itemDesc = draft.itemDesc,
            itemAmt = draft.itemAmt,
            elapsedSeconds = draft.elapsedSeconds,
            startTime = draft.startTime,
            timerRunning = draft.timerRunning,
            saveToClientDirectory = draft.saveToClientDirectory,
            lineItemsJson = json.encodeToString(draft.lineItems.map { LineItemDto.fromDomain(it) }),
            capturedPhotosJson = json.encodeToString(draft.capturedPhotos),
            linkedReceiptIdsJson = json.encodeToString(draft.linkedReceiptIds)
        )
        draftDao.saveDraft(entity)
    }

    override suspend fun clearDraft() {
        draftDao.clearDraft()
    }
}
