package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.AuthOutcome
import com.fordham.toolbelt.domain.repository.OperationError
import com.fordham.toolbelt.domain.repository.FordhamUser
import com.fordham.toolbelt.domain.repository.UserId
import com.fordham.toolbelt.domain.repository.EmailAddress
import com.fordham.toolbelt.domain.repository.DisplayName
import com.fordham.toolbelt.domain.repository.PhotoUrl
import com.fordham.toolbelt.domain.repository.IdToken
import com.fordham.toolbelt.domain.repository.IsPremium
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val _currentUser = MutableStateFlow(auth.currentUser?.toFordhamUser())
    override val currentUser: StateFlow<FordhamUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser?.toFordhamUser()
        }
    }

    override suspend fun signInWithGoogle(idToken: IdToken): AuthOutcome {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken.value, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user?.toFordhamUser()
            if (user != null) {
                AuthOutcome.Authenticated(user)
            } else {
                AuthOutcome.Failure(OperationError("User is null after sign in"))
            }
        } catch (e: Exception) {
            AuthOutcome.Failure(OperationError(e.message ?: "Google Sign-In failed"))
        }
    }

    override suspend fun signOut(): AuthOutcome {
        return try {
            auth.signOut()
            AuthOutcome.SignedOut
        } catch (e: Exception) {
            AuthOutcome.Failure(OperationError(e.message ?: "Sign out failed"))
        }
    }

    private fun com.google.firebase.auth.FirebaseUser.toFordhamUser() = FordhamUser(
        id = UserId(uid),
        email = email?.let { EmailAddress(it) },
        displayName = displayName?.let { DisplayName(it) },
        photoUrl = photoUrl?.toString()?.let { PhotoUrl(it) },
        isPremium = IsPremium(true)
    )
}
