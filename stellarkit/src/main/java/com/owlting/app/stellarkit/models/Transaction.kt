package com.owlting.app.stellarkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Transaction(
    @PrimaryKey
    val hash: String,
    val createdAt: String,
    val timestamp: Long,
    val sourceAccount: String,
    val feeAccount: String,
    val feeCharged: Long,
    val memo: String,
    val ledger: Long,
    val isSuccessful: Boolean,
)