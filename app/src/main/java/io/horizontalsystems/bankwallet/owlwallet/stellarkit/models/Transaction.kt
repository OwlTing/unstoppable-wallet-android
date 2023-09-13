package io.horizontalsystems.bankwallet.owlwallet.stellarkit.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.horizontalsystems.tronkit.models.Transaction
import io.horizontalsystems.tronkit.toRawHexString

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