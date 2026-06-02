package com.fordham.toolbelt.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume

class AndroidPlatformGeofenceManager(private val context: Context) : PlatformGeofenceManager {
    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val geocoder = Geocoder(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 3001, intent, flags)
    }

    override suspend fun registerClientGeofence(clientId: String, address: String) = suspendCancellableCoroutine<Unit> { continuation ->
        if (address.isBlank()) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }

        try {
            // Geocode address string to latitude/longitude coordinates dynamically
            val results = geocoder.getFromLocationName(address, 1)
            val location = results?.firstOrNull()
            if (location == null) {
                continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }

            val geofence = Geofence.Builder()
                .setRequestId(clientId)
                .setCircularRegion(location.latitude, location.longitude, 200f) // 200 meters radius
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener {
                    continuation.resume(Unit)
                }
        } catch (e: Exception) {
            continuation.resume(Unit)
        }
    }

    override suspend fun removeClientGeofence(clientId: String) = suspendCancellableCoroutine<Unit> { continuation ->
        try {
            geofencingClient.removeGeofences(listOf(clientId))
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener {
                    continuation.resume(Unit)
                }
        } catch (e: Exception) {
            continuation.resume(Unit)
        }
    }
}

class AndroidPlatformNotificationManager(private val context: Context) : PlatformNotificationManager {
    override fun postBriefingNotification(title: String, body: String) {
        val channelId = "foreman_briefings"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Foreman Briefings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Just-in-time site briefings from Foreman AI"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent().apply {
            setClassName(context.packageName, "com.fordham.toolbelt.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            pendingIntentFlags
        )

        val iconId = context.applicationInfo.icon.takeIf { it != 0 }
            ?: android.R.drawable.ic_dialog_info

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconId)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2002, notification)
    }
}

class AndroidOcrEngine : KmpOcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun extractText(imageBytes: ByteArray): String = suspendCancellableCoroutine { continuation ->
        try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap == null) {
                continuation.resume("")
                return@suspendCancellableCoroutine
            }
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    // Recover gracefully by returning empty string instead of failing background OCR tasks
                    continuation.resume("")
                }
        } catch (e: Exception) {
            continuation.resume("")
        }
    }
}
