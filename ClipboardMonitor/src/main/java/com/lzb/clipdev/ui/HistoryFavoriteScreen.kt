package com.lzb.clipdev.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lzb.clipboardmonitor.domain.model.History

@Composable
fun HistoryScreen(
    items: List<History>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onItemClick: (History) -> Unit,
    onTogglePin: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "历史", style = MaterialTheme.typography.titleLarge)
        SearchBar(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "搜索文本或结果"
        )

        if (items.isEmpty()) {
            Text(text = "暂无历史记录", style = MaterialTheme.typography.bodyMedium)
            return
        }

        val pinned = items.filter { it.isPinned }
        val normal = items.filterNot { it.isPinned }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pinned.isNotEmpty()) {
                item { SectionHeader("⭐ 收藏") }
                items(pinned, key = { it.id }) { item ->
                    HistoryItem(
                        item = item,
                        onClick = { onItemClick(item) },
                        onTogglePin = { onTogglePin(item.id) },
                        onDelete = { onDelete(item.id) },
                        onShowMessage = onShowMessage
                    )
                }
                item { SectionHeader("普通记录") }
            } else {
                item { SectionHeader("普通记录") }
            }
            items(normal, key = { it.id }) { item ->
                HistoryItem(
                    item = item,
                    onClick = { onItemClick(item) },
                    onTogglePin = { onTogglePin(item.id) },
                    onDelete = { onDelete(item.id) },
                    onShowMessage = onShowMessage
                )
            }
        }
    }
}

@Composable
fun FavoriteScreen(
    items: List<History>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onItemClick: (History) -> Unit,
    onTogglePin: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "收藏", style = MaterialTheme.typography.titleLarge)
        SearchBar(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "搜索收藏内容"
        )

        if (items.isEmpty()) {
            Text(text = "暂无收藏记录", style = MaterialTheme.typography.bodyMedium)
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                HistoryItem(
                    item = item,
                    onClick = { onItemClick(item) },
                    onTogglePin = { onTogglePin(item.id) },
                    onDelete = { onDelete(item.id) },
                    onShowMessage = onShowMessage
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

