import Foundation
import ComposeApp
import Security

class KeyChainSecurityBridge: IosSecurityBridge {
    
    private let service = "com.fordham.toolbelt.db"
    private let account = "db_passphrase_v1"

    func getDatabasePassphrase() -> String {
        if let existing = readValue(account: account) {
            return existing
        } else {
            let newPassphrase = UUID().uuidString + UUID().uuidString // 72 chars
            saveValue(newPassphrase, account: account)
            return newPassphrase
        }
    }

    func saveSecret(key: String, value: String) {
        saveValue(value, account: key)
    }

    func getSecret(key: String) -> String? {
        readValue(account: key)
    }

    private func saveValue(_ value: String, account: String) {
        let data = value.data(using: .utf8)!
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]
        
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
    }

    private func readValue(account: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var dataTypeRef: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)
        
        if status == errSecSuccess, let data = dataTypeRef as? Data {
            return String(data: data, encoding: .utf8)
        }
        return nil
    }
}
