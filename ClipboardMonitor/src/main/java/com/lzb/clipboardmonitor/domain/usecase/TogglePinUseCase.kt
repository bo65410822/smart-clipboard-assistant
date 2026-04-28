package com.lzb.clipboardmonitor.domain.usecase

import com.lzb.clipboardmonitor.domain.repository.HistoryRepository
import javax.inject.Inject

class TogglePinUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.togglePin(id)
    }
}
