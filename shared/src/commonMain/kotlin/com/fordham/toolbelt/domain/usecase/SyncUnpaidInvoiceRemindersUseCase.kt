package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.util.Permission
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.util.UnpaidInvoiceReminderFormatter
import com.fordham.toolbelt.util.UnpaidInvoiceReminders
import kotlinx.coroutines.flow.first

/**
 * Enables or disables platform reminders for unpaid invoices based on [BusinessSettings.notificationsEnabled].
 */
class SyncUnpaidInvoiceRemindersUseCase(
    private val settingsRepository: SettingsRepository,
    private val invoiceRepository: InvoiceRepository,
    private val platformActions: PlatformActions
) {
    suspend fun execute() {
        val enabled = settingsRepository.getBusinessSettings().notificationsEnabled
        if (!enabled) {
            platformActions.cancelScheduledNotification(UnpaidInvoiceReminders.WORK_ID)
            return
        }

        val body = try {
            val invoices = invoiceRepository.allInvoices.first()
            UnpaidInvoiceReminderFormatter.formatBody(invoices)
        } catch (_: Exception) {
            UnpaidInvoiceReminders.BODY
        }

        platformActions.requestPermission(Permission.POST_NOTIFICATIONS) {
            platformActions.scheduleNotification(
                id = UnpaidInvoiceReminders.WORK_ID,
                title = UnpaidInvoiceReminders.TITLE,
                body = body,
                delayMillis = UnpaidInvoiceReminders.INITIAL_DELAY_MS
            )
        }
    }
}
