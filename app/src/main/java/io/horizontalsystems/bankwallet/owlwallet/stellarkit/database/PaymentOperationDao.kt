package io.horizontalsystems.bankwallet.owlwallet.stellarkit.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.PaymentOperation

@Dao
interface PaymentOperationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPaymentOperationIfNotExists(operation: PaymentOperation)

    @Query("SELECT * FROM `PaymentOperation` WHERE transactionHash=:hash")
    fun getPaymentOperations(hash: String): List<PaymentOperation>
}
