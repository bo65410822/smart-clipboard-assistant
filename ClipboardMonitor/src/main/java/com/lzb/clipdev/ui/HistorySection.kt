package com.lzb.clipdev.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lzb.clipboardmonitor.domain.model.History
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistorySection(
    historyList: List<History>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onHistoryClick: (History) -> Unit,
    onTogglePin: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pinned = historyList.filter { it.isPinned }
    val normal = historyList.filterNot { it.isPinned }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "历史记录", style = MaterialTheme.typography.titleMedium)
            Text(text = "查看全部", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("搜索历史记录") },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            }
        )
        if (historyList.isEmpty()) {
            Text(text = "暂无历史记录", style = MaterialTheme.typography.bodyMedium)
            return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            if (pinned.isNotEmpty()) {
                item { Text("⭐ 收藏", style = MaterialTheme.typography.labelLarge) }
                items(pinned, key = { it.id }) { item ->
                    HistoryItem(
                        item = item,
                        onClick = { onHistoryClick(item) },
                        onTogglePin = { onTogglePin(item.id) },
                        onDelete = { onDelete(item.id) },
                        onShowMessage = onShowMessage
                    )
                }
                item { Divider() }
            }
            item { Text("普通记录", style = MaterialTheme.typography.labelLarge) }
            items(normal, key = { it.id }) { item ->
                HistoryItem(
                    item = item,
                    onClick = { onHistoryClick(item) },
                    onTogglePin = { onTogglePin(item.id) },
                    onDelete = { onDelete(item.id) },
                    onShowMessage = onShowMessage
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HistoryItem(
    item: History,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "类型: ${item.contentType}  动作: ${item.action.toDisplayName()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(text = formatTime(item.timestamp), style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                        Text(text = if (item.isPinned) "★" else "☆")
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(item.result))
                            onShowMessage("已复制结果")
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(text = "复")
                    }
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("复制结果") },
                    onClick = {
                        clipboardManager.setText(AnnotatedString(item.result))
                        onShowMessage("已复制结果")
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (item.isPinned) "取消收藏" else "加入收藏") },
                    onClick = {
                        onTogglePin()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
