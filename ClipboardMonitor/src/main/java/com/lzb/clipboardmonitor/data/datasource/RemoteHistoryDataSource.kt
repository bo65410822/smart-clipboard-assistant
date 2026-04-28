package com.lzb.clipboardmonitor.data.datasource

import com.lzb.clipboardmonitor.domain.model.History

/**
 * 远端历史记录数据源抽象。
 *
 * 当前可注入 No-Op 实现；未来接入云端时仅需替换该实现，无需改业务层。
 */
interface RemoteHistoryDataSource {
    suspend fun syncHistory(item: History)
    suspend fun toggleFavorite(item: History)
    suspend fun deleteHistory(item: History)
    suspend fun fetchHistory(): List<History>
}
