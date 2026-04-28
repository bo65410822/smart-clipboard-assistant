package com.lzb.clipdev.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.core.app.NotificationCompat
import com.lzb.clipdev.MainActivity
import com.lzb.clipdev.R
import com.lzb.clipboardmonitor.domain.executor.ClipboardActionExecutor
import com.lzb.clipboardmonitor.domain.model.ActionResult
import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.domain.model.ContentType
import com.lzb.clipboardmonitor.domain.usecase.ClassifyClipboardContentUseCase
import com.lzb.clipboardmonitor.domain.usecase.GetAvailableActionsUseCase
import com.lzb.clipboardmonitor.domain.usecase.ObserveClipboardChangesUseCase
import com.lzb.clipboardmonitor.presentation.notifier.ResultNotifyDispatcher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ClipboardForegroundService : Service() {

    @Inject
    lateinit var observeClipboardChangesUseCase: ObserveClipboardChangesUseCase

    @Inject
    lateinit var classifyClipboardContentUseCase: ClassifyClipboardContentUseCase

    @Inject
    lateinit var getAvailableActionsUseCase: GetAvailableActionsUseCase

    @Inject
    lateinit var clipboardActionExecutor: ClipboardActionExecutor

    @Inject
    lateinit var resultNotifyDispatcher: ResultNotifyDispatcher

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null
    private var lastText: String? = null
    private var isAppForeground: Boolean = true
    private var hasShownBackgroundHint = false
    private val processLifecycle by lazy { ProcessLifecycleOwner.get().lifecycle }
    private val appStateObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            isAppForeground = true
            hasShownBackgroundHint = false
            updateForegroundNotification(
                title = "Clipboard Assistant Running",
                text = "前台可自动处理剪贴板"
            )
        }

        override fun onStop(owner: LifecycleOwner) {
            isAppForeground = false
            showBackgroundHintNotificationIfNeeded()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "ClipboardForegroundService onCreate")
        createNotificationChannel()
        processLifecycle.addObserver(appStateObserver)
        isAppForeground = processLifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "ClipboardForegroundService onStartCommand")
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        if (collectJob == null) {
            startClipboardCollect()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "ClipboardForegroundService onDestroy")
        collectJob?.cancel()
        collectJob = null
        processLifecycle.removeObserver(appStateObserver)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startClipboardCollect() {
        collectJob = serviceScope.launch {
            observeClipboardChangesUseCase()
                .debounce(DEBOUNCE_MS)
                .distinctUntilChangedBy { it.text }
                .collectLatest { content ->
                    if (content.text == lastText) return@collectLatest
                    lastText = content.text

                    // 前台由 ViewModel 统一处理，避免 Service 与 UI 双执行导致“有提示无结果”。
                    if (isAppForeground) return@collectLatest

                    val contentType = classifyClipboardContentUseCase(content).contentType
                    val actions = getAvailableActionsUseCase(contentType)
                    val defaultAction = getDefaultAction(contentType)
                        ?.takeIf { it in actions } ?: return@collectLatest
                    if (!isAppForeground) {
                        showBackgroundHintNotificationIfNeeded()
                        return@collectLatest
                    }

                    when (val result = clipboardActionExecutor.execute(defaultAction, content.text)) {
                        is ActionResult.Success -> {
                            resultNotifyDispatcher.dispatch(result.result)
                        }
                        is ActionResult.Error -> Unit
                    }
                }
        }
    }

    private fun getDefaultAction(contentType: ContentType): ClipboardAction? {
        return when (contentType) {
            ContentType.JSON -> ClipboardAction.ConvertToKotlinDataClass
            ContentType.URL -> ClipboardAction.OpenUrl
            ContentType.CODE -> ClipboardAction.ExplainCode
            ContentType.TEXT -> ClipboardAction.SummarizeText
            ContentType.UNKNOWN -> null
        }
    }

    private fun createForegroundNotification(): Notification {
        val pendingIntent = createMainPendingIntent()
        val contentText = if (isAppForeground) {
            "前台可自动处理剪贴板"
        } else {
            "后台读取受限，点击返回应用处理"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Clipboard Assistant Running")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createMainPendingIntent(): PendingIntent {
        val activityIntent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showBackgroundHintNotificationIfNeeded() {
        if (hasShownBackgroundHint) return
        hasShownBackgroundHint = true
        updateForegroundNotification(
            title = "后台读取受限",
            text = "点击返回应用后将自动处理剪贴板"
        )
    }

    private fun updateForegroundNotification(title: String, text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(createMainPendingIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Clipboard Assistant Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "clipboard_assistant_channel"
        private const val NOTIFICATION_ID = 1001
        private const val DEBOUNCE_MS = 500L

        fun start(context: Context) {
            val intent = Intent(context, ClipboardForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.i(TAG, "ClipboardForegroundService start requested")
            } catch (t: Throwable) {
                Log.e(TAG, "ClipboardForegroundService start failed", t)
            }
        }

        private const val TAG = "ClipboardFGService"
    }
}
