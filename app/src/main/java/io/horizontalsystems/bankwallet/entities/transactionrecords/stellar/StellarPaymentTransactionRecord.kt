package io.horizontalsystems.bankwallet.entities.transactionrecords.stellar

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.PaymentOperation
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class StellarPaymentTransactionRecord(
    accountId: String,
    transaction: Transaction,
    val operation: PaymentOperation,
    val baseToken: Token,
    source: TransactionSource,
    val value: TransactionValue,
) : StellarTransactionRecord(accountId, transaction, baseToken, source) {

    override val mainValue = value
}