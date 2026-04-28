package com.lzb.clipboardmonitor.domain.usecase

import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.domain.model.ContentType
import com.lzb.clipboardmonitor.domain.repository.HistoryRepository
import javax.inject.Inject

class SaveHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    suspend operator fun invoke(
        text: String,
        contentType: ContentType,
        action: ClipboardAction,
        result: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        repository.saveHistory(
            text = text,
            contentType = contentType,
            action = action,
            result = result,
            timestamp = timestamp
        )
        repository.clearOldIfNeeded()
    }
}
