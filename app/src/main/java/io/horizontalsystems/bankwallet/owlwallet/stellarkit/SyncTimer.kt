package io.horizontalsystems.bankwallet.owlwallet.stellarkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SyncTimer(
    private val syncInterval: Long,
    private val connectionManager: ConnectionManager
) {
    interface Listener {
        fun onUpdateSyncTimerState(state: State)
        fun sync()
    }

    private var scope: CoroutineScope? = null
    private var isStarted = false
    private var timerJob: Job? = null
    private var listener: Listener? = null

    init {
        connectionManager.listener = object : ConnectionManager.Listener {
            override fun onConnectionChange() {
                handleConnectionChange()
            }
        }
    }

    var state: State = State.NotReady(StellarKit.SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.onUpdateSyncTimerState(value)
            }
        }

    fun start(listener: Listener, scope: CoroutineScope) {
        isStarted = true

        this.listener = listener
        this.scope = scope

        handleConnectionChange()
    }

    fun stop() {
        isStarted = false

        connectionManager.stop()
        state = State.NotReady(StellarKit.SyncError.NotStarted())
        scope = null
        stopTimer()
    }

    private fun handleConnectionChange() {
        if (!isStarted) return

        if (connectionManager.isConnected) {
            state = State.Ready
            startTimer()
        } else {
            state = State.NotReady(StellarKit.SyncError.NoNetworkConnection())
            stopTimer()
        }
    }

    private fun startTimer() {
        timerJob = scope?.launch {
            flow {
                while (isActive) {
                    emit(Unit)
                    delay(syncInterval.toDuration(DurationUnit.SECONDS))
                }
            }.collect {
                listener?.sync()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    sealed class State {
        object Ready : State()
        class NotReady(val error: Throwable) : State()
    }
}