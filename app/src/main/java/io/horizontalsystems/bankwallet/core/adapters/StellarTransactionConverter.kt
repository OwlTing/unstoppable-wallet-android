package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.tokenIconPlaceholder
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.stellar.StellarCreateAccountTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.stellar.StellarPaymentTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.stellar.StellarTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.bankwallet.core.managers.StellarKitWrapper
import com.owlting.app.stellarkit.models.CreateAccountOperation
import com.owlting.app.stellarkit.models.FullTransaction
import com.owlting.app.stellarkit.models.PaymentOperation
import io.horizontalsystems.erc20kit.events.TokenInfo
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal
import java.math.BigInteger

class StellarTransactionConverter(
    private val coinManager: ICoinManager,
    private val source: TransactionSource,
    private val stellarKitWrapper: StellarKitWrapper,
    private val baseToken: Token,
) {

    fun transactionRecord(fullTransaction: FullTransaction): StellarTransactionRecord {
        val transaction = fullTransaction.transaction
        when (val operation = fullTransaction.operation) {
            is CreateAccountOperation -> {
                return StellarCreateAccountTransactionRecord(
                    accountId = stellarKitWrapper.stellarKit.accountId,
                    transaction = transaction,
                    operation = operation,
                    baseToken = baseToken,
                    source = source,
                    value = getBaseCoinValue(operation.startingBalance, false)
                )
            }

            is PaymentOperation -> {
                val negative = operation.from == stellarKitWrapper.stellarKit.accountId
                val value = if (operation.assetCode == "USDC") {
                    getUSDCCoinValue(operation.amount, negative)
                } else {
                    getBaseCoinValue(operation.amount, negative)
                }
                return StellarPaymentTransactionRecord(
                    accountId = stellarKitWrapper.stellarKit.accountId,
                    transaction = transaction,
                    operation = operation,
                    baseToken = baseToken,
                    source = source,
                    value = value,
                )
            }

            else -> {
                return StellarTransactionRecord(
                    accountId = stellarKitWrapper.stellarKit.accountId,
                    transaction = transaction,
                    baseToken = baseToken,
                    source = source,
                )
            }
        }
    }

    private fun getBaseCoinValue(value: String, negative: Boolean): TransactionValue {
        val amount = convertAmount(value, negative)

        return TransactionValue.CoinValue(baseToken, amount)
    }

    private fun getUSDCCoinValue(
        amount: String,
        negative: Boolean,
        tokenInfo: TokenInfo? = null
    ): TransactionValue {
        val query = TokenQuery(BlockchainType.Stellar, TokenType.Alphanum4(""))
        val token = coinManager.getToken(query)

        return when {
            token != null -> {
                TransactionValue.CoinValue(token, convertAmount(amount, negative))
            }

            tokenInfo != null -> {
                TransactionValue.TokenValue(
                    tokenName = tokenInfo.tokenName,
                    tokenCode = tokenInfo.tokenSymbol,
                    tokenDecimals = tokenInfo.tokenDecimal,
                    value = convertAmount(amount, negative),
                    coinIconPlaceholder = BlockchainType.Stellar.tokenIconPlaceholder
                )
            }

            else -> {
                TransactionValue.RawValue(
                    value = BigInteger.valueOf(
                        (convertAmount(
                            amount,
                            negative
                        ).movePointRight(7)).toLong()
                    )
                )
            }
        }
    }

    private fun convertAmount(amount: String, negative: Boolean): BigDecimal {
        var significandAmount = BigDecimal.valueOf(amount.toDouble())

        if (significandAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO
        }

        if (negative) {
            significandAmount = significandAmount.negate()
        }

        return significandAmount
    }
}