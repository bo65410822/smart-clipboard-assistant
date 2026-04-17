package com.lzb.clipboardmonitor.domain.model

/**
 * 分类后的剪贴板内容。
 *
 * @property original 原始剪贴板内容。
 * @property contentType 规则分类结果。
 */
data class ClassifiedClipboardContent(
    val original: ClipboardContent,
    val contentType: ContentType
)
