import Foundation
import ComposeApp

final class DriveAuthBridge: IosDriveAuthBridge {
    func getDriveAccessToken() async throws -> String {
        throw NSError(
            domain: "InvoiceHammer.DriveAuth",
            code: 1,
            userInfo: [
                NSLocalizedDescriptionKey: "iOS Google Drive auth is not wired yet. Android Drive sync is available; iOS native Drive auth needs a Mac-side Google Sign-In scope implementation."
            ]
        )
    }
}
