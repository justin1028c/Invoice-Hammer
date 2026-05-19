package com.fordham.toolbelt.util

object NativeSecrets {
    init {
        System.loadLibrary("toolbelt-secrets")
    }

    external fun getGeminiKey(): String
}
