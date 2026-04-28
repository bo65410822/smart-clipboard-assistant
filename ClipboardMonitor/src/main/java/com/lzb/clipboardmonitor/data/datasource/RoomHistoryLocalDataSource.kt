package com.lzb.clipboardmonitor.data.datasource

import com.lzb.clipboardmonitor.data.local.dao.HistoryDao
import com.lzb.clipboardmonitor.data.local.entity.HistoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomHistoryLocalDataSource @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryLocalDataSource {
    override suspend fun insert(history: HistoryEntity) {
        historyDao.insert(history)
    }

    override fun getAll(): Flow<List<HistoryEntity>> = historyDao.getAll()

    override fun search(query: String): Flow<List<HistoryEntity>> = historyDao.search(query)

    override suspend fun updatePin(id: Long, isPinned: Boolean) {
        historyDao.updatePin(id, isPinned)
    }

    override suspend fun getPinState(id: Long): Boolean? = historyDao.getPinState(id)

    override suspend fun delete(id: Long) {
        historyDao.delete(id)
    }

    override suspend fun clearAll() {
        historyDao.clearAll()
    }

    override suspend fun deleteOld(limit: Int) {
        historyDao.deleteOld(limit)
    }
}
