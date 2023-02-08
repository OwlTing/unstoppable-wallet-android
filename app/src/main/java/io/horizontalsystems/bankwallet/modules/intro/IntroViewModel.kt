package io.horizontalsystems.bankwallet.modules.intro

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.LanguageManager

class IntroViewModel(
    private val localStorage: ILocalStorage,
    private val languageManager: LanguageManager,
): ViewModel() {

    val termsOfUseAgreement = mutableStateOf(false)

    val slides = listOf(
        IntroModule.IntroSliderData(
            R.string.Intro_Wallet_Screen2Title,
            R.string.Intro_Wallet_Screen2Description,
            R.drawable.ic_independence_light,
            R.drawable.ic_independence
        ),
        IntroModule.IntroSliderData(
            R.string.Intro_Wallet_Screen3Title,
            R.string.Intro_Wallet_Screen3Description,
            R.drawable.ic_knowledge_light,
            R.drawable.ic_knowledge
        ),
        IntroModule.IntroSliderData(
            R.string.Intro_Wallet_Screen4Title,
            R.string.Intro_Wallet_Screen4Description,
            R.drawable.ic_privacy_light,
            R.drawable.ic_privacy
        ),
    )

    fun onStartClicked() {
        localStorage.mainShowedOnce = true
    }

    fun getTermsOfUseUrl(): String {
        return when (languageManager.currentLanguage) {
            "zh-TW", "zh-CN" -> "https://www.owlting.com/owlpay/wallet-terms?lang=zh_tw"
            else -> "https://www.owlting.com/owlpay/wallet-terms?lang=en"
        }
    }
}
