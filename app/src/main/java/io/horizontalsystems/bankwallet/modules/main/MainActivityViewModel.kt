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
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val owlTingRepo: OTRepository,
    private val preferenceHelper: PreferenceHelper,
) : ViewModel() {
    private val disposables = CompositeDisposable()

    val openWalletConnectRequestLiveEvent = SingleLiveEvent<WC2Request>()

    private var isLoggedIn = preferenceHelper.getLoginState()

    init {
        viewModelScope.launch {
            owlTingRepo.loginStateFlow().collect {
                Timber.d("loginState: $it")
                isLoggedIn = it
                logAllWallets()
            }
        }

        viewModelScope.launch {
            logAllWallets()
        }

        viewModelScope.launch {
            walletManager.activeWalletsUpdatedObservable.asFlow().collect {
                logAllWallets()
            }
        }

        wcSessionManager.pendingRequestObservable
            .subscribeIO {
                openWalletConnectRequestLiveEvent.postValue(it)
            }.let {
                disposables.add(it)
            }
    }

    private suspend fun logAllWallets() {
        Timber.d("before logAllWallets $isLoggedIn")
        if (!isLoggedIn) {
            return
        }

        Timber.d("logAllWallets")
        var needRetry: Boolean
        do {
            needRetry = false
            val otWallets = mutableListOf<OTWallet>()
            val accounts = accountManager.accounts
            accounts.forEach { account ->
                val wallets = walletManager.getWallets(account)
                otWallets.addAll(
                    wallets.map { wallet ->
                        Timber.d("wallet: $wallet")
                        val address = getAddress(account, wallet)
                        OTWallet(
                            address = address,
                            currency = getCurrency(wallet.token.blockchainType),
                            symbol = wallet.token.coin.code,
                            decimals = wallet.token.decimals.toString()
                        )
                    }
                )
            }
            if (otWallets.isNotEmpty()) {

                val result = owlTingRepo.syncWallets(SyncWalletsRequest(otWallets))

                needRetry = if (result.succeeded) {
                    false
                } else {
                    if ((result as OTResult.Error).exception is RefreshTokenExpiredException) {
                        false
                    } else {
                        delay(1000)
                        true
                    }
                }
            }
        } while (needRetry && isLoggedIn)
    }

    override fun onCleared() {
        disposables.clear()
    }

    private fun getAddress(account: Account, wallet: Wallet): String {
        return when (account.type) {
            is AccountType.Address -> account.type.address
            is AccountType.Mnemonic -> {
                val seed = Mnemonic().toSeed(account.type.words, account.type.passphrase)
                val privateKey =
                    EthereumKit.privateKey(seed, getChain(wallet.token.blockchainType))
                val publicKey =
                    CryptoUtils.ecKeyFromPrivate(privateKey).publicKeyPoint.getEncoded(false)
                        .drop(1).toByteArray()
                Address(CryptoUtils.sha3(publicKey).takeLast(20).toByteArray()).eip55
            }
            else -> ""
        }
    }

    private fun getChain(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Ethereum -> Chain.Ethereum
        BlockchainType.Polygon -> Chain.Polygon
        BlockchainType.Avalanche -> Chain.Avalanche
        else -> throw IllegalArgumentException("Unsupported blockchain type $blockchainType")
    }

    private fun getCurrency(blockchainType: BlockchainType) = when(blockchainType) {
        BlockchainType.Ethereum -> "ETH"
        BlockchainType.Polygon -> "MATIC"
        BlockchainType.Avalanche -> "AVAX"
        else -> throw IllegalArgumentException("Unsupported blockchain type $blockchainType")
    }
}
