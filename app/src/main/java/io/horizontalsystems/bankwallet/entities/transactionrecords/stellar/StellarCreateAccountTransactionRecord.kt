package io.horizontalsystems.bankwallet.entities.transactionrecords.stellar

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import com.owlting.app.stellarkit.models.CreateAccountOperation
import com.owlting.app.stellarkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class StellarCreateAccountTransactionRecord(
    accountId: String,
    transaction: Transaction,
    val operation: CreateAccountOperation,
    baseToken: Token,
    source: TransactionSource,
    val value: TransactionValue,
) : StellarTransactionRecord(accountId, transaction, baseToken, source) {

    override val mainValue = value
}