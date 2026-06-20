package com.fordham.toolbelt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.agent.AppTab
import com.fordham.toolbelt.navigation.MainTabNavigation
import com.fordham.toolbelt.ui.MainScreen
import com.fordham.toolbelt.ui.theme.ToolbeltTheme
import com.fordham.toolbelt.ui.viewmodel.*
import com.fordham.toolbelt.domain.usecase.SyncUnpaidInvoiceRemindersUseCase
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.util.VoiceAssistant
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch
import com.fordham.toolbelt.domain.repository.AuthRepository
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(initialTab: AppTab? = null) {
    val platformActions: PlatformActions = koinInject()
    val syncQueueProcessor: com.fordham.toolbelt.data.implementation.SyncQueueProcessor = koinInject()
    val syncUnpaidInvoiceReminders: SyncUnpaidInvoiceRemindersUseCase = koinInject()
    val settingsRepository: com.fordham.toolbelt.domain.repository.SettingsRepository = koinInject()
    val authRepository: AuthRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var isAuthenticated by remember { mutableStateOf(false) }
    var isCheckingBiometrics by remember { mutableStateOf(true) }
    var showBiometricChangedDialog by remember { mutableStateOf(false) }

    val vaultLockedTitle = stringResource(Res.string.vault_locked)
    val vaultLockedSubtitle = stringResource(Res.string.vault_locked_subtitle)
    val vaultLockedSubtitleShort = stringResource(Res.string.vault_locked_subtitle_short)
    val unlockVaultText = stringResource(Res.string.unlock_vault)
    val biometricsChangedTitle = stringResource(Res.string.biometrics_changed_title)
    val biometricsChangedBody = stringResource(Res.string.biometrics_changed_body)
    val okText = stringResource(Res.string.ok)

    LaunchedEffect(Unit) {
        syncQueueProcessor.start()
        val settings = settingsRepository.getBusinessSettings()
        if (settings.biometricLockEnabled && platformActions.isBiometricAvailable()) {
            platformActions.authenticateBiometric(
                title = vaultLockedTitle,
                subtitle = vaultLockedSubtitle,
                onSuccess = { isAuthenticated = true; isCheckingBiometrics = false },
                onError = { error ->
                    if (error == "BIOMETRIC_LOCK_INVALIDATED") {
                        showBiometricChangedDialog = true
                    }
                    isCheckingBiometrics = false
                }
            )
        } else {
            isAuthenticated = true
            isCheckingBiometrics = false
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            syncUnpaidInvoiceReminders.execute()
        }
    }

    ToolbeltTheme(darkTheme = true) {
        if (isCheckingBiometrics) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFFD700))
            }
        } else if (isAuthenticated) {
            val newInvoiceViewModel: NewInvoiceViewModel = koinViewModel()
            val historyViewModel: HistoryViewModel = koinViewModel()
            val receiptsViewModel: ReceiptsViewModel = koinViewModel()
            val statsViewModel: StatsViewModel = koinViewModel()
            val clientsViewModel: ClientsViewModel = koinViewModel()
            val suppliersViewModel: SuppliersViewModel = koinViewModel()
            val agentViewModel: AgentViewModel = koinViewModel()
            val authViewModel: AuthViewModel = koinViewModel()
            val paymentViewModel: PaymentViewModel = koinViewModel()
            val sharedViewModel: SharedViewModel = koinViewModel()
            val subscriptionViewModel: SubscriptionViewModel = koinViewModel()
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val voiceAssistant: VoiceAssistant = koinInject()

            MainScreen(
                newInvoiceViewModel = newInvoiceViewModel,
                historyViewModel = historyViewModel,
                receiptsViewModel = receiptsViewModel,
                statsViewModel = statsViewModel,
                clientsViewModel = clientsViewModel,
                suppliersViewModel = suppliersViewModel,
                agentViewModel = agentViewModel,
                authViewModel = authViewModel,
                paymentViewModel = paymentViewModel,
                sharedViewModel = sharedViewModel,
                subscriptionViewModel = subscriptionViewModel,
                settingsViewModel = settingsViewModel,
                voiceAssistant = voiceAssistant,
                platformActions = platformActions,
                initialPage = initialTab?.pageIndex ?: AppTab.NewInvoice.pageIndex
            )
        } else if (!isCheckingBiometrics) {
            // Biometric Lock Screen (Common UI)
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Lock, 
                        contentDescription = null, 
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(vaultLockedTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            platformActions.authenticateBiometric(
                                title = vaultLockedTitle,
                                subtitle = vaultLockedSubtitleShort,
                                onSuccess = { isAuthenticated = true },
                                onError = { error ->
                                    if (error == "BIOMETRIC_LOCK_INVALIDATED") {
                                        showBiometricChangedDialog = true
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(unlockVaultText)
                    }
                }
            }
        }

        if (showBiometricChangedDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(biometricsChangedTitle, fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Text(
                        biometricsChangedBody,
                        color = Color.LightGray
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val currentSettings = settingsRepository.getBusinessSettings()
                                settingsRepository.saveBusinessSettings(
                                    currentSettings.copy(biometricLockEnabled = false)
                                )
                                authRepository.signOut()
                                isAuthenticated = false
                                isCheckingBiometrics = false
                                showBiometricChangedDialog = false
                            }
                        }
                    ) {
                        Text(okText, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
