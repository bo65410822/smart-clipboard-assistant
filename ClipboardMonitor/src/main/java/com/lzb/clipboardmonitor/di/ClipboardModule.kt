package com.lzb.clipboardmonitor.di

import com.lzb.clipboardmonitor.data.datasource.ClipboardDataSource
import com.lzb.clipboardmonitor.data.datasource.AndroidClipboardDataSource
import com.lzb.clipboardmonitor.data.executor.DefaultClipboardActionExecutor
import com.lzb.clipboardmonitor.data.repository.ClipboardRepositoryImpl
import com.lzb.clipboardmonitor.domain.executor.ClipboardActionExecutor
import com.lzb.clipboardmonitor.domain.repository.ClipboardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 绑定模块：
 * - 将接口与具体实现解耦
 * - 统一对象生命周期，便于替换测试实现
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ClipboardModule {

    @Binds
    @Singleton
    abstract fun bindClipboardDataSource(
        impl: AndroidClipboardDataSource
    ): ClipboardDataSource

    @Binds
    @Singleton
    abstract fun bindClipboardRepository(
        impl: ClipboardRepositoryImpl
    ): ClipboardRepository

    @Binds
    @Singleton
    abstract fun bindClipboardActionExecutor(
        impl: DefaultClipboardActionExecutor
    ): ClipboardActionExecutor
}
