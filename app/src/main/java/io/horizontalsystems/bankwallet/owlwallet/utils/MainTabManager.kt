package io.horizontalsystems.bankwallet.owlwallet.utils

import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.core.SingleLiveEvent
import timber.log.Timber

class MainTabManager {

    val setCurrentTabLiveEvent = SingleLiveEvent<MainModule.MainTab>()

    fun setCurrentTab(tab: MainModule.MainTab) {
        Timber.d("setCurrentTab $tab")
        setCurrentTabLiveEvent.postValue(tab)
    }
}