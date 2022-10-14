package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Request
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.RefreshTokenExpiredException
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.OTWallet
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.SyncWalletsRequest
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import io.horizontalsystems.bankwallet.owlwallet.utils.PreferenceHelper
import io.horizontalsystems.bankwallet.owlwallet.utils.WalletSyncHelper
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber

class MainActivityViewModel(
    wcSessionManager: WC2SessionManager,
    private val walletManager: IWalletManager,
    private val owlTingRepo: OTRepository,
    preferenceHelper: PreferenceHelper,
    private val walletSyncHelper: WalletSyncHelper,
) : ViewModel() {
    private val disposables = CompositeDisposable()

    val openWalletConnectRequestLiveEvent = SingleLiveEvent<WC2Request>()

    private var isLoggedIn = preferenceHelper.getLoginState()

    init {
        viewModelScope.launch {
            owlTingRepo.loginStateFlow().collect {
                Timber.d("loginState: $it")
                isLoggedIn = it
                if (isLoggedIn) {
                    walletSyncHelper.syncWithRetry()
                }
            }
        }

        viewModelScope.launch {
            if (isLoggedIn) {
                walletSyncHelper.syncWithRetry()
            }
        }

        viewModelScope.launch {
            walletManager.activeWalletsUpdatedObservable.asFlow().collect {
                if (isLoggedIn) {
                    walletSyncHelper.syncWithRetry()
                }
            }
        }

        wcSessionManager.pendingRequestObservable
            .subscribeIO {
                openWalletConnectRequestLiveEvent.postValue(it)
            }.let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        disposables.clear()
    }
}
