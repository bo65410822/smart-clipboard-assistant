package com.lzb.clipboardmonitor.domain.repository

import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.domain.model.ContentType
import com.lzb.clipboardmonitor.domain.model.History
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    suspend fun saveHistory(item: History) {
        saveHistory(
            text = item.text,
            contentType = item.contentType,
            action = item.action,
            result = item.result,
            timestamp = item.timestamp
        )
    }

    suspend fun saveHistory(
        text: String,
        contentType: ContentType,
        action: ClipboardAction,
        result: String,
        timestamp: Long
    )

    fun observeHistory(): Flow<List<History>>

    fun searchHistory(query: String): Flow<List<History>>

    suspend fun toggleFavorite(item: History) {
        togglePin(item.id)
    }

    suspend fun togglePin(id: Long)

    suspend fun deleteHistoryItem(item: History) {
        deleteHistory(item.id)
    }

    suspend fun deleteHistory(id: Long)

    // 云同步扩展点：默认空实现，保证现有业务层零改动。
    suspend fun syncToCloud(item: History) = Unit
    suspend fun fetchFromCloud(): List<History> = emptyList()

    suspend fun clearOldIfNeeded()
}
