package com.lzb.clipboardmonitor.di

import com.lzb.clipboardmonitor.presentation.notifier.ResultNotifier
import com.lzb.clipboardmonitor.presentation.notifier.ToastNotifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotifierModule {

    @Binds
    @IntoSet
    abstract fun bindToastNotifier(impl: ToastNotifier): ResultNotifier

    companion object {
        @Provides
        @Singleton
        fun provideNotifierList(notifierSet: Set<@JvmSuppressWildcards ResultNotifier>): List<ResultNotifier> {
            return notifierSet.toList()
        }
    }
}
