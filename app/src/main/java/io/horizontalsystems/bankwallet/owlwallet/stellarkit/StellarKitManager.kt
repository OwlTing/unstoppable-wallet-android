package io.horizontalsystems.bankwallet.owlwallet.stellarkit

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.BackgroundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class StellarKitManager(
        backgroundManager: BackgroundManager,
) : BackgroundManager.Listener {

    private val network = if (BuildConfig.DEBUG) Network.Testnet else Network.Mainnet

    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    var stellarKitWrapper: StellarKitWrapper? = null
        private set(value) {
            field = value

//            _kitStartedFlow.update { value != null }
        }

    var stellarUSDCKitWrapper: StellarKitWrapper? = null
        private set(value) {
            field = value

//            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set

    init {
        backgroundManager.registerListener(this)
    }

    @Synchronized
    fun getStellarKitWrapper(account: Account): StellarKitWrapper {
        if (this.stellarKitWrapper != null && currentAccount != account) {
            stopKit()
        }

        if (this.stellarKitWrapper == null) {
            val accountType = account.type
            this.stellarKitWrapper = when (accountType) {
                is AccountType.Mnemonic -> {
                    createKitInstance(accountType, account, "stellar")
                }

                else -> throw UnsupportedAccountException()
            }
            startKit()
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.stellarKitWrapper!!
    }

    @Synchronized
    fun getStellarUSDCKitWrapper(account: Account): StellarKitWrapper {
        if (this.stellarUSDCKitWrapper != null && currentAccount != account) {
            stopKit()
        }

        if (this.stellarUSDCKitWrapper == null) {
            val accountType = account.type
            this.stellarUSDCKitWrapper = when (accountType) {
                is AccountType.Mnemonic -> {
                    createKitInstance(accountType, account, "usd-coin")
                }

                else -> throw UnsupportedAccountException()
            }
            startUSDCKit()
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.stellarUSDCKitWrapper!!
    }

    private fun createKitInstance(
            accountType: AccountType.Mnemonic,
            account: Account,
            coinUid: String,
    ): StellarKitWrapper {
        val kit = StellarKit.getInstance(
                application = App.instance,
                seed = accountType.seed,
                network = network,
                coinUid = coinUid,
                walletId = account.id,
        )

        return StellarKitWrapper(kit)
    }

    private fun stopKit() {
        stellarKitWrapper?.stellarKit?.stop()
        stellarUSDCKitWrapper?.stellarKit?.stop()
        stellarKitWrapper = null
        stellarUSDCKitWrapper = null
        currentAccount = null
    }

    private fun startKit() {
        stellarKitWrapper?.stellarKit?.start()
    }

    private fun startUSDCKit() {
        stellarUSDCKitWrapper?.stellarKit?.start()
    }

    override fun willEnterForeground() {
        super.willEnterForeground()
    }

    override fun didEnterBackground() = Unit
}

class StellarKitWrapper(val stellarKit: StellarKit)