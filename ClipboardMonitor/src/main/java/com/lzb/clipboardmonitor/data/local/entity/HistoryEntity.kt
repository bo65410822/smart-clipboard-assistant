package com.lzb.clipboardmonitor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val contentType: String,
    val action: String,
    val result: String,
    val timestamp: Long,
    val isPinned: Boolean = false
)
