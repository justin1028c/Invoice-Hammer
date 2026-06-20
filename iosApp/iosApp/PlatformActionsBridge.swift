import Foundation
import ComposeApp
import GoogleSignIn
import UIKit
import BackgroundTasks

class PlatformActionsBridge: NSObject, IosPlatformActionsBridge {

    func signInWithGoogle(onSuccess: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
        guard let rootViewController = InvoiceHammerRootViewController.current else {
            onError("Root view controller not found")
            return
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { result, error in
            if let error = error {
                onError(error.localizedDescription)
                return
            }

            guard let user = result?.user,
                  let idToken = user.idToken?.tokenString else {
                onError("Failed to obtain ID token")
                return
            }

            onSuccess(idToken)
        }
    }

    func getJpegData(image: UIImage, compressionQuality: Double) -> Data? {
        return image.jpegData(compressionQuality: CGFloat(compressionQuality))
    }

    func submitBackgroundSyncTask() -> Bool {
        let request = BGProcessingTaskRequest(identifier: "com.fordham.toolbelt.syncbackup")
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false
        
        do {
            try BGTaskScheduler.shared.submit(request)
            print("Successfully submitted background task: com.fordham.toolbelt.syncbackup")
            return true
        } catch {
            print("Failed to submit background task: \(error.localizedDescription)")
            return false
        }
    }
}
