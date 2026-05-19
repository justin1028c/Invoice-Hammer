package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlin.jvm.JvmInline

@JvmInline
value class DriveAccessToken(val value: String) {
    init {
        require(value.isNotBlank()) { "DriveAccessToken cannot be blank." }
    }
}

sealed interface DriveTokenOutcome {
    data class Success(val token: DriveAccessToken) : DriveTokenOutcome
    data class Failure(val error: FailureMessage) : DriveTokenOutcome
}

interface DriveAuthTokenProvider {
    suspend fun getDriveAccessToken(): DriveTokenOutcome
}
