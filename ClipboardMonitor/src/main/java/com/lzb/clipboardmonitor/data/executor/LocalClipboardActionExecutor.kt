package com.lzb.clipboardmonitor.data.executor

import android.net.Uri
import com.lzb.clipboardmonitor.domain.executor.ClipboardActionExecutor
import com.lzb.clipboardmonitor.domain.model.ActionResult
import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * 纯本地动作执行器（无网络依赖）。
 */
@Singleton
class LocalClipboardActionExecutor @Inject constructor() : ClipboardActionExecutor {

    override suspend fun execute(
        action: ClipboardAction?,
        content: String
    ): ActionResult {
        return try {
            when (action) {
                ClipboardAction.FormatJson -> formatJson(content)
                ClipboardAction.ConvertToKotlinDataClass -> convertToDataClass(content)
                ClipboardAction.OpenUrl -> parseUrl(content)
                ClipboardAction.SummarizeText -> summarize(content)
                ClipboardAction.TranslateText -> translate(content)
                ClipboardAction.ExplainCode -> explainCode(content)
                ClipboardAction.AnalyzeCode -> analyzeCode(content)
                ClipboardAction.SummarizeUrl -> summarizeUrl(content)
                null -> TODO()
            }
        } catch (t: Throwable) {
            ActionResult.Error(t.message ?: "Action execution failed")
        }
    }

    private fun formatJson(input: String): ActionResult {
        val text = input.trim().trimStart(BOM_CHAR)
        if (text.isEmpty()) return ActionResult.Error("Invalid JSON")

        return try {
            val pretty = when {
                text.startsWith("{") -> JSONObject(text).toString(JSON_INDENT_SPACES)
                text.startsWith("[") -> JSONArray(text).toString(JSON_INDENT_SPACES)
                else -> return ActionResult.Error("Invalid JSON")
            }
            ActionResult.Success(pretty)
        } catch (_: Throwable) {
            ActionResult.Error("Invalid JSON")
        }
    }

    private fun convertToDataClass(input: String): ActionResult {
        val text = input.trim().trimStart(BOM_CHAR)
        if (!text.startsWith("{")) return ActionResult.Error("Invalid JSON")

        return try {
            val json = JSONObject(text)
            val classDefs = linkedMapOf<String, MutableList<Pair<String, String>>>()
            buildClassFromJson(
                className = ROOT_CLASS_NAME,
                json = json,
                classDefs = classDefs
            )

            val rendered = classDefs.entries.joinToString("\n\n") { (className, props) ->
                val body = if (props.isEmpty()) {
                    ""
                } else {
                    "\n" + props.joinToString(",\n") { (name, type) ->
                        "    val $name: $type"
                    } + "\n"
                }
                "data class $className($body)"
            }
            ActionResult.Success(rendered)
        } catch (_: Throwable) {
            ActionResult.Error("Invalid JSON")
        }
    }

    private fun parseUrl(input: String): ActionResult {
        return try {
            val uri = Uri.parse(input.trim())
            val names = uri.queryParameterNames
            if (names.isEmpty()) return ActionResult.Success("No query params")

            val result = names.joinToString("\n") { name ->
                "$name = ${uri.getQueryParameter(name).orEmpty()}"
            }
            ActionResult.Success(result)
        } catch (t: Throwable) {
            ActionResult.Error(t.message ?: "Invalid URL")
        }
    }

    private fun summarize(input: String): ActionResult {
        val text = input.trim()
        return if (text.length < SUMMARY_LIMIT) {
            ActionResult.Success(text)
        } else {
            ActionResult.Success(text.take(SUMMARY_LIMIT) + "...")
        }
    }

    private fun translate(input: String): ActionResult {
        return ActionResult.Success("Translated: $input")
    }

    private fun explainCode(input: String): ActionResult {
        val text = input.trim()
        if (text.isEmpty()) return ActionResult.Error("代码内容为空")
        val preview = if (text.length <= CODE_PREVIEW_LIMIT) text else text.take(CODE_PREVIEW_LIMIT) + "..."
        return ActionResult.Success("本地代码说明：\n$preview")
    }

    private fun analyzeCode(input: String): ActionResult {
        val lineCount = input.lineSequence().count().coerceAtLeast(1)
        val charCount = input.length
        return ActionResult.Success("本地分析：共 ${lineCount} 行，${charCount} 个字符。")
    }

    private fun summarizeUrl(input: String): ActionResult {
        val text = input.trim()
        return try {
            val uri = Uri.parse(text)
            val host = uri.host.orEmpty()
            val path = uri.path.orEmpty().ifEmpty { "/" }
            val queryCount = uri.queryParameterNames.size
            ActionResult.Success(
                "URL摘要：\nHost: $host\nPath: $path\nQuery参数数量: $queryCount"
            )
        } catch (_: Throwable) {
            ActionResult.Error("Invalid URL")
        }
    }

    private fun inferKotlinType(value: Any?): String {
        return when (value) {
            is Boolean -> "Boolean"
            is Int, is Long -> "Int"
            is Float, is Double -> "Double"
            JSONObject.NULL, null -> "String?"
            else -> "String"
        }
    }

    private fun buildClassFromJson(
        className: String,
        json: JSONObject,
        classDefs: LinkedHashMap<String, MutableList<Pair<String, String>>>
    ) {
        if (classDefs.containsKey(className)) return
        classDefs[className] = mutableListOf()

        val properties = classDefs.getValue(className)
        json.keys().asSequence().forEach { rawKey ->
            val propName = rawKey.toSafeKotlinIdentifier()
            val type = resolveTypeForValue(
                value = json.opt(rawKey),
                parentClassName = className,
                rawKey = rawKey,
                classDefs = classDefs
            )
            properties += propName to type
        }
    }

    private fun resolveTypeForValue(
        value: Any?,
        parentClassName: String,
        rawKey: String,
        classDefs: LinkedHashMap<String, MutableList<Pair<String, String>>>
    ): String {
        return when (value) {
            is JSONObject -> {
                val nestedClassName = parentClassName + rawKey.toPascalCase()
                buildClassFromJson(nestedClassName, value, classDefs)
                nestedClassName
            }
            is JSONArray -> resolveArrayType(value, parentClassName, rawKey, classDefs)
            is Boolean -> "Boolean"
            is Int -> "Int"
            is Long -> if (value in Int.MIN_VALUE..Int.MAX_VALUE) "Int" else "Long"
            is Float, is Double -> "Double"
            JSONObject.NULL, null -> "String?"
            else -> "String"
        }
    }

    private fun resolveArrayType(
        array: JSONArray,
        parentClassName: String,
        rawKey: String,
        classDefs: LinkedHashMap<String, MutableList<Pair<String, String>>>
    ): String {
        if (array.length() == 0) return "List<String>"
        val firstMeaningful = (0 until array.length())
            .asSequence()
            .map { array.opt(it) }
            .firstOrNull { it != null && it != JSONObject.NULL } ?: return "List<String?>"

        val elementType = when (firstMeaningful) {
            is JSONObject -> {
                val nestedClassName = parentClassName + rawKey.toSingularPascalCase()
                buildClassFromJson(nestedClassName, firstMeaningful, classDefs)
                nestedClassName
            }
            is JSONArray -> "List<Any>"
            is Boolean -> "Boolean"
            is Int -> "Int"
            is Long -> if (firstMeaningful in Int.MIN_VALUE..Int.MAX_VALUE) "Int" else "Long"
            is Float, is Double -> "Double"
            else -> "String"
        }
        return "List<$elementType>"
    }

    private fun String.toPascalCase(): String {
        val parts = replace(Regex("[^A-Za-z0-9]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (parts.isEmpty()) return "Field"
        return parts.joinToString("") { part ->
            part.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun String.toSingularPascalCase(): String {
        val normalized = toPascalCase()
        return when {
            normalized.endsWith("ies") && normalized.length > 3 ->
                normalized.dropLast(3) + "y"
            normalized.endsWith("s") && normalized.length > 1 ->
                normalized.dropLast(1)
            else -> normalized
        }
    }

    private fun String.toSafeKotlinIdentifier(): String {
        val cleaned = replace(Regex("[^A-Za-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifEmpty { "field" }
        val firstHandled = if (cleaned.first().isDigit()) "_$cleaned" else cleaned
        return if (firstHandled in KOTLIN_KEYWORDS) "_$firstHandled" else firstHandled
    }

    private companion object {
        private const val BOM_CHAR: Char = '\uFEFF'
        private const val ROOT_CLASS_NAME = "Generated"
        private const val JSON_INDENT_SPACES = 2
        private const val SUMMARY_LIMIT = 100
        private const val CODE_PREVIEW_LIMIT = 220
        private val KOTLIN_KEYWORDS = setOf(
            "class", "object", "interface", "fun", "val", "var", "when", "if", "else",
            "for", "while", "do", "try", "catch", "finally", "return", "break", "continue",
            "is", "in", "as", "package", "import", "null", "true", "false", "typealias"
        )
    }
}
