package com.lzb.clipboardmonitor.data.repository

import com.lzb.clipboardmonitor.data.datasource.HistoryLocalDataSource
import com.lzb.clipboardmonitor.data.datasource.RemoteHistoryDataSource
import com.lzb.clipboardmonitor.data.local.entity.HistoryEntity
import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.domain.model.ContentType
import com.lzb.clipboardmonitor.domain.model.History
import com.lzb.clipboardmonitor.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val localDataSource: HistoryLocalDataSource,
    private val remoteDataSource: RemoteHistoryDataSource
) : HistoryRepository {
    override suspend fun saveHistory(
        text: String,
        contentType: ContentType,
        action: ClipboardAction,
        result: String,
        timestamp: Long
    ) {
        val item = History(
            id = 0L,
            text = text,
            contentType = contentType,
            action = action,
            result = result,
            timestamp = timestamp,
            isPinned = false
        )
        localDataSource.insert(
            HistoryEntity(
                text = text,
                contentType = contentType.name,
                action = action.toPersistedName(),
                result = result,
                timestamp = timestamp,
                isPinned = false
            )
        )
        runCatching { syncToCloud(item) }
    }

    override fun observeHistory(): Flow<List<History>> {
        return localDataSource.getAll().mapEntitiesToDomain()
    }

    override fun searchHistory(query: String): Flow<List<History>> {
        return localDataSource.search(query.trim()).mapEntitiesToDomain()
    }

    override suspend fun togglePin(id: Long) {
        val current = localDataSource.getPinState(id) ?: return
        localDataSource.updatePin(id = id, isPinned = !current)
        val target = observeHistorySnapshotById(id) ?: return
        runCatching { remoteDataSource.toggleFavorite(target) }
    }

    override suspend fun deleteHistory(id: Long) {
        val target = observeHistorySnapshotById(id)
        localDataSource.delete(id)
        if (target != null) {
            runCatching { remoteDataSource.deleteHistory(target) }
        }
    }

    override suspend fun syncToCloud(item: History) {
        remoteDataSource.syncHistory(item)
    }

    override suspend fun fetchFromCloud(): List<History> {
        return remoteDataSource.fetchHistory()
    }

    override suspend fun clearOldIfNeeded() {
        localDataSource.deleteOld(MAX_HISTORY_COUNT)
    }

    private fun ClipboardAction.toPersistedName(): String = when (this) {
        ClipboardAction.FormatJson -> "FormatJson"
        ClipboardAction.ConvertToKotlinDataClass -> "ConvertToKotlinDataClass"
        ClipboardAction.ExplainCode -> "ExplainCode"
        ClipboardAction.AnalyzeCode -> "AnalyzeCode"
        ClipboardAction.OpenUrl -> "OpenUrl"
        ClipboardAction.SummarizeUrl -> "SummarizeUrl"
        ClipboardAction.SummarizeText -> "SummarizeText"
        ClipboardAction.TranslateText -> "TranslateText"
    }

    private fun String.toClipboardAction(): ClipboardAction = when (this) {
        "FormatJson" -> ClipboardAction.FormatJson
        "ConvertToKotlinDataClass" -> ClipboardAction.ConvertToKotlinDataClass
        "ExplainCode" -> ClipboardAction.ExplainCode
        "AnalyzeCode" -> ClipboardAction.AnalyzeCode
        "OpenUrl" -> ClipboardAction.OpenUrl
        "SummarizeUrl" -> ClipboardAction.SummarizeUrl
        "SummarizeText" -> ClipboardAction.SummarizeText
        "TranslateText" -> ClipboardAction.TranslateText
        else -> ClipboardAction.SummarizeText
    }

    private fun String.toContentType(): ContentType {
        return ContentType.entries.firstOrNull { it.name == this } ?: ContentType.UNKNOWN
    }

    private fun Flow<List<HistoryEntity>>.mapEntitiesToDomain(): Flow<List<History>> {
        return map { entities ->
            entities.map { entity ->
                History(
                    id = entity.id,
                    text = entity.text,
                    contentType = entity.contentType.toContentType(),
                    action = entity.action.toClipboardAction(),
                    result = entity.result,
                    timestamp = entity.timestamp,
                    isPinned = entity.isPinned
                )
            }
        }
    }

    private companion object {
        private const val MAX_HISTORY_COUNT = 100
    }

    private suspend fun observeHistorySnapshotById(id: Long): History? {
        return localDataSource.getAll()
            .mapEntitiesToDomain()
            .map { list -> list.firstOrNull { it.id == id } }
            .firstOrNull()
    }
}
