package com.lzb.clipboardmonitor.data.ai

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/**
 * 本地 Mock：无网络 / API 失败时用于演示与回退。
 */
@Singleton
class MockExecutor @Inject constructor() : AIExecutor {

    override suspend fun explainCode(code: String): String {
        delay(NETWORK_MOCK_MS)
        return "[Mock] 解释代码：\n$code"
    }

    override suspend fun translateText(text: String): String {
        delay(NETWORK_MOCK_MS)
        return "[Mock] 翻译：$text"
    }

    override suspend fun formatJson(json: String): String {
        delay(NETWORK_MOCK_MS)
        return "[Mock] 格式化 JSON：\n$json"
    }

    override suspend fun summarizeUrl(url: String): String {
        delay(NETWORK_MOCK_MS)
        return "[Mock] 链接摘要：$url"
    }

    private companion object {
        private const val NETWORK_MOCK_MS = 50L
    }
}
