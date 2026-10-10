package com.example.idatum.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY ticketCreationDate DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE ticketId = :ticketId")
    suspend fun getTicketById(ticketId: String): TicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<TicketEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity)

    @Update
    suspend fun updateTicket(ticket: TicketEntity)

    @Query("UPDATE tickets SET status = :status WHERE ticketId = :ticketId")
    suspend fun updateStatus(ticketId: String, status: String)

    @Query("SELECT COUNT(*) FROM tickets")
    suspend fun getCount(): Int
}

@Dao
interface ManualReadDao {
    @Query("SELECT * FROM manual_reads ORDER BY id DESC")
    fun getAllReads(): Flow<List<ManualReadEntity>>

    @Insert
    suspend fun insertRead(read: ManualReadEntity): Long

    @Query("UPDATE manual_reads SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM manual_reads")
    suspend fun clearAll()
}

@Dao
interface CommissioningDao {
    @Query("SELECT * FROM commissioning_records ORDER BY id DESC")
    fun getAllCommissioning(): Flow<List<CommissioningEntity>>

    @Insert
    suspend fun insert(record: CommissioningEntity): Long

    @Query("UPDATE commissioning_records SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY id DESC LIMIT 200")
    fun getLatestLogs(): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'Pending' ORDER BY id ASC")
    fun getPendingItems(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue ORDER BY id DESC")
    fun getAllQueueItems(): Flow<List<SyncQueueEntity>>

    @Insert
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = 'Synced' WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'Synced'")
    suspend fun clearCompleted()
}
