package com.example.idatum.model

/**
 * Exact OBIS codes and DLMS Constants recovered from the i-DATUM APK assembly
 * (IntelliSmart.HHU.App.Constants.Constant) and Indian Smart Metering Standards.
 */
object Constant {
    const val InvocationCounterMR = "0.0.43.1.2.255"
    const val InvocationCounterUS = "0.0.43.1.3.255"
    const val DisConnectControlOBIS = "0.0.96.3.10.255"
    const val MdResetOBIS = "0.0.10.0.1.255"
    const val MeteringModeOBIS = "0.0.94.96.19.255"
    const val MeteringModeOBIS_SEIPL = "0.0.94.91.19.255"

    // Standard Energy & Demand OBIS
    const val KwhImportOBIS = "1.0.1.8.0.255"
    const val KwhExportOBIS = "1.0.2.8.0.255"
    const val KvahImportOBIS = "1.0.9.8.0.255"
    const val KvahExportOBIS = "1.0.10.8.0.255"
    const val KwMDOBIS = "1.0.1.6.0.255"
    const val KvaMDOBIS = "1.0.9.6.0.255"

    // Instantaneous 3-Phase & Telemetry OBIS
    const val VoltageRPhaseOBIS = "1.0.32.7.0.255"
    const val VoltageYPhaseOBIS = "1.0.52.7.0.255"
    const val VoltageBPhaseOBIS = "1.0.72.7.0.255"
    const val CurrentRPhaseOBIS = "1.0.31.7.0.255"
    const val CurrentYPhaseOBIS = "1.0.51.7.0.255"
    const val CurrentBPhaseOBIS = "1.0.71.7.0.255"
    const val PowerFactorOBIS = "1.0.13.7.0.255"
    const val FrequencyOBIS = "1.0.14.7.0.255"
    const val ActivePowerKwOBIS = "1.0.1.7.0.255"
    const val ReactivePowerKvarOBIS = "1.0.3.7.0.255"
    const val ApparentPowerKvaOBIS = "1.0.9.7.0.255"

    // CT Parameters (Current Transformer Commissioning)
    const val CtRatioPrimaryOBIS = "1.0.0.4.2.255"
    const val CtRatioSecondaryOBIS = "1.0.0.4.5.255"
    const val PtRatioPrimaryOBIS = "1.0.0.4.3.255"
    const val PtRatioSecondaryOBIS = "1.0.0.4.6.255"

    // Tamper Diagnostics Count OBIS
    const val TamperMagneticOBIS = "0.0.94.91.41.255"
    const val TamperNeutralDisturbanceOBIS = "0.0.94.91.42.255"
    const val TamperCoverOpenOBIS = "0.0.94.91.43.255"
    const val TamperCtBypassOBIS = "0.0.94.91.44.255"
    const val TamperOverVoltageOBIS = "0.0.94.91.45.255"
    const val TamperReverseCurrentOBIS = "0.0.94.91.46.255"

    // TOU Profile OBIS
    const val TouActivityCalendarOBIS = "0.0.13.0.0.255"
    const val TouPassiveDayProfileOBIS = "0.0.13.0.1.255"

    // Meter Clock OBIS
    const val ClockOBIS = "0.0.1.0.0.255"
}

enum class ProbeInterfaceType {
    BLUETOOTH,
    SERIAL_USB,
    SIMULATOR
}

enum class Discom(val displayName: String, val code: String) {
    SBPDCL("South Bihar Power Distribution (SBPDCL)", "SBPDCL"),
    NBPDCL("North Bihar Power Distribution (NBPDCL)", "NBPDCL"),
    MGVCL("Madhya Gujarat Vij Company (MGVCL)", "MGVCL"),
    DGVCL("Dakshin Gujarat Vij Company (DGVCL)", "DGVCL"),
    APDCL("Assam Power Distribution (APDCL)", "APDCL"),
    PVVNL("Paschimanchal Vidyut Vitran (PVVNL)", "PVVNL"),
    MVVNL("Madhyanchal Vidyut Vitran (MVVNL)", "MVVNL"),
    INTELLISMART("IntelliSmart Infra Cloud", "INTELLI")
}

data class ProfileItem(
    val id: String,
    val isEnable: Boolean,
    var isChecked: Boolean,
    val scaler: String,
    val profile: String,
    val objectName: String,
    val shortName: String
)

data class ManualReadModel(
    val id: Long = 0,
    val smoid: String = "",
    val meterNumber: String = "",
    val meterDTTM: String = "",
    val kwhImport: String = "",
    val kvahImport: String = "",
    val kwhExport: String = "",
    val kvahExport: String = "",
    val kwMD: String = "",
    val kvaMD: String = "",
    val kwhImportImage: String = "",
    val kwhExportImage: String = "",
    val kvahImportImage: String = "",
    val kvahExportImage: String = "",
    val kwMDImage: String = "",
    val kvaMDImage: String = "",
    val remarks: String = "",
    val audit: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class MeterDataModel(
    val serialNumber: String = "",
    val rtcDrift: String = "0s",
    val coverOpenStatus: String = "Normal (Closed)",
    val meterHealthStatus: String = "Healthy",
    val energyKwh: String = "0.00",
    val relayStatus: String = "Connected",
    val rssiValue: String = "-68 dBm",
    val rssiNumber: Int = 4,
    val meterClock: String = "",
    val rtcBatteryStatus: String = "Good (3.6V)",
    val status: String = "Success",
    val message: String = "Meter responded with valid AARE packet"
)

data class WfmTicketModel(
    val ticketId: String,
    val meterNumber: String,
    val method: String = "DLMS Optical",
    val userId: String = "",
    val hesId: String = "",
    val meterMake: String = "Genus / Schneider",
    val endPoint: String = "",
    val smoid: String = "",
    val appPackage: String = "com.intellismartinfra.IntelliSmart.HHU",
    val packet: String = "",
    val ticketCreationDate: String = "",
    val eventDate: String = "",
    val eventCode: String = "",
    val eventType: String = "Periodic Readout & Meter Health",
    val consumerNumber: String = "",
    val consumerName: String = "",
    val subDivision: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "Pending", // Pending, Completed, Synced
    val tokenValue: String = ""
)

data class DefaultSecurityKeyModel(
    val id: Int = 1,
    val meterMake: String = "All Makes",
    val discomm: String = "All DISCOMs",
    val lls: String = "00000000",
    val hls: String = "IntelliSmart@2024",
    val ek: String = "00112233445566778899AABBCCDDEEFF",
    val ak: String = "FFEEDDCCBBAA99887766554433221100",
    val fw: String = "1.0.4"
)

data class ValidateMeterModel(
    val serialNumber: String,
    val meterMake: String,
    val discomm: String,
    val lls: String,
    val hls: String,
    val ak: String,
    val ek: String,
    val fw: String,
    val ip: String,
    val port: Int
)

data class TouSlot(
    val slotNumber: Int,
    val startTime: String,
    val endTime: String,
    val tariffRateName: String,
    val multiplier: Double
)

data class CommissioningParameter(
    val obis: String,
    val name: String,
    val value: String,
    val unit: String,
    val isWritable: Boolean,
    val verified: Boolean
)
