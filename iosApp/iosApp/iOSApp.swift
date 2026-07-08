import SwiftUI
import ComposeApp
import FirebaseCore
import GoogleSignIn
import UserNotifications
import BackgroundTasks

@main
struct iOSApp: App {
    private static let notificationDelegate = NotificationNavigationDelegate()
    
    @Environment(\.scenePhase) private var scenePhase
    @State private var showPrivacyOverlay = false

    init() {
        // Initialize Firebase with safety guards
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            fatalError("GoogleService-Info.plist is missing from the iOS application bundle.")
        }
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        UNUserNotificationCenter.current().delegate = Self.notificationDelegate
        
        // Register iOS-specific bridges before Koin can resolve platform services.
        IosAuthServiceProvider.shared.bridge = FirebaseAuthBridge()
        let keychainBridge = KeyChainSecurityBridge()
        seedPowerPaySecretsIfConfigured(into: keychainBridge)
        seedSupabaseSecretsIfConfigured(into: keychainBridge)
        seedStripeSecretsIfConfigured(into: keychainBridge)
        seedAppSecretsIfConfigured(into: keychainBridge)
        IosSecurityServiceProvider.shared.bridge = keychainBridge
        IosPlatformActionsServiceProvider.shared.bridge = PlatformActionsBridge()
        IosDriveAuthServiceProvider.shared.bridge = DriveAuthBridge()
        IosStoreBillingServiceProvider.shared.bridge = StoreBillingBridge()
        IosStripePaymentBridgeProvider.shared.bridge = StripePaymentBridge()
        IosLocalLlmEngine.companion.bridge = LocalLlmBridge()

        // Initialize KMP Shared Layer after native bridges are available.
        MainViewControllerKt.initKoinIos()
        
        // Apply Hardware File Protection to the Database
        secureDatabaseFile()

        // Register background sync task
        BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.fordham.toolbelt.syncbackup", using: nil) { task in
            self.handleSyncBackupTask(task: task as! BGProcessingTask)
        }
    }

    private func handleSyncBackupTask(task: BGProcessingTask) {
        scheduleNextBackgroundSync()
        
        let queue = OperationQueue()
        queue.maxConcurrentOperationCount = 1
        
        task.expirationHandler = {
            queue.cancelAllOperations()
        }
        
        MainViewControllerKt.triggerIosBackgroundSync { success in
            task.setTaskCompleted(success: success)
        }
    }
    
    private func scheduleNextBackgroundSync() {
        let request = BGProcessingTaskRequest(identifier: "com.fordham.toolbelt.syncbackup")
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        
        do {
            try BGTaskScheduler.shared.submit(request)
            print("Scheduled next background sync task.")
        } catch {
            print("Could not schedule background sync task: \(error)")
        }
    }
    
    var body: some Scene {
        WindowGroup {
            ZStack {
                ContentView()
                if showPrivacyOverlay {
                    PrivacyShieldView()
                        .transition(.opacity)
                }
            }
            .onOpenURL { url in
                if url.scheme == "invoicehammer" {
                    _ = DeepLinkRouter.shared.dispatch(url: url.absoluteString)
                } else {
                    GIDSignIn.sharedInstance.handle(url)
                }
            }
            .onChange(of: scenePhase) { newPhase in
                withAnimation(.easeInOut(duration: 0.2)) {
                    showPrivacyOverlay = (newPhase == .inactive || newPhase == .background)
                }
            }
        }
    }
    
    private func secureDatabaseFile() {
        let fileManager = FileManager.default
        let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        let dbURL = documentsURL.appendingPathComponent("invoice_hammer.db")
        let walURL = documentsURL.appendingPathComponent("invoice_hammer.db-wal")
        let shmURL = documentsURL.appendingPathComponent("invoice_hammer.db-shm")
        
        let files = [dbURL, walURL, shmURL]
        
        for file in files {
            if fileManager.fileExists(atPath: file.path) {
                do {
                    var attributes = try fileManager.attributesOfItem(atPath: file.path)
                    let protection = attributes[.protectionKey] as? FileProtectionType
                    if protection != .complete {
                        try fileManager.setAttributes([.protectionKey: FileProtectionType.complete], ofItemAtPath: file.path)
                        print("Applied NSFileProtectionComplete to \(file.lastPathComponent)")
                    }
                } catch {
                    print("Failed to secure \(file.lastPathComponent): \(error)")
                }
            }
        }
    }

    private func seedPowerPaySecretsIfConfigured(into bridge: KeyChainSecurityBridge) {
        let mappings = [
            ("PowerPayBaseUrl", "powerpay_base_url"),
            ("PowerPayAppId", "powerpay_app_id"),
            ("PowerPayPublicKey", "powerpay_public_key"),
            ("PowerPaySigningSecret", "powerpay_signing_secret"),
            ("PowerPayEnvironment", "powerpay_env"),
            ("PowerPayApiVariant", "powerpay_api_variant")
        ]

        for (plistKey, keychainKey) in mappings {
            guard let value = Bundle.main.object(forInfoDictionaryKey: plistKey) as? String else { continue }
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty, !trimmed.hasPrefix("$(") else { continue }
            bridge.saveSecret(key: keychainKey, value: trimmed)
        }
    }

    private func seedStripeSecretsIfConfigured(into bridge: KeyChainSecurityBridge) {
        let mappings = [
            ("StripePublishableKey", "stripe_publishable_key"),
            ("StripePaymentBackendUrl", "stripe_payment_backend_url"),
            ("StripeConnectOnboardingUrl", "stripe_connect_onboarding_url"),
        ]

        for (plistKey, keychainKey) in mappings {
            guard let value = Bundle.main.object(forInfoDictionaryKey: plistKey) as? String else { continue }
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty, !trimmed.hasPrefix("$(") else { continue }
            bridge.saveSecret(key: keychainKey, value: trimmed)
        }
    }

    private func seedAppSecretsIfConfigured(into bridge: KeyChainSecurityBridge) {
        let mappings = [
            ("ForemanGeminiBackendUrl", "foreman_gemini_backend_url"),
            ("ForemanBackendApiKey", "foreman_backend_api_key"),
            ("GeminiModelName", "gemini_model_name"),
            ("GoogleClientId", "google_client_id")
        ]

        for (plistKey, keychainKey) in mappings {
            guard let value = Bundle.main.object(forInfoDictionaryKey: plistKey) as? String else { continue }
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty, !trimmed.hasPrefix("$(") else { continue }
            bridge.saveSecret(key: keychainKey, value: trimmed)
        }
    }

    private func seedSupabaseSecretsIfConfigured(into bridge: KeyChainSecurityBridge) {
        let mappings = [
            ("SupabaseUrl", "supabase_url"),
            ("SupabaseAnonKey", "supabase_anon_key"),
            ("SupabaseServiceRoleKey", "supabase_service_role_key"),
            ("SupabaseSchema", "supabase_schema")
        ]

        for (plistKey, keychainKey) in mappings {
            guard let value = Bundle.main.object(forInfoDictionaryKey: plistKey) as? String else { continue }
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty, !trimmed.hasPrefix("$(") else { continue }
            bridge.saveSecret(key: keychainKey, value: trimmed)
        }
    }
}

struct VisualEffectView: UIViewRepresentable {
    let effect: UIVisualEffect
    
    func makeUIView(context: Context) -> UIVisualEffectView {
        UIVisualEffectView(effect: effect)
    }
    
    func updateUIView(_ uiView: UIVisualEffectView, context: Context) {
        uiView.effect = effect
    }
}

struct PrivacyShieldView: View {
    var body: some View {
        ZStack {
            VisualEffectView(effect: UIBlurEffect(style: .systemUltraThinMaterialDark))
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 64))
                    .foregroundColor(.white.opacity(0.85))
                
                Text("Invoice Hammer")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Text("Secure Billing Environment")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.6))
            }
        }
    }
}

final class NotificationNavigationDelegate: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let target = response.notification.request.content.userInfo["NAVIGATE_TO"] as? String
        IosNotificationNavigationKt.handleIosNotificationNavigationTarget(target: target)
        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}
