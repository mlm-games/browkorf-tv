package org.mlm.browkorftv.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.mlmgames.settings.core.locale.AppLanguage
import java.util.Locale

fun applyAppLocale(languageTag: String?) {
    val target = languageTag
        ?.let { LocaleListCompat.forLanguageTags(it) }
        ?: LocaleListCompat.getEmptyLocaleList()
    if (AppCompatDelegate.getApplicationLocales().toLanguageTags() == target.toLanguageTags()) return
    AppCompatDelegate.setApplicationLocales(target)
}

fun currentAppLanguage(): AppLanguage {
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: return AppLanguage.System
    return AppLanguage.entries.firstOrNull { it.matches(locale) } ?: AppLanguage.System
}

private fun AppLanguage.matches(locale: Locale): Boolean {
    val tag = languageTag ?: return false
    return when (this) {
        AppLanguage.ChineseSimplified -> locale.language == "zh" &&
            (locale.script == "Hans" || locale.country == "CN")

        AppLanguage.ChineseTraditional -> locale.language == "zh" &&
            (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO"))

        else -> locale.language == Locale.forLanguageTag(tag).language
    }
}
