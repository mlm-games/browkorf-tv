package org.mlm.browkorftv.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

enum class AppLanguage(val tag: String?, val displayName: String?) {
    System(null, null),
    English("en", "English"),
    German("de", "Deutsch"),
    Persian("fa", "فارسی"),
    Italian("it", "Italiano"),
    Hebrew("iw", "עברית"),
    Polish("pl", "Polski"),
    Russian("ru", "Русский"),
    Ukrainian("uk", "Українська"),
    Vietnamese("vi", "Tiếng Việt"),
    SimplifiedChinese("zh-CN", "简体中文"),
    TraditionalChinese("zh-TW", "繁體中文");

    fun matches(locale: Locale): Boolean {
        val languageTag = tag ?: return false
        return when (this) {
            SimplifiedChinese -> locale.language == "zh" &&
                (locale.script == "Hans" || locale.country == "CN")
            TraditionalChinese -> locale.language == "zh" &&
                (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO"))
            else -> {
                val expectedLanguage = Locale.forLanguageTag(languageTag).language
                locale.language == expectedLanguage ||
                    (expectedLanguage == "iw" && locale.language == "he")
            }
        }
    }

    companion object {
        fun current(): AppLanguage {
            val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: return System
            return entries.firstOrNull { it.matches(locale) } ?: System
        }

        fun select(language: AppLanguage) {
            val locales = language.tag?.let(LocaleListCompat::forLanguageTags)
                ?: LocaleListCompat.getEmptyLocaleList()
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != language.tag.orEmpty()) {
                AppCompatDelegate.setApplicationLocales(locales)
            }
        }
    }
}
