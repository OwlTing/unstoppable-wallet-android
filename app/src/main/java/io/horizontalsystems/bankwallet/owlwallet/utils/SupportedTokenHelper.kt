package io.horizontalsystems.bankwallet.owlwallet.utils

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class SupportedTokenHelper {

    companion object {

        fun filterTokens(tokens: List<Token>): List<Token> {
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
                    -> true

                    "USDC" -> {
                        it.blockchain.type == BlockchainType.Ethereum
                                || it.blockchain.type == BlockchainType.Avalanche
                                || it.blockchain.type == BlockchainType.Stellar
                    }

                    "USDC.E" -> {
                        it.blockchain.type == BlockchainType.Polygon
                    }

                    else -> {
                        false
                    }
                }
            }
        }

        fun filterFullCoin(fullCoins: List<FullCoin>): List<FullCoin> {
            val supportedCoinCodes = arrayOf("ETH", "MATIC", "AVAX", "BTC", "XLM", "USDC", "USDC.E")
            return fullCoins
                .filter { supportedCoinCodes.contains(it.coin.code) }
                .map {
                    FullCoin(
                        it.coin,
                        filterTokens(it.tokens)
                    )
                }
                .filter { it.tokens.isNotEmpty() }
        }

        fun createUSDCWallets(account: Account, blockchains: List<BlockchainType>): List<Wallet> {
            val wallets = mutableListOf<Wallet>()

            wallets.addAll(
                App.marketKit.fullCoins(listOf("usd-coin"))
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
            )
            wallets.addAll(
                App.marketKit.fullCoins(listOf("usd-coin-bridged"))
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
            )
            return wallets
        }

        fun getUSDCTokens(blockchains: List<BlockchainType>): List<Token> {
            val tokenQueries = blockchains
                .map {
                    when (it) {
                        BlockchainType.Ethereum -> {
                            if (BuildConfig.DEBUG) {
                                listOf(
                                    TokenQuery(
                                        it,
                                        TokenType.Eip20("0x07865c6E87B9F70255377e024ace6630C1Eaa37F")
                                    )
                                )
                            } else {
                                listOf(
                                    TokenQuery(
                                        it,
                                        TokenType.Eip20("0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48")
                                    )
                                )
                            }
                        }

                        BlockchainType.Polygon -> {
                            if (BuildConfig.DEBUG) {
                                listOf(
                                    TokenQuery(
                                        it,
                                        TokenType.Eip20("0x0fa8781a83e46826621b3bc094ea2a0212e71b23")
                                    )
                                )
                            } else {
                                listOf(
                                    TokenQuery(
                                        it,
                                        TokenType.Eip20("0x2791bca1f2de4661ed88a30c99a7a9449aa84174")
                                    )
                                )
                            }
                        }

                        BlockchainType.Avalanche -> {
                            if (BuildConfig.DEBUG) {
                                listOf(
                                    TokenQuery(
                                        it,
                                        TokenType.Eip20("0x5425890298aed601595a70AB815c96711a31Bc65")
                                    )
                                )
                            } else {
                                listOf(
                                    TokenQuery(
                                        it,
                                        TokenType.Eip20("0xb97ef9ef8734c71904d8002f8b6bc66dd9c48a6e")
                                    )
                                )
                            }
                        }

                        BlockchainType.Stellar -> {
                            if (BuildConfig.DEBUG) {
                                listOf(
                                    TokenQuery(
                                        it,
                                        TokenType.Alphanum4("GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
                                    )
                                )
                            } else {
                                listOf(
                                    TokenQuery(
                                        it,
                                        TokenType.Alphanum4("GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN")
                                    )
                                )
                            }
                        }

                        else -> {
                            listOf<TokenQuery>()
                        }
                    }
                }
                .flatten()
            return App.marketKit.tokens(tokenQueries)
        }
    }
}