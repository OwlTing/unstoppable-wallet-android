package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Manager
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import io.horizontalsystems.bankwallet.owlwallet.utils.PreferenceHelper
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SnackBarState {
    object Loading : SnackBarState()
    class LogoutSuccess(val msg: String) : SnackBarState()
    class SyncSuccess(val msg: String) : SnackBarState()
    class Failed(val msg: String) : SnackBarState()
}

class MainSettingsViewModel(
    private val service: MainSettingsService,
    val companyWebPage: String,
    private val repo: OTRepository,
    preferenceHelper: PreferenceHelper,
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()

    val manageWalletShowAlertLiveData = MutableLiveData(!service.allBackedUp)
    val securityCenterShowAlertLiveData = MutableLiveData(!service.isPinSet)
    val aboutAppShowAlertLiveData = MutableLiveData(!service.termsAccepted)
    val walletConnectSessionCountLiveData = MutableLiveData(service.walletConnectSessionCount)
    val baseCurrencyLiveData = MutableLiveData(service.baseCurrency)
    val languageLiveData = MutableLiveData(service.currentLanguageDisplayName)
    val appVersion by service::appVersion

    val loginState: StateFlow<Boolean> = repo.loginStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = preferenceHelper.getLoginState()
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
            .subscribeIO { manageWalletShowAlertLiveData.postValue(!it) }
            .let { disposables.add(it) }

        service.pinSetObservable
            .subscribeIO { securityCenterShowAlertLiveData.postValue(!it) }
            .let { disposables.add(it) }

        service.baseCurrencyObservable
            .subscribeIO { baseCurrencyLiveData.postValue(it) }
            .let { disposables.add(it) }

        service.walletConnectSessionCountObservable
            .subscribeIO { walletConnectSessionCountLiveData.postValue(it) }
            .let { disposables.add(it) }

        service.start()
    }

    // ViewModel

    fun getWallets() {
        viewModelScope.launch {
            _snackBarState.value = SnackBarState.Loading
            val result = repo.getWallets()
            if (result.succeeded) {
                _snackBarState.value = SnackBarState.SyncSuccess("Sync success")
            } else {
                _snackBarState.value = SnackBarState.Failed((result as OTResult.Error).exception.message!!)
            }
        }
    }

    fun doLogout() {
        viewModelScope.launch {
            _snackBarState.value = SnackBarState.Loading
            repo.doLogout()
            _snackBarState.value = SnackBarState.LogoutSuccess("Logged out")
        }
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    fun getWalletConnectSupportState(): WC1Manager.SupportState {
        return service.getWalletConnectSupportState()
    }
}
