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
    BENGALI("bn", R.string.language_bangla),
    TELUGU("te", R.string.language_telugu),
    MARATHI("mr", R.string.language_marathi),
    TAMIL("ta", R.string.language_tamil),
    URDU("ur", R.string.language_urdu),
    GUJARATI("gu", R.string.language_gujarati),
    KANNADA("kn", R.string.language_kannada),
    MALAYALAM("ml", R.string.language_malayalam),
    ODIA("or", R.string.language_odia),
    PUNJABI("pa", R.string.language_punjabi);

    fun storedTag(): String = tag ?: SYSTEM_TAG

    companion object {
        private const val SYSTEM_TAG = "system"

        fun fromStoredTag(tag: String?): LanguageOption {
            return when (tag) {
                "en" -> ENGLISH
                "hi" -> HINDI
                "bn" -> BENGALI
                "te" -> TELUGU
                "mr" -> MARATHI
                "ta" -> TAMIL
                "ur" -> URDU
                "gu" -> GUJARATI
                "kn" -> KANNADA
                "ml" -> MALAYALAM
                "or" -> ODIA
                "pa" -> PUNJABI
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
