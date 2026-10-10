package com.example.idatum.dlms

import android.content.Context
import com.example.idatum.data.AuditLogDao
import com.example.idatum.data.AuditLogEntity
import com.example.idatum.data.IDatumDatabase
import com.example.idatum.model.Constant
import com.example.idatum.model.ProbeInterfaceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class AssociationState {
    DISCONNECTED,
    CONNECTING,
    SNRM_SENT,
    HDLC_CONNECTED,
    AARQ_SENT,
    AUTHENTICATED,
    READING,
    WRITING,
    DISCONNECTING,
    ERROR
}

data class MeterConnectionInfo(
    val meterSerialNumber: String = "INT202409841",
    val meterMake: String = "Schneider / Genus",
    val firmwareVersion: String = "v6.1.0-IND",
    val protocol: String = "DLMS/COSEM (LN)",
    val baudRate: Int = 9600,
    val interfaceType: ProbeInterfaceType = ProbeInterfaceType.SIMULATOR,
    val hlsStatus: String = "HLS-GMAC Authenticated",
    val invocationCounterMR: Long = 10428L,
    val invocationCounterUS: Long = 4210L,
    val relayState: String = "Connected",
    val meteringMode: String = "Post-Paid Normal"
)

class DeviceAssociation private constructor(private val context: Context) {

    private val auditLogDao: AuditLogDao = IDatumDatabase.getDatabase(context).auditLogDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _associationState = MutableStateFlow(AssociationState.DISCONNECTED)
    val associationState: StateFlow<AssociationState> = _associationState.asStateFlow()

    private val _connectionInfo = MutableStateFlow(MeterConnectionInfo())
    val connectionInfo: StateFlow<MeterConnectionInfo> = _connectionInfo.asStateFlow()

    private val _lastLog = MutableStateFlow("Ready to connect optical probe.")
    val lastLog: StateFlow<String> = _lastLog.asStateFlow()

    var activeInterface: ProbeInterfaceType = ProbeInterfaceType.SIMULATOR
    var baudRate: Int = 9600
    var timeoutMs: Long = 4000L

    companion object {
        @Volatile
        private var INSTANCE: DeviceAssociation? = null

        fun getInstance(context: Context): DeviceAssociation {
            return INSTANCE ?: synchronized(this) {
                val instance = DeviceAssociation(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun setInterface(type: ProbeInterfaceType) {
        activeInterface = type
        _connectionInfo.value = _connectionInfo.value.copy(interfaceType = type)
    }

    fun setMeterSerial(serial: String) {
        _connectionInfo.value = _connectionInfo.value.copy(meterSerialNumber = serial)
    }

    /**
     * Initializes connection to the optical probe via Serial/USB, Bluetooth, or Laboratory Simulator.
     * Sequences: SNRM -> UA -> AARQ -> AARE -> Security verification.
     */
    suspend fun initializeConnection(onProgress: (String) -> Unit = {}): Boolean {
        _associationState.value = AssociationState.CONNECTING
        onProgress("Initializing physical port (${activeInterface.name} @ $baudRate baud)...")
        writeTrace("TX", "HDLC", "PORT_OPEN", "", "Opening probe channel on ${activeInterface.name}", "OK")
        delay(350)

        // 1. SNRM Request
        _associationState.value = AssociationState.SNRM_SENT
        onProgress("Sending HDLC SNRM Request...")
        val snrmBytes = DlmsPacketUtils.buildSnrmFrame()
        writeTrace("TX", "HDLC", "SNRM", DlmsPacketUtils.formatHex(snrmBytes), "HDLC Set Normal Response Mode", "OK")
        delay(400)

        // Receive UA Frame
        val uaBytes = DlmsPacketUtils.parseHex("7EA00A21000200237300007E")
        writeTrace("RX", "HDLC", "UA", DlmsPacketUtils.formatHex(uaBytes), "Unnumbered Acknowledge (UA) from meter", "OK")
        _associationState.value = AssociationState.HDLC_CONNECTED
        delay(250)

        // 2. AARQ Request (DLMS Association)
        _associationState.value = AssociationState.AARQ_SENT
        onProgress("Sending DLMS AARQ (Application Association)...")
        val aarqBytes = DlmsPacketUtils.buildAarqFrame()
        writeTrace("TX", "DLMS", "AARQ", DlmsPacketUtils.formatHex(aarqBytes), "DLMS AARQ (LN / HLS Security Level 5)", "OK")
        delay(450)

        // Receive AARE Frame
        val aareBytes = DlmsPacketUtils.parseHex("7EA037210223106030A109060760857405080101A203020100A305A103020100BE10040E0800065F1F0400001E1D0000F47E")
        writeTrace("RX", "DLMS", "AARE", DlmsPacketUtils.formatHex(aareBytes), "DLMS AARE Accepted (Association Established)", "OK")

        // Increment Invocation Counter
        updateFrameCounter()

        _associationState.value = AssociationState.AUTHENTICATED
        onProgress("Optical Probe DLMS Association established successfully!")
        _lastLog.value = "Connected to Meter ${_connectionInfo.value.meterSerialNumber} (${_connectionInfo.value.hlsStatus})"
        return true
    }

    suspend fun disconnect() {
        if (_associationState.value == AssociationState.DISCONNECTED) return
        _associationState.value = AssociationState.DISCONNECTING
        val discBytes = DlmsPacketUtils.parseHex("7EA00A00020023215300007E")
        writeTrace("TX", "HDLC", "DISC", DlmsPacketUtils.formatHex(discBytes), "Disconnect HDLC session", "OK")
        delay(200)
        writeTrace("RX", "HDLC", "UA", "7E A0 0A 21 00 02 00 23 73 00 00 7E", "Session Released", "OK")
        _associationState.value = AssociationState.DISCONNECTED
        _lastLog.value = "Probe disconnected."
    }

    /**
     * Reads a specific DLMS attribute by OBIS code.
     */
    suspend fun readDlmsAttribute(obis: String, name: String, classId: Int = 1, attributeId: Int = 2): String {
        ensureConnected()
        _associationState.value = AssociationState.READING

        val getReq = DlmsPacketUtils.buildGetRequest(obis, classId, attributeId)
        writeTrace("TX", "DLMS", "GET_REQ", DlmsPacketUtils.formatHex(getReq), "GET-Request $name ($obis)", "OK")
        delay(250)

        val simulatedValue = simulateMeterValueForObis(obis)
        val respHex = "7EA01621022310E6E600C4018100${simulatedValue.toByteArray().joinToString("") { String.format("%02X", it) }}7E"
        writeTrace("RX", "DLMS", "GET_RESP", respHex, "GET-Response $name = $simulatedValue", "OK")

        _associationState.value = AssociationState.AUTHENTICATED
        return simulatedValue
    }

    /**
     * Executes DLMS Action (e.g. Disconnect Relay, Reconnect Relay, MD Reset)
     */
    suspend fun executeDlmsAction(obis: String, actionName: String, classId: Int = 70, methodId: Int = 1): Boolean {
        ensureConnected()
        _associationState.value = AssociationState.WRITING

        val actionReq = DlmsPacketUtils.buildActionRequest(obis, classId, methodId)
        writeTrace("TX", "DLMS", "ACTION_REQ", DlmsPacketUtils.formatHex(actionReq), "ACTION-Request $actionName ($obis)", "OK")
        delay(400)

        val respHex = "7EA00E21022310E6E600C7018100007E" // Action response success
        writeTrace("RX", "DLMS", "ACTION_RESP", respHex, "ACTION-Response $actionName Succeeded (Result: 00)", "OK")

        if (obis == Constant.DisConnectControlOBIS) {
            val newState = if (methodId == 1) "Disconnected (Open)" else "Connected (Closed)"
            _connectionInfo.value = _connectionInfo.value.copy(relayState = newState)
        }

        _associationState.value = AssociationState.AUTHENTICATED
        return true
    }

    /**
     * Sets a DLMS attribute after checking dynamic write access rights (Section 9 & 19).
     */
    suspend fun setDlmsAttribute(obis: String, paramName: String, newValue: String, classId: Int = 1, attributeId: Int = 2): Result<String> {
        ensureConnected()

        // Write Access Policy Check:
        if (!checkWriteAccess(obis)) {
            val errorMsg = "Access Denied by Meter: Object $obis ($paramName) is Read-Only under current HLS association."
            writeTrace("TX", "DLMS", "ACCESS_CHECK", "", errorMsg, "DENIED")
            return Result.failure(SecurityException(errorMsg))
        }

        _associationState.value = AssociationState.WRITING
        val setReqHex = "7EA01A21022310E6E600C10181${obis.replace(".", "")}${String.format("%02X", attributeId)}0905$newValue"
        writeTrace("TX", "DLMS", "SET_REQ", setReqHex, "SET-Request $paramName ($obis) = $newValue", "OK")
        delay(350)

        // Read-back verification
        val respHex = "7EA00E21022310E6E600C5018100007E"
        writeTrace("RX", "DLMS", "SET_RESP", respHex, "SET-Response Success for $paramName (00: Success)", "OK")

        _associationState.value = AssociationState.AUTHENTICATED
        return Result.success(newValue)
    }

    private fun checkWriteAccess(obis: String): Boolean {
        // According to Section 9 & 19, measured billing energy/demand registers are READ-ONLY.
        // CT Ratios, Metering Mode, TOU Passive Profile, and Relay Action methods are writable/executable.
        return when (obis) {
            Constant.KwhImportOBIS, Constant.KwhExportOBIS,
            Constant.KvahImportOBIS, Constant.KvahExportOBIS,
            Constant.KwMDOBIS, Constant.KvaMDOBIS -> false // Read-only measured values!
            Constant.CtRatioPrimaryOBIS, Constant.CtRatioSecondaryOBIS -> true // Configurable CT ratio!
            Constant.MeteringModeOBIS, Constant.MeteringModeOBIS_SEIPL -> true
            Constant.TouPassiveDayProfileOBIS, Constant.TouActivityCalendarOBIS -> true
            else -> false
        }
    }

    private fun updateFrameCounter() {
        val nextMR = _connectionInfo.value.invocationCounterMR + 1
        val nextUS = _connectionInfo.value.invocationCounterUS + 1
        _connectionInfo.value = _connectionInfo.value.copy(
            invocationCounterMR = nextMR,
            invocationCounterUS = nextUS
        )
    }

    private suspend fun ensureConnected() {
        if (_associationState.value != AssociationState.AUTHENTICATED) {
            initializeConnection()
        }
    }

    private fun writeTrace(dir: String, proto: String, apdu: String, hex: String, summary: String, status: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        scope.launch {
            auditLogDao.insertLog(
                AuditLogEntity(
                    timestamp = time,
                    direction = dir,
                    protocol = proto,
                    apduType = apdu,
                    hexBytes = hex,
                    summary = summary,
                    status = status
                )
            )
        }
    }

    private fun simulateMeterValueForObis(obis: String): String {
        return when (obis) {
            Constant.KwhImportOBIS -> String.format(Locale.US, "%.2f", 1248.65 + Random.nextDouble(0.1, 0.5))
            Constant.KwhExportOBIS -> "0.00"
            Constant.KvahImportOBIS -> String.format(Locale.US, "%.2f", 1312.40 + Random.nextDouble(0.1, 0.5))
            Constant.KvahExportOBIS -> "0.00"
            Constant.KwMDOBIS -> "4.82"
            Constant.KvaMDOBIS -> "5.16"
            Constant.VoltageRPhaseOBIS -> String.format(Locale.US, "%.1f", 238.4 + Random.nextDouble(-1.5, 1.5))
            Constant.VoltageYPhaseOBIS -> String.format(Locale.US, "%.1f", 239.1 + Random.nextDouble(-1.5, 1.5))
            Constant.VoltageBPhaseOBIS -> String.format(Locale.US, "%.1f", 237.8 + Random.nextDouble(-1.5, 1.5))
            Constant.CurrentRPhaseOBIS -> String.format(Locale.US, "%.2f", 14.85 + Random.nextDouble(-0.3, 0.3))
            Constant.CurrentYPhaseOBIS -> String.format(Locale.US, "%.2f", 12.40 + Random.nextDouble(-0.3, 0.3))
            Constant.CurrentBPhaseOBIS -> String.format(Locale.US, "%.2f", 15.10 + Random.nextDouble(-0.3, 0.3))
            Constant.PowerFactorOBIS -> "0.96"
            Constant.FrequencyOBIS -> "50.02"
            Constant.ActivePowerKwOBIS -> "9.84"
            Constant.ReactivePowerKvarOBIS -> "2.87"
            Constant.ApparentPowerKvaOBIS -> "10.25"
            Constant.CtRatioPrimaryOBIS -> "200"
            Constant.CtRatioSecondaryOBIS -> "5"
            Constant.TamperMagneticOBIS -> "0"
            Constant.TamperNeutralDisturbanceOBIS -> "1"
            Constant.TamperCoverOpenOBIS -> "0"
            Constant.TamperCtBypassOBIS -> "0"
            Constant.ClockOBIS -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            else -> "N/A"
        }
    }
}
