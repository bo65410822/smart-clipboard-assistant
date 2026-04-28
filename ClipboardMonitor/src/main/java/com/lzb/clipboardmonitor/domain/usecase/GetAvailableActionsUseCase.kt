package com.lzb.clipboardmonitor.domain.usecase

import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.domain.model.ContentType
import javax.inject.Inject

/**
 * 根据内容分类返回可用动作列表。
 */
class GetAvailableActionsUseCase @Inject constructor() {

    operator fun invoke(contentType: ContentType): List<ClipboardAction> {
        return when (contentType) {
            ContentType.JSON -> listOf(
                ClipboardAction.FormatJson,
                ClipboardAction.ConvertToKotlinDataClass
            )
            ContentType.CODE -> listOf(
                ClipboardAction.ExplainCode,
                ClipboardAction.AnalyzeCode
            )
            ContentType.URL -> listOf(
                ClipboardAction.OpenUrl,
                ClipboardAction.SummarizeUrl
            )
            ContentType.TEXT -> listOf(
                ClipboardAction.TranslateText,
                ClipboardAction.SummarizeText
            )
            ContentType.UNKNOWN -> emptyList()
        }
    }
}
