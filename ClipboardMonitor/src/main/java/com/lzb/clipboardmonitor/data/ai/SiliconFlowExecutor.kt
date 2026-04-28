package com.lzb.clipboardmonitor.data.ai

import android.util.Log
import com.lzb.clipboardmonitor.BuildConfig
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * SiliconFlow 的 OpenAI-compatible Responses API 实现。
 */
@Singleton
class SiliconFlowExecutor @Inject constructor() : AIExecutor {

    override suspend fun explainCode(code: String): String {
        return complete("你是资深工程师，用中文简洁解释代码。", "代码如下：\n\n$code")
    }

    override suspend fun translateText(text: String): String {
        return complete("你是翻译助手，将用户文本译为中文，保持原意。", "待翻译文本：\n\n$text")
    }

    override suspend fun formatJson(json: String): String {
        return complete("你只输出格式化后的 JSON，不要解释。", "JSON：\n\n$json")
    }

    override suspend fun summarizeUrl(url: String): String {
        return complete("你是摘要助手，用中文要点列出该链接核心信息。", "URL：\n$url")
    }

    private suspend fun complete(instructions: String, userText: String): String {
        if (BuildConfig.SILICONFLOW_API_KEY.isBlank()) {
            throw IllegalStateException("SILICONFLOW_API_KEY is empty")
        }
        return withContext(Dispatchers.IO) {
            val url = URL("${BuildConfig.SILICONFLOW_BASE_URL}/v1/responses")
            Log.d(TAG, "Requesting SiliconFlow Responses API: $url")
            val connection = openConnection(url).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${BuildConfig.SILICONFLOW_API_KEY}")
            }
            try {
                val body = buildRequestBody(instructions, userText)
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
                val responseCode = connection.responseCode
                val responseText = readResponseText(connection, responseCode in 200..299)
                Log.d(TAG, "SiliconFlow response code=$responseCode, bodyLength=${responseText.length}")
                if (responseCode !in 200..299) {
                    Log.e(TAG, "SiliconFlow request failed. HTTP=$responseCode body=$responseText")
                    throw IllegalStateException("HTTP $responseCode: $responseText")
                }
                parseResponseText(responseText)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun openConnection(url: URL): HttpURLConnection {
        val port = BuildConfig.OPENAI_PROXY_PORT
        val host = BuildConfig.OPENAI_PROXY_HOST.trim()
        return if (port > 0 && host.isNotEmpty()) {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
            url.openConnection(proxy) as HttpURLConnection
        } else {
            url.openConnection() as HttpURLConnection
        }
    }

    private fun buildRequestBody(instructions: String, userText: String): String {
        val input = JSONArray()
            .put(
                JSONObject()
                    .put("role", "user")
                    .put(
                        "content",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("type", "input_text")
                                    .put("text", userText)
                            )
                    )
            )
        return JSONObject()
            .put("model", BuildConfig.SILICONFLOW_MODEL)
            .put("instructions", instructions)
            .put("input", input)
            .put("temperature", 0.2)
            .toString()
    }

    private fun readResponseText(connection: HttpURLConnection, success: Boolean): String {
        val stream = if (success) connection.inputStream else connection.errorStream
        if (stream == null) return ""
        return stream.bufferedReader().use(BufferedReader::readText)
    }

    private fun parseResponseText(responseText: String): String {
        val root = JSONObject(responseText)
        root.optJSONObject("error")?.let { err ->
            throw IllegalStateException(err.optString("message", "API error"))
        }
        root.optString("output_text").trim().takeIf { it.isNotEmpty() }?.let { return it }
        return ""
    }

    private companion object {
        private const val TAG = "ClipAI"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
    }
}
