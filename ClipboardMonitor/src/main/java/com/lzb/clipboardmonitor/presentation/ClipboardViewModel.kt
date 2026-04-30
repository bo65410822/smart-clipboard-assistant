package com.lzb.clipboardmonitor.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzb.clipboardmonitor.domain.executor.ClipboardActionExecutor
import com.lzb.clipboardmonitor.domain.model.ActionResult
import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.domain.model.ClipboardContent
import com.lzb.clipboardmonitor.domain.model.ContentType
import com.lzb.clipboardmonitor.domain.model.History
import com.lzb.clipboardmonitor.domain.usecase.ClassifyClipboardContentUseCase
import com.lzb.clipboardmonitor.domain.usecase.DeleteHistoryItemUseCase
import com.lzb.clipboardmonitor.domain.usecase.GetAvailableActionsUseCase
import com.lzb.clipboardmonitor.domain.usecase.ObserveHistoryUseCase
import com.lzb.clipboardmonitor.domain.usecase.ObserveClipboardChangesUseCase
import com.lzb.clipboardmonitor.domain.usecase.SaveHistoryUseCase
import com.lzb.clipboardmonitor.domain.usecase.SearchHistoryUseCase
import com.lzb.clipboardmonitor.domain.usecase.TogglePinUseCase
import com.lzb.clipboardmonitor.presentation.notifier.NotifyChannel
import com.lzb.clipboardmonitor.presentation.notifier.ResultNotifierController
import com.lzb.clipboardmonitor.presentation.notifier.ResultNotifyDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ClipboardViewModel @Inject constructor(
    private val observeClipboardChangesUseCase: ObserveClipboardChangesUseCase,
    private val observeHistoryUseCase: ObserveHistoryUseCase,
    private val searchHistoryUseCase: SearchHistoryUseCase,
    private val togglePinUseCase: TogglePinUseCase,
    private val deleteHistoryItemUseCase: DeleteHistoryItemUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
    private val classifyClipboardContentUseCase: ClassifyClipboardContentUseCase,
    private val getAvailableActionsUseCase: GetAvailableActionsUseCase,
    private val clipboardActionExecutor: ClipboardActionExecutor,
    private val resultNotifierController: ResultNotifierController,
    private val resultNotifyDispatcher: ResultNotifyDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClipboardUiState())
    val uiState: StateFlow<ClipboardUiState> = _uiState.asStateFlow()
    val enabledNotifierChannels: StateFlow<Set<NotifyChannel>> = resultNotifierController.enabledChannels
    private var lastAutoExecutedText: String? = null
    private var lastHandledSharedText: String? = null
    private var pendingIgnoreClipboardText: String? = null

    init {
        observeClipboard()
        observeHistory()
    }

    fun executeAction(action: ClipboardAction) {
        val currentText = _uiState.value.content?.text.orEmpty()
        if (currentText.isBlank()) {
            _uiState.update {
                it.copy(executionResult = "没有可执行的剪贴板文本。")
            }
            return
        }

        viewModelScope.launch {
            executeAndUpdateResult(currentText, action)
        }
    }

    fun enableNotifier(channel: NotifyChannel) {
        resultNotifierController.enable(channel)
    }

    fun disableNotifier(channel: NotifyChannel) {
        resultNotifierController.disable(channel)
    }

    fun setNotifierChannels(channels: Set<NotifyChannel>) {
        resultNotifierController.setEnabledChannels(channels)
    }

    fun setAutoExecuteEnabled(enabled: Boolean) {
        _uiState.update { it.copy(autoExecuteEnabled = enabled) }
    }

    fun handleSharedText(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return
        viewModelScope.launch {
            processIncomingContent(
                content = ClipboardContent(
                    text = normalized,
                    timestampMillis = System.currentTimeMillis()
                ),
                isFromShare = true,
                dedupeWithLastAutoText = false
            )
        }
    }

    fun restoreFromHistory(history: History) {
        val restoredContent = ClipboardContent(
            text = history.text,
            timestampMillis = history.timestamp
        )
        val availableActions = getAvailableActionsUseCase(history.contentType)
        _uiState.update {
            it.copy(
                content = restoredContent,
                contentType = history.contentType,
                actions = availableActions,
                executionResult = history.result,
                isLoading = false,
                isFromShare = false
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun togglePin(historyId: Long) {
        viewModelScope.launch {
            togglePinUseCase(historyId)
        }
    }

    fun toggleFavorite(historyId: Long) = togglePin(historyId)

    fun deleteHistoryItem(historyId: Long) {
        viewModelScope.launch {
            deleteHistoryItemUseCase(historyId)
        }
    }

    fun restoreHistoryItem(history: History) = restoreFromHistory(history)

    fun formatJson() = executeAction(ClipboardAction.FormatJson)

    fun generateKotlinClass() = executeAction(ClipboardAction.ConvertToKotlinDataClass)

    fun copyResult() = markAppCopiedResult(_uiState.value.executionResult)

    fun shareResult(): String = _uiState.value.executionResult

    fun addHistoryItem(item: History) {
        viewModelScope.launch {
            saveHistoryUseCase(
                text = item.text,
                contentType = item.contentType,
                action = item.action,
                result = item.result,
                timestamp = item.timestamp
            )
        }
    }

    fun markAppCopiedResult(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return
        pendingIgnoreClipboardText = normalized
    }

    private fun observeClipboard() {
        viewModelScope.launch {
            observeClipboardChangesUseCase()
                .distinctUntilChangedBy { it.text }
                .collectLatest { content ->
                    processIncomingContent(content = content, isFromShare = false, dedupeWithLastAutoText = true)
                }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            _uiState
                .map { it.searchQuery.trim() }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isEmpty()) observeHistoryUseCase() else searchHistoryUseCase(query)
                }
                .distinctUntilChanged()
                .collectLatest { list ->
                    _uiState.update {
                        it.copy(
                            historyList = list,
                            favoriteList = list.filter { history -> history.isPinned }
                        )
                    }
                }
        }
    }

    private suspend fun processIncomingContent(
        content: ClipboardContent,
        isFromShare: Boolean,
        dedupeWithLastAutoText: Boolean
    ) {
        val pendingIgnored = pendingIgnoreClipboardText
        if (!isFromShare && pendingIgnored != null && content.text.trim() == pendingIgnored) {
            pendingIgnoreClipboardText = null
            return
        }
        if (isFromShare && lastHandledSharedText == content.text) return
        if (isFromShare) {
            lastHandledSharedText = content.text
        }

        val classified = classifyClipboardContentUseCase(content)
        val availableActions = getAvailableActionsUseCase(classified.contentType)
        val defaultAction = getDefaultAction(classified.contentType)
            ?.takeIf { it in availableActions }

        _uiState.update {
            it.copy(
                content = content,
                contentType = classified.contentType,
                actions = availableActions,
                executionResult = "",
                processingTimeMs = null,
                isLoading = false,
                isFromShare = isFromShare
            )
        }

        val shouldAutoExecute = when {
            !_uiState.value.autoExecuteEnabled -> false
            defaultAction == null -> false
            dedupeWithLastAutoText -> lastAutoExecutedText != content.text
            else -> true
        }
        if (shouldAutoExecute) {
            lastAutoExecutedText = content.text
            executeAndUpdateResult(content.text, defaultAction)
        }
    }

    private fun getDefaultAction(contentType: ContentType): ClipboardAction? {
        return when (contentType) {
            ContentType.JSON -> ClipboardAction.ConvertToKotlinDataClass
            ContentType.URL -> ClipboardAction.SummarizeUrl
            ContentType.CODE -> ClipboardAction.ExplainCode
            ContentType.TEXT -> ClipboardAction.SummarizeText
            ContentType.UNKNOWN -> null
        }
    }

    private suspend fun executeAndUpdateResult(
        text: String,
        action: ClipboardAction?
    ) {
        _uiState.update { it.copy(isLoading = true, processingTimeMs = null) }
        val startTime = SystemClock.elapsedRealtime()
        when (val result = clipboardActionExecutor.execute(action, text)) {
            is ActionResult.Success -> {
                val duration = SystemClock.elapsedRealtime() - startTime
                resultNotifyDispatcher.dispatch(result.result)
                if (action != null) {
                    saveHistoryUseCase(
                        text = text,
                        contentType = _uiState.value.contentType,
                        action = action,
                        result = result.result
                    )
                }
                _uiState.update {
                    it.copy(
                        executionResult = result.result,
                        processingTimeMs = duration,
                        isLoading = false
                    )
                }
            }

            is ActionResult.Error -> {
                val duration = SystemClock.elapsedRealtime() - startTime
                _uiState.update {
                    it.copy(
                        executionResult = result.message,
                        processingTimeMs = duration,
                        isLoading = false
                    )
                }
            }
        }
    }
}
