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
import com.fordham.toolbelt.ui.MainScreen
import com.fordham.toolbelt.ui.theme.ToolbeltTheme
import com.fordham.toolbelt.ui.viewmodel.*
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.util.VoiceAssistant
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val platformActions: PlatformActions = koinInject()
    var isAuthenticated by remember { mutableStateOf(false) }
    var isCheckingBiometrics by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (platformActions.isBiometricAvailable()) {
            platformActions.authenticateBiometric(
                title = "Vault Locked",
                subtitle = "Authenticate to access Invoice Hammer",
                onSuccess = { isAuthenticated = true; isCheckingBiometrics = false },
                onError = { isCheckingBiometrics = false }
            )
        } else {
            isAuthenticated = true
            isCheckingBiometrics = false
        }
    }

    ToolbeltTheme {
        if (isAuthenticated) {
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
                voiceAssistant = voiceAssistant,
                platformActions = platformActions
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
                    Text("Vault Locked", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { 
                            platformActions.authenticateBiometric(
                                title = "Vault Locked", 
                                subtitle = "Authenticate to access",
                                onSuccess = { isAuthenticated = true },
                                onError = {}
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Unlock Vault")
                    }
                }
            }
        }
    }
}
