package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import timber.log.Timber
import io.horizontalsystems.core.helpers.LocaleHelper
import java.util.*

class LanguageManager {

    val fallbackLocale by LocaleHelper::fallbackLocale

    var currentLocale: Locale = App.instance.getLocale()
        set(value) {
            field = value
            Timber.d("setLocale: $currentLocale")
            App.instance.setLocale(currentLocale)
        }

    var currentLocaleTag: String
        get() = currentLocale.toLanguageTag()
        set(value) {
            Timber.d("currentLocaleTag: $value")
            currentLocale = Locale.forLanguageTag(value)
            Timber.d("currentLocaleTag: $currentLocale")
        }

    val currentLanguageName: String
        get() {
            return when (currentLocaleTag) {
                "zh-TW" -> traditionalChinese.getDisplayScript(traditionalChinese).replaceFirstChar(Char::uppercase)
                "zh-CN" -> simplifiedChinese.getDisplayScript(simplifiedChinese).replaceFirstChar(Char::uppercase)
                else -> currentLocale.displayLanguage.replaceFirstChar(Char::uppercase)
            }
        }

    val currentLanguage: String
        get() = currentLocale.language

    fun getName(tag: String): String {
        return when (tag) {
            "zh-TW" -> {
                val locale = Locale.Builder().setLanguage("zh").setScript("Hant").build()
                if (currentLocale.language == "en") {
                    return locale.displayLanguage + ", " + locale.displayScript.split(" Han")[0]
                } else {
                    return locale.displayLanguage
                }
            }
            "zh-CN" -> {
                val locale = Locale.Builder().setLanguage("zh").setScript("Hans").build()
                if (currentLocale.language == "en") {
                    return locale.displayLanguage + ", " + locale.displayScript.split(" Han")[0]
                } else {
                    return locale.displayLanguage
                }
            }
            else -> Locale.forLanguageTag(tag)
                .getDisplayName(currentLocale)
                .replaceFirstChar(Char::uppercase)
        }
    }

    fun getNativeName(tag: String): String {
        return when (tag) {
            "zh-TW" -> traditionalChinese.getDisplayScript(traditionalChinese).replaceFirstChar(Char::uppercase)
            "zh-CN" -> simplifiedChinese.getDisplayScript(simplifiedChinese).replaceFirstChar(Char::uppercase)
            else -> {
                val locale = Locale.forLanguageTag(tag)
                locale.getDisplayName(locale).replaceFirstChar(Char::uppercase)
            }
        }
    }

    companion object {
        val traditionalChinese: Locale = Locale.Builder().setLanguage("zh").setScript("Hant").setRegion("TW").build()
        val simplifiedChinese: Locale = Locale.Builder().setLanguage("zh").setScript("Hans").setRegion("CN").build()
    }
}
