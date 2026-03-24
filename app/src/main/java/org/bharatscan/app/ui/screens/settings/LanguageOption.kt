package org.bharatscan.app.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.bharatscan.app.R

enum class LanguageOption(
    val tag: String?,
    val labelRes: Int,
) {
    ENGLISH("en", R.string.language_english_current),
    MATCH_DEVICE(null, R.string.language_match_device),
    HINDI("hi", R.string.language_hindi),
    BENGALI("bn", R.string.language_bangla);

    fun storedTag(): String = tag ?: SYSTEM_TAG

    companion object {
        private const val SYSTEM_TAG = "system"

        fun fromStoredTag(tag: String?): LanguageOption {
            return when (tag) {
                "en" -> ENGLISH
                "hi" -> HINDI
                "bn" -> BENGALI
                else -> MATCH_DEVICE
            }
        }
    }
}

fun applyAppLanguage(tag: String?) {
    val locales = when (tag) {
        null, "", "system" -> LocaleListCompat.getEmptyLocaleList()
        else -> LocaleListCompat.forLanguageTags(tag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}
