package com.owlting.app.stellarkit

import android.app.Application
import com.owlting.app.stellarkit.database.StellarDatabaseManager
import com.owlting.app.stellarkit.database.Storage
import com.owlting.app.stellarkit.models.FullTransaction
import com.owlting.app.stellarkit.models.StellarAsset
import com.owlting.app.stellarkit.transaction.TransactionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.stellar.sdk.KeyPair
import timber.log.Timber
import java.math.BigInteger
import java.util.Objects

class StellarKit(
    private val keyPair: KeyPair,
    val network: Network,
    private val syncer: Syncer,
    private val stellarService: StellarService,
    private val transactionManager: TransactionManager,
) {
    private var started = false
    private var scope: CoroutineScope? = null

    val accountId: String
        get() = keyPair.accountId

    val lastLedgerSequence: Long
        get() = syncer.lastLedgerSequence

    val lastLedgerSequenceFlow: StateFlow<Long>
        get() = syncer.lastLedgerSequenceFlow

    val balance: BigInteger
        get() = stellarService.balance

    val baseTokenBalance: BigInteger
        get() = stellarService.baseTokenBalance

    val balanceFlow: StateFlow<BigInteger>
        get() = stellarService.balanceFlow

    val syncState: SyncState
        get() = syncer.syncState

    val syncStateFlow: StateFlow<SyncState>
        get() = syncer.syncStateFlow

    fun start() {
        if (started) return
        started = true

        scope = CoroutineScope(Dispatchers.IO)
            .apply {
                syncer.start(this)
            }
    }

    fun stop() {
        started = false
        syncer.stop()

        scope?.cancel()
    }

    fun refresh() {
        syncer.refresh()
    }

    fun isAccountActive(accountId: String): Boolean {
        return stellarService.isAccountActive(accountId)
    }

    fun send(stellarAsset: StellarAsset, amount: BigInteger, accountId: String, memo: String) {
        stellarService.send(stellarAsset, amount, accountId, memo)
        syncer.refresh()
    }

    suspend fun getFullTransactions(tags: List<List<String>>, fromHash: String? = null, limit: Int? = null): List<FullTransaction> {
        return transactionManager.getFullTransactions(tags, fromHash, limit)
    }

    fun getFullTransactionsFlow(tags: List<List<String>>): Flow<List<FullTransaction>> {
        return transactionManager.getFullTransactionsFlow(tags)
    }

    sealed class SyncState {
        class Synced : SyncState()
        class NotSynced(val error: Throwable) : SyncState()
        class Syncing(val progress: Double? = null) : SyncState()

        override fun toString(): String = when (this) {
            is Syncing -> "Syncing ${progress?.let { "${it * 100}" } ?: ""}"
            is NotSynced -> "NotSynced ${error.javaClass.simpleName} - message: ${error.message}"
            else -> this.javaClass.simpleName
        }

        override fun equals(other: Any?): Boolean {
            if (other !is SyncState)
                return false

            if (other.javaClass != this.javaClass)
                return false

            if (other is Syncing && this is Syncing) {
                return other.progress == this.progress
            }

            return true
        }

        override fun hashCode(): Int {
            if (this is Syncing) {
                return Objects.hashCode(this.progress)
            }
            return Objects.hashCode(this.javaClass.name)
        }
    }

    sealed class SyncError : Throwable() {
        class NotStarted : SyncError()
        class NoNetworkConnection : SyncError()
    }

    companion object {
        fun getInstance(
            application: Application,
            seed: ByteArray,
            network: Network,
            coinUid: String,
            walletId: String,
        ): StellarKit {
            val keyPair = KeyPair.fromBip39Seed(seed, 0)
            Timber.d("keyPair: ${keyPair.accountId}")

            val syncTimer = SyncTimer(15, ConnectionManager(application))
            val stellarService = StellarService(network, keyPair, coinUid)
            val mainDatabase =
                StellarDatabaseManager.getMainDatabase(application, network, walletId)
            val storage = Storage(mainDatabase)

            val transactionManager = TransactionManager(keyPair.accountId, storage)
            val syncer = Syncer(syncTimer, stellarService, keyPair, transactionManager, storage)

            return StellarKit(keyPair, network, syncer, stellarService, transactionManager)
        }

        fun fromAccountId(accountId: String): KeyPair {
            return KeyPair.fromAccountId(accountId)
        }
    }
}