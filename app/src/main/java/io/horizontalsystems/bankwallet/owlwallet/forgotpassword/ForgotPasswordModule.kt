package io.horizontalsystems.bankwallet.owlwallet.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.owlwallet.login.LoginViewModel

object ForgotPasswordModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ForgotPasswordViewModel(
                App.owlTingRepo,
            ) as T
        }
    }
}