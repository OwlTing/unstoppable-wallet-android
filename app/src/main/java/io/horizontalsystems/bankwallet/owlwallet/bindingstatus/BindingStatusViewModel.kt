package io.horizontalsystems.bankwallet.owlwallet.bindingstatus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.RefreshTokenExpiredException
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.AmlChainRegisterChain
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.AmlChainRegisterRequest
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.Chain
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import io.horizontalsystems.bankwallet.owlwallet.utils.PreferenceHelper
import io.horizontalsystems.bankwallet.owlwallet.utils.getBlockchainTypeByNetwork
import io.horizontalsystems.bankwallet.owlwallet.utils.isWalletSupported
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import timber.log.Timber

enum class BindingStatus {
    UNBIND, BOUND, ANOTHER_WALLET
}

data class StatusItem(
    val wallet: Wallet,
    val status: BindingStatus
)

sealed class ActionState {
    object Loading : ActionState()
    object UnbindAllSuccess : ActionState()
    object Failed : ActionState()
    object Expired : ActionState()
}

class BindingStatusViewModel(
    private val marketKit: MarketKitWrapper,
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val prefs: PreferenceHelper,
    private val repo: OTRepository,
) : ViewModel() {

    var actionState by mutableStateOf<ActionState?>(null)
        private set

    val items = mutableStateListOf<StatusItem>()

    val fullCoins: MutableList<FullCoin> = mutableListOf()

    private val boundWallets: MutableList<Wallet> = mutableListOf()

    val hasWalletToUnbind = mutableStateOf(false)

    init {
        viewModelScope.launch {
            fullCoins.addAll(fetchFullCoins())
            val account = accountManager.activeAccount
            if (account != null) {
                val wallets = walletManager.getWallets(account).filter {
                    isWalletSupported(it)
                }

                if (wallets.isNotEmpty()) {
                    val userChains: MutableList<Chain> =
                        prefs.getUserMeta()?.ssoUserChains?.toMutableList() ?: mutableListOf()

                    wallets.forEach { wallet ->
                        var index = -1
                        for (i in 0..userChains.size) {
                            try {
                                val userChain = userChains[i]
                                val blockchainType =
                                    getBlockchainTypeByNetwork(userChain.chainNetwork)

                                if (userChain.chainAddress == App.getReceiveAddress(wallet)
                                    && blockchainType == wallet.token.blockchainType
                                    && userChain.chainAsset == wallet.coin.code
                                ) {
                                    items.add(
                                        StatusItem(
                                            wallet,
                                            if (userChain.chainIsBinding == 1) BindingStatus.BOUND else BindingStatus.UNBIND
                                        )
                                    )
                                    if (userChain.chainIsBinding == 1) {
                                        hasWalletToUnbind.value = true
                                        boundWallets.add(wallet)
                                    }
                                    index = i
                                }
                            } catch (_: Exception) {
                            }
                        }
                        if (index != -1) {
                            userChains.removeAt(index)
                            index = -1
                        } else {
                            items.add(StatusItem(wallet, BindingStatus.UNBIND))
                        }
                    }

                    userChains.forEach {
                        val token =
                            fetchToken(it.chainAsset, getBlockchainTypeByNetwork(it.chainNetwork))

                        if (token != null) {
                            items.add(
                                StatusItem(
                                    Wallet(token, account),
                                    BindingStatus.ANOTHER_WALLET
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun unbindAll() {
        viewModelScope.launch {
            actionState = ActionState.Loading
            val unbindTargets: MutableList<AmlChainRegisterChain> = mutableListOf()
            val account = accountManager.activeAccount
            if (account != null) {
                val wallets = walletManager.getWallets(account)

                if (wallets.isNotEmpty()) {
                    val userMetaResult = repo.getUserMeta()
                    if (!userMetaResult.succeeded) {
                        if ((userMetaResult as OTResult.Error).exception is RefreshTokenExpiredException) {
                            actionState = ActionState.Expired
                            return@launch
                        }
                    }

                    val userChains: MutableList<Chain> =
                        prefs.getUserMeta()?.ssoUserChains?.toMutableList() ?: mutableListOf()

                    wallets.forEach { wallet ->
                        var index = -1
                        for (i in 0..userChains.size) {
                            try {
                                val userChain = userChains[i]
                                val blockchainType =
                                    getBlockchainTypeByNetwork(userChain.chainNetwork)

                                if (userChain.chainAddress == App.getReceiveAddress(wallet)
                                    && blockchainType == wallet.token.blockchainType
                                    && userChain.chainAsset == wallet.coin.code
                                    && userChain.chainIsBinding == 1
                                ) {
                                    unbindTargets.add(
                                        AmlChainRegisterChain(
                                            userChain.chainAddress,
                                            userChain.chainNetwork,
                                            userChain.chainAsset,
                                            0
                                        )
                                    )
                                }
                            } catch (_: Exception) {
                            }
                        }
                        if (index != -1) {
                            userChains.removeAt(index)
                            index = -1
                        }
                    }
                    val result = repo.amlChainRegister(AmlChainRegisterRequest(unbindTargets))
                    if (result.succeeded) {
                        actionState = ActionState.UnbindAllSuccess
                        return@launch
                    } else {
                        val exception = (result as OTResult.Error).exception
                        actionState = if (exception is RefreshTokenExpiredException) {
                            ActionState.Expired
                        } else {
                            ActionState.Failed
                        }
                    }
                }
            }
        }
    }

    private fun fetchFullCoins(): List<FullCoin> {
        return marketKit.fullCoins(
            listOf(
                "ethereum",
                "matic-network",
                "avalanche-2",
                "usd-coin",
                "stellar",
            )
        ).map {
            when (it.coin.code) {
                "ETH" -> {
                    FullCoin(
                        it.coin,
                        it.tokens.filter { token -> token.blockchainType == BlockchainType.Ethereum }
                    )
                }
                "MATIC" -> {
                    FullCoin(
                        it.coin,
                        it.tokens.filter { token -> token.blockchainType == BlockchainType.Polygon }
                    )
                }
                "AVAX" -> {
                    FullCoin(
                        it.coin,
                        it.tokens.filter { token -> token.blockchainType == BlockchainType.Avalanche }
                    )
                }
                "XLM" -> {
                    FullCoin(
                        it.coin,
                        it.tokens.filter { token -> token.blockchainType == BlockchainType.Stellar }
                    )
                }
                "USDC" -> {
                    FullCoin(
                        it.coin,
                        it.tokens.filter { token ->
                            token.blockchainType == BlockchainType.Ethereum
                                    || token.blockchainType == BlockchainType.Polygon
                                    || token.blockchainType == BlockchainType.Avalanche
                                    || token.blockchainType == BlockchainType.Stellar
                        }
                    )
                }
                else -> it
            }
        }
    }

    private fun fetchToken(code: String, blockchainType: BlockchainType): Token? {
        return when (code) {
            "ETH" -> {
                fullCoins.firstOrNull {
                    it.coin.code == "ETH"
                }?.tokens?.get(0)
            }
            "MATIC" -> {
                fullCoins.firstOrNull {
                    it.coin.code == "MATIC"
                }?.tokens?.get(0)
            }
            "AVAX" -> {
                fullCoins.firstOrNull {
                    it.coin.code == "AVAX"
                }?.tokens?.get(0)
            }
            "XLM" -> {
                fullCoins.firstOrNull {
                    it.coin.code == "XLM"
                }?.tokens?.get(0)
            }
            "USDC" -> {
                fullCoins.firstOrNull {
                    it.coin.code == "USDC"
                }?.tokens?.firstOrNull {
                    it.blockchainType == blockchainType
                }
            }
            else -> null
        }
    }
}
