import Foundation
import ComposeApp
import GoogleSignIn
import UIKit

final class DriveAuthBridge: IosDriveAuthBridge {
    func getDriveAccessToken() async throws -> String {
        let scope = "https://www.googleapis.com/auth/drive.appdata"
        guard let presentingViewController = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap({ $0.windows })
            .first(where: { $0.isKeyWindow })?
            .rootViewController else {
            throw driveError("Root view controller not found for Google Drive authorization.")
        }

        guard var user = GIDSignIn.sharedInstance.currentUser else {
            throw driveError("Sign in with Google before starting Drive backup.")
        }

        if !(user.grantedScopes?.contains(scope) ?? false) {
            user = try await withCheckedThrowingContinuation { continuation in
                user.addScopes([scope], presenting: presentingViewController) { updatedUser, error in
                    if let error = error {
                        continuation.resume(throwing: error)
                    } else if let updatedUser = updatedUser {
                        continuation.resume(returning: updatedUser)
                    } else {
                        continuation.resume(throwing: self.driveError("Google Drive authorization did not return a user."))
                    }
                }
            }
        }

        return user.accessToken.tokenString
    }

    private func driveError(_ message: String) -> NSError {
        NSError(
            domain: "InvoiceHammer.DriveAuth",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
    }
}
