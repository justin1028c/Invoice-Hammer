package com.fordham.toolbelt.data

import com.fordham.toolbelt.domain.model.Client as DomainClient
import com.fordham.toolbelt.domain.model.Invoice as DomainInvoice
import com.fordham.toolbelt.domain.model.ReceiptItem as DomainReceiptItem
import com.fordham.toolbelt.domain.model.JobNote as DomainJobNote
import com.fordham.toolbelt.domain.model.JobPhoto as DomainJobPhoto
import com.fordham.toolbelt.domain.model.Supplier as DomainSupplier
import com.fordham.toolbelt.domain.model.*

fun SupplierEntity.toDomain(): DomainSupplier = DomainSupplier(
    id = SupplierId(id),
    name = name,
    category = try { SupplierCategory.valueOf(category) } catch (e: Exception) { SupplierCategory.OTHER },
    address = address,
    phone = PhoneNumber(phone),
    webUrl = webUrl,
    packageName = packageName,
    displayOrder = displayOrder,
    isPinned = isPinned,
    isHidden = isHidden,
    customLogoPath = customLogoPath,
    logoResName = logoResName,
    isDefault = isDefault,
    analytics = SupplierAnalytics()
)

fun DomainSupplier.toEntity(): SupplierEntity = SupplierEntity(
    id = id.value,
    name = name,
    category = category.name,
    address = address,
    phone = phone.value,
    webUrl = webUrl,
    packageName = packageName,
    displayOrder = displayOrder,
    isPinned = isPinned,
    isHidden = isHidden,
    customLogoPath = customLogoPath,
    logoResName = logoResName,
    isDefault = isDefault
)

fun ClientEntity.toDomain(): DomainClient = DomainClient(
    id = ClientId(id),
    name = name,
    email = EmailAddress(email),
    phone = PhoneNumber(phone),
    address = address,
    notes = notes,
    totalInvoiced = totalInvoiced,
    isFavorite = isFavorite
)

fun DomainClient.toEntity(): ClientEntity = ClientEntity(
    id = id.value,
    name = name,
    email = email.value,
    phone = phone.value,
    address = address,
    notes = notes,
    totalInvoiced = totalInvoiced,
    isFavorite = isFavorite
)

fun InvoiceEntity.toDomain(): DomainInvoice = DomainInvoice(
    id = InvoiceId(id),
    clientName = clientName,
    clientAddress = clientAddress,
    clientPhone = PhoneNumber(clientPhone),
    clientEmail = EmailAddress(clientEmail),
    date = date,
    totalAmount = totalAmount,
    depositAmount = depositAmount,
    itemsSummary = itemsSummary,
    pdfPath = pdfPath,
    isPaid = isPaid,
    isEstimate = isEstimate,
    lastUpdated = lastUpdated,
    durationSeconds = durationSeconds
)

fun DomainInvoice.toEntity(): InvoiceEntity = InvoiceEntity(
    id = id.value,
    clientName = clientName,
    clientAddress = clientAddress,
    clientPhone = clientPhone.value,
    clientEmail = clientEmail.value,
    date = date,
    totalAmount = totalAmount,
    depositAmount = depositAmount,
    itemsSummary = itemsSummary,
    pdfPath = pdfPath,
    isPaid = isPaid,
    isEstimate = isEstimate,
    lastUpdated = lastUpdated,
    durationSeconds = durationSeconds
)

fun ReceiptEntity.toDomain(): DomainReceiptItem = DomainReceiptItem(
    id = ReceiptId(id),
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    totalPrice = totalPrice,
    category = "Other",
    clientName = clientName,
    imagePath = imagePath,
    isBilled = isBilled,
    lastUpdated = lastUpdated,
    supplierName = supplierName,
    linkedInvoiceId = linkedInvoiceId?.let { InvoiceId(it) }
)

fun DomainReceiptItem.toEntity(): ReceiptEntity = ReceiptEntity(
    id = id.value,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    totalPrice = totalPrice,
    clientName = clientName,
    imagePath = imagePath,
    isBilled = isBilled,
    lastUpdated = lastUpdated,
    supplierName = supplierName,
    linkedInvoiceId = linkedInvoiceId?.value
)

fun JobNoteEntity.toDomain(): DomainJobNote = DomainJobNote(
    id = NoteId(id),
    clientName = clientName,
    invoiceId = invoiceId?.let { InvoiceId(it) },
    text = text,
    timestamp = timestamp
)

fun DomainJobNote.toEntity(): JobNoteEntity = JobNoteEntity(
    id = id.value,
    clientName = clientName,
    invoiceId = invoiceId?.value,
    text = text,
    timestamp = timestamp
)

fun JobPhotoEntity.toDomain(): DomainJobPhoto = DomainJobPhoto(
    id = PhotoId(id),
    invoiceId = InvoiceId(invoiceId),
    localUri = localUri,
    timestamp = timestamp
)

fun DomainJobPhoto.toEntity(): JobPhotoEntity = JobPhotoEntity(
    id = id.value,
    invoiceId = invoiceId.value,
    localUri = localUri,
    timestamp = timestamp
)
