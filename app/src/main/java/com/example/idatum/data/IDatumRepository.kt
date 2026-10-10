package com.example.idatum.data

import android.content.Context
import com.example.idatum.model.ManualReadModel
import com.example.idatum.model.WfmTicketModel
import com.example.idatum.service.FileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IDatumRepository(private val context: Context) {
    private val db = IDatumDatabase.getDatabase(context)
    private val ticketDao = db.ticketDao()
    private val manualReadDao = db.manualReadDao()
    private val commissioningDao = db.commissioningDao()
    private val auditLogDao = db.auditLogDao()
    private val syncQueueDao = db.syncQueueDao()
    private val fileService = FileService(context)

    fun getAllTickets(): Flow<List<TicketEntity>> = ticketDao.getAllTickets()
    fun getAllReads(): Flow<List<ManualReadEntity>> = manualReadDao.getAllReads()
    fun getAllCommissioning(): Flow<List<CommissioningEntity>> = commissioningDao.getAllCommissioning()
    fun getLatestAuditLogs(): Flow<List<AuditLogEntity>> = auditLogDao.getLatestLogs()
    fun getPendingSyncItems(): Flow<List<SyncQueueEntity>> = syncQueueDao.getPendingItems()
    fun getAllSyncItems(): Flow<List<SyncQueueEntity>> = syncQueueDao.getAllQueueItems()

    suspend fun seedInitialTicketsIfEmpty() = withContext(Dispatchers.IO) {
        if (ticketDao.getCount() == 0) {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val seed = listOf(
                TicketEntity(
                    ticketId = "WFM-2024-8901",
                    meterNumber = "INT202409841",
                    method = "DLMS Optical Probe",
                    userId = "TECH_7721",
                    meterMake = "Schneider Electric",
                    consumerNumber = "CONS-782190",
                    consumerName = "Rajesh Sharma",
                    subDivision = "Patna Central (SBPDCL)",
                    latitude = 25.6093,
                    longitude = 85.1235,
                    eventType = "Periodic Billing Readout & CT Check",
                    ticketCreationDate = now,
                    status = "Pending",
                    tokenValue = "AUTH-TK-9921"
                ),
                TicketEntity(
                    ticketId = "WFM-2024-8902",
                    meterNumber = "INT202409842",
                    method = "DLMS Optical Probe",
                    userId = "TECH_7721",
                    meterMake = "Genus Power",
                    consumerNumber = "CONS-782195",
                    consumerName = "Pooja Patel",
                    subDivision = "Vadodara South (MGVCL)",
                    latitude = 22.3072,
                    longitude = 73.1812,
                    eventType = "Relay Re-Connection Order",
                    ticketCreationDate = now,
                    status = "Pending",
                    tokenValue = "AUTH-TK-9922"
                ),
                TicketEntity(
                    ticketId = "WFM-2024-8903",
                    meterNumber = "INT202409843",
                    method = "DLMS Optical Probe",
                    userId = "TECH_7721",
                    meterMake = "L&T Electrical",
                    consumerNumber = "CONS-782201",
                    consumerName = "Amit Kumar Verma",
                    subDivision = "Guwahati Urban (APDCL)",
                    latitude = 26.1445,
                    longitude = 91.7362,
                    eventType = "CT Commissioning & Tamper Investigation",
                    ticketCreationDate = now,
                    status = "Pending",
                    tokenValue = "AUTH-TK-9923"
                ),
                TicketEntity(
                    ticketId = "WFM-2024-8904",
                    meterNumber = "INT202409844",
                    method = "DLMS Optical Probe",
                    userId = "TECH_7721",
                    meterMake = "Secure Meters",
                    consumerNumber = "CONS-782218",
                    consumerName = "Sunita Devi",
                    subDivision = "Noida Sector 62 (PVVNL)",
                    latitude = 28.6280,
                    longitude = 77.3649,
                    eventType = "Maximum Demand (MD) Reset",
                    ticketCreationDate = now,
                    status = "Pending",
                    tokenValue = "AUTH-TK-9924"
                )
            )
            ticketDao.insertTickets(seed)
        }
    }

    suspend fun saveManualRead(read: ManualReadModel, associatedTicketId: String? = null): Long = withContext(Dispatchers.IO) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val entity = ManualReadEntity(
            smoid = read.smoid,
            meterNumber = read.meterNumber,
            meterDTTM = read.meterDTTM.ifEmpty { now },
            kwhImport = read.kwhImport,
            kvahImport = read.kvahImport,
            kwhExport = read.kwhExport,
            kvahExport = read.kvahExport,
            kwMD = read.kwMD,
            kvaMD = read.kvaMD,
            kwhImportImage = read.kwhImportImage,
            kwhExportImage = read.kwhExportImage,
            kvahImportImage = read.kvahImportImage,
            kvahExportImage = read.kvahExportImage,
            kwMDImage = read.kwMDImage,
            kvaMDImage = read.kvaMDImage,
            remarks = read.remarks,
            audit = read.audit,
            createdAt = now
        )
        val readId = manualReadDao.insertRead(entity)

        // Save local encrypted file backup for field inspection
        fileService.writeOfflineFile("READOUT_${read.meterNumber}_${System.currentTimeMillis()}.txt", entity.toString())

        // Enqueue to MDM sync queue
        syncQueueDao.enqueue(
            SyncQueueEntity(
                itemType = "READOUT",
                referenceId = "READ_$readId",
                payloadJson = "{\"meter\":\"${read.meterNumber}\",\"kwhImport\":\"${read.kwhImport}\",\"kvahImport\":\"${read.kvahImport}\",\"kwMD\":\"${read.kwMD}\",\"time\":\"$now\"}",
                createdAt = now
            )
        )

        // Mark associated ticket completed if applicable
        if (!associatedTicketId.isNullOrEmpty()) {
            ticketDao.updateStatus(associatedTicketId, "Completed")
        }

        readId
    }

    suspend fun saveCommissioningRecord(entity: CommissioningEntity): Long = withContext(Dispatchers.IO) {
        val id = commissioningDao.insert(entity)
        fileService.writeOfflineFile("CT_COMM_${entity.meterNumber}_${System.currentTimeMillis()}.txt", entity.toString())
        syncQueueDao.enqueue(
            SyncQueueEntity(
                itemType = "COMMISSIONING",
                referenceId = "COMM_$id",
                payloadJson = "{\"meter\":\"${entity.meterNumber}\",\"ctPrimary\":\"${entity.ctPrimary}\",\"ctSecondary\":\"${entity.ctSecondary}\",\"activeKw\":\"${entity.activePowerKw}\"}",
                createdAt = entity.timestamp
            )
        )
        id
    }

    suspend fun syncAllPending(): Int = withContext(Dispatchers.IO) {
        val pending = syncQueueDao.getPendingItems().first()
        var count = 0
        for (item in pending) {
            // Emulate secure upload to MDM endpoint (e.g. SBPDCL / MGVCL MDM API)
            syncQueueDao.markSynced(item.id)
            count++
        }
        count
    }

    suspend fun clearAuditLogs() = withContext(Dispatchers.IO) {
        auditLogDao.clearLogs()
    }

    suspend fun clearOfflineFiles(): Int = withContext(Dispatchers.IO) {
        fileService.clearOfflineFiles()
    }
}
