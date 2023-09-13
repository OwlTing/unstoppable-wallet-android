package io.horizontalsystems.bankwallet.owlwallet.stellarkit.database

import androidx.sqlite.db.SimpleSQLiteQuery
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.CreateAccountOperation
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.LastLedgerSequence
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.PaymentOperation
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.Transaction
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.TransactionTag

class Storage(
    private val database: MainDatabase
) {
    fun getLastLedgerSequence(): Long? {
        return database.lastLedgerSequenceDao().getLastLedgerSequence()?.sequence
    }

    fun saveLastLedgerSequence(lastLedgerSequence: Long) {
        database.lastLedgerSequenceDao().insert(LastLedgerSequence(lastLedgerSequence))
    }

    fun getTransactions(hashes: List<String>): List<Transaction> {
        return database.transactionDao().getTransactions(hashes)
    }

    fun getTransactions(): List<Transaction> {
        return database.transactionDao().getTransactions()
    }

    fun getTransaction(hash: String): Transaction {
        return database.transactionDao().getTransaction(hash)
    }

    fun saveTransactionsIfNotExists(transactions: List<Transaction>) {
        database.transactionDao().insertTransactionsIfNotExists(transactions)
    }

    suspend fun getTransactionsBefore(
        tags: List<List<String>>,
        hash: String?,
        limit: Int?
    ): List<Transaction> {
        val whereConditions = mutableListOf<String>()

        if (tags.isNotEmpty()) {
            val tagConditions = tags
                .mapIndexed { index, andTags ->
                    val tagsString = andTags.joinToString(", ") { "'$it'" }
                    "transaction_tags_$index.name IN ($tagsString)"
                }
                .joinToString(" AND ")

            whereConditions.add(tagConditions)
        }

        hash?.let { database.transactionDao().getTransaction(hash) }?.let { fromTransaction ->
            val fromCondition = """
                           (
                                tx.timestamp < ${fromTransaction.timestamp} OR 
                                (
                                    tx.timestamp = ${fromTransaction.timestamp} AND 
                                    LOWER(HEX(tx.hash)) < "${fromTransaction.hash.lowercase()}"
                                )
                           )
                           """

            whereConditions.add(fromCondition)
        }

        val transactionTagJoinStatements = tags
            .mapIndexed { index, _ ->
                "INNER JOIN TransactionTag AS transaction_tags_$index ON tx.hash = transaction_tags_$index.hash"
            }
            .joinToString("\n")

        val whereClause =
            if (whereConditions.isNotEmpty()) "WHERE ${whereConditions.joinToString(" AND ")}" else ""
        val orderClause = "ORDER BY tx.timestamp DESC, HEX(tx.hash) DESC"
        val limitClause = limit?.let { "LIMIT $limit" } ?: ""

        val sqlQuery = """
                      SELECT tx.*
                      FROM `Transaction` as tx
                      $transactionTagJoinStatements
                      $whereClause
                      $orderClause
                      $limitClause
                      """

        return database.transactionDao().getTransactionsBefore(SimpleSQLiteQuery(sqlQuery))
    }

    fun getCreateAccountOperations(hash: String): List<CreateAccountOperation> {
        return database.createAccountOperationDao().getCreateAccountOperations(hash)
    }

    fun saveCreateAccountOperationIfNotExists(createAccountOperation: CreateAccountOperation) {
        database.createAccountOperationDao()
            .insertCreateAccountOperationIfNotExists(createAccountOperation)
    }

    fun getPaymentOperations(hash: String): List<PaymentOperation> {
        return database.paymentOperationDao().getPaymentOperations(hash)
    }

    fun savePaymentOperationIfNotExists(paymentOperation: PaymentOperation) {
        database.paymentOperationDao().insertPaymentOperationIfNotExists(paymentOperation)
    }

    fun saveTags(tags: List<TransactionTag>) {
        database.tagsDao().insert(tags)
    }
}