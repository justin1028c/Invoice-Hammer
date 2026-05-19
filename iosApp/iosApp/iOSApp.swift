import SwiftUI
import ComposeApp
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        // Initialize Firebase
        FirebaseApp.configure()
        
        // Register iOS-specific bridges before Koin can resolve platform services.
        IosAuthServiceProvider.shared.bridge = FirebaseAuthBridge()
        IosSecurityServiceProvider.shared.bridge = KeyChainSecurityBridge()
        IosPlatformActionsServiceProvider.shared.bridge = PlatformActionsBridge()
        IosDriveAuthServiceProvider.shared.bridge = DriveAuthBridge()

        // Initialize KMP Shared Layer after native bridges are available.
        MainViewControllerKt.initKoinIos()
        
        // Apply Hardware File Protection to the Database
        secureDatabaseFile()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
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
}
