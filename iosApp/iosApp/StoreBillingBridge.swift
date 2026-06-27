import Foundation
import StoreKit
import ComposeApp

@MainActor
final class StoreBillingBridge: NSObject, IosStoreBillingBridge {
    func purchase(productId: String) async -> IosStoreBillingNativeResult {
        do {
            let products = try await Product.products(for: [productId])
            guard let product = products.first else {
                return IosStoreBillingNativeResult(
                    success: false,
                    cancelled: false,
                    productId: nil,
                    transactionId: nil,
                    errorMessage: "Product \(productId) not found in App Store Connect."
                )
            }
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                switch verification {
                case .verified(let transaction):
                    if product.type != .consumable {
                        await transaction.finish()
                    }
                    return IosStoreBillingNativeResult(
                        success: true,
                        cancelled: false,
                        productId: productId,
                        transactionId: String(transaction.id),
                        errorMessage: nil
                    )
                case .unverified:
                    return IosStoreBillingNativeResult(
                        success: false,
                        cancelled: false,
                        productId: nil,
                        transactionId: nil,
                        errorMessage: "Transaction could not be verified."
                    )
                }
            case .userCancelled:
                return IosStoreBillingNativeResult(
                    success: false,
                    cancelled: true,
                    productId: nil,
                    transactionId: nil,
                    errorMessage: nil
                )
            case .pending:
                return IosStoreBillingNativeResult(
                    success: false,
                    cancelled: false,
                    productId: nil,
                    transactionId: nil,
                    errorMessage: "Purchase is pending approval."
                )
            @unknown default:
                return IosStoreBillingNativeResult(
                    success: false,
                    cancelled: false,
                    productId: nil,
                    transactionId: nil,
                    errorMessage: "Unknown purchase result."
                )
            }
        } catch {
            return IosStoreBillingNativeResult(
                success: false,
                cancelled: false,
                productId: nil,
                transactionId: nil,
                errorMessage: error.localizedDescription
            )
        }
    }

    func restorePurchases() async -> IosStoreBillingRestoreResult {
        var activeIds: [String] = []
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result {
                activeIds.append(transaction.productID)
            }
        }
        return IosStoreBillingRestoreResult(
            success: true,
            activeProductIds: activeIds,
            errorMessage: nil
        )
    }

    func finishTransaction(transactionId: String) async -> Bool {
        guard let transactionIdUInt = UInt64(transactionId) else { return false }
        for await result in Transaction.unfinished {
            if case .verified(let transaction) = result {
                if transaction.id == transactionIdUInt {
                    await transaction.finish()
                    return true
                }
            }
        }
        return false
    }
}
