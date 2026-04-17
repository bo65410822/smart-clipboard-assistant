package com.lzb.clipboardmonitor.domain.executor

import com.lzb.clipboardmonitor.domain.model.ActionResult
import com.lzb.clipboardmonitor.domain.model.ClipboardAction

/**
 * 动作执行器抽象。
 */
interface ClipboardActionExecutor {
    suspend fun execute(
        action: ClipboardAction,
        content: String
    ): ActionResult
}
