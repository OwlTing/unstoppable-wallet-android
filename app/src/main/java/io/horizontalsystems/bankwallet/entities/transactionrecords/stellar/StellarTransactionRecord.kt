package io.horizontalsystems.bankwallet.entities.transactionrecords.stellar

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

open class StellarTransactionRecord(
    val accountId: String,
    val transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
) : TransactionRecord(
    uid = transaction.hash,
    transactionHash = transaction.hash,
    transactionIndex = 0,
    blockHeight = transaction.ledger.toInt(),
    confirmationsThreshold = 0,
    timestamp = transaction.timestamp,
    failed = !transaction.isSuccessful,
    spam = false,
    source = source
) {
    val fee: TransactionValue?

    init {
        val feeAmount: Long? = transaction.feeCharged
        fee = if (feeAmount != null) {
            val feeDecimal = feeAmount.toBigDecimal()
                .movePointLeft(baseToken.decimals).stripTrailingZeros()

            TransactionValue.CoinValue(baseToken, feeDecimal)
        } else {
            null
        }
    }
}