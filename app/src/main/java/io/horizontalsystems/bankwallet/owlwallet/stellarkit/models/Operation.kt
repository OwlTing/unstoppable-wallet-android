package io.horizontalsystems.bankwallet.owlwallet.stellarkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey


open class Operation(
    open val id: String,
    open val pagingToken: String,
    open val type: String,
)

@Entity
data class CreateAccountOperation(
    @PrimaryKey
    override val id: String,
    override val pagingToken: String,
    val transactionSuccessful: Boolean,
    val sourceAccount: String,
    override val type: String,
    val createdAt: String,
    val transactionHash: String,
    val startingBalance: String,
    val founder: String,
    val account: String,
) : Operation(id, pagingToken, type)

@Entity
data class PaymentOperation(
    @PrimaryKey
    override val id: String,
    override val pagingToken: String,
    val transactionSuccessful: Boolean,
    val sourceAccount: String,
    override val type: String,
    val createdAt: String,
    val transactionHash: String,
    val assetType: String,
    val assetCode: String,
    val assetIssuer: String,
    val from: String,
    val to: String,
    val amount: String,
) : Operation(id, pagingToken, type)