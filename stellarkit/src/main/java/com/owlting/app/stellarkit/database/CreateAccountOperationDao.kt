package com.owlting.app.stellarkit.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.owlting.app.stellarkit.models.CreateAccountOperation

@Dao
interface CreateAccountOperationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCreateAccountOperationIfNotExists(operation: CreateAccountOperation)

    @Query("SELECT * FROM `CreateAccountOperation` WHERE transactionHash=:hash")
    fun getCreateAccountOperations(hash: String): List<CreateAccountOperation>
}