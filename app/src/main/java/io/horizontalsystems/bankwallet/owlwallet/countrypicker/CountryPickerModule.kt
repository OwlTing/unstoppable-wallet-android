package io.horizontalsystems.bankwallet.owlwallet.countrypicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object CountryPickerModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CountryPickerViewModel(
                App.languageManager,
                App.owlTingRepo,
                App.preferenceHelper,
            ) as T
        }
    }
}