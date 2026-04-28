package com.lzb.clipboardmonitor.data.observer

import com.lzb.clipboardmonitor.domain.observer.ClipboardBackgroundObserver
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认空实现：显式禁用后台监听能力。
 */
@Singleton
class NoOpClipboardBackgroundObserver @Inject constructor() : ClipboardBackgroundObserver {
    override fun start() = Unit

    override fun stop() = Unit
}
