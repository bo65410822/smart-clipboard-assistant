package com.lzb.clipboardmonitor.domain.usecase

import com.lzb.clipboardmonitor.domain.model.History
import com.lzb.clipboardmonitor.domain.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    operator fun invoke(): Flow<List<History>> = repository.observeHistory()
}
