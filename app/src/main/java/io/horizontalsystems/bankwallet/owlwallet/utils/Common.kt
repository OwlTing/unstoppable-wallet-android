package io.horizontalsystems.bankwallet.owlwallet.utils

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
    else -> throw IllegalArgumentException("Unsupported network type $network")
}