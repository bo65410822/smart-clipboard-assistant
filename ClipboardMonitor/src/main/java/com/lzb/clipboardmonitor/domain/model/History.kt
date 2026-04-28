package com.lzb.clipboardmonitor.domain.model

data class History(
    val id: Long,
    val text: String,
    val contentType: ContentType,
    val action: ClipboardAction,
    val result: String,
    val timestamp: Long,
    val isPinned: Boolean
)
