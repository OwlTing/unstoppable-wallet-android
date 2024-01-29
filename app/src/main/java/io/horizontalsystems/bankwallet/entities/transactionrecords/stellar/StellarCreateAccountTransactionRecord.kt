package io.horizontalsystems.bankwallet.entities.transactionrecords.stellar

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import com.owlting.app.stellarkit.models.CreateAccountOperation
import com.owlting.app.stellarkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class StellarCreateAccountTransactionRecord(
    accountId: String,
    transaction: Transaction,
    val operation: CreateAccountOperation,
    val baseToken: Token,
    source: TransactionSource,
    val value: TransactionValue,
) : StellarTransactionRecord(accountId, transaction, baseToken, source) {

    override val mainValue = value

    fun copyWithCoinValue(value: BigDecimal): StellarCreateAccountTransactionRecord {
        return StellarCreateAccountTransactionRecord(
            accountId,
            transaction,
            operation,
            baseToken,
            source,
            TransactionValue.CoinValue(baseToken, value)
        )
    }


}