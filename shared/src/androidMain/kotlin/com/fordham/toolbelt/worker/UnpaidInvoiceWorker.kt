package com.fordham.toolbelt.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.navigation.MainTabNavigation
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.util.UnpaidInvoiceReminderFormatter
import com.fordham.toolbelt.domain.usecase.ForemanOrchestrator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.flow.first

class UnpaidInvoiceWorker(
    context: Context,
    params: WorkerParameters,
    private val invoiceRepository: InvoiceRepository
) : CoroutineWorker(context, params), KoinComponent {

    private val foremanOrchestrator: ForemanOrchestrator by inject()
    private val authRepository: AuthRepository by inject()

    override suspend fun doWork(): Result {
        if (!canPostNotifications()) {
            return Result.success()
        }

        return try {
            val invoices = invoiceRepository.allInvoices.first()
            val unpaid = invoices.firstOrNull { !it.isPaid && !it.isEstimate }
            if (unpaid != null) {
                var message = UnpaidInvoiceReminderFormatter.formatBody(invoices)
                try {
                    val user = authRepository.currentUser.value
                    val contractorName = user?.displayName?.value?.takeIf { it.isNotBlank() } ?: "Justin"
                    val prompt = "You are drafting an unpaid invoice notification reminder for the contractor, $contractorName. " +
                        "The client, ${unpaid.clientName}, owes $contractorName a total of $${unpaid.totalAmount} for unpaid invoice with ID ${unpaid.id.value}. " +
                        "Draft a notification body addressing the contractor directly (e.g., 'Hi $contractorName,') " +
                        "and reminding them that ${unpaid.clientName} owes $${unpaid.totalAmount} for invoice ${unpaid.id.value}. " +
                        "Keep the greeting to the contractor ($contractorName), and clearly specify that ${unpaid.clientName} is the debtor (the one who owes the money)."
                    val result = foremanOrchestrator.run(
                        command = com.fordham.toolbelt.domain.model.agent.NaturalLanguage(prompt),
                        systemPrompt = com.fordham.toolbelt.domain.model.agent.NaturalLanguage("You are Foreman AI, drafting billing reminders for $contractorName."),
                        runtime = com.fordham.toolbelt.domain.model.agent.ForemanRuntimeSnapshot(
                            lastSavedInvoiceId = unpaid.id,
                            lastSavedInvoiceClientName = unpaid.clientName
                        )
                    )
                    when (val outcome = result.outcome) {
                        is com.fordham.toolbelt.domain.model.agent.AgentOutcome.TextResponse -> {
                            message = outcome.response.value
                        }
                        is com.fordham.toolbelt.domain.model.agent.AgentOutcome.ToolChainExecuted -> {
                            outcome.finalMessage?.value?.let {
                                message = it
                            }
                        }
                        else -> {}
                    }
                } catch (_: Exception) {
                    // Fallback to static reminder
                }
                val title = "Payment Reminder: ${unpaid.clientName}"
                showNotification(title, message)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "unpaid_invoices_reminder"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Unpaid Invoices",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for unpaid invoices"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent().apply {
            setClassName(applicationContext.packageName, "com.fordham.toolbelt.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainTabNavigation.EXTRA_NAVIGATE_TO, MainTabNavigation.TARGET_HISTORY)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconId = applicationContext.applicationInfo.icon.takeIf { it != 0 }
            ?: android.R.drawable.ic_dialog_info

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(iconId)
            .setContentTitle(title)
            .setContentText("Tap to review unpaid invoices")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
