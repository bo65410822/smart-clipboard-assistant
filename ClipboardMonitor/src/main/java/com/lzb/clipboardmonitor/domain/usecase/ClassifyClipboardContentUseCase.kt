package com.lzb.clipboardmonitor.domain.usecase

import android.util.Patterns
import com.lzb.clipboardmonitor.domain.model.ClassifiedClipboardContent
import com.lzb.clipboardmonitor.domain.model.ClipboardContent
import com.lzb.clipboardmonitor.domain.model.ContentType
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

/**
 * 基于本地规则的剪贴板内容分类用例。
 *
 * 规则顺序：
 * 1) JSON -> 2) URL -> 3) CODE -> 4) TEXT
 *
 * 说明：
 * - UNKNOWN 仅用于“不可判定或不值得判定”的场景（如空文本、超长文本）。
 * - 其余可消费文本默认归类为 TEXT。
 */
class ClassifyClipboardContentUseCase @Inject constructor() {

    operator fun invoke(content: ClipboardContent): ClassifiedClipboardContent {
        val trimmed = content.text.trim()

        val type = when {
            trimmed.isEmpty() -> ContentType.UNKNOWN
            trimmed.length > MAX_CLASSIFY_TEXT_LENGTH -> ContentType.UNKNOWN
            isJson(trimmed) -> ContentType.JSON
            isUrl(trimmed) -> ContentType.URL
            isCode(trimmed) -> ContentType.CODE
            else -> ContentType.TEXT
        }

        return ClassifiedClipboardContent(
            original = content,
            contentType = type
        )
    }

    private fun isJson(text: String): Boolean {
        // 性能保护：超长文本不做 JSON 解析，避免频繁异常和卡顿。
        if (text.length > MAX_JSON_PARSE_LENGTH) return false
        if (!text.startsWith("{") && !text.startsWith("[")) return false

        return try {
            when {
                text.startsWith("{") -> {
                    JSONObject(text)
                    true
                }

                text.startsWith("[") -> {
                    JSONArray(text)
                    true
                }

                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isUrl(text: String): Boolean {
        // find() 可识别“包含 URL 的自然语言文本”，避免 matches() 过严漏判。
        if (Patterns.WEB_URL.matcher(text).find()) return true

        // 兜底：某些边界场景中 WEB_URL 可能漏判，补充 scheme 前缀判断。
        return text.startsWith("http://", ignoreCase = true) ||
                text.startsWith("https://", ignoreCase = true)
    }

    private fun isCode(text: String): Boolean {
        val hasKeyword = CODE_KEYWORD_REGEX.containsMatchIn(text)
        if (!hasKeyword) return false

        val hasStructure = (text.contains("{") && text.contains("}")) ||
                (text.contains("(") && text.contains(")"))
        val hasNewLine = text.contains('\n')
        val hasSemicolon = text.contains(';')

        // 降低误判：仅“关键词 + 结构特征”才判定为代码。
        return hasStructure || hasNewLine || hasSemicolon
    }

    private companion object {
        private const val MAX_CLASSIFY_TEXT_LENGTH = 50_000
        private const val MAX_JSON_PARSE_LENGTH = 10_000

        private val CODE_KEYWORD_REGEX = Regex(
            pattern = "\\b(class|fun|import|interface|object|extends|implements|public|private|protected|val|var|def|function|const|let|return)\\b",
            option = RegexOption.IGNORE_CASE
        )
    }
}
