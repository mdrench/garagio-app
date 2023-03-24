package net.pelennor.garagio.network

import net.pelennor.garagio.data.GaragioDoorState
import net.pelennor.garagio.data.GaragioStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Credentials
import okhttp3.RequestBody.Companion.toRequestBody

interface GaragioApi {
    fun setAccount(baseUrl: String, username: String, password: String)
    fun getStatus(): GaragioStatus
    fun openDoor0(): Boolean
    fun closeDoor0(): Boolean
    fun openDoor1(): Boolean
    fun closeDoor1(): Boolean
}

class GaragioWebService: GaragioApi {
    private val www = OkHttpClient()
    private val empty = "".toRequestBody()

    private var reqBuilder: Request.Builder? = null
    private var statusReq: Request? = null
    private var door0OpenReq: Request? = null
    private var door1OpenReq: Request? = null
    private var door0CloseReq: Request? = null
    private var door1CloseReq: Request? = null

    override fun setAccount(baseUrl: String, username: String, password: String) {
        require(baseUrl.startsWith("https://")) { "Only 'https://' URLs are supported." }
        reqBuilder = Request.Builder().header("Authorization", Credentials.basic(username, password))
        checkNotNull(reqBuilder) { "Unable to construct Auth header from username/password." }
        statusReq = reqBuilder?.url("$baseUrl/")?.build()
        checkNotNull(statusReq) { "Unable to parse base URL." }
        door0OpenReq = reqBuilder?.url("$baseUrl/door1/open")?.post(empty)?.build()
        door1OpenReq = reqBuilder?.url("$baseUrl/door1/open")?.post(empty)?.build()
        door0CloseReq = reqBuilder?.url("$baseUrl/door0/close")?.post(empty)?.build()
        door1CloseReq = reqBuilder?.url("$baseUrl/door1/close")?.post(empty)?.build()
        checkNotNull(door0OpenReq)
        checkNotNull(door1OpenReq)
        checkNotNull(door0CloseReq)
        checkNotNull(door1CloseReq)
    }

    private fun parseStatus(status: String): GaragioStatus {
        val tokens = status.split(' ')

        if (tokens.count() < 14)
            return GaragioStatus.ProtoError("Unable to parse status response")

        return GaragioStatus.OK(
            timestamp = (System.nanoTime()/1_000_000_000L).toUInt(),
            door0State = parseDoorState(tokens[1]),
            door0Age = try { tokens[2].toUInt() } catch (e: Exception) { 0u },
            door1State = parseDoorState(tokens[4]),
            door1Age = try { tokens[5].toUInt() } catch (e: Exception) { 0u },
            temp = try { tokens[7].toFloat() } catch (e: Exception) { -198.4f },
            rssi = try { tokens[9].toInt() } catch (e: Exception) { 0 },
            heap = try { tokens[11].toUInt() } catch (e: Exception) { 0u },
            uptime = try { tokens[13].toUInt() } catch (e: Exception) { 0u },
        )
    }

    internal fun parseDoorState(name: String): GaragioDoorState {
        return try {
            GaragioDoorState.valueOf(name.substring(0, 1).uppercase() + name.substring(1).lowercase())
        } catch (e: Exception) {
            GaragioDoorState.Unknown
        }
    }

    internal fun getRawStatus(): String? {
        return www.newCall(statusReq!!).execute().body?.string()
    }

    override fun getStatus(): GaragioStatus {
        www.newCall(statusReq!!).execute().run {
            val body = body?.string()
            if (code == 403) return GaragioStatus.AuthError("HTTP 403/Unauthorized ($body)")
            if (isSuccessful) return parseStatus(body ?: "")
            return GaragioStatus.ProtoError("HTTP ${code}/$body")
        }
    }

    private fun sendCommand(cmd: Request?): Boolean {
        try {
            www.newCall(cmd!!).execute().run { body?.close(); return isSuccessful }
        } catch (e: Exception) {
            return false
        }
    }

    override fun openDoor0(): Boolean = sendCommand(door0OpenReq)
    override fun openDoor1(): Boolean = sendCommand(door1OpenReq)
    override fun closeDoor0(): Boolean = sendCommand(door0CloseReq)
    override fun closeDoor1(): Boolean = sendCommand(door1CloseReq)
}

@Suppress("unused")
class GaragioNullService: GaragioApi {
    override fun setAccount(baseUrl: String, username: String, password: String) = Unit
    override fun getStatus(): GaragioStatus = GaragioStatus.OtherError("Null")
    override fun openDoor0(): Boolean = false
    override fun closeDoor0(): Boolean = false
    override fun openDoor1(): Boolean = false
    override fun closeDoor1(): Boolean = false
}