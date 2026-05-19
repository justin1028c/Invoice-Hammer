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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.native.concurrent.ThreadLocal

class IosUserBridgeDto(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isPremium: Boolean
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

class IosAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<FordhamUser?>(null)
    override val currentUser: StateFlow<FordhamUser?> = _currentUser.asStateFlow()

    init {
        _currentUser.value = IosAuthServiceProvider.bridge?.getCurrentUser()?.toFordhamUser()
    }

    override suspend fun signInWithGoogle(idToken: IdToken): AuthOutcome {
        val bridge = IosAuthServiceProvider.bridge 
            ?: return AuthOutcome.Failure(OperationError("iOS Google Sign-In bridge not initialized"))
            
        return try {
            val dto = bridge.signInWithGoogle(idToken.value)
            val user = dto.toFordhamUser()
            _currentUser.value = user
            AuthOutcome.Authenticated(user)
        } catch (e: Exception) {
            AuthOutcome.Failure(OperationError(e.message ?: "Native iOS Auth Bridge Failed"))
        }
    }

    override suspend fun signOut(): AuthOutcome {
        val bridge = IosAuthServiceProvider.bridge 
            ?: return AuthOutcome.Failure(OperationError("iOS Google Sign-In bridge not initialized"))
            
        return try {
            bridge.signOut()
            _currentUser.value = null
            AuthOutcome.SignedOut
        } catch (e: Exception) {
            AuthOutcome.Failure(OperationError(e.message ?: "Native iOS Sign-Out Failed"))
        }
    }

    private fun IosUserBridgeDto.toFordhamUser() = FordhamUser(
        id = UserId(id),
        email = email?.let { EmailAddress(it) },
        displayName = displayName?.let { DisplayName(it) },
        photoUrl = photoUrl?.let { PhotoUrl(it) },
        isPremium = IsPremium(isPremium)
    )
}
