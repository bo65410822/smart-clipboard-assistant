package com.lzb.clipboardmonitor.presentation.notifier

interface ResultNotifier {
    val channel: NotifyChannel
    fun canHandle(result: String): Boolean
    suspend fun notify(result: String)
}

enum class NotifyChannel {
    TOAST,
    DIALOG,
    LOG
}
