package com.lzb.clipboardmonitor.data.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.lzb.clipboardmonitor.domain.executor.ClipboardActionExecutor
import com.lzb.clipboardmonitor.domain.model.ActionResult
import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

/**
 * 默认动作执行器实现。
 */
class DefaultClipboardActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) : ClipboardActionExecutor {

    override suspend fun execute(
        action: ClipboardAction,
        content: String
    ): ActionResult {
        return try {
            when (action) {
                ClipboardAction.FormatJson -> executeFormatJson(content)
                ClipboardAction.OpenUrl -> executeOpenUrl(content)
                ClipboardAction.TranslateText -> ActionResult.Success("Translated: $content")
                ClipboardAction.ExplainCode -> ActionResult.Success("Explain: $content")
                else -> ActionResult.Error("Action is not supported yet: $action")
            }
        } catch (t: Throwable) {
            ActionResult.Error(
                message = t.message ?: "Unknown action execute error",
                throwable = t
            )
        }
    }

    private fun executeFormatJson(content: String): ActionResult {
        val trimmed = content.trim()
        return try {
            val formatted = when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(JSON_INDENT_SPACES)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(JSON_INDENT_SPACES)
                else -> return ActionResult.Error("Input is not a valid JSON text")
            }
            ActionResult.Success(formatted)
        } catch (t: Throwable) {
            ActionResult.Error(
                message = t.message ?: "JSON formatting failed",
                throwable = t
            )
        }
    }

    private fun executeOpenUrl(content: String): ActionResult {
        val url = content.trim()
        if (url.isEmpty()) return ActionResult.Error("URL is empty")

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
        return ActionResult.Success()
    }

    private companion object {
        private const val JSON_INDENT_SPACES = 2
    }
}
