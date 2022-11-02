package io.horizontalsystems.bankwallet.owlwallet.utils

import io.horizontalsystems.bankwallet.core.App.Companion.preferenceHelper
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.RefreshTokenExpiredException
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTRepository
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.OTWallet
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.SyncWalletsRequest
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import timber.log.Timber

class WalletSyncHelper(
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val repo: OTRepository,
    private val preferenceHelper: PreferenceHelper,
) {

    suspend fun sync(): OTResult<Boolean> {
        Timber.d("syncAllWallets")
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
        return if (otWallets.isNotEmpty()) {
            repo.syncWallets(SyncWalletsRequest(otWallets))
        } else {
            OTResult.Success(true)
        }
    }

    suspend fun syncWithRetry(): OTResult<Boolean> {
        var retryCount = 0
        var result: OTResult<Boolean>
        do {
            result = sync()
            if (result.succeeded) {
                return result
            } else {
                delay(1000)
                retryCount++
            }
        } while (preferenceHelper.getLoginState() && retryCount < 10)
        return result
    }

    private fun getAddress(account: Account, wallet: Wallet): String {
        return when (account.type) {
            is AccountType.EvmAddress -> account.type.address
            is AccountType.Mnemonic -> {
                val seed = Mnemonic().toSeed(account.type.words, account.type.passphrase)
                val privateKey =
                    Signer.privateKey(seed, getChain(wallet.token.blockchainType))
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

    private fun getCurrency(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Ethereum -> "ETH"
        BlockchainType.Polygon -> "MATIC"
        BlockchainType.Avalanche -> "AVAX"
        else -> throw IllegalArgumentException("Unsupported blockchain type $blockchainType")
    }
}