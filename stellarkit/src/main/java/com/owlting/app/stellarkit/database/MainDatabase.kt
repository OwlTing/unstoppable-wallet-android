package com.owlting.app.stellarkit.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.owlting.app.stellarkit.models.CreateAccountOperation
import com.owlting.app.stellarkit.models.LastLedgerSequence
import com.owlting.app.stellarkit.models.PaymentOperation
import com.owlting.app.stellarkit.models.Transaction
import com.owlting.app.stellarkit.models.TransactionTag

@Database(
        entities = [
            LastLedgerSequence::class,
            Transaction::class,
            TransactionTag::class,
            CreateAccountOperation::class,
            PaymentOperation::class,
        ],
        version = 1,
        exportSchema = false
)
//@TypeConverters(RoomTypeConverters::class)
abstract class MainDatabase : RoomDatabase() {

    abstract fun lastLedgerSequenceDao(): LastLedgerSequenceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun tagsDao(): TransactionTagDao
    abstract fun createAccountOperationDao(): CreateAccountOperationDao
    abstract fun paymentOperationDao(): PaymentOperationDao

    companion object {
        fun getInstance(context: Context, databaseName: String): MainDatabase {
            return Room.databaseBuilder(context, MainDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }
}