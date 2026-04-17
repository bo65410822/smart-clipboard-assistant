package com.lzb.clipboardmonitor.presentation

import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.domain.model.ContentType

data class ClipboardUiState(
    val text: String = "",
    val contentType: ContentType = ContentType.UNKNOWN,
    val actions: List<ClipboardAction> = emptyList(),
    val executionResult: String = ""
)
