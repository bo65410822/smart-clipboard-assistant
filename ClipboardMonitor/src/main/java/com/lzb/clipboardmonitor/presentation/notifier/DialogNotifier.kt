package com.lzb.clipboardmonitor.presentation.notifier

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 对话框通知通道结构。
 *
 * 当前仅负责发出对话框事件，UI 层可选订阅 [events] 后展示 Dialog。
 */
@Singleton
class DialogNotifier @Inject constructor() : ResultNotifier {
    override val channel: NotifyChannel = NotifyChannel.DIALOG

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events

    override fun canHandle(result: String): Boolean {
        return result.length > MIN_DIALOG_LENGTH
    }

    override suspend fun notify(result: String) {
        _events.emit(result)
    }

    private companion object {
        private const val MIN_DIALOG_LENGTH = 120
    }
}
