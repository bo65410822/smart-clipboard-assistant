package com.lzb.clipboardmonitor.data.ai

/**
 * AI 能力抽象，由 data 层实现（[OpenAIExecutor] + [MockExecutor] 组合等）。
 */
interface AIExecutor {

    suspend fun explainCode(code: String): String

    suspend fun translateText(text: String): String

    suspend fun formatJson(json: String): String

    suspend fun summarizeUrl(url: String): String
}
