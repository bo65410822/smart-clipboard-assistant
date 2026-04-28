package com.lzb.clipboardmonitor.domain.observer

/**
 * 后台剪贴板观察扩展点。
 *
 * 当前版本可注入 No-Op 实现禁用后台能力，后续可替换为真实实现。
 */
interface ClipboardBackgroundObserver {
    fun start()
    fun stop()
}
