package com.lzb.clipdev.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import com.lzb.clipboardmonitor.domain.model.ClipboardAction

@Composable
fun ActionSection(
    actions: List<ClipboardAction>,
    onActionClick: (ClipboardAction) -> Unit
) {
    Text(text = "可执行操作", style = MaterialTheme.typography.titleMedium)
    if (actions.isEmpty()) {
        Text(text = "当前内容暂无可执行操作", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val displayed = actions.take(2)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        displayed.forEach { action ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 82.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F8FD)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                onClick = { onActionClick(action) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val icon = when (action) {
                            ClipboardAction.FormatJson -> Icons.Outlined.Code
                            ClipboardAction.ConvertToKotlinDataClass -> Icons.Outlined.DataObject
                            else -> Icons.Outlined.Code
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF5B8DEF),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = action.toDisplayName(), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = when (action) {
                            ClipboardAction.FormatJson -> "美化输出结果"
                            ClipboardAction.ConvertToKotlinDataClass -> "生成数据模型"
                            else -> "执行操作"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF98A2B3)
                    )
                }
            }
        }
        if (displayed.size == 1) {
            // keep alignment if only one action
            Column(modifier = Modifier.weight(1f)) {}
        }
    }
}
