package com.fordham.toolbelt.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.fordham.toolbelt.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GeofenceBroadcastReceiver : BroadcastReceiver(), KoinComponent {
    private val database: AppDatabase by inject()
    private val notificationManager: PlatformNotificationManager by inject()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
            for (geofence in triggeringGeofences) {
                val clientId = geofence.requestId
                coroutineScope.launch {
                    try {
                        val clients = database.clientDao().getAllClientsOnce()
                        val client = clients.firstOrNull { it.id == clientId } ?: return@launch
                        val notes = database.jobNoteDao().getRelevantContext(client.name).take(3)
                        val briefing = if (notes.isNotEmpty()) {
                            "Remember:\n" + notes.joinToString("\n") { "- ${it.text}" }
                        } else {
                            "Welcome back to ${client.name}'s job site. No prior job notes."
                        }
                        notificationManager.postBriefingNotification(
                            title = "Foreman AI Site Briefing: ${client.name}",
                            body = briefing
                        )
                    } catch (e: Exception) {
                        AppLogger.e("GeofenceReceiver", "Failed to build site briefing", e)
                    }
                }
            }
        }
    }
}
