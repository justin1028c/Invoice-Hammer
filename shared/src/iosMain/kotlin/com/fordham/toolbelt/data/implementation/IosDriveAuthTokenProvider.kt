package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.repository.DriveAccessToken
import com.fordham.toolbelt.domain.repository.DriveAuthTokenProvider
import com.fordham.toolbelt.domain.repository.DriveTokenOutcome
import kotlin.native.concurrent.ThreadLocal

interface IosDriveAuthBridge {
    suspend fun getDriveAccessToken(): String
}

@ThreadLocal
object IosDriveAuthServiceProvider {
    var bridge: IosDriveAuthBridge? = null
}

class IosDriveAuthTokenProvider : DriveAuthTokenProvider {
    override suspend fun getDriveAccessToken(): DriveTokenOutcome {
        val bridge = IosDriveAuthServiceProvider.bridge
            ?: return DriveTokenOutcome.Failure(
                FailureMessage("iOS Drive auth bridge is not initialized.")
            )

        return try {
            DriveTokenOutcome.Success(DriveAccessToken(bridge.getDriveAccessToken()))
        } catch (e: Exception) {
            DriveTokenOutcome.Failure(
                FailureMessage(e.message ?: "Unable to get iOS Drive access token.")
            )
        }
    }
}
