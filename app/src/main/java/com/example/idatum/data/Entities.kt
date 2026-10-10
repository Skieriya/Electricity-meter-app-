package com.example.idatum.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val ticketId: String,
    val meterNumber: String,
    val method: String,
    val userId: String,
    val meterMake: String,
    val consumerNumber: String,
    val consumerName: String,
    val subDivision: String,
    val latitude: Double,
    val longitude: Double,
    val eventType: String,
    val ticketCreationDate: String,
    val status: String,
    val tokenValue: String
)

@Entity(tableName = "manual_reads")
data class ManualReadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val smoid: String,
    val meterNumber: String,
    val meterDTTM: String,
    val kwhImport: String,
    val kvahImport: String,
    val kwhExport: String,
    val kvahExport: String,
    val kwMD: String,
    val kvaMD: String,
    val kwhImportImage: String,
    val kwhExportImage: String,
    val kvahImportImage: String,
    val kvahExportImage: String,
    val kwMDImage: String,
    val kvaMDImage: String,
    val remarks: String,
    val audit: String,
    val isSynced: Boolean = false,
    val createdAt: String
)

@Entity(tableName = "commissioning_records")
data class CommissioningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meterNumber: String,
    val ctPrimary: String,
    val ctSecondary: String,
    val voltageR: String,
    val voltageY: String,
    val voltageB: String,
    val currentR: String,
    val currentY: String,
    val currentB: String,
    val powerFactor: String,
    val activePowerKw: String,
    val reactivePowerKvar: String,
    val apparentPowerKva: String,
    val tamperMagneticCount: Int,
    val tamperNeutralCount: Int,
    val tamperCoverOpenCount: Int,
    val tamperCtBypassCount: Int,
    val isCtRatioVerified: Boolean,
    val technicianId: String,
    val timestamp: String,
    val isSynced: Boolean = false
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: String,
    val direction: String, // TX or RX
    val protocol: String,  // HDLC / DLMS / COSEM
    val apduType: String,  // SNRM, UA, AARQ, AARE, GET, SET, ACTION
    val hexBytes: String,
    val summary: String,
    val status: String     // OK / ERROR / PENDING
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemType: String, // READOUT, COMMISSIONING, RELAY_ACTION, MD_RESET
    val referenceId: String,
    val payloadJson: String,
    val status: String = "Pending", // Pending, Uploading, Synced, Failed
    val createdAt: String,
    val attempts: Int = 0,
    val lastError: String? = null
)
