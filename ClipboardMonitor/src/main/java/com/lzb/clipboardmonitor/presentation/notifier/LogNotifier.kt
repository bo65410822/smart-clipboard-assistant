package com.lzb.clipboardmonitor.presentation.notifier

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogNotifier @Inject constructor() : ResultNotifier {
    override val channel: NotifyChannel = NotifyChannel.LOG

    override fun canHandle(result: String): Boolean = true

    override suspend fun notify(result: String) {
        Log.i(TAG, "Execution result: $result")
    }

    private companion object {
        private const val TAG = "ResultNotifier"
    }
}
