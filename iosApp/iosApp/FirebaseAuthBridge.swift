import Foundation
import ComposeApp
import FirebaseAuth
import GoogleSignIn

class FirebaseAuthBridge: IosAuthBridge {

    func signInWithGoogle(idToken: String) async throws -> IosUserBridgeDto {
        let credential = GoogleAuthProvider.credential(
            withIDToken: idToken,
            accessToken: nil
        )

        let result = try await Auth.auth().signIn(with: credential)
        return IosUserBridgeDto(
            id: result.user.uid,
            email: result.user.email,
            displayName: result.user.displayName,
            photoUrl: result.user.photoURL?.absoluteString
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
            photoUrl: firebaseUser.photoURL?.absoluteString
        )
    }
}
