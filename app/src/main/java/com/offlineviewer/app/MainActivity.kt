package com.offlineviewer.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlineviewer.app.data.FileLoader
import com.offlineviewer.app.data.ThemePreferences
import com.offlineviewer.app.ui.ViewerScreen
import com.offlineviewer.app.ui.ViewerViewModel
import com.offlineviewer.app.ui.theme.AppTheme

/**
 * Single activity. Hosts the Compose UI, wires the Storage Access Framework
 * open-document flow, and routes inbound ACTION_VIEW / ACTION_SEND intents to
 * the viewer. Fully offline: no INTERNET permission is declared.
 */
class MainActivity : ComponentActivity() {

    private lateinit var themePreferences: ThemePreferences

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Smooth cold start via the AndroidX SplashScreen compat library.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        themePreferences = ThemePreferences(this)
        enableEdgeToEdge()
        setContent {
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle()
            AppTheme(themeMode = themeMode) {
                val windowSizeClass = calculateWindowSizeClass(this)
                ViewerApp(
                    initialUri = extractUri(intent),
                    windowSizeClass = windowSizeClass,
                    themeMode = themeMode,
                    onThemeModeChange = themePreferences::setThemeMode,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingUri = extractUri(intent)
    }

    /** Uri delivered via onNewIntent while the activity is already running. */
    private var pendingUri: Uri? by mutableStateOf(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    private fun ViewerApp(
        initialUri: Uri?,
        windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
        themeMode: com.offlineviewer.app.ui.theme.ThemeMode,
        onThemeModeChange: (com.offlineviewer.app.ui.theme.ThemeMode) -> Unit,
    ) {
        val context = LocalContext.current
        val viewModel: ViewerViewModel = viewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val loader = remember(context) { FileLoader(context.contentResolver) }

        val openLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                takePersistableReadPermission(uri)
                viewModel.open(uri, loader)
            }
        }

        // Handle a file delivered via VIEW/SEND on first composition.
        LaunchedEffect(initialUri) {
            if (initialUri != null) viewModel.open(initialUri, loader)
        }
        // Handle a file delivered via onNewIntent while running.
        LaunchedEffect(pendingUri) {
            pendingUri?.let {
                viewModel.open(it, loader)
                pendingUri = null
            }
        }

        ViewerScreen(
            state = state,
            onOpenFile = { openLauncher.launch(OPEN_MIME_FILTER) },
            windowSizeClass = windowSizeClass,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onShare = { shareCurrentFile(viewModel.currentUri) },
            modifier = Modifier,
        )
    }

    private fun takePersistableReadPermission(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Shares the currently-open file via the system share sheet. Fully local:
     * the Uri is a content:// reference already granted to this app; no network
     * access is involved.
     */
    private fun shareCurrentFile(uri: Uri?) {
        if (uri == null) return
        runCatching {
            val mime = contentResolver.getType(uri) ?: "*/*"
            val share = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, getString(R.string.share_file)))
        }
    }

    /** Pulls a content/file Uri out of a VIEW or SEND intent, if present. */
    private fun extractUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }

            else -> null
        }
    }

    companion object {
        /** Accept every type in the SAF picker; our renderers cover anything. */
        private val OPEN_MIME_FILTER = arrayOf("*/*")
    }
}
