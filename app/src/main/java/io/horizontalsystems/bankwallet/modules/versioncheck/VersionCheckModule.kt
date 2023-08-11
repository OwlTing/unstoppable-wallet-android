package io.horizontalsystems.bankwallet.modules.versioncheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object VersionCheckModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VersionCheckViewModel(App.rateAppManager) as T
        }
    }
}
