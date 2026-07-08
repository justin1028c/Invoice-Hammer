package com.fordham.toolbelt.data.implementation

import android.content.Context
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.AuthOutcome
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.repository.FordhamUser
import com.fordham.toolbelt.domain.repository.UserId
import com.fordham.toolbelt.domain.repository.EmailAddress
import com.fordham.toolbelt.domain.repository.DisplayName
import com.fordham.toolbelt.domain.repository.PhotoUrl
import com.fordham.toolbelt.domain.repository.IdToken
import com.fordham.toolbelt.domain.repository.IsPremium
import com.fordham.toolbelt.domain.repository.SubscriptionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val context: Context,
    private val subscriptionRepository: Lazy<SubscriptionRepository>
) : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val _currentUser = MutableStateFlow(auth.currentUser?.toFordhamUser(IsPremium(false)))
    override val currentUser: StateFlow<FordhamUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            // Do not touch SubscriptionRepository here — Koin may still be constructing this instance.
            _currentUser.value = firebaseAuth.currentUser?.toFordhamUser(IsPremium(false))
        }
    }

    override suspend fun signInWithGoogle(idToken: IdToken): AuthOutcome {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken.value, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val entitlement = try {
                    subscriptionRepository.value.syncEntitlementFromSupabase()
                } catch (_: Exception) {
                    try {
                        subscriptionRepository.value.entitlement.first()
                    } catch (_: Exception) {
                        subscriptionRepository.value.peekEntitlement()
                    }
                }
                val user = firebaseUser.toFordhamUser(IsPremium(entitlement.isPro))
                _currentUser.value = user
                AuthOutcome.Authenticated(user)
            } else {
                AuthOutcome.Failure(FailureMessage("User is null after sign in"))
            }
        } catch (e: Exception) {
            AuthOutcome.Failure(FailureMessage(e.message ?: "Google Sign-In failed"))
        }
    }

    override suspend fun refreshCurrentUserPremiumStatus() {
        val firebaseUser = auth.currentUser ?: return
        _currentUser.value = firebaseUser.toFordhamUser(
            IsPremium(subscriptionRepository.value.peekEntitlement().isPro)
        )
    }

    override suspend fun getBackendIdToken(): IdToken? =
        auth.currentUser?.getIdToken(false)?.await()?.token?.let(::IdToken)

    override suspend fun signOut(): AuthOutcome {
        return try {
            auth.signOut()
            val webClientId = context.getString(
                context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            )
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(webClientId)
                .requestEmail()
                .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.appdata"))
                .build()
            GoogleSignIn.getClient(context, gso).signOut().await()
            _currentUser.value = null
            AuthOutcome.SignedOut
        } catch (e: Exception) {
            AuthOutcome.Failure(FailureMessage(e.message ?: "Sign out failed"))
        }
    }

    private fun com.google.firebase.auth.FirebaseUser.toFordhamUser(isPremium: IsPremium): FordhamUser =
        FordhamUser(
            id = UserId(uid),
            email = email?.let { EmailAddress(it) },
            displayName = displayName?.let { DisplayName(it) },
            photoUrl = photoUrl?.toString()?.let { PhotoUrl(it) },
            isPremium = isPremium
        )
}
