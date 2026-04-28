package com.lzb.clipboardmonitor.data.datasource

import com.lzb.clipboardmonitor.domain.model.History
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认远端空实现，保证离线可用与架构可扩展。
 */
@Singleton
class NoOpRemoteHistoryDataSource @Inject constructor() : RemoteHistoryDataSource {
    override suspend fun syncHistory(item: History) = Unit

    override suspend fun toggleFavorite(item: History) = Unit

    override suspend fun deleteHistory(item: History) = Unit

    override suspend fun fetchHistory(): List<History> = emptyList()
}
