package io.horizontalsystems.bankwallet.owlwallet.utils

import io.horizontalsystems.bankwallet.modules.main.MainModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import timber.log.Timber

class MainTabManager {

    private val _currentTabFlow = MutableStateFlow<MainModule.MainNavigation?>(null)
    val currentTabFlow = _currentTabFlow.filterNotNull()

    fun setCurrentTab(tab: MainModule.MainNavigation) {
        Timber.d("setCurrentTab $tab")
        _currentTabFlow.update { tab }
    }
}