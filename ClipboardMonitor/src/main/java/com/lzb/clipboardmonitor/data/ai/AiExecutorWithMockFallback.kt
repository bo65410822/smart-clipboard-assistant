package com.lzb.clipboardmonitor.data.ai

import android.util.Log
import com.lzb.clipboardmonitor.BuildConfig
import com.lzb.clipboardmonitor.di.FallbackAI
import com.lzb.clipboardmonitor.di.PrimaryAI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 对外暴露的 [AIExecutor]：优先真实 OpenAI，失败或无 Key 时回退 [MockExecutor]。
 */
@Singleton
class AiExecutorWithMockFallback @Inject constructor(
    @PrimaryAI private val primaryExecutor: AIExecutor,
    @FallbackAI private val fallbackExecutor: AIExecutor
) : AIExecutor {

    override suspend fun explainCode(code: String): String {
        return runWithFallback(
            primary = { primaryExecutor.explainCode(code) },
            fallback = { fallbackExecutor.explainCode(code) }
        )
    }

    override suspend fun translateText(text: String): String {
        return runWithFallback(
            primary = { primaryExecutor.translateText(text) },
            fallback = { fallbackExecutor.translateText(text) }
        )
    }

    override suspend fun formatJson(json: String): String {
        return runWithFallback(
            primary = { primaryExecutor.formatJson(json) },
            fallback = { fallbackExecutor.formatJson(json) }
        )
    }

    override suspend fun summarizeUrl(url: String): String {
        return runWithFallback(
            primary = { primaryExecutor.summarizeUrl(url) },
            fallback = { fallbackExecutor.summarizeUrl(url) }
        )
    }

    private suspend fun runWithFallback(
        primary: suspend () -> String,
        fallback: suspend () -> String
    ): String {
        if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            Log.w(TAG, "OPENAI_API_KEY is empty, fallback to Mock")
            return fallback()
        }
        return try {
            val out = primary().trim()
            if (out.isEmpty()) {
                Log.w(TAG, "OpenAI output empty, fallback to Mock")
                // 请求成功但正文为空时仍回退 Mock；Debug 附带说明便于排查解析/模型问题
                val mock = fallback()
                return if (BuildConfig.DEBUG) {
                    "$mock\n[调试] OpenAI 返回内容为空，已使用 Mock。"
                } else {
                    mock
                }
            }
            out
        } catch (t: Throwable) {
            Log.e(TAG, "OpenAI call failed, fallback to Mock", t)
            val mock = fallback()
            if (BuildConfig.DEBUG) {
                val reason = (t.message ?: t.javaClass.simpleName).take(200)
                "$mock\n[调试] OpenAI 调用失败，已使用 Mock。原因：$reason"
            } else {
                mock
            }
        }
    }

    private companion object {
        private const val TAG = "ClipAI"
    }
}
