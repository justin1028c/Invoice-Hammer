import Foundation
import ComposeApp
import FirebaseAuth
import GoogleSignIn

class FirebaseAuthBridge: IosAuthBridge {
    
    func signInWithGoogle(idToken: String) async throws -> IosUserBridgeDto {
        // Exchange the idToken from Google for Firebase credentials
        let credential = GoogleAuthProvider.credential(withIDToken: idToken,
                                                       accessToken: nil) // Access token is optional for ID Token exchange
        
        let result = try await Auth.auth().signIn(with: credential)
        return IosUserBridgeDto(
            id: result.user.uid,
            email: result.user.email,
            displayName: result.user.displayName,
            photoUrl: result.user.photoURL?.absoluteString,
            isPremium: true
        )
    }
    
    func signOut() async throws {
        try Auth.auth().signOut()
        GIDSignIn.sharedInstance.signOut()
    }
    
    func getCurrentUser() -> IosUserBridgeDto? {
        guard let firebaseUser = Auth.auth().currentUser else { return nil }
        return IosUserBridgeDto(
            id: firebaseUser.uid,
            email: firebaseUser.email,
            displayName: firebaseUser.displayName,
            photoUrl: firebaseUser.photoURL?.absoluteString,
            isPremium: true
        )
    }
}
