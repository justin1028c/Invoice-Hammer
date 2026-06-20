package com.fordham.toolbelt.util

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual object LocaleUtil {
    actual fun getLanguage(): String {
        return NSLocale.currentLocale.languageCode ?: "en"
    }
}
