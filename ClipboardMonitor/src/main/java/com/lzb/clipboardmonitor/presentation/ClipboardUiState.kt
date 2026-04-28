package com.lzb.clipboardmonitor.presentation

import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.domain.model.ClipboardContent
import com.lzb.clipboardmonitor.domain.model.ContentType
import com.lzb.clipboardmonitor.domain.model.History

data class ClipboardUiState(
    val content: ClipboardContent? = null,
    val contentType: ContentType = ContentType.UNKNOWN,
    val actions: List<ClipboardAction> = emptyList(),
    val historyList: List<History> = emptyList(),
    val favoriteList: List<History> = emptyList(),
    val searchQuery: String = "",
    val executionResult: String = "",
    val processingTimeMs: Long? = null,
    val autoExecuteEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isFromShare: Boolean = false
) {
    val text: String
        get() = content?.text.orEmpty()
}
