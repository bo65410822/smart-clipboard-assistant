package com.lzb.clipboardmonitor.data.datasource

import com.lzb.clipboardmonitor.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

interface HistoryLocalDataSource {
    suspend fun insert(history: HistoryEntity)
    fun getAll(): Flow<List<HistoryEntity>>
    fun search(query: String): Flow<List<HistoryEntity>>
    suspend fun updatePin(id: Long, isPinned: Boolean)
    suspend fun getPinState(id: Long): Boolean?
    suspend fun delete(id: Long)
    suspend fun clearAll()
    suspend fun deleteOld(limit: Int)
}
