package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

data class Client(
    val id: ClientId = ClientId(randomUUID()),
    val name: ClientName,
    val email: EmailAddress = EmailAddress(""),
    val phone: PhoneNumber = PhoneNumber(""),
    val address: ClientAddress = ClientAddress(""),
    val notes: String = "",
    val totalInvoiced: MoneyAmount = MoneyAmount(0.0),
    val isFavorite: Boolean = false,
    val lastUpdated: Long = Clock.System.now().toEpochMilliseconds()
)