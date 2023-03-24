package net.pelennor.garagio.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import net.pelennor.garagio.network.GaragioApi
import net.pelennor.garagio.network.NetworkMonitor

const val UNKNOWN_TEMPERATURE = -198.4f

enum class GaragioDoorState {
    Unknown,
    Open,
    Closed,
    Opening,
    Closing
}

sealed interface GaragioStatus {
    data class OK(
        val timestamp: UInt,
        val door0State: GaragioDoorState,
        val door0Age: UInt,
        val door1State: GaragioDoorState,
        val door1Age: UInt,
        val temp: Float,
        val uptime: UInt,
        val rssi: Int,
        val heap: UInt,
    ): GaragioStatus
    sealed interface Error: GaragioStatus { val error: String }
    data class AccountError(override val error: String): Error
    data class AuthError(override val error: String): Error
    data class ProtoError(override val error: String): Error
    data class NetworkError(override val error: String): Error
    object OfflineError: Error { override val error = "" }
    data class OtherError(override val error: String): Error
}

private class GaragioRepositoryRefreshPolicy {
    companion object {
        const val MIN_REFRESH_INTERVAL_MS = 4_000L
        const val DEF_REFRESH_INTERVAL_MS = 64_000L
        const val MAX_REFRESH_INTERVAL_MS = 64_000L
    }

    private var errorCount = 0
    private var movingCount = 0

    private fun GaragioDoorState.isMoving(): Boolean {
        return this==GaragioDoorState.Opening || this==GaragioDoorState.Closing
    }

    private fun GaragioStatus.isMoving(): Boolean {
        return this is GaragioStatus.OK && (door0State.isMoving() || door1State.isMoving())
    }

    var curRefreshInterval: Long = DEF_REFRESH_INTERVAL_MS
        private set

    fun applyStatus(status: GaragioStatus) {
        errorCount = if (status is GaragioStatus.OK) 0 else errorCount+1
        movingCount = if (status.isMoving()) movingCount+1 else 0

        if (errorCount == 1 || movingCount in 1..5)
            curRefreshInterval = MIN_REFRESH_INTERVAL_MS
        else if (errorCount>0 || movingCount>0)
            curRefreshInterval = curRefreshInterval.times(2).coerceAtMost(MAX_REFRESH_INTERVAL_MS)
        else
            curRefreshInterval = DEF_REFRESH_INTERVAL_MS
    }

    fun reset() {
        errorCount = 0
        movingCount = 0
    }
}

class GaragioRepository(
    private val api: GaragioApi,
    private val accountRepository: GaragioAccountRepository,
    private val networkMonitor: NetworkMonitor,
) {
    private val refreshNow = Mutex(true)
    private val refreshPolicy = GaragioRepositoryRefreshPolicy()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val observeStatus: Flow<GaragioStatus> =
        accountRepository.garagioAccount.transformLatest { account ->
            try {
                with(account) { api.setAccount(baseUrl, username, password ?: "") }
            } catch (e: Exception) {
                emit(GaragioStatus.AccountError(e.toString()))
                return@transformLatest
            }
            networkMonitor.isOnline
                .distinctUntilChanged()
                .debounce { if (it) 1_000L else 0L }
                .transformLatest isOnline@ { online ->
                    if (!online) {
                        emit(GaragioStatus.OfflineError)
                        return@isOnline
                    }

                    // collect status from API
                    while (true) {
                        try {
                            withContext(Dispatchers.IO) { api.getStatus() }
                        } catch (e: Exception) {
                            GaragioStatus.NetworkError(e.toString())
                        }.let { status ->
                            emit(status)
                            refreshPolicy.applyStatus(status)
                        }
                        // wait current refresh interval, or until manually refreshed
                        withTimeoutOrNull(refreshPolicy.curRefreshInterval) { refreshNow.lock() }
                    }
                }
                .collect { status -> emit(status) }
        }

    val observeAccount: Flow<GaragioAccount> =
        accountRepository.garagioAccount.map { account ->
            GaragioAccount(account.baseUrl, account.username, null)
        }

    suspend fun updateAccount(baseUrl: String, username: String, password: String?) {
        accountRepository.saveGaragioAccount(GaragioAccount(baseUrl, username, password))
    }

    private fun refreshNow() {
        try {
            refreshNow.unlock()
        } catch (_: IllegalStateException) {
        }
        refreshPolicy.reset()
    }

    suspend fun openDoor0(): Boolean {
        return withContext(Dispatchers.IO) { api.openDoor0() }.also { if (it) refreshNow() }
    }
    suspend fun openDoor1(): Boolean {
        return withContext(Dispatchers.IO) { api.openDoor1() }.also { if (it) refreshNow() }
    }
    suspend fun closeDoor0(): Boolean {
        return withContext(Dispatchers.IO) { api.closeDoor0() }.also { if (it) refreshNow() }
    }
    suspend fun closeDoor1(): Boolean {
        return withContext(Dispatchers.IO) { api.closeDoor1() }.also { if (it) refreshNow() }
    }
}