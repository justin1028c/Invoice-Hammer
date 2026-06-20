package com.fordham.toolbelt.util

sealed class AppLocale(val languageCode: String, val geminiLabel: String) {
    object English : AppLocale("en", "English")
    object Spanish : AppLocale("es", "Spanish")

    companion object {
        fun fromSystem(): AppLocale =
            if (LocaleUtil.getLanguage().startsWith("es")) Spanish else English
    }
}
