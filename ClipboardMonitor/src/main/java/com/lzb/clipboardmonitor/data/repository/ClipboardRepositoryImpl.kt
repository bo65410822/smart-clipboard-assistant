package com.lzb.clipboardmonitor.data.repository

import com.lzb.clipboardmonitor.data.datasource.ClipboardDataSource
import com.lzb.clipboardmonitor.domain.model.ClipboardContent
import com.lzb.clipboardmonitor.domain.repository.ClipboardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Repository 实现：
 * 负责衔接 domain 与 data，不直接关心 Android 平台细节。
 */
class ClipboardRepositoryImpl @Inject constructor(
    private val clipboardDataSource: ClipboardDataSource
) : ClipboardRepository {

    override fun observeClipboardChanges(): Flow<ClipboardContent> {
        // 当前仅透传数据源；后续可在这里追加业务级转换/去重策略。
        return clipboardDataSource.observeClipboardChanges()
    }
}
