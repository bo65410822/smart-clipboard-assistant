package com.lzb.clipboardmonitor.domain.model

/**
 * 动作执行结果。
 */
sealed class ActionResult {
    data class Success(val output: String? = null) : ActionResult()
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : ActionResult()
}
