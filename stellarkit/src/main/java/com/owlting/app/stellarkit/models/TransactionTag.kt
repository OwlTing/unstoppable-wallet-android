package com.owlting.app.stellarkit.models

import androidx.room.Entity

@Entity(primaryKeys = ["name", "hash"])
class TransactionTag(
    val name: String,
    val hash: String
) {
    companion object {
        const val STELLAR_COIN = "XLM"
        const val STELLAR_USDC = "USDC"
        const val INCOMING = "incoming"
        const val OUTGOING = "outgoing"
        const val STELLAR_COIN_INCOMING = "${STELLAR_COIN}_$INCOMING"
        const val STELLAR_COIN_OUTGOING = "${STELLAR_COIN}_$OUTGOING"
        const val STELLAR_USDC_INCOMING = "${STELLAR_USDC}_$INCOMING"
        const val STELLAR_USDC_OUTGOING = "${STELLAR_USDC}_$OUTGOING"
        const val SWAP = "swap"
        const val APPROVE = "Approve"

        fun tokenIncoming(contractAddress: String): String = "${contractAddress}_$INCOMING"
        fun tokenOutgoing(contractAddress: String): String = "${contractAddress}_$OUTGOING"
    }
}