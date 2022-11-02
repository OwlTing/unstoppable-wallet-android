package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.ILanguageManager
import java.util.*

class LanguageManager : ILanguageManager {

    override var fallbackLocale: Locale = Locale.ENGLISH

    override var currentLocale: Locale = App.instance.getLocale()
        set(value) {
            field = value

            App.instance.setLocale(currentLocale)
        }

    override var currentLanguage: String
        get() {
            return if (currentLocale.language == "zh" && currentLocale.country == "TW") {
                "tw"
            } else {
                currentLocale.language
            }
        }
        set(value) {
            currentLocale = when (value) {
                "tw" -> traditionalChinese
                "zh" -> simplifiedChinese
                else -> Locale(value)
            }
        }

    override val currentLanguageName: String
        get() {
            return if (currentLocale.language == "zh" && currentLocale.country == "TW") {
                traditionalChinese.getDisplayScript(traditionalChinese).replaceFirstChar(Char::uppercase)
            } else if (currentLocale.language == "zh" && currentLocale.country == "CN") {
                simplifiedChinese.getDisplayScript(simplifiedChinese).replaceFirstChar(Char::uppercase)
            } else {
                currentLocale.displayLanguage.replaceFirstChar(Char::uppercase)
            }
        }

    override fun getName(language: String): String {
        return when (language) {
            "tw" -> Locale.TRADITIONAL_CHINESE.displayName.replaceFirstChar(Char::uppercase)
            "zh" -> Locale.SIMPLIFIED_CHINESE.displayName.replaceFirstChar(Char::uppercase)
            else -> Locale(language).displayLanguage.replaceFirstChar(Char::uppercase)
        }
    }

    override fun getNativeName(language: String): String {
        return when (language) {
            "tw" -> traditionalChinese.getDisplayScript(traditionalChinese).replaceFirstChar(Char::uppercase)
            "zh" -> simplifiedChinese.getDisplayScript(simplifiedChinese).replaceFirstChar(Char::uppercase)
            else -> {
                val locale = Locale(language)
                locale.getDisplayLanguage(locale).replaceFirstChar(Char::uppercase)
            }
        }
    }

    companion object {
        val traditionalChinese: Locale = Locale.Builder().setLanguage("zh").setScript("Hant").setRegion("TW").build()
        val simplifiedChinese: Locale = Locale.Builder().setLanguage("zh").setScript("Hans").setRegion("CN").build()
    }
}
