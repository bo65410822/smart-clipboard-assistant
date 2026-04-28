package com.lzb.clipboardmonitor.domain.model

/**
 * 剪贴板内容可执行动作定义。
 *
 * 使用 sealed class 便于后续按类型扩展参数化动作。
 */
sealed class ClipboardAction {
    data object FormatJson : ClipboardAction()
    data object ConvertToKotlinDataClass : ClipboardAction()
    data object ExplainCode : ClipboardAction()
    data object AnalyzeCode : ClipboardAction()
    data object OpenUrl : ClipboardAction()
    /** 对剪贴板中的 URL 做摘要（走 AI）。 */
    data object SummarizeUrl : ClipboardAction()
    data object SummarizeText : ClipboardAction()
    data object TranslateText : ClipboardAction()
}
