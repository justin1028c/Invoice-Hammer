import Foundation
import ComposeApp
import GoogleSignIn
import UIKit

class PlatformActionsBridge: NSObject, IosPlatformActionsBridge {
    
    func signInWithGoogle(onSuccess: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
        guard let rootViewController = UIApplication.shared.windows.first?.rootViewController else {
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
}
