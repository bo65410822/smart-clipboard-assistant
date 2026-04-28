package com.lzb.clipdev.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import com.lzb.clipdev.ui.mode.ResultModeRegistry
import com.lzb.clipdev.ui.mode.ResultRenderStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lzb.clipboardmonitor.domain.model.ContentType
import java.util.regex.Pattern
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ResultSection(
    result: String,
    isLoading: Boolean,
    isFromShare: Boolean,
    contentType: ContentType,
    resultModeRegistry: ResultModeRegistry,
    onResultCopied: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val autoCopyEnabled = false
    val content = when {
        isLoading -> "执行中..."
        result.isBlank() -> "暂无结果"
        else -> result
    }
    val modeSpec = remember(contentType, resultModeRegistry) { resultModeRegistry.resolve(contentType) }

    Surface(
        color = Color(0xFFF7F9FC),
        tonalElevation = 0.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isFromShare) {
                Text(text = "来源：分享", style = MaterialTheme.typography.labelMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Color(0xFF98A2B3),
                            modifier = Modifier.size(14.dp)
                        )
                        Text("解析结果", style = MaterialTheme.typography.labelLarge, color = Color(0xFF667085))
                    }
                    Text(modeSpec.primaryActionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(modeSpec.typeLabel, style = MaterialTheme.typography.labelLarge, color = Color(0xFF667085))
                    Text(modeSpec.secondaryActionLabel, style = MaterialTheme.typography.labelLarge, color = Color(0xFF667085))
                }
                Row(
                    modifier = Modifier.clickable(enabled = result.isNotBlank()) {
                        onResultCopied(result)
                        copyResultToClipboard(clipboardManager, result)
                        onShowMessage("已复制到剪贴板")
                    },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("复制", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Divider(color = Color(0xFFE4E7EC))
            ResultText(
                text = content,
                contentType = contentType,
                renderStyle = modeSpec.renderStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 340.dp)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            LaunchedEffect(result, isLoading) {
                if (!autoCopyEnabled || isLoading || result.isBlank()) return@LaunchedEffect
                onResultCopied(result)
                copyResultToClipboard(clipboardManager, result)
                onShowMessage("结果已复制")
            }
        }
    }
}

@Composable
fun ResultText(
    text: String,
    contentType: ContentType,
    renderStyle: ResultRenderStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val jsonNode = remember(text, contentType) {
        if (renderStyle == ResultRenderStyle.JSON_TREE) parseJsonNode(text) else null
    }
    if (jsonNode != null) {
        JsonNodeView(node = jsonNode, modifier = modifier, path = "root")
        return
    }
    val annotated = remember(text) { parseResultAnnotatedText(text) }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        onClick = { offset ->
            val url = annotated.getStringAnnotations(TAG_URL, offset, offset).firstOrNull()?.item
            if (!url.isNullOrBlank()) {
                val target = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                return@ClickableText
            }
            val phone = annotated.getStringAnnotations(TAG_PHONE, offset, offset).firstOrNull()?.item
            if (!phone.isNullOrBlank()) {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }
        }
    )
}

@Composable
fun ResultActions(
    result: String,
    contentType: ContentType,
    onFormatJson: () -> Unit,
    onGenerateKotlinClass: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (contentType == ContentType.JSON) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onFormatJson, modifier = Modifier.weight(1f)) {
                    Text("格式化 JSON")
                }
                Button(onClick = onGenerateKotlinClass, modifier = Modifier.weight(1f)) {
                    Text("转 Kotlin 数据类")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCopy, enabled = result.isNotBlank(), modifier = Modifier.weight(1f)) { Text("复制") }
            OutlinedButton(onClick = onShare, enabled = result.isNotBlank(), modifier = Modifier.weight(1f)) { Text("分享") }
        }
    }
}

@Composable
private fun JsonNodeView(
    node: JsonNode,
    modifier: Modifier = Modifier,
    level: Int = 0,
    name: String? = null,
    path: String
) {
    var expanded by rememberSaveable(path) { mutableStateOf(level < 1) }
    val maxPreviewChildren = 40
    when (node) {
        is JsonNode.ObjectNode -> {
            Column(modifier = modifier.padding(start = (level * 12).dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (expanded) "▾" else "▸", color = Color(0xFF98A2B3))
                    if (name != null) Text("\"$name\":", color = Color(0xFF2962FF))
                    Text(if (expanded) "{" else "{...}", color = Color(0xFF667085))
                }
                if (expanded) {
                    val entries = node.fields.entries.toList()
                    entries.take(maxPreviewChildren).forEach { (k, v) ->
                        JsonNodeView(node = v, level = level + 1, name = k, path = "$path.$k")
                    }
                    if (entries.size > maxPreviewChildren) {
                        Text(
                            text = "... 还有 ${entries.size - maxPreviewChildren} 项",
                            modifier = Modifier.padding(start = ((level + 1) * 12).dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("}", modifier = Modifier.padding(start = ((level + 1) * 12).dp), color = Color(0xFF667085))
                }
            }
        }
        is JsonNode.ArrayNode -> {
            Column(modifier = modifier.padding(start = (level * 12).dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (expanded) "▾" else "▸", color = Color(0xFF98A2B3))
                    if (name != null) Text("\"$name\":", color = Color(0xFF2962FF))
                    Text(if (expanded) "[" else "[...]", color = Color(0xFF667085))
                }
                if (expanded) {
                    val items = node.items
                    items.take(maxPreviewChildren).forEachIndexed { index, item ->
                        JsonNodeView(node = item, level = level + 1, path = "$path[$index]")
                    }
                    if (items.size > maxPreviewChildren) {
                        Text(
                            text = "... 还有 ${items.size - maxPreviewChildren} 项",
                            modifier = Modifier.padding(start = ((level + 1) * 12).dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("]", modifier = Modifier.padding(start = ((level + 1) * 12).dp), color = Color(0xFF667085))
                }
            }
        }
        is JsonNode.ValueNode -> {
            Row(
                modifier = modifier.padding(start = (level * 12).dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(" ", color = Color.Transparent)
                if (name != null) Text("\"$name\":", color = Color(0xFF2962FF))
                val valueColor = if (node.value.startsWith("\"")) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                Text(node.value, color = valueColor)
            }
        }
    }
}

fun copyResultToClipboard(clipboardManager: ClipboardManager, text: String) {
    if (text.isBlank()) return
    clipboardManager.setText(AnnotatedString(text))
}

fun shareResult(context: Context, text: String) {
    if (text.isBlank()) return
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(shareIntent, "分享结果"))
}

private fun parseJsonNode(text: String): JsonNode? {
    val candidate = text.trim()
    if (!(candidate.startsWith("{") || candidate.startsWith("["))) return null
    return try {
        if (candidate.startsWith("{")) parseJsonObject(JSONObject(candidate)) else parseJsonArray(JSONArray(candidate))
    } catch (_: Throwable) {
        null
    }
}

private fun parseJsonObject(obj: JSONObject): JsonNode.ObjectNode {
    val fields = linkedMapOf<String, JsonNode>()
    obj.keys().asSequence().forEach { key -> fields[key] = parseJsonValue(obj.opt(key)) }
    return JsonNode.ObjectNode(fields)
}

private fun parseJsonArray(array: JSONArray): JsonNode.ArrayNode {
    val items = buildList {
        for (i in 0 until array.length()) add(parseJsonValue(array.opt(i)))
    }
    return JsonNode.ArrayNode(items)
}

private fun parseJsonValue(value: Any?): JsonNode {
    return when (value) {
        is JSONObject -> parseJsonObject(value)
        is JSONArray -> parseJsonArray(value)
        JSONObject.NULL, null -> JsonNode.ValueNode("null")
        is String -> JsonNode.ValueNode("\"$value\"")
        else -> JsonNode.ValueNode(value.toString())
    }
}

private fun parseResultAnnotatedText(text: String): AnnotatedString {
    val tokens = mutableListOf<ResultToken>()
    val urlMatcher = Patterns.WEB_URL.matcher(text)
    while (urlMatcher.find()) {
        tokens += ResultToken(urlMatcher.start(), urlMatcher.end(), urlMatcher.group().orEmpty(), TAG_URL)
    }
    val phoneMatcher = Pattern.compile("1[3-9]\\d{9}").matcher(text)
    while (phoneMatcher.find()) {
        tokens += ResultToken(phoneMatcher.start(), phoneMatcher.end(), phoneMatcher.group().orEmpty(), TAG_PHONE)
    }
    val sorted = tokens.sortedBy { it.start }
    val linkStyle = SpanStyle(color = Color(0xFF1E88E5), textDecoration = TextDecoration.Underline)

    return buildAnnotatedString {
        var cursor = 0
        for (token in sorted) {
            if (token.start < cursor) continue
            append(text.substring(cursor, token.start))
            pushStringAnnotation(tag = token.tag, annotation = token.value)
            withStyle(linkStyle) { append(token.value) }
            pop()
            cursor = token.end
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}

private data class ResultToken(val start: Int, val end: Int, val value: String, val tag: String)

private sealed interface JsonNode {
    data class ObjectNode(val fields: LinkedHashMap<String, JsonNode>) : JsonNode
    data class ArrayNode(val items: List<JsonNode>) : JsonNode
    data class ValueNode(val value: String) : JsonNode
}

private const val TAG_URL = "result_url"
private const val TAG_PHONE = "result_phone"
