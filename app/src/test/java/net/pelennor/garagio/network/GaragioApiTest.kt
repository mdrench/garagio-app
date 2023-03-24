package net.pelennor.garagio.network

import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.test.runTest
import net.pelennor.garagio.data.GaragioDoorState

class GaragioApiTest {
    private val api = GaragioWebService()
    @Test
    fun garagioApi_getRawStatus_noExceptions() {
        val status = api.getRawStatus()
        println("getRawStatus => '$status'")
    }
    @Test
    fun garagioApi_getStatus_noExceptions() {
        val status = api.getStatus()
        println("getStatus => '$status'")
    }
    @Test
    fun garagioApi_controlDoor0_noExceptions() {
        api.closeDoor0()
        api.openDoor0()
    }
    @Test
    fun garagioApi_controlDoor1_noExceptions() {
        api.closeDoor1()
        api.openDoor1()
    }
    @Test
    fun garagioApi_parseDoorState_open() {
        assertEquals(GaragioDoorState.Open, api.parseDoorState("open"))
        assertEquals(GaragioDoorState.Open, api.parseDoorState("OPEN"))
        assertEquals(GaragioDoorState.Open, api.parseDoorState("oPeN"))
        assertEquals(GaragioDoorState.Open, api.parseDoorState("Open"))
    }
    @Test
    fun garagioApi_parseDoorState_closed() {
        assertEquals(GaragioDoorState.Closed, api.parseDoorState("closed"))
        assertEquals(GaragioDoorState.Closed, api.parseDoorState("CLOSED"))
        assertEquals(GaragioDoorState.Closed, api.parseDoorState("cLoSeD"))
        assertEquals(GaragioDoorState.Closed, api.parseDoorState("Closed"))
    }
    @Test
    fun garagioApi_parseDoorState_opening() {
        assertEquals(GaragioDoorState.Opening, api.parseDoorState("opening"))
        assertEquals(GaragioDoorState.Opening, api.parseDoorState("OPENING"))
        assertEquals(GaragioDoorState.Opening, api.parseDoorState("oPeNiNg"))
        assertEquals(GaragioDoorState.Opening, api.parseDoorState("Opening"))
    }
    @Test
    fun garagioApi_parseDoorState_closing() {
        assertEquals(GaragioDoorState.Closing, api.parseDoorState("closing"))
        assertEquals(GaragioDoorState.Closing, api.parseDoorState("CLOSING"))
        assertEquals(GaragioDoorState.Closing, api.parseDoorState("cLoSiNg"))
        assertEquals(GaragioDoorState.Closing, api.parseDoorState("Closing"))
    }
    @Test
    fun garagioApi_parseDoorState_unknown() {
        assertEquals(GaragioDoorState.Unknown, api.parseDoorState("unknown"))
        assertEquals(GaragioDoorState.Unknown, api.parseDoorState("UNKNOWN"))
        assertEquals(GaragioDoorState.Unknown, api.parseDoorState("NotFound"))
        assertEquals(GaragioDoorState.Unknown, api.parseDoorState(""))
    }
}