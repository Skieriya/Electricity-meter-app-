package com.example.idatum.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TicketEntity::class,
        ManualReadEntity::class,
        CommissioningEntity::class,
        AuditLogEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class IDatumDatabase : RoomDatabase() {
    abstract fun ticketDao(): TicketDao
    abstract fun manualReadDao(): ManualReadDao
    abstract fun commissioningDao(): CommissioningDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: IDatumDatabase? = null

        fun getDatabase(context: Context): IDatumDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IDatumDatabase::class.java,
                    "idatum_hhu_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
