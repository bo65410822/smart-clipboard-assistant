package com.lzb.clipdev.ui

import com.lzb.clipboardmonitor.domain.model.ClipboardAction

fun ClipboardAction.toDisplayName(): String {
    return when (this) {
        ClipboardAction.FormatJson -> "格式化 JSON"
        ClipboardAction.ConvertToKotlinDataClass -> "转 Kotlin 数据类"
        ClipboardAction.ExplainCode -> "解释代码"
        ClipboardAction.AnalyzeCode -> "分析代码"
        ClipboardAction.OpenUrl -> "打开链接"
        ClipboardAction.SummarizeUrl -> "总结链接"
        ClipboardAction.SummarizeText -> "总结文本"
        ClipboardAction.TranslateText -> "翻译文本"
    }
}
