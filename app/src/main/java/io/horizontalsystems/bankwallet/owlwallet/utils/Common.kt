package io.horizontalsystems.bankwallet.owlwallet.utils

import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin

val passwordRegex = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}\$")

fun getLangParam(language: String): String {
    return when (language) {
        "tw", "zh" -> "zh_tw"
        else -> "en"
    }
}

//fun getAddress(accountType: AccountType, wallet: Wallet): String {
//    return when (accountType) {
//        is AccountType.EvmAddress -> accountType.address
//        is AccountType.Mnemonic -> {
//            val seed = Mnemonic().toSeed(accountType.words, accountType.passphrase)
//            val privateKey =
//                Signer.privateKey(seed, getChain(wallet.token.blockchainType))
//            val publicKey =
//                CryptoUtils.ecKeyFromPrivate(privateKey).publicKeyPoint.getEncoded(false)
//                    .drop(1).toByteArray()
//            Address(CryptoUtils.sha3(publicKey).takeLast(20).toByteArray()).eip55
//        }
//        else -> ""
//    }
//}

//fun getChain(blockchainType: BlockchainType) = when (blockchainType) {
//    BlockchainType.Ethereum -> Chain.Ethereum
//    BlockchainType.Polygon -> Chain.Polygon
//    BlockchainType.Avalanche -> Chain.Avalanche
//    else -> throw IllegalArgumentException("Unsupported blockchain type $blockchainType")
//}

fun getBlockchainTypeByNetwork(network: String) = when (network) {
    "Ethereum" -> BlockchainType.Ethereum
    "Avalanche" -> BlockchainType.Avalanche
    "Polygon" -> BlockchainType.Polygon
    else -> throw IllegalArgumentException("Unsupported network type $network")
}