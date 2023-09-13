package io.horizontalsystems.bankwallet.owlwallet.stellarkit.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.TransactionTag

@Dao
interface TransactionTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tags: List<TransactionTag>)
}