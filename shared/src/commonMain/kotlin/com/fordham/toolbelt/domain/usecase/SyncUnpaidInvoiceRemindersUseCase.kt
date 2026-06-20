package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.UnpaidInvoiceReminderContent
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.util.Permission
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.util.UnpaidInvoiceReminders
import kotlinx.coroutines.flow.first

/**
 * Enables or disables platform reminders for unpaid invoices based on [BusinessSettings.notificationsEnabled].
 */
class SyncUnpaidInvoiceRemindersUseCase(
    private val settingsRepository: SettingsRepository,
    private val invoiceRepository: InvoiceRepository,
    private val platformActions: PlatformActions,
    private val authRepository: AuthRepository,
    private val composeUnpaidInvoiceReminder: ComposeUnpaidInvoiceReminderUseCase
) {
    suspend fun execute() {
        val enabled = settingsRepository.getBusinessSettings().notificationsEnabled
        if (!enabled) {
            platformActions.cancelScheduledNotification(UnpaidInvoiceReminders.WORK_ID)
            return
        }

        val content = try {
            val invoices = invoiceRepository.allInvoices.first()
            val contractorName = authRepository.currentUser.value?.displayName?.value
            composeUnpaidInvoiceReminder(invoices, contractorName)
        } catch (_: Exception) {
            null
        } ?: return

        platformActions.requestPermission(Permission.POST_NOTIFICATIONS) {
            platformActions.scheduleNotification(
                id = UnpaidInvoiceReminders.WORK_ID,
                title = content.title,
                body = content.body,
                delayMillis = UnpaidInvoiceReminders.INITIAL_DELAY_MS
            )
        }
    }
}
