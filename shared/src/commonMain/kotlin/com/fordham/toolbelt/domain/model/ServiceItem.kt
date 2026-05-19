package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil

data class ServiceItem(
    val name: String,
    val price: Double
) {
    val formattedPrice: String get() = DateTimeUtil.formatMoney(price)
}
