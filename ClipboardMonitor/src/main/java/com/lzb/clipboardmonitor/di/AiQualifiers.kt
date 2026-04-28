package com.lzb.clipboardmonitor.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PrimaryAI

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FallbackAI
