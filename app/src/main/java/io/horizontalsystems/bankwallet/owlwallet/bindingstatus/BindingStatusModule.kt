package io.horizontalsystems.bankwallet.owlwallet.bindingstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object BindingStatusModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BindingStatusViewModel(
                App.marketKit,
                App.accountManager,
                App.walletManager,
                App.preferenceHelper,
                App.owlTingRepo,
            ) as T
        }
    }
}