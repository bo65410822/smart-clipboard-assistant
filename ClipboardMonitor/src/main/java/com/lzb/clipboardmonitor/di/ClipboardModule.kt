package com.lzb.clipboardmonitor.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lzb.clipboardmonitor.BuildConfig
import com.lzb.clipboardmonitor.data.datasource.ClipboardDataSource
import com.lzb.clipboardmonitor.data.datasource.AndroidClipboardDataSource
import com.lzb.clipboardmonitor.data.datasource.HistoryLocalDataSource
import com.lzb.clipboardmonitor.data.datasource.NoOpRemoteHistoryDataSource
import com.lzb.clipboardmonitor.data.datasource.RemoteHistoryDataSource
import com.lzb.clipboardmonitor.data.datasource.RoomHistoryLocalDataSource
import com.lzb.clipboardmonitor.data.ai.AIExecutor
import com.lzb.clipboardmonitor.data.ai.AiExecutorWithMockFallback
import com.lzb.clipboardmonitor.data.ai.MockExecutor
import com.lzb.clipboardmonitor.data.ai.OpenAIExecutor
import com.lzb.clipboardmonitor.data.ai.SiliconFlowExecutor
import com.lzb.clipboardmonitor.data.executor.DefaultClipboardActionExecutor
import com.lzb.clipboardmonitor.data.executor.LocalClipboardActionExecutor
import com.lzb.clipboardmonitor.data.local.dao.HistoryDao
import com.lzb.clipboardmonitor.data.local.db.ClipboardMonitorDatabase
import com.lzb.clipboardmonitor.data.observer.NoOpClipboardBackgroundObserver
import com.lzb.clipboardmonitor.data.repository.ClipboardRepositoryImpl
import com.lzb.clipboardmonitor.data.repository.HistoryRepositoryImpl
import com.lzb.clipboardmonitor.domain.executor.ClipboardActionExecutor
import com.lzb.clipboardmonitor.domain.observer.ClipboardBackgroundObserver
import com.lzb.clipboardmonitor.domain.repository.ClipboardRepository
import com.lzb.clipboardmonitor.domain.repository.HistoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
        impl: LocalClipboardActionExecutor
    ): ClipboardActionExecutor

    @Binds
    @Singleton
    abstract fun bindAIExecutor(
        impl: AiExecutorWithMockFallback
    ): AIExecutor

    @Binds
    @Singleton
    abstract fun bindClipboardBackgroundObserver(
        impl: NoOpClipboardBackgroundObserver
    ): ClipboardBackgroundObserver

    @Binds
    @Singleton
    abstract fun bindHistoryLocalDataSource(
        impl: RoomHistoryLocalDataSource
    ): HistoryLocalDataSource

    @Binds
    @Singleton
    abstract fun bindRemoteHistoryDataSource(
        impl: NoOpRemoteHistoryDataSource
    ): RemoteHistoryDataSource

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        impl: HistoryRepositoryImpl
    ): HistoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideClipboardMonitorDatabase(
            @ApplicationContext context: Context
        ): ClipboardMonitorDatabase {
            return Room.databaseBuilder(
                context,
                ClipboardMonitorDatabase::class.java,
                "clipboard_monitor.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        @Provides
        @Singleton
        fun provideHistoryDao(
            db: ClipboardMonitorDatabase
        ): HistoryDao = db.historyDao()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE clipboard_history ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        @Provides
        @Singleton
        @FallbackAI
        fun provideFallbackAI(
            mockExecutor: MockExecutor
        ): AIExecutor = mockExecutor

        @Provides
        @Singleton
        @PrimaryAI
        fun providePrimaryAI(
            openAIExecutor: OpenAIExecutor,
            siliconFlowExecutor: SiliconFlowExecutor,
            @FallbackAI fallbackExecutor: AIExecutor
        ): AIExecutor {
            return when (BuildConfig.AI_PROVIDER.trim().lowercase()) {
                "openai" -> openAIExecutor
                "siliconflow" -> siliconFlowExecutor
                "mock" -> fallbackExecutor
                else -> openAIExecutor
            }
        }
    }
}
