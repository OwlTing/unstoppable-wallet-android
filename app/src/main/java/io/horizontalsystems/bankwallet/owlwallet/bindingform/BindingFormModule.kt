package io.horizontalsystems.bankwallet.owlwallet.bindingform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object BindingFormModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BindingFormViewModel(
                App.accountManager,
                App.walletManager,
                App.preferenceHelper,
                App.owlTingRepo,
            ) as T
        }
    }
}