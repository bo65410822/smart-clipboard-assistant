package com.lzb.clipdev.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.lzb.clipdev.ui.mode.ResultModeRegistry
import com.lzb.clipboardmonitor.presentation.ClipboardViewModel
import com.lzb.clipboardmonitor.presentation.notifier.NotifyChannel
import androidx.compose.ui.platform.LocalContext

@Composable
fun ClipboardScreen(
    viewModel: ClipboardViewModel,
    resultModeRegistry: ResultModeRegistry,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val enabledChannels by viewModel.enabledNotifierChannels.collectAsState()
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.setNotifierChannels(setOf(NotifyChannel.TOAST))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            BottomTabBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    // 搜索只在“历史/收藏”页使用，离开后清空，避免影响其他页面。
                    if (it != BottomTab.HISTORY && it != BottomTab.FAVORITE) {
                        viewModel.updateSearchQuery("")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (selectedTab) {
            BottomTab.HOME -> MainScreen(
                uiState = uiState,
                resultModeRegistry = resultModeRegistry,
                onActionClick = viewModel::executeAction,
                onHistoryClick = viewModel::restoreFromHistory,
                onSearchChange = viewModel::updateSearchQuery,
                onTogglePin = viewModel::togglePin,
                onDeleteHistory = viewModel::deleteHistoryItem,
                onResultCopied = viewModel::markAppCopiedResult,
                onShareClick = { text -> shareResult(context, text) },
                modifier = Modifier.padding(innerPadding)
            )

            BottomTab.FAVORITE -> FavoriteScreen(
                items = uiState.favoriteList,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onItemClick = viewModel::restoreFromHistory,
                onTogglePin = viewModel::togglePin,
                onDelete = viewModel::deleteHistoryItem,
                onShowMessage = {},
                modifier = Modifier.padding(innerPadding)
            )

            BottomTab.HISTORY -> HistoryScreen(
                items = uiState.historyList,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onItemClick = viewModel::restoreFromHistory,
                onTogglePin = viewModel::togglePin,
                onDelete = viewModel::deleteHistoryItem,
                onShowMessage = {},
                modifier = Modifier.padding(innerPadding)
            )

            BottomTab.SETTINGS -> SettingsScreen(
                autoExecuteEnabled = uiState.autoExecuteEnabled,
                enabledChannels = enabledChannels,
                onAutoExecuteEnabledChange = viewModel::setAutoExecuteEnabled,
                onChannelToggle = { channel, enabled ->
                    if (enabled) viewModel.enableNotifier(channel) else viewModel.disableNotifier(channel)
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

private enum class BottomTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("解析", Icons.Outlined.Code),
    HISTORY("历史", Icons.Outlined.AccessTime),
    FAVORITE("收藏", Icons.Outlined.StarBorder),
    SETTINGS("设置", Icons.Outlined.Settings)
}

@Composable
private fun TabPlaceholder(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$title 页面开发中")
    }
}

@Composable
private fun BottomTabBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryBlue = MaterialTheme.colorScheme.primary
    val neutralGray = Color(0xFF6B7280)
    val selectedBg = Color(0xFFE8F1FF)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x14000000))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 10.dp, end = 10.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    val tint = if (selected) primaryBlue else neutralGray

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTabSelected(tab) }
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .then(
                                    if (selected) Modifier
                                        .background(
                                            color = selectedBg,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = tab.title,
                            color = tint,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
