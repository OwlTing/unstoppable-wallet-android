package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.CounterType
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Manager
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.VerifyState
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import io.horizontalsystems.bankwallet.owlwallet.utils.MainTabManager
import io.horizontalsystems.bankwallet.owlwallet.utils.PreferenceHelper
import io.horizontalsystems.bankwallet.owlwallet.utils.isWalletSupported
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SnackBarState {
    object Loading : SnackBarState()
    class LogoutSuccess(val msg: String) : SnackBarState()
    class DeleteSuccess(val msg: String) : SnackBarState()
    class Failed(val msg: String) : SnackBarState()
}

class MainSettingsViewModel(
    private val service: MainSettingsService,
    val companyWebPage: String,
    private val repo: OTRepository,
    private val preferenceHelper: PreferenceHelper,
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val mainTabManager: MainTabManager,
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()

    val manageWalletShowAlertLiveData = MutableLiveData(
        shouldShowAlertForManageWallet(
            service.allBackedUp,
            service.hasNonStandardAccount
        )
    )
    val securityCenterShowAlertLiveData = MutableLiveData(!service.isPinSet)
    val aboutAppShowAlertLiveData = MutableLiveData(!service.termsAccepted)
    val wcCounterLiveData = MutableLiveData<CounterType?>(null)
    val baseCurrencyLiveData = MutableLiveData(service.baseCurrency)
    val languageLiveData = MutableLiveData(service.currentLanguageDisplayName)
    val appVersion by service::appVersion

    private var wcSessionsCount = service.walletConnectSessionCount
    private var wc2PendingRequestCount = 0

    val loginState: StateFlow<Boolean> = repo.loginStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = preferenceHelper.getLoginState()
        )

    val verifyState: StateFlow<VerifyState> = repo.verifyStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = preferenceHelper.getVerifyState()
        )

    private val _snackBarState: MutableStateFlow<SnackBarState?> = MutableStateFlow(null)
    val snackBarState: StateFlow<SnackBarState?> = _snackBarState

    init {
        viewModelScope.launch {
            service.termsAcceptedFlow.collect {
                aboutAppShowAlertLiveData.postValue(!it)
            }
        }

        service.backedUpObservable
            .subscribeIO {
                manageWalletShowAlertLiveData.postValue(
                    shouldShowAlertForManageWallet(
                        it,
                        service.hasNonStandardAccount
                    )
                )
            }
            .let { disposables.add(it) }

        service.pinSetObservable
            .subscribeIO { securityCenterShowAlertLiveData.postValue(!it) }
            .let { disposables.add(it) }

        service.baseCurrencyObservable
            .subscribeIO { baseCurrencyLiveData.postValue(it) }
            .let { disposables.add(it) }

        service.walletConnectSessionCountObservable
            .subscribeIO {
                wcSessionsCount = it
                syncCounter()
            }
            .let { disposables.add(it) }

        viewModelScope.launch {
            service.pendingRequestCountFlow.collect {
                wc2PendingRequestCount = it
                syncCounter()
            }
        }
        syncCounter()
        service.start()
    }

    private fun shouldShowAlertForManageWallet(allBackedUp: Boolean, hasNonStandardAccount: Boolean): Boolean {
        return !allBackedUp || hasNonStandardAccount
    }
    // ViewModel

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    fun getWalletConnectSupportState(): WC1Manager.SupportState {
        return service.getWalletConnectSupportState()
    }

    private fun syncCounter() {
        if (wc2PendingRequestCount > 0) {
            wcCounterLiveData.postValue(CounterType.PendingRequestCounter(wc2PendingRequestCount))
        } else if (wcSessionsCount > 0) {
            wcCounterLiveData.postValue(CounterType.SessionCounter(wcSessionsCount))
        } else {
            wcCounterLiveData.postValue(null)
        }
    }

    fun canLogin(): Boolean {
        return accountManager.accounts.isNotEmpty() && walletManager.activeWallets.isNotEmpty()
    }

    fun canBind(): Boolean {
        return accountManager.accounts.isNotEmpty() && walletManager.activeWallets.isNotEmpty() && walletManager.activeWallets.any { isWalletSupported(it) }
    }

    fun doLogout() {
        viewModelScope.launch {
            _snackBarState.value = SnackBarState.Loading
            delay(100)
            repo.logout()
            _snackBarState.value = SnackBarState.LogoutSuccess("Logged out")
            delay(100)
            _snackBarState.value = null
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _snackBarState.value = SnackBarState.Loading
            val result = repo.deleteAccount()
            if (result.succeeded) {
                _snackBarState.value = SnackBarState.DeleteSuccess("Account Deleted")
            } else {
                _snackBarState.value = SnackBarState.Failed("Delete account failed")
            }
            delay(100)
            _snackBarState.value = null
        }
    }

    fun setCurrentTab(tab: MainModule.MainNavigation) {
        mainTabManager.setCurrentTab(tab)
    }
}
