package com.lzb.clipboardmonitor.domain.model

/**
 * 领域层剪贴板内容模型。
 *
 * @property text 已提取的文本内容（业务真正消费的数据）。
 * @property timestampMillis 本次采集时间戳，便于排序、去重策略和后续分析。
 */
data class ClipboardContent(
    val text: String,
    val timestampMillis: Long
)
