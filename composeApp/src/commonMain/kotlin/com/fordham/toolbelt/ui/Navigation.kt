package com.fordham.toolbelt.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object NewInvoice : Screen
    @Serializable
    data object History : Screen
    @Serializable
    data object Receipts : Screen
    @Serializable
    data object Stats : Screen
    @Serializable
    data object Clients : Screen
    @Serializable
    data object Suppliers : Screen
    @Serializable
    data object Settings : Screen
}
