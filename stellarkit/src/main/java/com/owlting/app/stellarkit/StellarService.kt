package com.owlting.app.stellarkit

import com.owlting.app.stellarkit.exception.ErrorType
import com.owlting.app.stellarkit.exception.KitException
import com.owlting.app.stellarkit.models.Alphanum4
import com.owlting.app.stellarkit.models.Native
import com.owlting.app.stellarkit.models.StellarAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.stellar.sdk.Asset
import org.stellar.sdk.AssetTypeCreditAlphaNum4
import org.stellar.sdk.AssetTypeNative
import org.stellar.sdk.ChangeTrustAsset
import org.stellar.sdk.ChangeTrustOperation
import org.stellar.sdk.CreateAccountOperation

import org.stellar.sdk.KeyPair
import org.stellar.sdk.Memo
import org.stellar.sdk.Operation
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

    private val usdcIssuer get() = if (network == Network.Testnet) usdcIssuerTestNet else usdcIssuerMainNet

    fun getBalance() {
        val account = server.accounts().account(keyPair.accountId)

        for (accountBalance in account.balances) {
            Timber.d("$coinUid balance: ${accountBalance.assetType} ${accountBalance.assetCode} ${accountBalance.assetIssuer} ${accountBalance.limit}")
        }

        if (coinUid == "stellar") {
            for (accountBalance in account.balances) {
                if (accountBalance.assetType == "native") {
                    balance = BigDecimal.valueOf(accountBalance.balance.toDouble() * 10_000_000)
                        .toBigInteger()
                    baseTokenBalance =
                        BigDecimal.valueOf(accountBalance.balance.toDouble() * 10_000_000)
                            .toBigInteger()
                    break
                }
            }
        } else if (coinUid == "usd-coin") {
            var containsUSDC = false
            for (accountBalance in account.balances) {
                if (accountBalance.assetIssuer.isPresent && accountBalance.assetIssuer.get() == usdcIssuer) {
                    containsUSDC = true
                    break
                }
            }
            Timber.d("containsUSDC: $containsUSDC")

            if (!containsUSDC) {
                addTrustToUSDC()
            }

            for (accountBalance in account.balances) {
                if (accountBalance.assetType == "native") {
                    baseTokenBalance =
                        BigDecimal.valueOf(accountBalance.balance.toDouble() * 10_000_000)
                            .toBigInteger()
                } else if (accountBalance.assetIssuer.isPresent && accountBalance.assetIssuer.get() == usdcIssuer) {
                    balance = BigDecimal.valueOf(accountBalance.balance.toDouble() * 10_000_000)
                        .toBigInteger()
                }
            }
        }
    }
    /**
     * Send the amount to the account.
     *
     * @param stellarAsset The asset to send.
     * @param amount The amount to send.
     * @param accountId The account to send to.
     * @param memo The memo to send with the transaction.
     * @param isInactiveAddress If the address is inactive, it will create the account.
     * @throws KitException If the transaction fails
     */
    @Throws(KitException::class)
    fun send(
        stellarAsset: StellarAsset,
        amount: BigInteger,
        accountId: String,
        memo: String,
        isInactiveAddress: Boolean
    )
    {
        val asset = when (stellarAsset) {
            is Native -> {
                AssetTypeNative()
            }

            is Alphanum4 -> {
                AssetTypeCreditAlphaNum4(stellarAsset.code, stellarAsset.issuer)
            }

            else -> throw Exception("The token is not supported.")
        }
        Timber.d("sendAmount: ${amount.toBigDecimal().movePointLeft(stellarAsset.decimals)}")
        /// if the address is inactive, need to create the account
        // or submit will return 400 error code in case response.ledger will be null
        val operation = if (isInactiveAddress) {
            CreateAccountOperation.Builder(
                accountId,
                amount.toBigDecimal().movePointLeft(stellarAsset.decimals).toString()
            ).build()
        } else {
            PaymentOperation.Builder(
                accountId,
                asset,
                amount.toBigDecimal().movePointLeft(stellarAsset.decimals).toString(),
            ).build()
        }

        val transaction: Transaction = TransactionBuilder(
            server.accounts().account(keyPair.accountId),
            if (network == Network.Testnet) org.stellar.sdk.Network.TESTNET else org.stellar.sdk.Network.PUBLIC
        )

            .addOperation(
                operation
            ) // A memo allows you to add your own metadata to a transaction. It's
            // optional and does not affect how Stellar treats the transaction.
            .addMemo(Memo.text(memo)) // Wait a maximum of three minutes for the transaction
            .setTimeout(0) // Set the amount of lumens you're willing to pay per operation to submit your transaction
            .setBaseFee(Transaction.MIN_BASE_FEE)
            .build()
        // Sign the transaction to prove you are actually the person sending it.
        transaction.sign(keyPair)

        try {
            val response = server.submitTransaction(transaction)
            Timber.d("Success!")
            Timber.d(response.isSuccess.toString())
            if (!response.isSuccess) {
                throw KitException(errorType =ErrorType.Transactions_Failed , message =  "Transaction failed! ,response : $response")
            }

        } catch (e: KitException) {
            throw e
        } catch (e: java.lang.Exception) {
            Timber.e("Something went wrong!")
            Timber.e(e.message.toString())
            // If the result is unknown (no response body, timeout etc.) we simply resubmit
            // already built transaction:
            // SubmitTransactionResponse response = server.submitTransaction(transaction);
        } catch (e: java.lang.Exception) {
            Timber.e("Something went wrong!")
            Timber.e(e.message.toString())
            // If the result is unknown (no response body, timeout etc.) we simply resubmit
            // already built transaction:
            // SubmitTransactionResponse response = server.submitTransaction(transaction);
        }
    }

    private fun addTrustToUSDC() {
        try {
            val transaction: Transaction = TransactionBuilder(
                server.accounts().account(keyPair.accountId),
                if (network == Network.Testnet) org.stellar.sdk.Network.TESTNET else org.stellar.sdk.Network.PUBLIC
            )
                .addOperation(
                    ChangeTrustOperation.Builder(
                        ChangeTrustAsset.create(
                            AssetTypeCreditAlphaNum4("USDC", usdcIssuer)
                        ),
                        "922337203685.4775807",
                    ).build()
                )
                .addMemo(Memo.text("Trust USDC"))
                .setTimeout(0)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .build()
            transaction.sign(keyPair)

            Timber.d("submitTransaction")
            val response = server.submitTransaction(transaction)
            Timber.d("Success!")
            Timber.d(response.isSuccess.toString())
        } catch (e: java.lang.Exception) {
            Timber.e("Something went wrong!")
            Timber.e(e.message.toString())
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

    companion object {
        const val usdcIssuerTestNet = "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        const val usdcIssuerMainNet = "GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"
    }
}