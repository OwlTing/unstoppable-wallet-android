package io.horizontalsystems.bankwallet.owlwallet.stellarkit.models

import androidx.room.Entity
import io.horizontalsystems.ethereumkit.models.TransactionTag

@Entity(primaryKeys = ["name", "hash"])
class TransactionTag(
    val name: String,
    val hash: String
) {
    companion object {
        const val STELLAR_COIN = "XLM"
        const val INCOMING = "incoming"
        const val OUTGOING = "outgoing"
        const val STELLAR_COIN_INCOMING = "${STELLAR_COIN}_${INCOMING}"
        const val STELLAR_COIN_OUTGOING = "${STELLAR_COIN}_${OUTGOING}"
        const val SWAP = "swap"
        const val APPROVE = "Approve"

        fun tokenIncoming(contractAddress: String): String = "${contractAddress}_$INCOMING"
        fun tokenOutgoing(contractAddress: String): String = "${contractAddress}_$OUTGOING"
    }
}