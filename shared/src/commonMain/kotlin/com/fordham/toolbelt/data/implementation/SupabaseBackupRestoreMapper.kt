package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.EmailAddress
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.PhoneNumber
import com.fordham.toolbelt.domain.model.ReceiptId
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.Supplier
import com.fordham.toolbelt.domain.model.SupplierCategory
import com.fordham.toolbelt.domain.model.SupplierId
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

internal data class ParsedAppBackup(
    val settings: BusinessSettings,
    val clients: List<Client>,
    val invoices: List<Invoice>,
    val receipts: List<ReceiptItem>,
    val suppliers: List<Supplier>
)

internal object SupabaseBackupRestoreMapper {
    fun parse(backup: JsonObject, preserveLogoUri: String?): ParsedAppBackup {
        val settingsObject = backup["settings"]?.jsonObject
        return ParsedAppBackup(
            settings = parseSettings(settingsObject, preserveLogoUri),
            clients = backup["clients"]?.jsonArray?.mapNotNull { parseClient(it.jsonObject) }.orEmpty(),
            invoices = backup["invoices"]?.jsonArray?.mapNotNull { parseInvoice(it.jsonObject) }.orEmpty(),
            receipts = backup["receipts"]?.jsonArray?.mapNotNull { parseReceipt(it.jsonObject) }.orEmpty(),
            suppliers = backup["suppliers"]?.jsonArray?.mapNotNull { parseSupplier(it.jsonObject) }.orEmpty()
        )
    }

    private fun parseSettings(settings: JsonObject?, preserveLogoUri: String?): BusinessSettings {
        if (settings == null) {
            return BusinessSettings(logoUri = preserveLogoUri)
        }
        return BusinessSettings(
            businessName = settings.stringValue("businessName"),
            businessSlogan = settings.stringValue("businessSlogan"),
            businessPhone = settings.stringValue("businessPhone"),
            businessEmail = settings.stringValue("businessEmail"),
            businessAddress = settings.stringValue("businessAddress"),
            taxRate = settings.doubleValue("taxRate"),
            markupPercentage = settings.doubleValue("markupPercentage"),
            logoUri = preserveLogoUri,
            isPremium = settings.booleanValue("isPremium"),
            isDarkMode = settings.booleanValue("isDarkMode", default = true),
            useMetricUnits = settings.booleanValue("useMetricUnits"),
            notificationsEnabled = settings.booleanValue("notificationsEnabled", default = true)
        )
    }

    private fun parseClient(obj: JsonObject): Client? {
        val id = obj.stringValue("id").takeIf { it.isNotBlank() } ?: return null
        val name = obj.stringValue("name").takeIf { it.isNotBlank() } ?: return null
        return Client(
            id = ClientId(id),
            name = name,
            email = EmailAddress(obj.stringValue("email")),
            phone = PhoneNumber(obj.stringValue("phone")),
            address = obj.stringValue("address"),
            notes = obj.stringValue("notes"),
            totalInvoiced = obj.doubleValue("totalInvoiced"),
            isFavorite = obj.booleanValue("isFavorite"),
            lastUpdated = obj.longValue("lastUpdated")
        )
    }

    private fun parseInvoice(obj: JsonObject): Invoice? {
        val id = obj.stringValue("id").takeIf { it.isNotBlank() } ?: return null
        return Invoice(
            id = InvoiceId(id),
            clientName = obj.stringValue("clientName"),
            clientAddress = obj.stringValue("clientAddress"),
            clientPhone = PhoneNumber(obj.stringValue("clientPhone")),
            clientEmail = EmailAddress(obj.stringValue("clientEmail")),
            date = obj.stringValue("date"),
            totalAmount = obj.doubleValue("totalAmount"),
            depositAmount = obj.doubleValue("depositAmount"),
            itemsSummary = obj.stringValue("itemsSummary"),
            pdfPath = obj.stringValue("pdfPath"),
            isPaid = obj.booleanValue("isPaid"),
            isEstimate = obj.booleanValue("isEstimate"),
            lastUpdated = obj.longValue("lastUpdated"),
            durationSeconds = obj.longValue("durationSeconds")
        )
    }

    private fun parseReceipt(obj: JsonObject): ReceiptItem? {
        val id = obj.stringValue("id").takeIf { it.isNotBlank() } ?: return null
        val linkedInvoiceId = obj.stringValue("linkedInvoiceId").takeIf { it.isNotBlank() }?.let { InvoiceId(it) }
        return ReceiptItem(
            id = ReceiptId(id),
            description = obj.stringValue("description"),
            quantity = obj.doubleValue("quantity", default = 1.0),
            unitPrice = obj.doubleValue("unitPrice"),
            totalPrice = obj.doubleValue("totalPrice"),
            category = obj.stringValue("category", default = "Other"),
            clientName = obj.stringValue("clientName"),
            imagePath = obj.stringValue("imagePath"),
            isBilled = obj.booleanValue("isBilled"),
            lastUpdated = obj.longValue("lastUpdated"),
            supplierName = obj.stringValue("supplierName"),
            linkedInvoiceId = linkedInvoiceId
        )
    }

    private fun parseSupplier(obj: JsonObject): Supplier? {
        val id = obj.stringValue("id").takeIf { it.isNotBlank() } ?: return null
        val name = obj.stringValue("name").takeIf { it.isNotBlank() } ?: return null
        val category = runCatching { SupplierCategory.valueOf(obj.stringValue("category")) }
            .getOrDefault(SupplierCategory.OTHER)
        return Supplier(
            id = SupplierId(id),
            name = name,
            category = category,
            address = obj.stringValue("address"),
            phone = PhoneNumber(obj.stringValue("phone")),
            webUrl = obj.stringValue("webUrl"),
            packageName = obj.stringValue("packageName"),
            displayOrder = obj.intValue("displayOrder"),
            isPinned = obj.booleanValue("isPinned"),
            isHidden = obj.booleanValue("isHidden"),
            customLogoPath = obj.stringValue("customLogoPath").takeIf { it.isNotBlank() },
            logoResName = obj.stringValue("logoResName").takeIf { it.isNotBlank() },
            isDefault = obj.booleanValue("isDefault")
        )
    }

    private fun JsonObject.stringValue(key: String, default: String = ""): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.doubleValue(key: String, default: Double = 0.0): Double =
        this[key]?.jsonPrimitive?.double ?: default

    private fun JsonObject.longValue(key: String, default: Long = 0L): Long =
        this[key]?.jsonPrimitive?.long ?: default

    private fun JsonObject.intValue(key: String, default: Int = 0): Int =
        this[key]?.jsonPrimitive?.int ?: default

    private fun JsonObject.booleanValue(key: String, default: Boolean = false): Boolean =
        this[key]?.jsonPrimitive?.boolean ?: default
}
