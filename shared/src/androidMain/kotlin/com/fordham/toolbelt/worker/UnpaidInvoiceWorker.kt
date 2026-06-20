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
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.usecase.ComposeUnpaidInvoiceReminderUseCase
import com.fordham.toolbelt.navigation.MainTabNavigation
import com.fordham.toolbelt.util.UserFacingCopy
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.flow.first

class UnpaidInvoiceWorker(
    context: Context,
    params: WorkerParameters,
    private val invoiceRepository: InvoiceRepository
) : CoroutineWorker(context, params), KoinComponent {

    private val authRepository: AuthRepository by inject()
    private val composeUnpaidInvoiceReminder: ComposeUnpaidInvoiceReminderUseCase by inject()

    override suspend fun doWork(): Result {
        if (!canPostNotifications()) {
            return Result.success()
        }

        return try {
            val invoices = invoiceRepository.allInvoices.first()
            val contractorName = authRepository.currentUser.value?.displayName?.value
            val content = composeUnpaidInvoiceReminder(invoices, contractorName)
            if (content != null) {
                showNotification(content.title, content.body)
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
                UserFacingCopy.Notifications.channelName(),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = UserFacingCopy.Notifications.channelDescription()
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
            .setContentText(UserFacingCopy.Notifications.tapToReview())
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
