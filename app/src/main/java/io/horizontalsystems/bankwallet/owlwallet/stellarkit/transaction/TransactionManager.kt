package io.horizontalsystems.bankwallet.owlwallet.stellarkit.transaction

import io.horizontalsystems.bankwallet.owlwallet.stellarkit.database.Storage
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.CreateAccountOperation
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.FullTransaction
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.PaymentOperation
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.Transaction
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.TransactionTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import timber.log.Timber

class TransactionManager(
    private val accountId: String,
    private val storage: Storage
) {
    private val _transactionsFlow = MutableStateFlow<List<FullTransaction>>(listOf())
    val transactionsFlow: StateFlow<List<FullTransaction>> = _transactionsFlow

    private val _transactionsWithTagsFlow = MutableStateFlow<List<TransactionWithTags>>(listOf())
    val transactionsWithTagsFlow: StateFlow<List<TransactionWithTags>> = _transactionsWithTagsFlow

    fun getFullTransactionsFlow(tags: List<List<String>>): Flow<List<FullTransaction>> {
        return _transactionsWithTagsFlow.map { transactions ->
            transactions.mapNotNull { transactionWithTags ->
                for (andTags in tags) {
                    if (transactionWithTags.tags.all { !andTags.contains(it) }) {
                        return@mapNotNull null
                    }
                }
                return@mapNotNull transactionWithTags.transaction
            }
        }.filter { it.isNotEmpty() }
    }

    suspend fun getFullTransactions(
        tags: List<List<String>>,
        fromHash: String? = null,
        limit: Int? = null
    ): List<FullTransaction> {
        val transactions = storage.getTransactionsBefore(tags, fromHash, limit)
        return handle(transactions)
    }

    fun getFullTransactions(hashes: List<String>): List<FullTransaction> {
        val transactions = storage.getTransactions(hashes)
        return handle(transactions)
    }

    fun process() {
        val transactions = storage.getTransactions()

        if (transactions.isEmpty()) return

        val fullTransactions = handle(transactions)

        val transactionWithTags = mutableListOf<TransactionWithTags>()
        val allTags: MutableList<TransactionTag> = mutableListOf()

        fullTransactions.forEach { fullTransaction ->

            val op = fullTransaction.operation
            val tags = when (op) {
                is CreateAccountOperation -> mutableListOf(
                    TransactionTag.STELLAR_COIN,
                    TransactionTag.STELLAR_COIN_INCOMING,
                    TransactionTag.INCOMING
                )

                is PaymentOperation -> {
                    val isSend = op.from == accountId

                    if (op.assetType == "native") {
                        if (isSend) {
                            mutableListOf(
                                TransactionTag.STELLAR_COIN,
                                TransactionTag.STELLAR_COIN_OUTGOING,
                                TransactionTag.OUTGOING
                            )
                        } else {
                            mutableListOf(
                                TransactionTag.STELLAR_COIN,
                                TransactionTag.STELLAR_COIN_INCOMING,
                                TransactionTag.INCOMING
                            )
                        }
                    } else {
                        if (isSend) {
                            mutableListOf(
                                TransactionTag.STELLAR_USDC,
                                TransactionTag.STELLAR_USDC_OUTGOING,
                                TransactionTag.OUTGOING
                            )
                        } else {
                            mutableListOf(
                                TransactionTag.STELLAR_USDC,
                                TransactionTag.STELLAR_USDC_INCOMING,
                                TransactionTag.INCOMING
                            )
                        }
                    }
                }

                else -> listOf()
            }
            allTags.addAll(tags.map { TransactionTag(it, fullTransaction.transaction.hash) })
            transactionWithTags.add(TransactionWithTags(fullTransaction, tags))
        }

        storage.saveTags(allTags)

        _transactionsWithTagsFlow.tryEmit(transactionWithTags)
        _transactionsFlow.tryEmit(fullTransactions)
    }

    fun handle(transactions: List<Transaction>): List<FullTransaction> {
        if (transactions.isEmpty()) return listOf()

        val fullTransactions: MutableList<FullTransaction> = mutableListOf()
        for (transaction in transactions) {
            val createAccountOps = storage.getCreateAccountOperations(transaction.hash)
            if (createAccountOps.isNotEmpty()) {
                fullTransactions.add(
                    FullTransaction(
                        transaction = transaction,
                        operation = createAccountOps.first(),
                    )
                )
                continue
            }
            val paymentOps = storage.getPaymentOperations(transaction.hash)
            if (paymentOps.isNotEmpty()) {
                fullTransactions.add(
                    FullTransaction(
                        transaction = transaction,
                        operation = paymentOps.first(),
                    )
                )
                continue
            }
        }

        return fullTransactions
    }

    data class TransactionWithTags(
        val transaction: FullTransaction,
        val tags: List<String>
    )
}