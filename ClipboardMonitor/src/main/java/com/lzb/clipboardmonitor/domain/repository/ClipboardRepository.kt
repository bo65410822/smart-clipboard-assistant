package com.lzb.clipboardmonitor.domain.repository

import com.lzb.clipboardmonitor.domain.model.ClipboardContent
import kotlinx.coroutines.flow.Flow

/**
 * 领域层仓储抽象。
 *
 * 上层（UseCase / ViewModel）只依赖该接口，不感知系统 ClipboardManager 细节。
 */
interface ClipboardRepository {
    /**
     * 持续观察系统剪贴板变化并输出领域模型流。
     */
    fun observeClipboardChanges(): Flow<ClipboardContent>
}
