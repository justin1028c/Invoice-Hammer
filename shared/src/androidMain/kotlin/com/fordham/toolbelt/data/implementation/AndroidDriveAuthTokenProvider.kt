package com.fordham.toolbelt.data.implementation

import android.content.Context
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.repository.DriveAccessToken
import com.fordham.toolbelt.domain.repository.DriveAuthTokenProvider
import com.fordham.toolbelt.domain.repository.DriveTokenOutcome
import com.fordham.toolbelt.util.AndroidPlatformActions
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidDriveAuthTokenProvider(
    private val context: Context,
    private val platformActions: AndroidPlatformActions
) : DriveAuthTokenProvider {

    override suspend fun getDriveAccessToken(): DriveTokenOutcome = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext DriveTokenOutcome.Failure(
                FailureMessage("Sign in with Google before starting Drive backup.")
            )

        val driveScope = Scope(DRIVE_APPDATA_SCOPE)
        if (!GoogleSignIn.hasPermissions(account, driveScope)) {
            return@withContext DriveTokenOutcome.Failure(
                FailureMessage(
                    "Google Drive access was not granted. Sign out, sign in again, and approve Drive when prompted."
                )
            )
        }

        val accountName = account.account?.name ?: account.email
            ?: return@withContext DriveTokenOutcome.Failure(
                FailureMessage("Google account name is unavailable for Drive backup.")
            )

        fetchToken(accountName)
    }

    private suspend fun fetchToken(accountName: String): DriveTokenOutcome {
        return try {
            val token = GoogleAuthUtil.getToken(
                context,
                accountName,
                "oauth2:$DRIVE_APPDATA_SCOPE"
            )
            DriveTokenOutcome.Success(DriveAccessToken(token))
        } catch (e: UserRecoverableAuthException) {
            val recoveryIntent = e.intent
            val launcher = platformActions.driveAuthRecoveryLauncher
            if (recoveryIntent != null && launcher != null) {
                suspendCoroutine { cont ->
                    launcher(recoveryIntent) {
                        cont.resume(Unit)
                    }
                }
                fetchToken(accountName)
            } else {
                DriveTokenOutcome.Failure(
                    FailureMessage(
                        "Drive permission is required. Sign out, sign in again, and approve Drive backup access."
                    )
                )
            }
        } catch (e: Exception) {
            DriveTokenOutcome.Failure(
                FailureMessage(e.message ?: "Unable to get Google Drive access token.")
            )
        }
    }

    private companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
