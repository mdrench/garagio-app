package net.pelennor.garagio

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import net.pelennor.garagio.data.GaragioAccountRepository
import net.pelennor.garagio.data.GaragioRepository
import net.pelennor.garagio.network.ConnectivityManagerNetworkMonitor
import net.pelennor.garagio.network.GaragioApi
import net.pelennor.garagio.network.GaragioWebService
import net.pelennor.garagio.network.NetworkMonitor

private const val GARAGIO_ACCOUNT_PREFERENCE_FILE = "garagio_service"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(GARAGIO_ACCOUNT_PREFERENCE_FILE)

class GaragioApplication: Application() {
    lateinit var networkMonitor: NetworkMonitor
    lateinit var garagioApi: GaragioApi
    lateinit var garagioAccountRepository: GaragioAccountRepository
    lateinit var garagioRepository: GaragioRepository

    override fun onCreate() {
        super.onCreate()
        networkMonitor = ConnectivityManagerNetworkMonitor(this.applicationContext)
        garagioApi = GaragioWebService()
        garagioAccountRepository = GaragioAccountRepository(dataStore)
        garagioRepository = GaragioRepository(garagioApi, garagioAccountRepository, networkMonitor)
    }
}