import Foundation
import UIKit
import StripeCore
import StripePaymentSheet
import ComposeApp

/// Register before `MainViewControllerKt.initKoinIos()`.
@MainActor
final class StripePaymentBridge: NSObject, IosStripePaymentBridge {
    func presentPaymentSheet(clientSecret: String, stripeAccountId: String) async -> IosStripePaymentNativeResult {
        guard let publishableKey = IosSecurityServiceProvider.shared.bridge?
            .getSecret(key: "stripe_publishable_key")?
            .trimmingCharacters(in: .whitespacesAndNewlines),
              !publishableKey.isEmpty,
              publishableKey.hasPrefix("pk_") else {
            return IosStripePaymentNativeResult(
                success: false,
                cancelled: false,
                errorMessage: "Stripe publishable key is not configured on iOS. Add STRIPE_PUBLISHABLE_KEY to the Xcode build settings or Keychain."
            )
        }

        guard let rootViewController = InvoiceHammerRootViewController.current else {
            return IosStripePaymentNativeResult(
                success: false,
                cancelled: false,
                errorMessage: "Root view controller not found for Stripe PaymentSheet."
            )
        }

        StripeAPI.defaultPublishableKey = publishableKey
        if !stripeAccountId.isEmpty {
            StripeAPI.shared.stripeAccount = stripeAccountId
        }

        var configuration = PaymentSheet.Configuration()
        configuration.merchantDisplayName = "Invoice Hammer"
        configuration.returnURL = "invoicehammer://payment-return"

        let paymentSheet = PaymentSheet(
            paymentIntentClientSecret: clientSecret,
            configuration: configuration
        )

        return await withCheckedContinuation { continuation in
            paymentSheet.present(from: rootViewController) { result in
                switch result {
                case .completed:
                    continuation.resume(
                        returning: IosStripePaymentNativeResult(
                            success: true,
                            cancelled: false,
                            errorMessage: nil
                        )
                    )
                case .canceled:
                    continuation.resume(
                        returning: IosStripePaymentNativeResult(
                            success: false,
                            cancelled: true,
                            errorMessage: nil
                        )
                    )
                case .failed(let error):
                    continuation.resume(
                        returning: IosStripePaymentNativeResult(
                            success: false,
                            cancelled: false,
                            errorMessage: error.localizedDescription
                        )
                    )
                }
            }
        }
    }
}
