package com.fordham.toolbelt.util

import java.util.Locale

actual object LocaleUtil {
    actual fun getLanguage(): String {
        val app = AndroidAppContext.application
        if (app != null) {
            return app.resources.configuration.locales[0].language
        }
        return Locale.getDefault().language
    }
}
