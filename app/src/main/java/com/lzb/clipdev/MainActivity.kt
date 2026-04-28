package com.lzb.clipdev

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.viewModels
import com.lzb.clipdev.ui.ClipboardScreen
import com.lzb.clipdev.ui.mode.ResultModeRegistry
import com.lzb.clipdev.ui.theme.ClipDevTheme
import com.lzb.clipboardmonitor.presentation.ClipboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: ClipboardViewModel by viewModels()
    @Inject
    lateinit var resultModeRegistry: ResultModeRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)

        // Keep system bars consistent with light theme.
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            ClipDevTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ClipboardRoute(
                        modifier = Modifier,
                        viewModel = viewModel,
                        resultModeRegistry = resultModeRegistry
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.type != "text/plain") return
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        viewModel.handleSharedText(sharedText)
    }
}

@Composable
private fun ClipboardRoute(
    modifier: Modifier = Modifier,
    viewModel: ClipboardViewModel = hiltViewModel(),
    resultModeRegistry: ResultModeRegistry
) {
    ClipboardScreen(
        viewModel = viewModel,
        resultModeRegistry = resultModeRegistry,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ClipDevTheme {
        // Preview does not create Hilt graph; use static placeholder.
        androidx.compose.material3.Text("剪贴板页面预览")
    }
}