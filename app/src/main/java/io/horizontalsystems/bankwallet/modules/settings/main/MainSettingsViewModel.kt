package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Manager
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.RefreshTokenExpiredException
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.OTWallet
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.SyncWalletsRequest
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import io.horizontalsystems.bankwallet.owlwallet.utils.PreferenceHelper
import io.horizontalsystems.bankwallet.owlwallet.utils.WalletSyncHelper
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

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
    private val walletSyncHelper: WalletSyncHelper
) : ViewModel() {

    init {
        Timber.d("init")
    }

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

    fun syncWallets() {
        viewModelScope.launch {
            _snackBarState.value = SnackBarState.Loading
            delay(100)
            val result = walletSyncHelper.sync()
            if (result.succeeded) {
                _snackBarState.value = SnackBarState.SyncSuccess("Sync success")
            } else {
                _snackBarState.value =
                    SnackBarState.Failed((result as OTResult.Error).exception.message!!)
            }
            delay(100)
            _snackBarState.value = null
        }
    }

    fun doLogout() {
        viewModelScope.launch {
            _snackBarState.value = SnackBarState.Loading
            delay(100)
            repo.doLogout()
            _snackBarState.value = SnackBarState.LogoutSuccess("Logged out")
            delay(100)
            _snackBarState.value = null
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
