package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.Network
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.StellarKit.SyncState
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.StellarKitWrapper
import io.horizontalsystems.bankwallet.owlwallet.stellarkit.models.TransactionTag
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.rxSingle
import timber.log.Timber

class StellarTransactionsAdapter(
    stellarKitWrapper: StellarKitWrapper,
    private val transactionConverter: StellarTransactionConverter
) : ITransactionsAdapter {

    private val stellarKit = stellarKitWrapper.stellarKit

    override val explorerTitle: String
        get() = "stellar.expert"

    override fun getTransactionUrl(transactionHash: String): String = when (stellarKit.network) {
        Network.Mainnet -> "https://stellar.expert/explorer/public/tx/$transactionHash"
        Network.Testnet -> "https://stellar.expert/explorer/testnet/tx/$transactionHash"
    }

    override val transactionsState: AdapterState
        get() = convertToAdapterState(stellarKit.syncState)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = stellarKit.syncStateFlow.asFlowable().map {}

    override val lastBlockInfo: LastBlockInfo?
        get() = stellarKit.lastLedgerSequence.toInt().let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = stellarKit.lastLedgerSequenceFlow.asFlowable().map { }

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>> {
        return rxSingle {
            stellarKit.getFullTransactions(
                getFilters(token, transactionType),
                from?.transactionHash,
                limit
            )
        }.map {
            it.map { tx -> transactionConverter.transactionRecord(tx) }
        }
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType
    ): Flowable<List<TransactionRecord>> {
        return stellarKit.getFullTransactionsFlow(getFilters(token, transactionType)).asFlowable().map {
            it.map { tx -> transactionConverter.transactionRecord(tx) }
        }
    }

    private fun convertToAdapterState(syncState: SyncState): AdapterState =
        when (syncState) {
            is SyncState.Synced -> AdapterState.Synced
            is SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is SyncState.Syncing -> AdapterState.Syncing()
        }

    private fun coinTagName(token: Token) = when (val type = token.type) {
        TokenType.Native -> TransactionTag.STELLAR_COIN
        is TokenType.Alphanum4 -> TransactionTag.STELLAR_USDC
        else -> ""
    }

    private fun getFilters(token: Token?, filter: FilterTransactionType): List<List<String>> {
        val filterCoin = token?.let {
            coinTagName(it)
        }

        val filterTag = when (filter) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> when {
                token != null -> TransactionTag.tokenIncoming(coinTagName(token))
                else -> TransactionTag.INCOMING
            }

            FilterTransactionType.Outgoing -> when {
                token != null -> TransactionTag.tokenOutgoing(coinTagName(token))
                else -> TransactionTag.OUTGOING
            }
            FilterTransactionType.Swap -> TransactionTag.SWAP
            FilterTransactionType.Approve -> TransactionTag.APPROVE
        }

        return listOfNotNull(filterCoin, filterTag).map { listOf(it) }
    }
}