package com.fordham.toolbelt.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.first

class UnpaidInvoiceWorker(
    context: Context,
    params: WorkerParameters,
    private val invoiceRepository: InvoiceRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val invoices = invoiceRepository.allInvoices.first()
            val unpaidInvoices = invoices.filter { !it.isPaid && !it.isEstimate }
                .sortedByDescending { it.lastUpdated }
                .take(3)

            if (unpaidInvoices.isNotEmpty()) {
                val message = unpaidInvoices.joinToString("\n") { 
                    "${it.clientName}: ${it.formattedTotal}" 
                }
                showNotification(message)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(message: String) {
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

        // Note: MainActivity is still in :app module, so we use a string-based intent or move it
        val intent = Intent().apply {
            setClassName(applicationContext.packageName, "com.fordham.toolbelt.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "HISTORY")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Unpaid Invoices Reminder")
            .setContentText("Tap to view unpaid invoices")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
