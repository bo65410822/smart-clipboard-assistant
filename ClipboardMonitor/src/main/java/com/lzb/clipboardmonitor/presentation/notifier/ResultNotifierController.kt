package com.lzb.clipboardmonitor.presentation.notifier

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ResultNotifierController @Inject constructor() {
    private val _enabledChannels = MutableStateFlow(setOf(NotifyChannel.TOAST))
    val enabledChannels: StateFlow<Set<NotifyChannel>> = _enabledChannels.asStateFlow()

    fun setEnabledChannels(channels: Set<NotifyChannel>) {
        _enabledChannels.value = channels
    }

    fun enable(channel: NotifyChannel) {
        _enabledChannels.value = _enabledChannels.value + channel
    }

    fun disable(channel: NotifyChannel) {
        _enabledChannels.value = _enabledChannels.value - channel
    }
}
