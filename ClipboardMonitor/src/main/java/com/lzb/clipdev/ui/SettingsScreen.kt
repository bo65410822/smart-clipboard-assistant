package com.lzb.clipdev.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lzb.clipboardmonitor.presentation.notifier.NotifyChannel

@Composable
fun SettingsScreen(
    autoExecuteEnabled: Boolean,
    enabledChannels: Set<NotifyChannel>,
    onAutoExecuteEnabledChange: (Boolean) -> Unit,
    onChannelToggle: (NotifyChannel, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "设置", style = MaterialTheme.typography.titleLarge)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "解析", style = MaterialTheme.typography.titleMedium)
                SettingSwitchRow(
                    title = "自动处理",
                    subtitle = "复制内容后自动执行默认操作",
                    checked = autoExecuteEnabled,
                    onCheckedChange = onAutoExecuteEnabledChange
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "通知", style = MaterialTheme.typography.titleMedium)

                NotifyChannel.entries.forEach { channel ->
                    val checked = channel in enabledChannels
                    SettingSwitchRow(
                        title = channel.displayName(),
                        subtitle = channel.subtitle(),
                        checked = checked,
                        onCheckedChange = { onChannelToggle(channel, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun NotifyChannel.displayName(): String = when (this) {
    NotifyChannel.TOAST -> "Toast 提示"
    NotifyChannel.DIALOG -> "弹窗提示"
    NotifyChannel.LOG -> "日志记录"
}

private fun NotifyChannel.subtitle(): String = when (this) {
    NotifyChannel.TOAST -> "轻量提示，适合日常使用"
    NotifyChannel.DIALOG -> "强提醒，适合关键结果"
    NotifyChannel.LOG -> "写入日志，方便排查问题"
}

