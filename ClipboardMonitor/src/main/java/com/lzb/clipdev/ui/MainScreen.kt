package com.lzb.clipdev.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Share
import com.lzb.clipdev.ui.mode.ResultModeRegistry
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.domain.model.History
import com.lzb.clipboardmonitor.presentation.ClipboardUiState

@Composable
fun MainScreen(
    uiState: ClipboardUiState,
    resultModeRegistry: ResultModeRegistry,
    onActionClick: (ClipboardAction) -> Unit,
    onHistoryClick: (History) -> Unit,
    onSearchChange: (String) -> Unit,
    onTogglePin: (Long) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onResultCopied: (String) -> Unit,
    onShareClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("解析结果", style = MaterialTheme.typography.headlineMedium)
                    Surface(
                        color = Color(0xFFEAF9F0),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(14.dp)
                            )
                            Text("成功", style = MaterialTheme.typography.labelMedium, color = Color(0xFF16A34A))
                        }
                    }
                }
                Text(
                    text = if ((uiState.processingTimeMs ?: 0L) > 0L) {
                        "数据解析完成，耗时 ${uiState.processingTimeMs}ms"
                    } else {
                        "数据解析完成"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
            val shareEnabled = uiState.executionResult.isNotBlank() && !uiState.isLoading
            Surface(
                color = Color(0xFFF3F7FF),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .clickable(enabled = shareEnabled) { onShareClick(uiState.executionResult) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "分享",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        ResultSection(
            result = uiState.executionResult,
            isLoading = uiState.isLoading,
            isFromShare = uiState.isFromShare,
            contentType = uiState.contentType,
            resultModeRegistry = resultModeRegistry,
            onResultCopied = onResultCopied,
            onShowMessage = {}
        )
        ActionSection(
            actions = uiState.actions,
            onActionClick = onActionClick
        )
        RawContentSection(
            text = uiState.text,
            contentTypeLabel = uiState.contentType.name
        )
    }
}
