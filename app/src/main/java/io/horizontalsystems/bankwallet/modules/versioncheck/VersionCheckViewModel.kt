package io.horizontalsystems.bankwallet.modules.versioncheck

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IRateAppManager

class VersionCheckViewModel(private val rateAppManager: IRateAppManager) : ViewModel() {

    fun onBalancePageActive() {
        rateAppManager.onBalancePageActive()
    }

    fun onBalancePageInactive() {
        rateAppManager.onBalancePageInactive()
    }

}
