package io.horizontalsystems.bankwallet.owlwallet.utils

import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.BlockchainType

val passwordRegex = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}\$")

fun getLangParam(language: String): String {
    return when (language) {
        "zh-TW", "zh-CN" -> "zh_tw"
        else -> "en"
    }
}

fun getBlockchainTypeByNetwork(network: String) = when (network) {
    "Ethereum" -> BlockchainType.Ethereum
    "Avalanche" -> BlockchainType.Avalanche
    "Polygon" -> BlockchainType.Polygon
    "Stellar" -> BlockchainType.Stellar
    else -> throw IllegalArgumentException("Unsupported network type $network")
}

fun isWalletSupported(wallet: Wallet): Boolean {
    return wallet.coin.code == "ETH"
            || wallet.coin.code == "MATIC"
            || wallet.coin.code == "AVAX"
            || wallet.coin.code == "USDC"
            || wallet.coin.code == "XLM"
//            || wallet.coin.code == "USDC.E"
}