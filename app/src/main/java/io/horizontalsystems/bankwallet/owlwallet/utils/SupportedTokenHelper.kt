package io.horizontalsystems.bankwallet.owlwallet.utils

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token

class SupportedTokenHelper {

    companion object {

        private fun filterTokens(tokens: List<Token>): List<Token> {
            return tokens.filter {
                when (it.coin.code) {
                    "ETH" -> {
                        it.blockchain.type == BlockchainType.Ethereum
                    }

                    "MATIC" -> {
                        it.blockchain.type == BlockchainType.Polygon
                    }

                    "AVAX" -> {
                        it.blockchain.type == BlockchainType.Avalanche
                    }

                    "BTC",
                    "XLM",
                    "USDC" -> true

                    else -> {
                        false
                    }
                }
            }
        }

        fun filterFullCoin(fullCoins: List<FullCoin>): List<FullCoin> {
            val supportedCoinCodes = arrayOf("ETH", "MATIC", "AVAX", "BTC", "XLM", "USDC")
            return fullCoins
                .filter { supportedCoinCodes.contains(it.coin.code) }
                .map {
                    FullCoin(
                        it.coin,
                        filterTokens(it.tokens)
                    )
                }
        }

        fun createUSDCWallets(account: Account, blockchains: List<BlockchainType>): List<Wallet> {
            return App.marketKit.fullCoins(listOf("usd-coin"))
                .map {
                    FullCoin(
                        it.coin,
                        filterTokens(it.tokens),
                    )
                }
                .first()
                .tokens
                .filter { blockchains.contains(it.blockchainType) }
                .map { Wallet(it, account) }
        }
    }
}