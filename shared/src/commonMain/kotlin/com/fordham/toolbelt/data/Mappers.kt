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
    name = ClientName(name),
    email = EmailAddress(email),
    phone = PhoneNumber(phone),
    address = ClientAddress(address),
    notes = notes,
    totalInvoiced = MoneyAmount(totalInvoiced),
    isFavorite = isFavorite
)

fun DomainClient.toEntity(): ClientEntity = ClientEntity(
    id = id.value,
    name = name.value,
    email = email.value,
    phone = phone.value,
    address = address.value,
    notes = notes,
    totalInvoiced = totalInvoiced.value,
    isFavorite = isFavorite
)

fun InvoiceEntity.toDomain(): DomainInvoice = DomainInvoice(
    id = InvoiceId(id),
    clientName = ClientName(clientName),
    clientAddress = ClientAddress(clientAddress),
    clientPhone = PhoneNumber(clientPhone),
    clientEmail = EmailAddress(clientEmail),
    date = date,
    totalAmount = MoneyAmount(totalAmount),
    depositAmount = MoneyAmount(depositAmount),
    itemsSummary = ItemsSummary(itemsSummary),
    pdfPath = PdfFilePath(pdfPath),
    isPaid = isPaid,
    isEstimate = isEstimate,
    lastUpdated = lastUpdated,
    durationSeconds = DurationSeconds(durationSeconds)
)

fun DomainInvoice.toEntity(): InvoiceEntity = InvoiceEntity(
    id = id.value,
    clientName = clientName.value,
    clientAddress = clientAddress.value,
    clientPhone = clientPhone.value,
    clientEmail = clientEmail.value,
    date = date,
    totalAmount = totalAmount.value,
    depositAmount = depositAmount.value,
    itemsSummary = itemsSummary.value,
    pdfPath = pdfPath.value,
    isPaid = isPaid,
    isEstimate = isEstimate,
    lastUpdated = lastUpdated,
    durationSeconds = durationSeconds.value
)

fun ReceiptEntity.toDomain(): DomainReceiptItem = DomainReceiptItem(
    id = ReceiptId(id),
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    totalPrice = totalPrice,
    category = category,
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
    category = category,
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
    phase = when (phase.uppercase()) {
        "AFTER" -> com.fordham.toolbelt.domain.model.JobPhotoPhase.After
        else -> com.fordham.toolbelt.domain.model.JobPhotoPhase.Before
    },
    timestamp = timestamp
)

fun DomainJobPhoto.toEntity(): JobPhotoEntity = JobPhotoEntity(
    id = id.value,
    invoiceId = invoiceId.value,
    localUri = localUri,
    phase = when (phase) {
        com.fordham.toolbelt.domain.model.JobPhotoPhase.Before -> "BEFORE"
        com.fordham.toolbelt.domain.model.JobPhotoPhase.After -> "AFTER"
    },
    timestamp = timestamp
)

fun PaymentRequestEntity.toDomain(): InvoicePaymentRequest = InvoicePaymentRequest(
    id = PaymentRequestId(id),
    invoiceId = InvoiceId(invoiceId),
    invoiceClientName = invoiceClientName,
    type = when (type) {
        "deposit" -> PaymentRequestType.Deposit
        else -> PaymentRequestType.FullBalance
    },
    provider = when (provider) {
        "google_pay" -> PaymentProviderType.GooglePay
        "apple_pay" -> PaymentProviderType.ApplePay
        "stellar_usdc" -> PaymentProviderType.CardLink
        "card_terminal" -> PaymentProviderType.CardTerminal
        "tap_to_pay" -> PaymentProviderType.TapToPay
        "bluetooth_reader" -> PaymentProviderType.BluetoothReader
        else -> PaymentProviderType.CardLink
    },
    requestedAmount = MoneyAmount(requestedAmount),
    status = when (status) {
        "requested" -> InvoicePaymentStatus.Requested
        "pending" -> InvoicePaymentStatus.Pending
        "paid" -> InvoicePaymentStatus.Paid
        "failed" -> InvoicePaymentStatus.Failed
        "expired" -> InvoicePaymentStatus.Expired
        else -> InvoicePaymentStatus.Pending
    },
    paymentLink = PaymentLinkUrl(paymentLink),
    createdAtMillis = createdAtMillis,
    paidAtMillis = paidAtMillis
)

fun InvoicePaymentRequest.toEntity(): PaymentRequestEntity = PaymentRequestEntity(
    id = id.value,
    invoiceId = invoiceId.value,
    invoiceClientName = invoiceClientName,
    type = when (type) {
        PaymentRequestType.Deposit -> "deposit"
        PaymentRequestType.FullBalance -> "full_balance"
    },
    provider = when (provider) {
        PaymentProviderType.GooglePay -> "google_pay"
        PaymentProviderType.ApplePay -> "apple_pay"
        PaymentProviderType.CardLink -> "card_link"
        PaymentProviderType.CardTerminal -> "card_terminal"
        PaymentProviderType.TapToPay -> "tap_to_pay"
        PaymentProviderType.BluetoothReader -> "bluetooth_reader"
    },
    requestedAmount = requestedAmount.value,
    status = when (status) {
        InvoicePaymentStatus.Requested -> "requested"
        InvoicePaymentStatus.Pending -> "pending"
        InvoicePaymentStatus.Paid -> "paid"
        InvoicePaymentStatus.Failed -> "failed"
        InvoicePaymentStatus.Expired -> "expired"
    },
    paymentLink = paymentLink.value,
    createdAtMillis = createdAtMillis,
    paidAtMillis = paidAtMillis,
    stellarTransactionHash = null,
    stellarExplorerUrl = null,
    assetCode = "USD"
)
