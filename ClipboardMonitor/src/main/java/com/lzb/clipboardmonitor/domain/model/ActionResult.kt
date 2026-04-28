package com.lzb.clipboardmonitor.domain.model

/**
 * 动作执行统一结果。
 */
sealed class ActionResult {
    data class Success(val result: String) : ActionResult()
    data class Error(val message: String) : ActionResult()
}
