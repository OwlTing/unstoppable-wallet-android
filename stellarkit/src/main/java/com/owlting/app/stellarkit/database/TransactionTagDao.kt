package com.owlting.app.stellarkit.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.owlting.app.stellarkit.models.TransactionTag

@Dao
interface TransactionTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tags: List<TransactionTag>)
}