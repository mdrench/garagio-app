package net.pelennor.garagio.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.pelennor.garagio.GaragioApplication
import net.pelennor.garagio.R
import net.pelennor.garagio.data.GaragioDoorState
import net.pelennor.garagio.data.GaragioRepository
import net.pelennor.garagio.data.GaragioStatus
import net.pelennor.garagio.data.UNKNOWN_TEMPERATURE
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class GaragioUiStatus(
    val door0State: GaragioDoorState = GaragioDoorState.Unknown,
    val door1State: GaragioDoorState = GaragioDoorState.Unknown,
    val temperature: Float? = null,
    val isError: Boolean = false,
    val error: String? = null,
    @StringRes val errorDesc: Int = 0,
)

fun GaragioStatus.toUiStatus(): GaragioUiStatus {
    return when (this) {
        is GaragioStatus.OK -> GaragioUiStatus(door0State, door1State, temp.takeIf { it>UNKNOWN_TEMPERATURE })
        is GaragioStatus.Error -> GaragioUiStatus(isError = true, error = error, errorDesc = when (this) {
            is GaragioStatus.AccountError -> R.string.error_account_error_desc
            is GaragioStatus.AuthError -> R.string.error_auth_error_desc
            is GaragioStatus.NetworkError -> R.string.error_network_error_desc
            is GaragioStatus.OfflineError -> R.string.error_offline_error_desc
            is GaragioStatus.ProtoError -> R.string.error_proto_error_desc
            else -> R.string.error_other_error_desc
        })
    }
}

class GaragioViewModel (
    private val garagioRepository: GaragioRepository,
): ViewModel() {
    // boilerplate...
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GaragioViewModel((this[APPLICATION_KEY] as GaragioApplication).garagioRepository)
            }
        }
    }

    // ui state
    val uiStatus = garagioRepository.observeStatus
        .map { it.toUiStatus() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GaragioUiStatus())

    // error dialog: state
    var showErrorDialog by mutableStateOf(false)
    var savError by mutableStateOf("")
    var savErrorDesc by mutableStateOf(0)
    fun openErrorDialog() {
        if (uiStatus.value.isError) {
            savError = uiStatus.value.error ?: ""
            savErrorDesc = uiStatus.value.errorDesc
            showErrorDialog = true
        }
    }
    fun closeErrorDialog() {
        showErrorDialog = false
        savError = ""
    }

    // account settings dialog: state
    private val curAccount = garagioRepository.observeAccount
        .transform {
            emit(it)
            enAccountSettings = true
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var enAccountSettings by mutableStateOf(false)
    var showAccountSettingsDialog by mutableStateOf(false)
    var updAccountBaseUrl by mutableStateOf("")
    var updAccountUsername by mutableStateOf("")
    var updAccountPassword by mutableStateOf<String?>(null)
    var errAccountBaseUrl by mutableStateOf(true)
    // account settings dialog: functions
    fun openAccountSettings() {
        if (!enAccountSettings) return
        updAccountBaseUrl = curAccount.value?.baseUrl ?: ""
        updAccountUsername = curAccount.value?.username ?: ""
        updAccountPassword = curAccount.value?.password
        errAccountBaseUrl = false
        showAccountSettingsDialog = true
    }
    fun cancelAccountSettings() {
        showAccountSettingsDialog = false
        updAccountBaseUrl = ""
        updAccountUsername = ""
        updAccountPassword = null
    }
    fun trySaveAccountSettings() {
        if (validateAccountSettings()) saveAccountSettings()
    }
    private fun saveAccountSettings() {
        enAccountSettings = false
        showAccountSettingsDialog = false
        viewModelScope.launch {
            garagioRepository.updateAccount(updAccountBaseUrl, updAccountUsername, updAccountPassword)
            updAccountBaseUrl = ""
            updAccountUsername = ""
            updAccountPassword = null
            enAccountSettings = true
        }
    }
    private fun validateAccountSettings(): Boolean {
        errAccountBaseUrl = !with (updAccountBaseUrl) {
            startsWith("https://") && toHttpUrlOrNull() != null
        }
        return !errAccountBaseUrl
    }

    suspend fun openDoor0(): Boolean {
        return withContext(viewModelScope.coroutineContext) { garagioRepository.openDoor0() }
    }
    suspend fun openDoor1(): Boolean {
        return withContext(viewModelScope.coroutineContext) { garagioRepository.openDoor1() }
    }
    suspend fun closeDoor0(): Boolean {
        return withContext(viewModelScope.coroutineContext) { garagioRepository.closeDoor0() }
    }
    suspend fun closeDoor1(): Boolean {
        return withContext(viewModelScope.coroutineContext) { garagioRepository.closeDoor1() }
    }
}