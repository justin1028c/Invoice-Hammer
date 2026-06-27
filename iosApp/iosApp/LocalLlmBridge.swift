import Foundation
import Llama
import ComposeApp

public class LocalLlmBridge: NSObject, IosLocalLlmBridge {
    
    public override init() {
        super.init()
    }
    
    public func isModelDownloaded() -> Bool {
        return false
    }
    
    public func getDownloadProgress() -> Float {
        return 0.0
    }
    
    public func isDownloading() -> Bool {
        return false
    }
    
    public func startDownload(onProgress: @escaping (Double) -> Void, onComplete: @escaping (Bool) -> Void) {
        onComplete(false)
    }
    
    public func deleteModel() -> Bool {
        return false
    }
    
    public func isSupported() -> Bool {
        return false
    }
    
    public func generateText(prompt: String, onResult: @escaping (String?) -> Void) {
        onResult(nil)
    }
}
