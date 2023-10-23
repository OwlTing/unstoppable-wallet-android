package com.owlting.app.stellarkit.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.owlting.app.stellarkit.models.LastLedgerSequence

@Dao
interface LastLedgerSequenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(lastLedgerSequence: LastLedgerSequence)

    @Query("SELECT * FROM LastLedgerSequence")
    fun getLastLedgerSequence(): LastLedgerSequence?
}