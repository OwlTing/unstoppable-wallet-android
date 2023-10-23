package com.owlting.app.stellarkit

import android.os.Build
import androidx.annotation.RequiresApi
import com.owlting.app.stellarkit.StellarKit.SyncError
import com.owlting.app.stellarkit.StellarKit.SyncState
import com.owlting.app.stellarkit.database.Storage
import com.owlting.app.stellarkit.models.CreateAccountOperation
import com.owlting.app.stellarkit.models.PaymentOperation
import com.owlting.app.stellarkit.models.Transaction
import com.owlting.app.stellarkit.transaction.TransactionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.stellar.sdk.AssetTypeCreditAlphaNum4
import org.stellar.sdk.KeyPair
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import org.stellar.sdk.xdr.OperationType
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Syncer(
    private val syncTimer: SyncTimer,
    private val stellarService: StellarService,
    private val keyPair: KeyPair,
    private val transactionManager: TransactionManager,
    private val storage: Storage,
) : SyncTimer.Listener {

    private var scope: CoroutineScope? = null

    var syncState: SyncState = SyncState.NotSynced(SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                _syncStateFlow.update { value }
            }
        }

    var lastLedgerSequence: Long = storage.getLastLedgerSequence() ?: 0
        private set(value) {
            if (value != field) {
                field = value
                _lastLedgerSequenceFlow.update { value }
            }
        }

    private val _syncStateFlow = MutableStateFlow(syncState)
    val syncStateFlow: StateFlow<SyncState> = _syncStateFlow

    private val _lastLedgerSequenceFlow = MutableStateFlow(lastLedgerSequence)
    val lastLedgerSequenceFlow: StateFlow<Long> = _lastLedgerSequenceFlow

    fun start(scope: CoroutineScope) {
        this.scope = scope

        syncTimer.start(this, scope)
    }

    fun stop() {
        syncState = SyncState.NotSynced(SyncError.NotStarted())

        syncTimer.stop()
    }

    fun refresh() {
        when (syncTimer.state) {
            SyncTimer.State.Ready -> {
                sync()
            }

            is SyncTimer.State.NotReady -> {
                scope?.let { syncTimer.start(this, it) }
            }
        }
    }

    override fun onUpdateSyncTimerState(state: SyncTimer.State) {
        syncState = when (state) {
            is SyncTimer.State.NotReady -> {
                SyncState.NotSynced(state.error)
            }

            SyncTimer.State.Ready -> {
                SyncState.Syncing()
            }
        }
    }

    override fun sync() {
        scope?.launch {
            syncLastLedgerSequence()
        }
    }

    private fun syncLastLedgerSequence() {
        try {
            val lastLedgerSequence = stellarService.getLastLedgerSequence()

            if (this.lastLedgerSequence == lastLedgerSequence) return

            storage.saveLastLedgerSequence(lastLedgerSequence)

            this.lastLedgerSequence = lastLedgerSequence

            onUpdateLastLedgerSequence()

        } catch (error: Throwable) {
            error.printStackTrace()
            syncState = SyncState.NotSynced(error)
        }
    }

    private fun onUpdateLastLedgerSequence() {
        stellarService.getBalance()

        syncTransactions()

        syncOperations()

        transactionManager.process()

        syncState = SyncState.Synced()
    }

    private fun syncTransactions() {
        val txs = stellarService.getTransactions(100)
        if (txs.isNotEmpty()) {
            storage.saveTransactionsIfNotExists(
                txs.map {
                    val createAt = LocalDateTime.parse(
                        it.createdAt,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    )
                    val timestamp = createAt.toEpochSecond(ZoneOffset.ofHours(0))

                    Transaction(
                        hash = it.hash,
                        createdAt = it.createdAt,
                        timestamp = timestamp,
                        sourceAccount = it.sourceAccount,
                        feeAccount = it.feeAccount,
                        feeCharged = it.feeCharged,
                        memo = it.memo.toString(),
                        ledger = it.ledger,
                        isSuccessful = it.isSuccessful,
                    )
                }
            )
        }
    }

    private fun syncOperations() {
        val ops = stellarService.getOperations(100)
        if (ops.isNotEmpty()) {
            for (op in ops) {
                when (op.type) {
                    OperationType.CREATE_ACCOUNT.name.lowercase() -> {
                        val inheritedOp = op as CreateAccountOperationResponse
                        val createAccountOperation = CreateAccountOperation(
                            id = inheritedOp.id.toString(),
                            pagingToken = inheritedOp.pagingToken,
                            transactionSuccessful = inheritedOp.isTransactionSuccessful,
                            sourceAccount = inheritedOp.sourceAccount,
                            type = inheritedOp.type,
                            createdAt = inheritedOp.createdAt,
                            transactionHash = inheritedOp.transactionHash,
                            startingBalance = inheritedOp.startingBalance,
                            founder = inheritedOp.funder,
                            account = inheritedOp.account,
                        )
                        storage.saveCreateAccountOperationIfNotExists(createAccountOperation)
                    }

                    OperationType.PAYMENT.name.lowercase() -> {
                        val inheritedOp = op as PaymentOperationResponse

                        val assetCode = when (inheritedOp.asset.type) {
                            "credit_alphanum4" -> {
                                val asset = inheritedOp.asset as AssetTypeCreditAlphaNum4
                                asset.code
                            }
                            else -> "XLM"
                        }

                        val paymentOperation = PaymentOperation(
                            id = inheritedOp.id.toString(),
                            pagingToken = inheritedOp.pagingToken,
                            transactionSuccessful = inheritedOp.isTransactionSuccessful,
                            sourceAccount = inheritedOp.sourceAccount,
                            type = inheritedOp.type,
                            createdAt = inheritedOp.createdAt,
                            transactionHash = inheritedOp.transactionHash,
                            assetType = inheritedOp.asset.type,
                            assetCode = assetCode,
                            assetIssuer = "",
                            from = inheritedOp.from,
                            to = inheritedOp.to,
                            amount = inheritedOp.amount
                        )
                        storage.savePaymentOperationIfNotExists(paymentOperation)
                    }
//                            allow_trust
//                            change_trust
//                            set_options
//                            account_merge
//                            manage_offer
//                            path_payment
//                            create_passive_offer
//                            inflation
//                            manage_data
                }
            }
        }
    }
}