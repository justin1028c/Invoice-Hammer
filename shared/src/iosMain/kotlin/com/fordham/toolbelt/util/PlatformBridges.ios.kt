package com.fordham.toolbelt.util

import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLCircularRegion
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.CLLocation
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizedTextObservation
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.UUID
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.coroutines.resume

class IosPlatformGeofenceManager : PlatformGeofenceManager {
    private val locationManager = CLLocationManager()
    private val geocoder = CLGeocoder()

    override suspend fun registerClientGeofence(clientId: String, address: String) = suspendCancellableCoroutine<Unit> { continuation ->
        if (address.isBlank()) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }

        geocoder.geocodeAddressString(address) { placemarks, error ->
            val placemark = placemarks?.firstOrNull() as? CLPlacemark
            val location = placemark?.location
            if (location == null || error != null) {
                continuation.resume(Unit)
                return@geocodeAddressString
            }

            val coordinates = location.coordinate
            val region = CLCircularRegion(
                center = coordinates,
                radius = 200.0, // 200 meters
                identifier = clientId
            ).apply {
                notifyOnEntry = true
                notifyOnExit = false
            }

            locationManager.startMonitoringForRegion(region)
            continuation.resume(Unit)
        }
    }

    override suspend fun removeClientGeofence(clientId: String) {
        val monitoredRegions = locationManager.monitoredRegions
        for (region in monitoredRegions) {
            if (region is CLCircularRegion && region.identifier == clientId) {
                locationManager.stopMonitoringForRegion(region)
            }
        }
    }
}

class IosPlatformNotificationManager : PlatformNotificationManager {
    override fun postBriefingNotification(title: String, body: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(platform.UserNotifications.UNNotificationSound.defaultNotificationSound())
        }

        val trigger = platform.UserNotifications.UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
        val request = UNNotificationRequest.requestWithIdentifier("foreman_briefing_${platform.Foundation.NSUUID().UUIDString()}", content, trigger)

        center.addNotificationRequest(request) { error ->
            if (error != null) {
                AppLogger.e("IosNotification", "Failed to post notification: ${error.localizedDescription}")
            }
        }
    }
}

class IosOcrEngine : KmpOcrEngine {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun extractText(imageBytes: ByteArray): String = suspendCancellableCoroutine { continuation ->
        try {
            val nsData = imageBytes.toNSData()
            val requestHandler = VNImageRequestHandler(data = nsData, options = emptyMap<Any?, Any?>())
            val request = VNRecognizeTextRequest { request, error ->
                if (error != null || request == null) {
                    continuation.resume("")
                    return@VNRecognizeTextRequest
                }
                val results = request.results
                if (results == null) {
                    continuation.resume("")
                    return@VNRecognizeTextRequest
                }
                val text = results.mapNotNull { observation ->
                    if (observation is VNRecognizedTextObservation) {
                        observation.topCandidates(1UL).firstOrNull()?.text
                    } else null
                }.joinToString("\n")
                continuation.resume(text)
            }
            
            requestHandler.performRequests(listOf(request), null)
        } catch (e: Exception) {
            continuation.resume("")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return this.usePinned {
        NSData.create(bytes = it.addressOf(0), length = this.size.toULong())
    }
}
