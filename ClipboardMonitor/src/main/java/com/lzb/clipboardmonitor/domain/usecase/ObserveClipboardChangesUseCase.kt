package com.lzb.clipboardmonitor.domain.usecase

import com.lzb.clipboardmonitor.domain.model.ClipboardContent
import com.lzb.clipboardmonitor.domain.repository.ClipboardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 用例层：对外暴露“监听剪贴板变化”这一业务动作。
 *
 * 该类存在的价值是收敛业务入口，后续可在此添加限流、埋点、权限校验等策略。
 */
class ObserveClipboardChangesUseCase @Inject constructor(
    private val clipboardRepository: ClipboardRepository
) {
    operator fun invoke(): Flow<ClipboardContent> {
        return clipboardRepository.observeClipboardChanges()
    }
}
