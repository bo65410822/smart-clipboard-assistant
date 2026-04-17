package com.lzb.clipboardmonitor.data.datasource

import com.lzb.clipboardmonitor.domain.model.ClipboardContent
import kotlinx.coroutines.flow.Flow

/**
 * Data 层数据源抽象。
 *
 * 定义“如何从系统侧拿到剪贴板变化数据”，但不暴露具体 Android 实现给上层。
 */
interface ClipboardDataSource {
    /**
     * 观察剪贴板变化并直接输出可被业务消费的领域模型流。
     */
    fun observeClipboardChanges(): Flow<ClipboardContent>
}
