package com.lzb.clipdev

import android.app.Application
import com.lzb.clipboardmonitor.domain.observer.ClipboardBackgroundObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ClipDevApplication : Application() {
    @Inject
    lateinit var clipboardBackgroundObserver: ClipboardBackgroundObserver

    override fun onCreate() {
        super.onCreate()
        clipboardBackgroundObserver.start()
    }

    override fun onTerminate() {
        clipboardBackgroundObserver.stop()
        super.onTerminate()
    }
}
