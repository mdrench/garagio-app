package net.pelennor.garagio.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class GaragioAccount(
    val baseUrl: String = "",
    val username: String = "",
    val password: String? = null,
)

class GaragioAccountRepository(
    private var dataStore: DataStore<Preferences>
) {
    private companion object {
        val GARAGIO_ACCOUNT_BASE_URL = stringPreferencesKey("base_url")
        val GARAGIO_ACCOUNT_USERNAME = stringPreferencesKey("username")
        val GARAGIO_ACCOUNT_PASSWORD = stringPreferencesKey("password")
    }

    suspend fun saveGaragioAccount(account: GaragioAccount) {
        dataStore.edit { preferences ->
            preferences[GARAGIO_ACCOUNT_BASE_URL] = account.baseUrl
            preferences[GARAGIO_ACCOUNT_USERNAME] = account.username
            if (account.password != null) preferences[GARAGIO_ACCOUNT_PASSWORD] = account.password
        }
    }

    val garagioAccount: Flow<GaragioAccount> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                Log.e("GaragioServiceRepo", "Error reading preferences.", e)
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { preferences ->
            GaragioAccount(
                baseUrl = preferences[GARAGIO_ACCOUNT_BASE_URL] ?: "",
                username = preferences[GARAGIO_ACCOUNT_USERNAME] ?: "",
                password = preferences[GARAGIO_ACCOUNT_PASSWORD]
            )
        }

}