package com.lzb.clipboardmonitor.data.datasource

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.lzb.clipboardmonitor.BuildConfig
import com.lzb.clipboardmonitor.domain.model.ClipboardContent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Android 平台数据源实现：
 * - 监听系统 ClipboardManager 回调
 * - 通过 callbackFlow 转换为冷流
 * - 在流关闭时移除监听器，避免泄漏
 */
class AndroidClipboardDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : ClipboardDataSource {
    private companion object {
        private const val INITIAL_RETRY_DELAY_MS = 350L
    }

    /**
     * 使用线程安全的 lazy 初始化，避免并发场景下重复获取系统服务。
     */
    private val clipboardManager: ClipboardManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun observeClipboardChanges(): Flow<ClipboardContent> = callbackFlow {
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        var isAppForeground = processLifecycle.currentState.isAtLeast(
            androidx.lifecycle.Lifecycle.State.STARTED
        )

        // 每次触发时读取当前主剪贴板并发射模型。
        fun emitCurrentClip() {
            val text = readPrimaryClipText() ?: return
            val sendResult = trySend(
                ClipboardContent(
                    text = text,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            // 当通道已关闭或背压失败时，静默丢弃当前事件，避免抛异常中断监听。
            if (sendResult.isFailure) {
                return
            }
        }

        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isAppForeground = true
                emitCurrentClip()
            }

            override fun onStop(owner: LifecycleOwner) {
                isAppForeground = false
            }
        }

        // 系统剪贴板变化回调。
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            emitCurrentClip()
        }

        processLifecycle.addObserver(lifecycleObserver)
        clipboardManager.addPrimaryClipChangedListener(listener)
        // 首次订阅时主动发一次当前值，便于消费者快速拿到状态。
        emitCurrentClip()
        // 某些系统在应用刚回到前台时第一次读取可能拿不到内容，这里补读一次。
        val delayedRefreshJob = launch {
            delay(INITIAL_RETRY_DELAY_MS)
            emitCurrentClip()
        }
        // 双保险：周期性主动读取，覆盖系统回调漏发/后台切前台不触发场景。
        val pollingJob = launch {
            while (isActive) {
                delay(BuildConfig.CLIPBOARD_POLLING_INTERVAL_MS)
                if (isAppForeground) {
                    emitCurrentClip()
                }
            }
        }

        awaitClose {
            // Flow 取消时移除监听器，防止内存泄漏。
            delayedRefreshJob.cancel()
            pollingJob.cancel()
            processLifecycle.removeObserver(lifecycleObserver)
            clipboardManager.removePrimaryClipChangedListener(listener)
        }
    }
        // 过滤空白内容，减少无意义事件。
        .filter { it.text.isNotBlank() }
        // 仅在文本内容变化时通知上层，避免轮询造成重复刷新。
        .distinctUntilChangedBy { it.text }
        // ClipboardManager 属于平台能力，约束在主线程上下文执行更稳妥。
        .flowOn(Dispatchers.Main.immediate)

    /**
     * 读取当前主剪贴板并尽可能转换为文本。
     *
     * 这里信任 coerceToText 的平台兜底能力，而不是手动做 MIME 预过滤。
     */
    private fun readPrimaryClipText(): String? {
        val clipData = clipboardManager.primaryClip ?: return null
        if (clipData.itemCount == 0) return null

        val text = clipData
            .getItemAt(0)
            .coerceToText(context)
            ?.toString()
            ?.trim()
        return text?.takeIf { it.isNotEmpty() && it.lowercase() != "null" }
    }
}
