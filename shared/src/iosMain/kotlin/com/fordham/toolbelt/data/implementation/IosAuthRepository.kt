package com.fordham.toolbelt.data.implementation

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlin.native.concurrent.ThreadLocal

class IosUserBridgeDto(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)

interface IosAuthBridge {
    suspend fun signInWithGoogle(idToken: String): IosUserBridgeDto
    suspend fun signOut()
    fun getCurrentUser(): IosUserBridgeDto?
}

@ThreadLocal
object IosAuthServiceProvider {
    var bridge: IosAuthBridge? = null
}

class IosAuthRepository(
    private val subscriptionRepository: Lazy<SubscriptionRepository>
) : AuthRepository {
    private val _currentUser = MutableStateFlow<FordhamUser?>(null)
    override val currentUser: StateFlow<FordhamUser?> = _currentUser.asStateFlow()

    init {
        _currentUser.value = IosAuthServiceProvider.bridge?.getCurrentUser()?.toFordhamUser(IsPremium(false))
    }

    override suspend fun signInWithGoogle(idToken: IdToken): AuthOutcome {
        val bridge = IosAuthServiceProvider.bridge
            ?: return AuthOutcome.Failure(FailureMessage("iOS Google Sign-In bridge not initialized"))

        return try {
            val dto = bridge.signInWithGoogle(idToken.value)
            val isPremium = resolvePremiumAfterAuth()
            val user = dto.toFordhamUser(isPremium)
            _currentUser.value = user
            AuthOutcome.Authenticated(user)
        } catch (e: Exception) {
            AuthOutcome.Failure(FailureMessage(e.message ?: "Native iOS Auth Bridge Failed"))
        }
    }

    override suspend fun refreshCurrentUserPremiumStatus() {
        val dto = IosAuthServiceProvider.bridge?.getCurrentUser() ?: return
        _currentUser.value = dto.toFordhamUser(
            IsPremium(subscriptionRepository.value.peekEntitlement().isPro)
        )
    }

    override suspend fun signOut(): AuthOutcome {
        val bridge = IosAuthServiceProvider.bridge
            ?: return AuthOutcome.Failure(FailureMessage("iOS Google Sign-In bridge not initialized"))

        return try {
            bridge.signOut()
            _currentUser.value = null
            AuthOutcome.SignedOut
        } catch (e: Exception) {
            AuthOutcome.Failure(FailureMessage(e.message ?: "Native iOS Sign-Out Failed"))
        }
    }

    private suspend fun resolvePremiumAfterAuth(): IsPremium {
        val entitlement = try {
            subscriptionRepository.value.syncEntitlementFromSupabase()
        } catch (_: Exception) {
            try {
                subscriptionRepository.value.entitlement.first()
            } catch (_: Exception) {
                subscriptionRepository.value.peekEntitlement()
            }
        }
        return IsPremium(entitlement.isPro)
    }

    private fun IosUserBridgeDto.toFordhamUser(isPremium: IsPremium) =
        FordhamUser(
            id = UserId(id),
            email = email?.let { EmailAddress(it) },
            displayName = displayName?.let { DisplayName(it) },
            photoUrl = photoUrl?.let { PhotoUrl(it) },
            isPremium = isPremium
        )
}
