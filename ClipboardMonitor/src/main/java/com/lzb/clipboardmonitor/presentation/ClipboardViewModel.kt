package com.lzb.clipboardmonitor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzb.clipboardmonitor.domain.executor.ClipboardActionExecutor
import com.lzb.clipboardmonitor.domain.model.ActionResult
import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.domain.usecase.ClassifyClipboardContentUseCase
import com.lzb.clipboardmonitor.domain.usecase.GetAvailableActionsUseCase
import com.lzb.clipboardmonitor.domain.usecase.ObserveClipboardChangesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ClipboardViewModel @Inject constructor(
    private val observeClipboardChangesUseCase: ObserveClipboardChangesUseCase,
    private val classifyClipboardContentUseCase: ClassifyClipboardContentUseCase,
    private val getAvailableActionsUseCase: GetAvailableActionsUseCase,
    private val clipboardActionExecutor: ClipboardActionExecutor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClipboardUiState())
    val uiState: StateFlow<ClipboardUiState> = _uiState.asStateFlow()

    init {
        observeClipboard()
    }

    fun executeAction(action: ClipboardAction) {
        val currentText = _uiState.value.text
        if (currentText.isBlank()) {
            _uiState.update {
                it.copy(executionResult = "No clipboard text to execute.")
            }
            return
        }

        viewModelScope.launch {
            when (val result = clipboardActionExecutor.execute(action, currentText)) {
                is ActionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            executionResult = result.output ?: "Action executed successfully."
                        )
                    }
                }
                is ActionResult.Error -> {
                    _uiState.update {
                        it.copy(
                            executionResult = result.message
                        )
                    }
                }
            }
        }
    }

    private fun observeClipboard() {
        viewModelScope.launch {
            observeClipboardChangesUseCase()
                .collectLatest { content ->
                    val classified = classifyClipboardContentUseCase(content)
                    val availableActions = getAvailableActionsUseCase(classified.contentType)

                    _uiState.update {
                        it.copy(
                            text = content.text,
                            contentType = classified.contentType,
                            actions = availableActions,
                            executionResult = ""
                        )
                    }
                }
        }
    }
}
