package com.lzb.clipdev.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lzb.clipboardmonitor.domain.model.ClipboardAction
import com.lzb.clipboardmonitor.presentation.ClipboardViewModel

@Composable
fun ClipboardScreen(
    viewModel: ClipboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Current Clipboard Text",
            style = MaterialTheme.typography.titleMedium
        )
        Text(text = uiState.text.ifBlank { "-" })

        Text(
            text = "ContentType: ${uiState.contentType}",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Available Actions",
            style = MaterialTheme.typography.titleMedium
        )
        uiState.actions.forEach { action ->
            Button(
                onClick = { viewModel.executeAction(action) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = action.toDisplayName())
            }
        }

        Text(
            text = "Execution Result",
            style = MaterialTheme.typography.titleMedium
        )
        Text(text = uiState.executionResult.ifBlank { "-" })
    }
}

private fun ClipboardAction.toDisplayName(): String {
    return when (this) {
        ClipboardAction.FormatJson -> "FormatJson"
        ClipboardAction.ConvertToKotlinDataClass -> "ConvertToKotlinDataClass"
        ClipboardAction.ExplainCode -> "ExplainCode"
        ClipboardAction.AnalyzeCode -> "AnalyzeCode"
        ClipboardAction.OpenUrl -> "OpenUrl"
        ClipboardAction.SummarizeText -> "SummarizeText"
        ClipboardAction.TranslateText -> "TranslateText"
    }
}
