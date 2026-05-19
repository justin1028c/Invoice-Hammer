import SwiftUI
import LocalAuthentication
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct VaultLockView: View {
    @Binding var isAuthenticated: Bool
    @State private var errorMessage: String? = nil

    var body: some View {
        ZStack {
            Color(red: 0.06, green: 0.06, blue: 0.06)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                Image(systemName: "lock.fill")
                    .font(.system(size: 64))
                    .foregroundColor(Color(red: 1.0, green: 0.84, blue: 0.0)) // Gold

                Text("Vault Locked")
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.white)

                if let error = errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                }

                Button(action: authenticate) {
                    Text("Unlock Vault")
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .padding()
                        .frame(maxWidth: 200)
                        .background(Color.blue)
                        .cornerRadius(12)
                }
            }
        }
        .onAppear {
            authenticate()
        }
    }

    func authenticate() {
        let context = LAContext()
        var error: NSError?

        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "Authenticate to access Invoice Hammer") { success, authenticationError in
                DispatchQueue.main.async {
                    if success {
                        isAuthenticated = true
                    } else {
                        errorMessage = authenticationError?.localizedDescription ?? "Authentication failed"
                    }
                }
            }
        } else {
            // Biometrics not available, fallback to passcode
            context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: "Unlock to access Invoice Hammer") { success, authenticationError in
                DispatchQueue.main.async {
                    if success {
                        isAuthenticated = true
                    } else {
                        errorMessage = "Authentication required"
                    }
                }
            }
        }
    }
}

struct ContentView: View {
    @State private var isAuthenticated = false

    var body: some View {
        if isAuthenticated {
            ComposeView()
                .ignoresSafeArea(.all, edges: .bottom)
        } else {
            VaultLockView(isAuthenticated: $isAuthenticated)
        }
    }
}
