package com.fordham.toolbelt.domain.repository

import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.JvmInline

@JvmInline
value class UserId(val value: String)

@JvmInline
value class EmailAddress(val value: String)

@JvmInline
value class DisplayName(val value: String)

@JvmInline
value class PhotoUrl(val value: String)

@JvmInline
value class IdToken(val value: String)

@JvmInline
value class IsPremium(val value: Boolean)

data class FordhamUser(
    val id: UserId,
    val email: EmailAddress?,
    val displayName: DisplayName?,
    val photoUrl: PhotoUrl?,
    val isPremium: IsPremium = IsPremium(true)
)

data class OperationError(val message: String)

sealed interface AuthOutcome {
    data class Authenticated(val user: FordhamUser) : AuthOutcome
    data class Failure(val error: OperationError) : AuthOutcome
    object SignedOut : AuthOutcome
}

interface AuthRepository {
    val currentUser: StateFlow<FordhamUser?>
    
    suspend fun signInWithGoogle(idToken: IdToken): AuthOutcome
    suspend fun signOut(): AuthOutcome
}
