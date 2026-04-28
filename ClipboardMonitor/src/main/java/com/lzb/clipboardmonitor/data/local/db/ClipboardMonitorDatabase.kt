package com.lzb.clipboardmonitor.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lzb.clipboardmonitor.data.local.dao.HistoryDao
import com.lzb.clipboardmonitor.data.local.entity.HistoryEntity

@Database(
    entities = [HistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ClipboardMonitorDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
