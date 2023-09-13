package io.horizontalsystems.bankwallet.owlwallet.stellarkit

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.stellar.sdk.AssetTypeCreditAlphaNum4
import org.stellar.sdk.AssetTypeNative
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Memo
import org.stellar.sdk.PaymentOperation
import org.stellar.sdk.Server
import org.stellar.sdk.Transaction
import org.stellar.sdk.TransactionBuilder
import org.stellar.sdk.requests.RequestBuilder
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.OperationResponse
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger


class StellarService(
        private val network: Network,
        private val keyPair: KeyPair,
        private val coinUid: String,
) {
    private val server = Server(network.url)

    var balance: BigInteger = BigInteger.ZERO
        private set(value) {
            if (value != field) {
                field = value
                _balanceFlow.update { value }
            }
        }

    var baseTokenBalance: BigInteger = BigInteger.ZERO
        private set(value) {
            if (value != field) {
                field = value
                _baseTokenBalanceFlow.update { value }
            }
        }

    private val _balanceFlow = MutableStateFlow(balance)
    val balanceFlow: StateFlow<BigInteger> = _balanceFlow

    private val _baseTokenBalanceFlow = MutableStateFlow(baseTokenBalance)
    val baseTokenBalanceFlow: StateFlow<BigInteger> = _baseTokenBalanceFlow

    fun getBalance() {
        val account = server.accounts().account(keyPair.accountId)

        if (coinUid == "stellar") {
            for (accountBalance in account.balances) {
                if (accountBalance.assetType == "native") {
                    balance = BigDecimal.valueOf(accountBalance.balance.toDouble() * 10_000_000)
                        .toBigInteger()
                    baseTokenBalance = BigDecimal.valueOf(accountBalance.balance.toDouble() * 10_000_000)
                        .toBigInteger()
                    break
                }
            }
        } else {
            for (accountBalance in account.balances) {
                if (accountBalance.assetType == "native") {
                    baseTokenBalance = BigDecimal.valueOf(accountBalance.balance.toDouble() * 10_000_000)
                        .toBigInteger()
                } else if (accountBalance.assetCode.isPresent && accountBalance.assetCode.get() == "USDC") {
                    balance = BigDecimal.valueOf(accountBalance.balance.toDouble() * 10_000_000)
                        .toBigInteger()
                }
            }
        }
    }

    fun send(token: Token, amount: BigInteger, accountId: String, memo: String) {
        val asset = when (token.type) {
            is TokenType.Native -> {
                AssetTypeNative()
            }

            is TokenType.Alphanum4 -> {
                AssetTypeCreditAlphaNum4(token.coin.code, token.type.values.reference)
            }

            else -> throw Exception("The token is not supported.")
        }
        Timber.d("sendAmount: ${amount.toBigDecimal().movePointLeft(token.decimals)}")

        val transaction: Transaction = TransactionBuilder(
                server.accounts().account(keyPair.accountId),
                if (network == Network.Testnet) org.stellar.sdk.Network.TESTNET else org.stellar.sdk.Network.PUBLIC
        )
                .addOperation(
                        PaymentOperation.Builder(
                                accountId,
                                asset,
                                amount.toBigDecimal().movePointLeft(token.decimals).toString(),
                        ).build()
                ) // A memo allows you to add your own metadata to a transaction. It's
                // optional and does not affect how Stellar treats the transaction.
                .addMemo(Memo.text(memo)) // Wait a maximum of three minutes for the transaction
                .setTimeout(180) // Set the amount of lumens you're willing to pay per operation to submit your transaction
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build()
        // Sign the transaction to prove you are actually the person sending it.
        transaction.sign(keyPair)

        try {
            val response = server.submitTransaction(transaction)
            Timber.d("Success!")
            Timber.d(response.toString())
        } catch (e: java.lang.Exception) {
            Timber.e("Something went wrong!")
            Timber.e(e.message.toString())
            // If the result is unknown (no response body, timeout etc.) we simply resubmit
            // already built transaction:
            // SubmitTransactionResponse response = server.submitTransaction(transaction);
        }
    }

    fun getLastLedgerSequence(): Long {
        val lastLedger = server.ledgers().limit(1).order(RequestBuilder.Order.DESC).execute()
        return lastLedger.records[0].sequence
    }

    fun getTransactions(limit: Int): List<TransactionResponse> {
        val builder =
                server.transactions()
                        .order(RequestBuilder.Order.DESC)
                        .forAccount(keyPair.accountId)
                        .limit(limit)
                        .includeFailed(true)

        val results = builder.execute()
        return results.records
    }

    fun getOperations(limit: Int): List<OperationResponse> {
        val builder =
            server.operations()
                .order(RequestBuilder.Order.DESC)
                .forAccount(keyPair.accountId)
                .limit(limit)
                .includeFailed(true)

        val results = builder.execute()
        return results.records
    }

    fun isAccountActive(accountId: String): Boolean {
        return try {
            server.accounts().account(accountId)
            true
        } catch (e: Exception) {
            false
        }
    }
}