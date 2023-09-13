package io.horizontalsystems.bankwallet.owlwallet.stellarkit.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.LastLedgerSequence

@Dao
interface LastLedgerSequenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(lastLedgerSequence: LastLedgerSequence)

    @Query("SELECT * FROM LastLedgerSequence")
    fun getLastLedgerSequence(): LastLedgerSequence?
}