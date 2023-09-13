package io.horizontalsystems.bankwallet.owlwallet.stellarkit.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.Transaction


@Dao
interface TransactionDao {

    @Query("SELECT * FROM `Transaction` WHERE hash=:hash")
    fun getTransaction(hash: String): Transaction

    @Query("SELECT * FROM `Transaction` WHERE hash IN (:hashes)")
    fun getTransactions(hashes: List<String>): List<Transaction>

    @Query("SELECT * FROM `Transaction` ORDER BY createdAt DESC")
    fun getTransactions(): List<Transaction>

    @RawQuery
    suspend fun getTransactionsBefore(query: SupportSQLiteQuery): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTransactionsIfNotExists(transactions: List<Transaction>)
}