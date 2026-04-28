package com.lzb.clipboardmonitor.presentation.notifier

import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ToastNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : ResultNotifier {
    override val channel: NotifyChannel = NotifyChannel.TOAST

    override fun canHandle(result: String): Boolean {
        return result.isNotBlank()
    }

    override suspend fun notify(result: String) {
        withContext(Dispatchers.Main.immediate) {
            Toast.makeText(context, SUCCESS_TIP, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        private const val SUCCESS_TIP = "已处理完成"
    }
}
