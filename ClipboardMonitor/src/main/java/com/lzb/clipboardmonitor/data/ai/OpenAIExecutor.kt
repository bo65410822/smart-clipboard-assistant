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
 * 使用 OpenAI **Responses** REST API（POST /v1/responses）。
 *
 * 需配置 [BuildConfig.OPENAI_API_KEY]；代理可选（VPN / HTTP 代理场景）。
 */
@Singleton
class OpenAIExecutor @Inject constructor() : AIExecutor {

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
        return complete(
            "你是摘要助手，用中文要点列出该链接可能涉及的主题（无法访问网页时基于 URL 做合理概括）。",
            "URL：\n$url"
        )
    }

    private suspend fun complete(instructions: String, userText: String): String {
        if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            throw IllegalStateException("OPENAI_API_KEY is empty")
        }
        return withContext(Dispatchers.IO) {
            val url = URL("${BuildConfig.OPENAI_BASE_URL}/v1/responses")
            Log.d(TAG, "Requesting OpenAI Responses API: $url")
            val connection = openConnection(url).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            }
            try {
                val body = buildRequestBody(instructions, userText)
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
                val responseCode = connection.responseCode
                val responseText = readResponseText(connection, responseCode in 200..299)
                Log.d(TAG, "OpenAI response code=$responseCode, bodyLength=${responseText.length}")
                if (responseCode !in 200..299) {
                    Log.e(TAG, "OpenAI request failed. HTTP=$responseCode body=$responseText")
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
            .put("model", BuildConfig.OPENAI_MODEL)
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
        val deep = extractOutputTextDeep(root)
        if (deep.isNotEmpty()) return deep
        val output = root.optJSONArray("output") ?: return ""
        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            if (item.optString("type") != "message") continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val part = content.optJSONObject(j) ?: continue
                if (part.optString("type") == "output_text") {
                    val text = part.optString("text").trim()
                    if (text.isNotEmpty()) return text
                }
            }
        }
        Log.w(TAG, "OpenAI response parsed but no output_text found")
        return ""
    }

    /**
     * Responses API 不同版本/模型返回结构可能略有差异，递归查找 type=output_text 的文本。
     */
    private fun extractOutputTextDeep(node: Any?, depth: Int = 0): String {
        if (depth > 40) return ""
        return when (node) {
            is JSONObject -> {
                if (node.optString("type") == "output_text") {
                    val t = node.optString("text").trim()
                    if (t.isNotEmpty()) return t
                }
                node.keys().asSequence().forEach { key ->
                    val found = extractOutputTextDeep(node.opt(key), depth + 1)
                    if (found.isNotEmpty()) return found
                }
                ""
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    val found = extractOutputTextDeep(node.opt(i), depth + 1)
                    if (found.isNotEmpty()) return found
                }
                ""
            }
            else -> ""
        }
    }

    private companion object {
        private const val TAG = "ClipAI"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
    }
}
