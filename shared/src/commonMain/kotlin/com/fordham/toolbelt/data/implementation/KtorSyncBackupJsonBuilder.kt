package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.ClientListOutcome
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.SupplierListOutcome
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal object KtorSyncBackupJsonBuilder {

    suspend fun build(
        invoiceRepository: InvoiceRepository,
        receiptRepository: ReceiptRepository,
        clientRepository: ClientRepository,
        supplierRepository: SupplierRepository,
        settingsRepository: SettingsRepository
    ): JsonObject {
        val invoices = invoiceRepository.allInvoices.first()
        val receipts = when (val outcome = receiptRepository.allItems.first()) {
            is ReceiptListOutcome.Success -> outcome.receipts
            is ReceiptListOutcome.Failure -> emptyList()
        }
        val clients = when (val outcome = clientRepository.getAllClients().first()) {
            is ClientListOutcome.Success -> outcome.clients
            is ClientListOutcome.Failure -> emptyList()
        }
        val visibleSuppliers = when (val outcome = supplierRepository.getVisibleSuppliers().first()) {
            is SupplierListOutcome.Success -> outcome.suppliers
            is SupplierListOutcome.Failure -> emptyList()
        }
        val hiddenSuppliers = when (val outcome = supplierRepository.getHiddenSuppliers().first()) {
            is SupplierListOutcome.Success -> outcome.suppliers
            is SupplierListOutcome.Failure -> emptyList()
        }
        val settings = settingsRepository.getBusinessSettings()

        return buildJsonObject {
            put("schemaVersion", 1)
            put("exportedAtMillis", Clock.System.now().toEpochMilliseconds())
            putJsonObject("settings") {
                put("businessName", settings.businessName)
                put("businessSlogan", settings.businessSlogan)
                put("businessPhone", settings.businessPhone)
                put("businessEmail", settings.businessEmail)
                put("businessAddress", settings.businessAddress)
                put("taxRate", settings.taxRate)
                put("markupPercentage", settings.markupPercentage)
                put("isPremium", settings.isPremium)
                put("isDarkMode", settings.isDarkMode)
                put("useMetricUnits", settings.useMetricUnits)
                put("notificationsEnabled", settings.notificationsEnabled)
                put("hasSeenPreLaunchPaywall", settings.hasSeenPreLaunchPaywall)
            }
            putJsonArray("clients") {
                clients.forEach { client ->
                    addJsonObject {
                        put("id", client.id.value)
                        put("name", client.name.value)
                        put("email", client.email.value)
                        put("phone", client.phone.value)
                        put("address", client.address.value)
                        put("notes", client.notes)
                        put("totalInvoiced", client.totalInvoiced.value)
                        put("isFavorite", client.isFavorite)
                        put("lastUpdated", client.lastUpdated)
                    }
                }
            }
            putJsonArray("invoices") {
                invoices.forEach { invoice ->
                    addJsonObject {
                        put("id", invoice.id.value)
                        put("clientName", invoice.clientName.value)
                        put("clientAddress", invoice.clientAddress.value)
                        put("clientPhone", invoice.clientPhone.value)
                        put("clientEmail", invoice.clientEmail.value)
                        put("date", invoice.date)
                        put("totalAmount", invoice.totalAmount.value)
                        put("depositAmount", invoice.depositAmount.value)
                        put("itemsSummary", invoice.itemsSummary.value)
                        put("pdfPath", invoice.pdfPath.value)
                        put("isPaid", invoice.isPaid)
                        put("isEstimate", invoice.isEstimate)
                        put("lastUpdated", invoice.lastUpdated)
                        put("durationSeconds", invoice.durationSeconds.value)
                    }
                }
            }
            putJsonArray("receipts") {
                receipts.forEach { receipt ->
                    addJsonObject {
                        put("id", receipt.id.value)
                        put("description", receipt.description)
                        put("quantity", receipt.quantity)
                        put("unitPrice", receipt.unitPrice)
                        put("totalPrice", receipt.totalPrice)
                        put("category", receipt.category)
                        put("clientName", receipt.clientName)
                        put("imagePath", receipt.imagePath)
                        put("isBilled", receipt.isBilled)
                        put("lastUpdated", receipt.lastUpdated)
                        put("supplierName", receipt.supplierName)
                        put("linkedInvoiceId", receipt.linkedInvoiceId?.value)
                    }
                }
            }
            putJsonArray("suppliers") {
                (visibleSuppliers + hiddenSuppliers).forEach { supplier ->
                    addJsonObject {
                        put("id", supplier.id.value)
                        put("name", supplier.name)
                        put("category", supplier.category.name)
                        put("address", supplier.address)
                        put("phone", supplier.phone.value)
                        put("webUrl", supplier.webUrl)
                        put("packageName", supplier.packageName)
                        put("displayOrder", supplier.displayOrder)
                        put("isPinned", supplier.isPinned)
                        put("isHidden", supplier.isHidden)
                        put("customLogoPath", supplier.customLogoPath)
                        put("logoResName", supplier.logoResName)
                        put("isDefault", supplier.isDefault)
                    }
                }
            }
        }
    }
}
