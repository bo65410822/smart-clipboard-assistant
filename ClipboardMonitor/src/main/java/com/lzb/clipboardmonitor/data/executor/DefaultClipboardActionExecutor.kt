package com.lzb.clipboardmonitor.data.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import com.lzb.clipboardmonitor.data.ai.AIExecutor
import com.lzb.clipboardmonitor.domain.executor.ClipboardActionExecutor
import com.lzb.clipboardmonitor.domain.model.ActionResult
import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 默认动作执行器：智能类动作委托 [AIExecutor]；打开浏览器等系统行为本地处理。
 */
class DefaultClipboardActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiExecutor: AIExecutor
) : ClipboardActionExecutor {

    override suspend fun execute(
        action: ClipboardAction?,
        content: String
    ): ActionResult {
        return try {
            when (action) {
                ClipboardAction.FormatJson -> executeFormatJson(content)
                ClipboardAction.ExplainCode -> executeAi {
                    ActionResult.Success(aiExecutor.explainCode(content))
                }
                ClipboardAction.TranslateText -> executeAi {
                    ActionResult.Success(aiExecutor.translateText(content))
                }
                ClipboardAction.SummarizeUrl -> executeSummarizeUrl(content)
                ClipboardAction.AnalyzeCode -> executeAi {
                    // v1：与 Explain 共用模型入口；真实接入时可拆独立 prompt / API。
                    ActionResult.Success(
                        aiExecutor.explainCode("Analyze this code:\n\n$content")
                    )
                }
                ClipboardAction.SummarizeText -> executeAi {
                    ActionResult.Success(
                        aiExecutor.explainCode("Summarize this text:\n\n$content")
                    )
                }
                ClipboardAction.ConvertToKotlinDataClass -> executeAi {
                    try {
                        val formatted = aiExecutor.formatJson(content.trim())
                        ActionResult.Success(
                            "[Mock pipeline] Kotlin data class sketch from JSON:\n$formatted"
                        )
                    } catch (t: Throwable) {
                        ActionResult.Error(t.message ?: "ConvertToKotlinDataClass failed")
                    }
                }
                ClipboardAction.OpenUrl -> executeOpenUrl(content)
                null -> TODO()
            }
        } catch (t: Throwable) {
            ActionResult.Error(t.message ?: "Unknown action execute error")
        }
    }

    private suspend fun executeAi(block: suspend () -> ActionResult): ActionResult {
        return withContext(Dispatchers.Default) {
            try {
                block()
            } catch (t: Throwable) {
                ActionResult.Error(t.message ?: "AI execution failed")
            }
        }
    }

    private suspend fun executeFormatJson(content: String): ActionResult {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return ActionResult.Error("JSON input is empty")
        }
        return executeAi {
            try {
                ActionResult.Success(aiExecutor.formatJson(trimmed))
            } catch (t: Throwable) {
                ActionResult.Error(t.message ?: "formatJson failed")
            }
        }
    }

    private suspend fun executeSummarizeUrl(content: String): ActionResult {
        val url = extractPrimaryUrl(content.trim())
            ?: return ActionResult.Error("No URL found in clipboard content")
        return executeAi {
            try {
                ActionResult.Success(aiExecutor.summarizeUrl(url))
            } catch (t: Throwable) {
                ActionResult.Error(t.message ?: "summarizeUrl failed")
            }
        }
    }

    private fun executeOpenUrl(content: String): ActionResult {
        val url = content.trim()
        if (url.isEmpty()) return ActionResult.Error("URL is empty")
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ActionResult.Success("Opened URL in browser")
        } catch (t: Throwable) {
            ActionResult.Error(t.message ?: "Failed to open URL")
        }
    }

    private fun extractPrimaryUrl(text: String): String? {
        val matcher = Patterns.WEB_URL.matcher(text)
        return if (matcher.find()) matcher.group() else null
    }
}
