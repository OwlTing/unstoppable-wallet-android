package io.horizontalsystems.bankwallet.core.adapters

import com.owlting.app.stellarkit.Network
import com.owlting.app.stellarkit.StellarKit
import com.owlting.app.stellarkit.models.Alphanum4
import com.owlting.app.stellarkit.models.Native
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.managers.StellarKitWrapper
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class StellarAdapter(
    stellarKitWrapper: StellarKitWrapper,
) : IAdapter, IBalanceAdapter, IReceiveAdapter {

    private val stellarKit = stellarKitWrapper.stellarKit

    private var syncState: AdapterState = AdapterState.Syncing()
        set(value) {
            if (value != field) {
                field = value
                adapterStateUpdatedSubject.onNext(Unit)
            }
        }

    override val receiveAddress: String
        get() = stellarKit.accountId

    override val isMainNet: Boolean
        get() = stellarKit.network == Network.Mainnet

    suspend fun isAccountActive(address: String): Boolean = withContext(Dispatchers.IO) {
        stellarKit.isAccountActive(address)
    }

    fun isOwnAccount(accountId: String): Boolean {
        return accountId == stellarKit.accountId
    }

    //    protected val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    override val balanceState: AdapterState
        get() = convertToAdapterState(stellarKit.syncState)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = stellarKit.syncStateFlow.map {}.asFlowable()

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(stellarKit.balance, decimal))

    val baseTokenBalanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(stellarKit.baseTokenBalance, decimal))

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = stellarKit.balanceFlow.map {}.asFlowable()

    override fun start() {
        stellarKit.start()
    }

    override fun stop() {
        stellarKit.stop()
    }

    override fun refresh() {
        stellarKit.refresh()
    }

    fun send(token: Token, amount: BigInteger, accountId: String, memo: String) {

        val asset = when (token.type) {
            is TokenType.Alphanum4 -> Alphanum4(token.coin.code, (token.type as TokenType.Alphanum4).issuer)
            else -> Native()
        }

        stellarKit.send(asset, amount, accountId, memo)
    }

    private fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
    }

    private fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
        return amount.movePointLeft(decimals).stripTrailingZeros()
    }

    protected fun scaleUp(amount: BigDecimal, decimals: Int = decimal): BigInteger {
        return amount.movePointRight(decimals).toBigInteger()
    }

    private fun convertToAdapterState(syncState: StellarKit.SyncState): AdapterState =
        when (syncState) {
            is StellarKit.SyncState.Synced -> AdapterState.Synced
            is StellarKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is StellarKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    override val debugInfo: String
        get() = TODO("Not yet implemented")

    companion object {
        const val decimal = 7
    }
}