package io.horizontalsystems.bankwallet.owlwallet.stellarkit

enum class Network(
    val url: String,
) {
    Mainnet("https://horizon.stellar.org/"),
    Testnet("https://horizon-testnet.stellar.org")
}