package com.lzb.clipboardmonitor.presentation.notifier

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Singleton
class ResultNotifyDispatcher @Inject constructor(
    private val notifiers: List<@JvmSuppressWildcards ResultNotifier>,
    private val controller: ResultNotifierController
) {
    suspend fun dispatch(result: String) {
        if (result.isBlank()) return
        val enabled = controller.enabledChannels.value
        coroutineScope {
            notifiers
                .filter { it.channel in enabled && it.canHandle(result) }
                .map { notifier ->
                    async {
                        runCatching { notifier.notify(result) }
                    }
                }
                .awaitAll()
        }
    }
}
