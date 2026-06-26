package com.chromeapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chromeapp.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notifications permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val browserViewModel: BrowserViewModel = viewModel()
            val isDarkMode by browserViewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                ChromeOverlayService.viewModel = browserViewModel

                // Safe WebView Re-attachment when Activity returns from background
                LaunchedEffect(Unit) {
                    val overlayService = ChromeOverlayService.serviceInstance
                    if (overlayService != null) {
                        val webView = overlayService.detachWebView()
                        if (webView != null) {
                            // Safely re-attached to layout
                            browserViewModel.log("Re-attached WebView session from floating overlay window.", LogType.SUCCESS)
                        }
                    }
                }

                BrowserMainScreen(viewModel = browserViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If the activity resumes, make sure the overlay isn't holding onto the webview
        val service = ChromeOverlayService.serviceInstance
        if (service != null) {
            service.detachWebView()
        }
    }
}

@Composable
fun BrowserMainScreen(viewModel: BrowserViewModel) {
    val showTabSwitcher by viewModel.showTabSwitcher.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAiMode by viewModel.isAiMode.collectAsState()
    val isIncognito by viewModel.isIncognito.collectAsState()
    val googleAccount by viewModel.googleAccount.collectAsState()
    val customDownloadPath by viewModel.customDownloadPath.collectAsState()
    val isSchedulerActive by viewModel.isSchedulerActive.collectAsState()
    val schedulerIntervalSeconds by viewModel.schedulerIntervalSeconds.collectAsState()

    var isSearching by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFindInPage by remember { mutableStateOf(false) }
    var findInPageQuery by remember { mutableStateOf("") }

    var showScriptWorkspace by remember { mutableStateOf(false) }
    var showBookmarksPanel by remember { mutableStateOf(false) }
    var showHistoryPanel by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showAutomationControlSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDarkMode) Color(0xFF1F1F1F) else Color(0xFFFFFFFF))
        ) {
            if (showTabSwitcher) {
                TabSwitcherScreen(
                    viewModel = viewModel,
                    onClose = { viewModel.setShowTabSwitcher(false) }
                )
            } else if (isSearching) {
                ActiveSearchScreen(
                    viewModel = viewModel,
                    onClose = { isSearching = false }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    val isAtHome = activeTab == null || activeTab.url == "chrome://newtab" || activeTab.url.isEmpty()
                    if (isAtHome) {
                        ChromeHomeHeader(
                            tabCount = tabs.size,
                            isDarkMode = isDarkMode,
                            isIncognito = isIncognito,
                            googleAccount = googleAccount,
                            viewModel = viewModel,
                            onOpenTabSwitcher = { viewModel.setShowTabSwitcher(true) },
                            onMenuAction = { action ->
                                when (action) {
                                    "New Tab" -> viewModel.createNewTab()
                                    "Refresh" -> activeTab?.webView?.reload()
                                    "Bookmarks" -> showBookmarksPanel = true
                                    "History" -> showHistoryPanel = true
                                    "YML Workspace" -> showScriptWorkspace = true
                                    "Automation Sheet" -> showAutomationControlSheet = true
                                    "Settings" -> showSettingsDialog = true
                                    "GoogleAccount" -> viewModel.loadUrlInActiveTab("https://myaccount.google.com/")
                                    "Find in page" -> showFindInPage = true
                                    "Desktop site" -> activeTab?.let { viewModel.toggleDesktopMode(it.id) }
                                    "Clear data" -> viewModel.clearBrowsingData()
                                    "New Incognito Tab" -> {
                                        viewModel.setIncognito(true)
                                        viewModel.createNewTab()
                                    }
                                    "Downloads" -> context.startActivity(android.content.Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS))
                                    "Recent tabs" -> showHistoryPanel = true
                                    "Share" -> {
                                        activeTab?.url?.let { url ->
                                            if (url.isNotEmpty() && !url.startsWith("chrome://")) {
                                                val sendIntent = android.content.Intent().apply {
                                                    this.action = android.content.Intent.ACTION_SEND
                                                    this.putExtra(android.content.Intent.EXTRA_TEXT, url)
                                                    this.type = "text/plain"
                                                }
                                                context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                                            }
                                        }
                                    }
                                    "Translate" -> {
                                        activeTab?.url?.let { url ->
                                            if (url.isNotEmpty() && !url.startsWith("chrome://")) {
                                                viewModel.loadUrlInActiveTab("https://translate.google.com/translate?sl=auto&tl=en&u=$url")
                                            }
                                        }
                                    }
                                    "Add tab to new group" -> android.widget.Toast.makeText(context, "Group created", android.widget.Toast.LENGTH_SHORT).show()
                                    "Show Reading mode" -> android.widget.Toast.makeText(context, "Reading mode activated", android.widget.Toast.LENGTH_SHORT).show()
                                    "Add to Home screen" -> android.widget.Toast.makeText(context, "Shortcut added to home screen", android.widget.Toast.LENGTH_SHORT).show()
                                    "Page Info" -> android.widget.Toast.makeText(context, "Page: ${activeTab?.title}", android.widget.Toast.LENGTH_SHORT).show()
                                    "Help & feedback" -> viewModel.loadUrlInActiveTab("https://support.google.com/chrome/")
                                }
                            },
                            onToggleDarkMode = { viewModel.setDarkMode(!isDarkMode) },
                            onOpenGoogleAccount = { viewModel.loadUrlInActiveTab("https://myaccount.google.com/") }
                        )
                    } else {
                        ChromeTopBar(
                            activeTab = activeTab,
                            tabCount = tabs.size,
                            viewModel = viewModel,
                            onOpenTabSwitcher = { viewModel.setShowTabSwitcher(true) },
                            onSearchTrigger = { isSearching = true },
                            onMenuAction = { action ->
                                when (action) {
                                    "New Tab" -> viewModel.createNewTab()
                                    "Refresh" -> activeTab?.webView?.reload()
                                    "Bookmarks" -> showBookmarksPanel = true
                                    "History" -> showHistoryPanel = true
                                    "YML Workspace" -> showScriptWorkspace = true
                                    "Automation Sheet" -> showAutomationControlSheet = true
                                    "Settings" -> showSettingsDialog = true
                                    "GoogleAccount" -> viewModel.loadUrlInActiveTab("https://myaccount.google.com/")
                                    "Find in page" -> showFindInPage = true
                                    "Desktop site" -> activeTab?.let { viewModel.toggleDesktopMode(it.id) }
                                    "Clear data" -> viewModel.clearBrowsingData()
                                    "New Incognito Tab" -> {
                                        viewModel.setIncognito(true)
                                        viewModel.createNewTab()
                                    }
                                    "Downloads" -> context.startActivity(android.content.Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS))
                                    "Recent tabs" -> showHistoryPanel = true
                                    "Share" -> {
                                        activeTab?.url?.let { url ->
                                            if (url.isNotEmpty() && !url.startsWith("chrome://")) {
                                                val sendIntent = android.content.Intent().apply {
                                                    this.action = android.content.Intent.ACTION_SEND
                                                    this.putExtra(android.content.Intent.EXTRA_TEXT, url)
                                                    this.type = "text/plain"
                                                }
                                                context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                                            }
                                        }
                                    }
                                    "Translate" -> {
                                        activeTab?.url?.let { url ->
                                            if (url.isNotEmpty() && !url.startsWith("chrome://")) {
                                                viewModel.loadUrlInActiveTab("https://translate.google.com/translate?sl=auto&tl=en&u=$url")
                                            }
                                        }
                                    }
                                    "Add tab to new group" -> android.widget.Toast.makeText(context, "Group created", android.widget.Toast.LENGTH_SHORT).show()
                                    "Show Reading mode" -> android.widget.Toast.makeText(context, "Reading mode activated", android.widget.Toast.LENGTH_SHORT).show()
                                    "Add to Home screen" -> android.widget.Toast.makeText(context, "Shortcut added to home screen", android.widget.Toast.LENGTH_SHORT).show()
                                    "Page Info" -> android.widget.Toast.makeText(context, "Page: ${activeTab?.title}", android.widget.Toast.LENGTH_SHORT).show()
                                    "Help & feedback" -> viewModel.loadUrlInActiveTab("https://support.google.com/chrome/")
                                    "Toggle Overlay" -> {
                                        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            Settings.canDrawOverlays(context)
                                        } else {
                                            true
                                        }
                                        if (hasPermission) {
                                            val isCurrentlyRunning = ChromeOverlayService.isServiceRunning.value
                                            if (isCurrentlyRunning) {
                                                context.stopService(Intent(context, ChromeOverlayService::class.java))
                                                viewModel.setOverlayEnabled(false)
                                                viewModel.log("Background floating overlay service stopped.", LogType.INFO)
                                            } else {
                                                val startIntent = Intent(context, ChromeOverlayService::class.java)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    context.startForegroundService(startIntent)
                                                } else {
                                                    context.startService(startIntent)
                                                }
                                                viewModel.setOverlayEnabled(true)
                                                viewModel.log("Background floating overlay service started! Close the app or press Home to float tab.", LogType.SUCCESS)
                                                
                                                // Detach WebView so service can display it in background overlay immediately if required
                                                activeTab?.webView?.let {
                                                    ChromeOverlayService.serviceInstance?.attachWebView(it)
                                                }
                                            }
                                        } else {
                                            showOverlayPermissionDialog = true
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Progress bar below url address bar
                    if (activeTab?.isLoading == true) {
                        LinearProgressIndicator(
                            progress = { activeTab.progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = Color(0xFF1A73E8),
                            trackColor = Color.Transparent,
                        )
                    }

                    // Main Viewport: Chrome Homepage or Live WebView
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (isAtHome) {
                            ChromeNewTabPage(
                                bookmarks = viewModel.bookmarks.collectAsState().value,
                                isDarkMode = isDarkMode,
                                isAiMode = isAiMode,
                                isIncognito = isIncognito,
                                onOpenUrl = { viewModel.loadUrlInActiveTab(it) },
                                onOpenWorkflowWorkspace = { showScriptWorkspace = true },
                                onToggleOverlay = {
                                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        Settings.canDrawOverlays(context)
                                    } else {
                                        true
                                    }
                                    if (hasPermission) {
                                        val running = ChromeOverlayService.isServiceRunning.value
                                        if (running) {
                                            context.stopService(Intent(context, ChromeOverlayService::class.java))
                                            viewModel.log("Overlay Stopped.", LogType.INFO)
                                        } else {
                                            val intent = Intent(context, ChromeOverlayService::class.java)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(intent)
                                            } else {
                                                context.startService(intent)
                                            }
                                            viewModel.log("Overlay Started! You can float this tab.", LogType.SUCCESS)
                                        }
                                    } else {
                                        showOverlayPermissionDialog = true
                                    }
                                },
                                onSearchTrigger = { isSearching = true },
                                onToggleAiMode = { viewModel.setAiMode(it) },
                                onToggleIncognito = { viewModel.setIncognito(it) }
                            )
                        } else {
                            key(activeTab.id) {
                                val webViewInstance = activeTab.webView
                                if (webViewInstance != null) {
                                    AndroidView(
                                        factory = { ctx ->
                                            webViewInstance.apply {
                                                (parent as? ViewGroup)?.removeView(this)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("chrome_webview"),
                                        update = { webView ->
                                            webView.onResume()
                                        }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(if (isDarkMode) Color(0xFF1F1F1F) else Color(0xFFFFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "WebView Initialization Error or Process Restriced.\nPlease restart the app or check WebView updates.",
                                            color = if (isDarkMode) Color.LightGray else Color.DarkGray,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showFindInPage) {
                Box(modifier = Modifier.fillMaxWidth().background(if (isDarkMode) Color(0xFF1F1F1F) else Color.White).padding(8.dp).align(Alignment.TopCenter)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.OutlinedTextField(
                            value = findInPageQuery,
                            onValueChange = { 
                                findInPageQuery = it 
                                activeTab?.let { tab -> viewModel.findInPage(tab.id, it) }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Find in page...") },
                            singleLine = true,
                            trailingIcon = {
                                if (findInPageQuery.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        findInPageQuery = "" 
                                        activeTab?.let { tab -> viewModel.clearFindMatches(tab.id) }
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                        IconButton(onClick = { activeTab?.let { viewModel.findNext(it.id, false) } }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                        }
                        IconButton(onClick = { activeTab?.let { viewModel.findNext(it.id, true) } }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                        }
                        IconButton(onClick = { 
                            showFindInPage = false
                            activeTab?.let { viewModel.clearFindMatches(it.id) }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            }

            // Slide-up Sheets & Panel Dialogs
            AnimatedVisibility(
                visible = showScriptWorkspace,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut()
            ) {
                YmlWorkflowWorkspaceSheet(
                    viewModel = viewModel,
                    onClose = { showScriptWorkspace = false }
                )
            }

            AnimatedVisibility(
                visible = showAutomationControlSheet,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut()
            ) {
                QuickAutomationPanel(
                    viewModel = viewModel,
                    onClose = { showAutomationControlSheet = false }
                )
            }

            // Secondary Floating Dialogs
            if (showBookmarksPanel) {
                BookmarksDialog(
                    bookmarks = viewModel.bookmarks.collectAsState().value,
                    onSelect = { url ->
                        viewModel.loadUrlInActiveTab(url)
                        showBookmarksPanel = false
                    },
                    onClose = { showBookmarksPanel = false }
                )
            }

            if (showHistoryPanel) {
                HistoryDialog(
                    history = viewModel.history.collectAsState().value,
                    onSelect = { url ->
                        viewModel.loadUrlInActiveTab(url)
                        showHistoryPanel = false
                    },
                    onDelete = { viewModel.deleteHistoryItem(it) },
                    onClearAll = { viewModel.clearHistory() },
                    onClose = { showHistoryPanel = false }
                )
            }

            if (showOverlayPermissionDialog) {
                OverlayPermissionExplanationDialog(
                    onGrant = {
                        showOverlayPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    onDismiss = { showOverlayPermissionDialog = false }
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    isDarkMode = isDarkMode,
                    isAiMode = isAiMode,
                    isIncognito = isIncognito,
                    googleAccount = googleAccount,
                    isSchedulerActive = isSchedulerActive,
                    schedulerIntervalSeconds = schedulerIntervalSeconds,
                    onToggleScheduler = { if (it) viewModel.startScheduler() else viewModel.stopScheduler() },
                    onSchedulerIntervalChange = { viewModel.setSchedulerInterval(it) },
                    customDownloadPath = customDownloadPath,
                    onCustomDownloadPathChange = { viewModel.setCustomDownloadPath(it) },
                    onToggleAiMode = { viewModel.setAiMode(it) },
                    onToggleIncognito = { viewModel.setIncognito(it) },
                    onTriggerGoogleLogin = { viewModel.loadUrlInActiveTab("https://myaccount.google.com/") },
                    onClose = { showSettingsDialog = false },
                    viewModel = viewModel
                )
            }

        }
    }
}

@Composable
fun ChromeTopBar(
    activeTab: BrowserTab?,
    tabCount: Int,
    viewModel: BrowserViewModel,
    onOpenTabSwitcher: () -> Unit,
    onSearchTrigger: () -> Unit,
    onMenuAction: (String) -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isIncognito by viewModel.isIncognito.collectAsState()
    
    var urlText by remember(activeTab?.url) { mutableStateOf(activeTab?.url ?: "chrome://newtab") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(activeTab?.url) {
        urlText = if (activeTab?.url == "chrome://newtab") "chrome://newtab" else (activeTab?.url ?: "")
    }

    val backgroundColor = if (isDarkMode) Color(0xFF202124) else Color(0xFFF2F2F2)
    val containerColor = if (isDarkMode) Color(0xFF2F3033) else Color(0xFFE8EAED)
    val tintColor = if (isDarkMode) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Navigation Buttons
        IconButton(
            onClick = { viewModel.loadUrlInActiveTab("chrome://newtab") },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // Address Field Container
        Row(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor)
                .clickable { onSearchTrigger() }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Connection Info",
                tint = if (urlText.startsWith("https")) Color(0xFF34A853) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = if (urlText.isEmpty() || urlText == "chrome://newtab") "Search or type URL" else urlText,
                color = if (urlText.isEmpty() || urlText == "chrome://newtab") Color.Gray else tintColor,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (urlText.isNotEmpty() && urlText != "chrome://newtab") {
                IconButton(
                    onClick = { viewModel.loadUrlInActiveTab("chrome://newtab") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Tab switcher counter indicator
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDarkMode) Color(0xFF3C4043) else Color(0xFFE8EAED))
                .clickable { onOpenTabSwitcher() }
                .testTag("tab_switcher_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tabCount.toString(),
                color = tintColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Overflow Menu button
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp).testTag("menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu Options",
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(if (isDarkMode) Color(0xFF2D2E30) else Color.White)
            ) {
                // Top row of icons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { viewModel.goForwardInActiveTab(); showMenu = false }, enabled = activeTab?.canGoForward == true) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = if (activeTab?.canGoForward == true) tintColor else Color.Gray)
                    }
                    val isBookmarked = viewModel.bookmarks.collectAsState().value.any { it.url == activeTab?.url }
                    IconButton(onClick = { viewModel.toggleBookmarkActiveTab(); showMenu = false }) {
                        Icon(if (isBookmarked) Icons.Default.Star else Icons.Outlined.Star, contentDescription = "Bookmark", tint = if (isBookmarked) Color(0xFFF4B400) else tintColor)
                    }
                    IconButton(onClick = { 
                        onMenuAction("Downloads")
                        showMenu = false 
                    }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Download", tint = tintColor)
                    }
                    IconButton(onClick = { 
                        onMenuAction("Page Info")
                        showMenu = false 
                    }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = tintColor)
                    }
                    IconButton(onClick = { activeTab?.webView?.reload(); showMenu = false }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = tintColor)
                    }
                }
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                DropdownMenuItem(
                    text = { Text("New tab", color = tintColor) },
                    onClick = {
                        onMenuAction("New Tab")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("New Incognito tab", color = tintColor) },
                    onClick = { 
                        onMenuAction("New Incognito Tab")
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Outlined.AccountBox, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("Add tab to new group", color = tintColor) },
                    onClick = { 
                        onMenuAction("Add tab to new group")
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = tintColor) }
                )
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                
                DropdownMenuItem(
                    text = { Text("History", color = tintColor) },
                    onClick = {
                        onMenuAction("History")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("Delete browsing data", color = tintColor) },
                    onClick = {
                        onMenuAction("Clear data")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = tintColor) }
                )
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                DropdownMenuItem(
                    text = { Text("Downloads", color = tintColor) },
                    onClick = { 
                        onMenuAction("Downloads")
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("Bookmarks", color = tintColor) },
                    onClick = {
                        onMenuAction("Bookmarks")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("Recent tabs", color = tintColor) },
                    onClick = { 
                        onMenuAction("Recent tabs")
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = tintColor) }
                )
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                DropdownMenuItem(
                    text = { Text("Share...", color = tintColor) },
                    onClick = { 
                        onMenuAction("Share")
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("Find in page", color = tintColor) },
                    onClick = {
                        onMenuAction("Find in page")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("Translate...", color = tintColor) },
                    onClick = { 
                        onMenuAction("Translate")
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("Show Reading mode", color = tintColor) },
                    onClick = { 
                        onMenuAction("Show Reading mode")
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("Add to Home screen", color = tintColor) },
                    onClick = { 
                        onMenuAction("Add to Home screen")
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = {
                        val isDesktop = activeTab?.isDesktopMode ?: false
                        Text(if (isDesktop) "Mobile site" else "Desktop site", color = tintColor)
                    },
                    onClick = {
                        onMenuAction("Desktop site")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = tintColor) },
                    trailingIcon = { 
                        androidx.compose.material3.Checkbox(checked = activeTab?.isDesktopMode ?: false, onCheckedChange = null)
                    }
                )
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                
                DropdownMenuItem(
                    text = { Text("Settings", color = tintColor) },
                    onClick = {
                        onMenuAction("Settings")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("Help & feedback", color = tintColor) },
                    onClick = { 
                        onMenuAction("Help & feedback")
                        showMenu = false 
                    },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = tintColor) }
                )
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                
                // Extra Automation Features
                DropdownMenuItem(
                    text = { Text(if (isDarkMode) "Light Mode" else "Night Mode", color = tintColor) },
                    onClick = {
                        viewModel.setDarkMode(!isDarkMode)
                        showMenu = false
                    },
                    leadingIcon = { Text(text = if (isDarkMode) "☀️" else "🌙", fontSize = 16.sp) }
                )
                val googleAcc = viewModel.googleAccount.collectAsState().value
                DropdownMenuItem(
                    text = { Text(if (googleAcc != null) "Google Account (${googleAcc.displayName})" else "Google Sign In", color = tintColor) },
                    onClick = {
                        onMenuAction("GoogleAccount")
                        showMenu = false
                    },
                    leadingIcon = { Text(text = "👤", fontSize = 16.sp) }
                )
                DropdownMenuItem(
                    text = { Text("Manual Automation Actions", color = tintColor) },
                    onClick = {
                        onMenuAction("Automation Sheet")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = { Text("YML Workflow Console", color = tintColor) },
                    onClick = {
                        onMenuAction("YML Workspace")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = tintColor) }
                )
                DropdownMenuItem(
                    text = {
                        val running = ChromeOverlayService.isServiceRunning.collectAsState().value
                        Text(if (running) "Stop Floating Overlay" else "Enable Background Overlay", color = tintColor)
                    },
                    onClick = {
                        onMenuAction("Toggle Overlay")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = tintColor) }
                )

                val workflowsListForMenu by viewModel.workflowsList.collectAsState()
                if (workflowsListForMenu.isNotEmpty()) {
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    workflowsListForMenu.forEach { file ->
                        val displayName = file.nameWithoutExtension.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                        DropdownMenuItem(
                            text = { Text("Run: $displayName", color = tintColor) },
                            onClick = {
                                viewModel.startWorkflow(file)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF34A853)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChromeHomeHeader(
    tabCount: Int,
    isDarkMode: Boolean,
    isIncognito: Boolean,
    googleAccount: GoogleAccount?,
    viewModel: BrowserViewModel,
    onOpenTabSwitcher: () -> Unit,
    onMenuAction: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onOpenGoogleAccount: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val tintColor = if (isDarkMode) Color.White else Color.Black
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDarkMode) Color(0xFF1F1F1F) else Color(0xFFFFFFFF))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Outline Home Icon
        IconButton(
            onClick = { /* Already on New Tab Page */ },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Google Doodle Center
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "G", color = Color(0xFF4285F4), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(Color(0xFF34A853), Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFF34A853))
                        )
                    )
            )
            Text(text = "o", color = Color(0xFFEA4335), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "g", color = Color(0xFF4285F4), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "l", color = Color(0xFF34A853), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "e", color = Color(0xFFEA4335), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            // User Avatar Jasim / Incognito Spy / Google Login
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isIncognito) Color.Black else if (googleAccount != null) Color(0xFFF57C00) else Color(0xFF1A73E8))
                    .clickable { onOpenGoogleAccount() },
                contentAlignment = Alignment.Center
            ) {
                if (isIncognito) {
                    Text(text = "🕶", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else if (googleAccount != null) {
                    Text(text = googleAccount.displayName.take(1).uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = "👤", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Tab counter indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDarkMode) Color(0xFF3C4043) else Color(0xFFE8EAED))
                    .clickable { onOpenTabSwitcher() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabCount.toString(),
                    color = tintColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // 3-dots
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu Options",
                        tint = tintColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(if (isDarkMode) Color(0xFF2D2E30) else Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("New Tab", color = tintColor) },
                        onClick = {
                            onMenuAction("New Tab")
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isDarkMode) "Light Mode" else "Night Mode", color = tintColor) },
                        onClick = {
                            onToggleDarkMode()
                            showMenu = false
                        },
                        leadingIcon = { Text(text = if (isDarkMode) "☀️" else "🌙", fontSize = 16.sp) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (googleAccount != null) "Google Account (${googleAccount.displayName})" else "Google Sign In", color = tintColor) },
                        onClick = {
                            onOpenGoogleAccount()
                            showMenu = false
                        },
                        leadingIcon = { Text(text = "👤", fontSize = 16.sp) }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = tintColor) },
                        onClick = {
                            onMenuAction("Settings")
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text("Bookmarks", color = tintColor) },
                        onClick = {
                            onMenuAction("Bookmarks")
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text("History", color = tintColor) },
                        onClick = {
                            onMenuAction("History")
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text("YML Workspace", color = tintColor) },
                        onClick = {
                            onMenuAction("YML Workspace")
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text("Manual Automation Actions", color = tintColor) },
                        onClick = {
                            onMenuAction("Automation Sheet")
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = Color.Gray) }
                    )

                    val workflowsListForHomeByMenu by viewModel.workflowsList.collectAsState()
                    if (workflowsListForHomeByMenu.isNotEmpty()) {
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                        workflowsListForHomeByMenu.forEach { file ->
                            val displayName = file.nameWithoutExtension.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                            DropdownMenuItem(
                                text = { Text("Run: $displayName", color = tintColor) },
                                onClick = {
                                    viewModel.startWorkflow(file)
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF34A853)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSearchScreen(
    viewModel: BrowserViewModel,
    onClose: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAiMode by viewModel.isAiMode.collectAsState()
    val isIncognito by viewModel.isIncognito.collectAsState()
    val history by viewModel.history.collectAsState()
    
    var searchInputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    val defaultSuggestions = remember {
        listOf(
            SuggestionItem("hhd", isHistory = true, isAiMode = false),
            SuggestionItem("how to know the day of a date far past by knowing the day of today", isHistory = true, isAiMode = true),
            SuggestionItem("If PAINT is coded as 74128 and EXCEL is coded as 93596, then how would you decode ACCEPT?", isHistory = true, isAiMode = true),
            SuggestionItem("numbwr of days in jan 1 to 28th may of 2006", isHistory = true, isAiMode = true),
            SuggestionItem("translate", isHistory = true, isAiMode = false),
            SuggestionItem("how to access and fetch reddit pages content using yml workflow", isHistory = true, isAiMode = true),
            SuggestionItem("how much did lic lost money due to recent investment fraud", isHistory = true, isAiMode = true),
            SuggestionItem("afilmywap", isHistory = true, isAiMode = false),
            SuggestionItem("how to self-study for having control/regulate on neurotransmitters of my brain", isHistory = true, isAiMode = true)
        )
    }
    
    val filteredSuggestions = remember(searchInputText, history) {
        val userHistory = history.map { SuggestionItem(it.url, isHistory = true, isAiMode = false) }
        val all = userHistory + defaultSuggestions
        if (searchInputText.isEmpty()) {
            all.distinctBy { it.text.lowercase() }
        } else {
            all.filter { it.text.contains(searchInputText, ignoreCase = true) }.distinctBy { it.text.lowercase() }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xFF1F1F1F) else Color(0xFFFFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Exit search",
                    tint = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDarkMode) Color(0xFF2D2E30) else Color(0xFFF1F3F4))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = searchInputText,
                    onValueChange = { searchInputText = it },
                    textStyle = TextStyle(
                        color = if (isDarkMode) Color.White else Color.Black,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    cursorBrush = SolidColor(Color(0xFF1A73E8)),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchInputText.isNotEmpty()) {
                                viewModel.loadUrlInActiveTab(searchInputText)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onClose()
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("active_search_input"),
                    decorationBox = { innerTextField ->
                        if (searchInputText.isEmpty()) {
                            Text(
                                "Search Google or type URL",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
                
                if (searchInputText.isNotEmpty()) {
                    IconButton(
                        onClick = { searchInputText = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear text",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Mic",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        HorizontalDivider(color = if (isDarkMode) Color(0xFF2D2E30) else Color(0xFFF1F3F4))
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(filteredSuggestions) { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.loadUrlInActiveTab(suggestion.text)
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onClose()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "History",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(270f)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = suggestion.text,
                            color = if (isDarkMode) Color.White else Color.Black,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (suggestion.isAiMode || isAiMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "AI Mode",
                                    color = Color(0xFF34A853),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = { searchInputText = suggestion.text },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Refill input",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(135f)
                        )
                    }
                }
                
                HorizontalDivider(color = if (isDarkMode) Color(0xFF2D2E30) else Color(0xFFF1F3F4), thickness = 0.5.dp)
            }
        }
    }
}

data class SuggestionItem(
    val text: String,
    val isHistory: Boolean = true,
    val isAiMode: Boolean = false
)

@Composable
fun SettingsDialog(
    isDarkMode: Boolean,
    isAiMode: Boolean,
    isIncognito: Boolean,
    googleAccount: GoogleAccount?,
    isSchedulerActive: Boolean,
    schedulerIntervalSeconds: Int,
    onToggleScheduler: (Boolean) -> Unit,
    onSchedulerIntervalChange: (Int) -> Unit,
    customDownloadPath: String,
    onCustomDownloadPathChange: (String) -> Unit,
    onToggleAiMode: (Boolean) -> Unit,
    onToggleIncognito: (Boolean) -> Unit,
    onTriggerGoogleLogin: () -> Unit,
    onClose: () -> Unit,
    viewModel: BrowserViewModel
) {
    val textColor = if (isDarkMode) Color.White else Color.Black
    val dialogBg = if (isDarkMode) Color(0xFF2B2D31) else Color(0xFFFFFFFF)
    
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Settings", color = textColor, fontWeight = FontWeight.Bold) },
        containerColor = dialogBg,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Google Account Section (New Profile login feature)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMode) Color(0xFF383A3E) else Color(0xFFF1F3F4))
                        .clickable { onTriggerGoogleLogin() }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (googleAccount != null) Color(0xFFF57C00) else Color(0xFF1A73E8)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (googleAccount != null) {
                            Text(
                                text = googleAccount.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(text = "👤", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = googleAccount?.displayName ?: "Google Sign In",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = googleAccount?.email ?: "Sync is off",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Mode Searches", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Run companion web automation searches", color = Color.Gray, fontSize = 11.sp)
                    }
                    androidx.compose.material3.Switch(
                        checked = isAiMode,
                        onCheckedChange = onToggleAiMode
                    )
                }
                
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Incognito Mode", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Browse without tracking search history", color = Color.Gray, fontSize = 11.sp)
                    }
                    androidx.compose.material3.Switch(
                        checked = isIncognito,
                        onCheckedChange = onToggleIncognito
                    )
                }
                
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Custom Download Path", 
                    color = textColor, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp
                )
                Text(
                    text = "Sub-directory inside standard Downloads folder", 
                    color = Color.Gray, 
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = customDownloadPath,
                    onValueChange = onCustomDownloadPathChange,
                    placeholder = { Text("e.g. MyWorkflows/Reports", color = Color.Gray, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = Color(0xFF1A73E8),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                val previewPath = if (customDownloadPath.trim().isNotEmpty()) {
                    "Download/${customDownloadPath.trim().trim('/')}/[filename]"
                } else {
                    "Download/[filename]"
                }
                Text(
                    text = "Target preview: $previewPath",
                    color = Color.Gray.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Periodic Trigger (Scheduler)", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Periodically auto-run active YML workflow", color = Color.Gray, fontSize = 11.sp)
                    }
                    androidx.compose.material3.Switch(
                        checked = isSchedulerActive,
                        onCheckedChange = onToggleScheduler
                    )
                }

                if (isSchedulerActive) {
                    val sType by viewModel.schedulerType.collectAsState()
                    val alarmTime by viewModel.dailyAlarmTime.collectAsState()
                    val cronExpr by viewModel.cronExpression.collectAsState()

                    Text(
                        text = "Trigger Type", 
                        color = textColor, 
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("periodic" to "Periodic", "alarm" to "Alarm", "cron" to "Cron").forEach { (typeKey, typeLabel) ->
                            val isSelected = sType == typeKey
                            OutlinedButton(
                                onClick = { viewModel.setSchedulerType(typeKey) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isSelected) Color(0xFF1A73E8) else Color.Gray
                                ),
                                border = if (isSelected) BorderStroke(2.dp, Color(0xFF1A73E8)) else BorderStroke(1.dp, Color.Gray),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Text(typeLabel, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }

                    if (sType == "periodic") {
                        Text(
                            text = "Trigger Interval (Seconds): $schedulerIntervalSeconds", 
                            color = textColor, 
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Slider(
                            value = schedulerIntervalSeconds.toFloat(),
                            onValueChange = { onSchedulerIntervalChange(it.toInt()) },
                            valueRange = 10f..600f,
                            steps = 59,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF1A73E8),
                                thumbColor = Color(0xFF1A73E8)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (sType == "alarm") {
                        Text(
                            text = "Alarm Time (24h format - HH:mm):", 
                            color = textColor, 
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BasicTextField(
                            value = alarmTime,
                            onValueChange = { viewModel.setDailyAlarmTime(it) },
                            textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color(0xFF1A73E8)),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDarkMode) Color(0xFF111213) else Color(0xFFE8EAED), RoundedCornerShape(4.dp))
                                .padding(12.dp)
                        )
                        Text(
                            text = "Example: 15:30 (runs daily at 3:30 PM)", 
                            color = Color.Gray, 
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else if (sType == "cron") {
                        Text(
                            text = "Cron Expression (Minutes):", 
                            color = textColor, 
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BasicTextField(
                            value = cronExpr,
                            onValueChange = { viewModel.setCronExpression(it) },
                            textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color(0xFF1A73E8)),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDarkMode) Color(0xFF111213) else Color(0xFFE8EAED), RoundedCornerShape(4.dp))
                                .padding(12.dp)
                        )
                        Text(
                            text = "Example: */5 * * * * (every 5 mins), * * * * * (every min)", 
                            color = Color.Gray, 
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            }
        },
        confirmButton = {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
fun ChromeNewTabPage(
    bookmarks: List<Bookmark>,
    isDarkMode: Boolean,
    isAiMode: Boolean,
    isIncognito: Boolean,
    onOpenUrl: (String) -> Unit,
    onOpenWorkflowWorkspace: () -> Unit,
    onToggleOverlay: () -> Unit,
    onSearchTrigger: () -> Unit,
    onToggleAiMode: (Boolean) -> Unit,
    onToggleIncognito: (Boolean) -> Unit
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1F1F1F) else Color(0xFFFFFFFF)
    val cardColor = if (isDarkMode) Color(0xFF2D2E30) else Color(0xFFF1F3F4)
    val textColor = if (isDarkMode) Color.White else Color(0xFF202124)

    var feedItems by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        if (feedItems.isEmpty()) {
            val items = RssParser.fetchFeed("https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en")
            feedItems = items
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Spacer(modifier = Modifier.height(28.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "G",
                    color = Color(0xFF4285F4),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(Color(0xFF34A853), Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFF34A853))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34A853).copy(alpha = 0.7f))
                    )
                }
                Text(
                    text = "o",
                    color = Color(0xFFEA4335),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "g",
                    color = Color(0xFF4285F4),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "l",
                    color = Color(0xFF34A853),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "e",
                    color = Color(0xFFEA4335),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (isDarkMode) Color(0xFF2F3033) else Color(0xFFF1F3F4))
                    .clickable { onSearchTrigger() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "G",
                    color = Color(0xFF4285F4),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(end = 12.dp)
                )
                
                Text(
                    text = "Search Google or type URL",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Voice Search",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Google Lens",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isAiMode) Color(0xFF34A853).copy(alpha = 0.2f) else cardColor)
                        .clickable { onToggleAiMode(!isAiMode) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "AI Mode",
                            tint = if (isAiMode) Color(0xFF34A853) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Mode",
                            color = if (isAiMode) Color(0xFF34A853) else textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isIncognito) Color(0xFF000000).copy(alpha = 0.4f) else cardColor)
                        .clickable { onToggleIncognito(!isIncognito) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🕶",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "Incognito",
                            color = if (isIncognito) Color(0xFF8AB4F8) else textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ShortcutItem(
                    title = "Colab",
                    initial = "CO",
                    backgroundColor = Color(0xFFF57C00),
                    isDarkMode = isDarkMode,
                    onClick = { onOpenUrl("https://colab.research.google.com") }
                )
                ShortcutItem(
                    title = "Server Conn...",
                    initial = "L",
                    backgroundColor = Color(0xFF7B1FA2),
                    isDarkMode = isDarkMode,
                    onClick = { onOpenUrl("http://localhost:8080") }
                )
                ShortcutItem(
                    title = "Vegamovies",
                    initial = "V",
                    backgroundColor = Color(0xFF0288D1),
                    isDarkMode = isDarkMode,
                    onClick = { onOpenUrl("https://vegamovies.pages.dev") }
                )
                ShortcutItem(
                    title = "Online Shop",
                    initial = "F",
                    backgroundColor = Color(0xFF388E3C),
                    isDarkMode = isDarkMode,
                    onClick = { onOpenUrl("https://www.amazon.com") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ShortcutItem(
                    title = "Google",
                    initial = "G",
                    backgroundColor = Color(0xFFEA4335),
                    isDarkMode = isDarkMode,
                    onClick = { onOpenUrl("https://www.google.com") }
                )
                ShortcutItem(
                    title = "YML Workspace",
                    initial = "YML",
                    backgroundColor = Color(0xFF1A73E8),
                    isDarkMode = isDarkMode,
                    onClick = { onOpenWorkflowWorkspace() }
                )
                ShortcutItem(
                    title = "Float Overlay",
                    initial = "Fl",
                    backgroundColor = Color(0xFF1A73E8),
                    isDarkMode = isDarkMode,
                    onClick = { onToggleOverlay() }
                )
                if (bookmarks.isNotEmpty()) {
                    val firstB = bookmarks.first()
                    ShortcutItem(
                        title = firstB.title,
                        initial = firstB.title.take(1).uppercase(),
                        backgroundColor = Color(0xFF455A64),
                        isDarkMode = isDarkMode,
                        onClick = { onOpenUrl(firstB.url) }
                    )
                } else {
                    ShortcutItem(
                        title = "GitHub",
                        initial = "GH",
                        backgroundColor = Color(0xFF24292E),
                        isDarkMode = isDarkMode,
                        onClick = { onOpenUrl("https://github.com") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            HorizontalDivider(color = if (isDarkMode) Color(0xFF3C4043) else Color(0xFFE0E0E0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discover",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Discover Info",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (feedItems.isEmpty()) {
            item {
                Text(
                    text = "Loading stories...",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(feedItems.take(15)) { item ->
                DiscoverCard(
                    title = item.title,
                    snippet = item.source,
                    source = item.source,
                    time = item.pubDate,
                    imageRes = null,
                    isDarkMode = isDarkMode,
                    onClick = { onOpenUrl(item.link) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ShortcutItem(
    title: String,
    initial: String,
    backgroundColor: Color,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0xFF2D2E30) else Color(0xFFF1F3F4)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = if (isDarkMode) Color.White else Color.Black,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DiscoverCard(
    title: String,
    snippet: String,
    source: String,
    time: String,
    imageRes: Int?,
    isDarkMode: Boolean,
    showPlayOverlay: Boolean = false,
    onClick: () -> Unit = {}
) {
    val cardColor = if (isDarkMode) Color(0xFF2D2E30) else Color(0xFFF1F3F4)
    val textColor = if (isDarkMode) Color.White else Color(0xFF202124)
    val subTextColor = if (isDarkMode) Color.LightGray else Color(0xFF5F6368)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (imageRes != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    
                    if (showPlayOverlay) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = snippet,
                    color = subTextColor,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$source • $time",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { /* Share simulation */ },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Share",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabSwitcherScreen(viewModel: BrowserViewModel, onClose: () -> Unit) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF17181A))
    ) {
        // Tab switcher header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tabs (${tabs.size})",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = { viewModel.createNewTab() }) {
                    Icon(Icons.Default.Add, contentDescription = "New Tab", tint = Color.White)
                }
                IconButton(onClick = { viewModel.closeAllTabs() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Close All Tabs", tint = Color.Red)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Tab Switcher", tint = Color.White)
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2F3033))

        // Grid of open tab cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tabs) { tab ->
                val isActive = tab.id == activeTabId
                Card(
                    modifier = Modifier
                        .height(160.dp)
                        .testTag("tab_card_${tab.id}")
                        .combinedClickable(
                            onClick = { viewModel.selectTab(tab.id) },
                            onLongClick = { viewModel.closeTab(tab.id) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) Color(0xFF2F3033) else Color(0xFF202124)
                    ),
                    border = if (isActive) {
                        androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1A73E8))
                    } else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header block inside tab card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.closeTab(tab.id) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Tab",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Thumbnail placeholder
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF17181A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tab.url == "chrome://newtab") "Chrome Home" else tab.url.substringAfter("://").substringBefore("/"),
                                color = Color.Gray,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(4.dp)
                            )
                        }

                        // Footer URL
                        Text(
                            text = tab.url,
                            color = Color.Gray,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YmlWorkflowWorkspaceSheet(
    viewModel: BrowserViewModel,
    onClose: () -> Unit
) {
    val scriptText by viewModel.scriptText.collectAsState()
    val isRunning by viewModel.isScriptRunning.collectAsState()
    val consoleLogs by viewModel.consoleLogs.collectAsState()
    val logListState = rememberLazyListState()

    // Scroll to bottom on new logs
    LaunchedEffect(consoleLogs.size) {
        if (consoleLogs.isNotEmpty()) {
            logListState.animateScrollToItem(consoleLogs.size - 1)
        }
    }

    var selectedPreset by remember { mutableStateOf("Colab Keep-Alive") }
    var showHelpDialog by remember { mutableStateOf(false) }

    val workflowsList by viewModel.workflowsList.collectAsState()
    val selectedFile by viewModel.selectedWorkflowFile.collectAsState()
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "Automation Syntax & Reference",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Write your workflow using standard YAML blocks. Select text within this panel to copy code blocks.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "YML WORKFLOW SYNTAX",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1A73E8),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = """steps:
  - action: log
    message: "Starting script"
  - action: open
    url: "https://www.google.com"
  - action: wait
    seconds: 4
  - action: type
    selector: "input[name='q']"
    text: "Google AI Studio"
  - action: touch
    x: 150
    y: 150
  - action: swipe
    start_x: 100
    start_y: 500
    end_x: 100
    end_y: 200
  - action: scroll
    direction: "down"
    pixels: 400
  - action: click
    target: "Google Search"
    yolo_fallback: true
  - action: click
    target: "Search Button"
    use_yolo: true   # Click directly using YOLO v26 model
  - action: back     # Go back in history
  - action: home     # Go to new tab
  - action: recent   # Open tab switcher
  - action: download
    url: "https://example.com/report.pdf"
  - action: wait_for_element
    selector: "#results-container"
    seconds: 15
  - action: wait_for_url
    url: "*search?q=*"
    seconds: 10
  - action: wait_until
    time: "15:30:00" # HH:mm:ss daily trigger""",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFC4C4C4),
                            modifier = Modifier
                                .background(Color(0xFF111213))
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                ) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = Color(0xFF202124)
        )
    }

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("New Workflow File", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFF202124),
            text = {
                Column {
                    Text("Enter name for the new workflow file:", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color(0xFF1A73E8)),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111213), RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.trim().isNotEmpty()) {
                            viewModel.createNewWorkflowFile(newFileName)
                            newFileName = ""
                            showNewFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF17181A))
    ) {
        // Workspace Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF202124))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A73E8)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("YML", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "YML Workflow Console",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Workflow Help", tint = Color.White)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Workspace", tint = Color.White)
                }
            }
        }

        // Active Workflow File Management Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .background(Color(0xFF202124), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ACTIVE WORKFLOW FILE",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedFile?.name ?: "None selected",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                var showFilesDropdown by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showFilesDropdown = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Select Workflow File", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showFilesDropdown,
                        onDismissRequest = { showFilesDropdown = false },
                        modifier = Modifier.background(Color(0xFF2D2E30))
                    ) {
                        workflowsList.forEach { file ->
                            val isSelected = file == selectedFile
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = file.name, 
                                        color = if (isSelected) Color(0xFF1A73E8) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                onClick = {
                                    viewModel.selectWorkflowFile(file)
                                    showFilesDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow, 
                                        contentDescription = null, 
                                        tint = if (isSelected) Color(0xFF1A73E8) else Color.Gray
                                    )
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = { showNewFileDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New File", tint = Color(0xFF34A853))
                }

                IconButton(
                    onClick = {
                        selectedFile?.let {
                            viewModel.deleteWorkflowFile(it)
                        }
                    },
                    enabled = workflowsList.size > 1
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete, 
                        contentDescription = "Delete File", 
                        tint = if (workflowsList.size > 1) Color(0xFFEA4335) else Color.DarkGray
                    )
                }

                Button(
                    onClick = {
                        viewModel.saveCurrentWorkflowText(scriptText)
                        viewModel.log("Changes saved to ${selectedFile?.name}", LogType.SUCCESS)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("SAVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Action controls / Preset Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Preset Buttons
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        selectedPreset = "Colab Keep-Alive"
                        viewModel.setScriptText(
                            """# Google Colab Keep-Alive Workflow
steps:
  - action: log
    message: "Starting Colab keep alive script..."
  - action: open
    url: "https://colab.research.google.com"
  - action: wait
    seconds: 6
  - action: log
    message: "Refreshing and checking connection..."
  - action: click
    target: "Connect"
  - action: click
    target: "Run cell"
  - action: click
    target: "Reconnect"
  - action: wait
    seconds: 10
  - action: scroll
    direction: "down"
    pixels: 400
  - action: wait
    seconds: 5
  - action: scroll
    direction: "up"
    pixels: 400
"""
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selectedPreset == "Colab Keep-Alive") Color(0xFF1A73E8) else Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text("Colab Keep-Alive", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = {
                        selectedPreset = "Auto Feed Scroll"
                        viewModel.setScriptText(
                            """# Auto Feed Scroll & Refresh
steps:
  - action: log
    message: "Initiating feed scroll..."
  - action: scroll
    direction: "down"
    pixels: 500
  - action: wait
    seconds: 3
  - action: scroll
    direction: "down"
    pixels: 500
  - action: wait
    seconds: 3
  - action: log
    message: "Completed feed scroll."
"""
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selectedPreset == "Auto Feed Scroll") Color(0xFF1A73E8) else Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text("Auto Scroll", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = {
                        selectedPreset = "YAML Workflow"
                        viewModel.setScriptText(
                            """# Custom YML Instruction Workflow
steps:
  - action: log
    message: "Starting YML Workflow Automation..."
  - action: open
    url: "https://www.google.com"
  - action: wait
    seconds: 4
  - action: log
    message: "Performing search input action..."
  - action: type
    selector: "input[name='q']"
    text: "Google AI Studio"
  - action: wait
    seconds: 2
  - action: log
    message: "Tapping Google logo to dismiss keyboard..."
  - action: touch
    x: 150
    y: 150
  - action: wait
    seconds: 2
  - action: log
    message: "Clicking search button with YOLO fallback..."
  - action: click
    target: "Google Search"
    yolo_fallback: true
  - action: wait
    seconds: 3
  - action: log
    message: "Going back..."
  - action: back
  - action: wait
    seconds: 2
  - action: log
    message: "Returning home..."
  - action: home
  - action: log
    message: "Workflow Completed successfully."
"""
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selectedPreset == "YAML Workflow") Color(0xFF1A73E8) else Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text("YML Workflow", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = {
                        selectedPreset = "YML YOLO Direct"
                        viewModel.setScriptText(
                            """# YOLO Direct search (no DOM search)
steps:
  - action: log
    message: "Starting YOLO Direct Workflow..."
  - action: open
    url: "https://www.google.com"
  - action: wait
    seconds: 4
  - action: log
    message: "Typing search query..."
  - action: type
    selector: "input[name='q']"
    text: "Google AI Studio"
  - action: wait
    seconds: 2
  - action: log
    message: "Tapping Google logo to dismiss keyboard..."
  - action: touch
    x: 150
    y: 150
  - action: wait
    seconds: 2
  - action: log
    message: "Directly clicking search button using YOLOv26 model..."
  - action: click
    target: "Google Search"
    use_yolo: true
  - action: wait
    seconds: 3
  - action: log
    message: "Workflow Completed successfully."
"""
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selectedPreset == "YML YOLO Direct") Color(0xFF1A73E8) else Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text("YML YOLO Direct", fontSize = 10.sp)
                }
            }

            // Run / Stop Execution buttons
            if (isRunning) {
                Button(
                    onClick = { viewModel.stopWorkflow() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("STOP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else {
                Button(
                    onClick = { viewModel.startWorkflow() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RUN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Script Editor Viewport
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = "SCRIPT CODE EDITOR",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            BasicTextField(
                value = scriptText,
                onValueChange = { viewModel.saveCurrentWorkflowText(it) },
                textStyle = TextStyle(
                    color = Color(0xFFF1F1F1),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                cursorBrush = SolidColor(Color(0xFF3776AB)),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF111213))
                    .padding(12.dp)
                    .testTag("yml_workflow_editor"),
                decorationBox = { innerTextField ->
                    innerTextField()
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Console Output Viewport
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORKFLOW TERMINAL OUTPUT",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Clear Logs",
                    color = Color(0xFF1A73E8),
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { viewModel.clearLogs() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                state = logListState,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF090A0B))
                    .padding(8.dp)
            ) {
                if (consoleLogs.isEmpty()) {
                    item {
                        Text(
                            text = "Terminal idle. Click RUN above to execute YML workflow...",
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    items(consoleLogs) { log ->
                        val color = when (log.type) {
                            LogType.INFO -> Color(0xFF81D4FA)
                            LogType.SUCCESS -> Color(0xFFA5D6A7)
                            LogType.ERROR -> Color(0xFFEF9A9A)
                            LogType.LOG -> Color(0xFFE0E0E0)
                        }
                        Text(
                            text = log.text,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun QuickAutomationPanel(
    viewModel: BrowserViewModel,
    onClose: () -> Unit
) {
    var clickTargetText by remember { mutableStateOf("") }
    var fillSelector by remember { mutableStateOf("") }
    var fillText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1F22))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manual Accessibility Automation",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scroll Actions Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2D31)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Simulate Touch Scrolling", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val active = viewModel.getActiveTab()
                            active?.webView?.let {
                                scope.launch {
                                    it.evaluateJavascript("window.scrollBy({ top: 300, left: 0, behavior: 'smooth' });", null)
                                    viewModel.log("Scrolled down page by 300px", LogType.INFO)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF35373C)),
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    ) {
                        Text("Scroll Down", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val active = viewModel.getActiveTab()
                            active?.webView?.let {
                                scope.launch {
                                    it.evaluateJavascript("window.scrollBy({ top: -300, left: 0, behavior: 'smooth' });", null)
                                    viewModel.log("Scrolled up page by 300px", LogType.INFO)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF35373C)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scroll Up", fontSize = 12.sp)
                    }
                }
            }
        }

        // Accessibility Finding and Clicking Elements Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2D31)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Find & Click Element", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Search by Button text, Link name, Image alternative, or CSS Selector.", color = Color.Gray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clickTargetText,
                    onValueChange = { clickTargetText = it },
                    label = { Text("Button/Link Text or CSS Selector") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF1A73E8),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val active = viewModel.getActiveTab()
                        if (active?.webView != null && clickTargetText.isNotEmpty()) {
                            viewModel.log("Manual trigger: Clicking target '$clickTargetText'", LogType.INFO)
                            val engine = PythonAutomationEngine("", active.webView!!, { m, t -> viewModel.log(m, t) }, {}, {})
                            scope.launch {
                                val engineClick = PythonAutomationEngine(
                                    "chrome.click(\"$clickTargetText\")",
                                    active.webView!!,
                                    { m, t -> viewModel.log(m, t) },
                                    {},
                                    {}
                                )
                                engineClick.start()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Automate Search & Click Element")
                }
            }
        }

        // Auto Form Filler Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2D31)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Auto Text Typing", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = fillSelector,
                        onValueChange = { fillSelector = it },
                        label = { Text("Input CSS Selector") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1A73E8),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    )

                    OutlinedTextField(
                        value = fillText,
                        onValueChange = { fillText = it },
                        label = { Text("Text content") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1A73E8),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val active = viewModel.getActiveTab()
                        if (active?.webView != null && fillSelector.isNotEmpty()) {
                            scope.launch {
                                val engineType = PythonAutomationEngine(
                                    "chrome.type(\"$fillSelector\", \"$fillText\")",
                                    active.webView!!,
                                    { m, t -> viewModel.log(m, t) },
                                    {},
                                    {}
                                )
                                engineType.start()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto Type In Element")
                }
            }
        }
    }
}

@Composable
fun BookmarksDialog(
    bookmarks: List<Bookmark>,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Bookmarks", color = Color.White) },
        containerColor = Color(0xFF212121),
        text = {
            if (bookmarks.isEmpty()) {
                Text("No bookmarks saved yet. Click the star icon on any webpage to save bookmarks.", color = Color.LightGray)
            } else {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(bookmarks) { bookmark ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(bookmark.url) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(bookmark.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(bookmark.url, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose) {
                Text("Close")
            }
        }
    )
}

@Composable
fun HistoryDialog(
    history: List<HistoryItem>,
    onSelect: (String) -> Unit,
    onDelete: (HistoryItem) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("History", color = Color.White)
                if (history.isNotEmpty()) {
                    Text(
                        "Clear All",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onClearAll() }
                    )
                }
            }
        },
        containerColor = Color(0xFF212121),
        text = {
            if (history.isEmpty()) {
                Text("No visited history yet.", color = Color.LightGray)
            } else {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(history) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelect(item.url) }
                            ) {
                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.url, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose) {
                Text("Close")
            }
        }
    )
}

@Composable
fun OverlayPermissionExplanationDialog(
    onGrant: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF252629),
        title = {
            Text("System Window Permission Required", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = "To allow your Google Colab and background scripts to continue running stable, the app uses a persistent background service with a floating monitoring window. This allows you to monitor and control your training notebooks in a small bubble overlay while you use other apps. Android requires you to grant 'Draw over other apps' permissions to enable this.",
                color = Color.LightGray,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
            ) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GoogleAccountDialog(
    googleAccount: GoogleAccount?,
    isDarkMode: Boolean,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    val textColor = if (isDarkMode) Color.White else Color.Black
    val dialogBg = if (isDarkMode) Color(0xFF202124) else Color(0xFFFFFFFF)
    val cardBg = if (isDarkMode) Color(0xFF2D2E30) else Color(0xFFF1F3F4)

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = dialogBg,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Google Logo Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "G", color = Color(0xFF4285F4), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = "o", color = Color(0xFFEA4335), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = "o", color = Color(0xFFFBBC05), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = "g", color = Color(0xFF4285F4), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = "l", color = Color(0xFF34A853), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = "e", color = Color(0xFFEA4335), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (googleAccount != null) {
                    // Logged In view
                    Text(
                        text = "Google Account Manager",
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF57C00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = googleAccount.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = googleAccount.displayName,
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = googleAccount.email,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDarkMode) Color(0xFF3C4043) else Color.White)
                                .border(
                                    width = 1.dp,
                                    color = if (isDarkMode) Color.Transparent else Color.LightGray,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { /* Manage google account */ }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Manage your Google Account",
                                color = if (isDarkMode) Color.LightGray else Color(0xFF1A73E8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Service Status Options
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔄", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sync is active", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Sync bookmarks, history, and passwords", color = Color.Gray, fontSize = 10.sp)
                            }
                            Text("ON", color = Color(0xFF34A853), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔑", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Google Password Manager", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Automated login and credential auto-fill", color = Color.Gray, fontSize = 10.sp)
                            }
                            Text("ACTIVE", color = Color(0xFF34A853), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                onLogout()
                                onClose()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sign out", fontSize = 12.sp)
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", fontSize = 12.sp)
                        }
                    }
                } else {
                    // Logged Out / Sign In form view
                    Text(
                        text = "Sign in to Chrome",
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Get your bookmarks, history, passwords, and more on all your devices.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var emailInput by remember { mutableStateOf("") }
                    var nameInput by remember { mutableStateOf("") }
                    
                    androidx.compose.material3.OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Google Account email") },
                        placeholder = { Text("jasimacc003@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = Color(0xFF1A73E8)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    androidx.compose.material3.OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("First / Last Name") },
                        placeholder = { Text("Jasim") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = Color(0xFF1A73E8)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = onClose) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                val finalEmail = emailInput.ifEmpty { "jasimacc003@gmail.com" }
                                val finalName = nameInput.ifEmpty { "Jasim" }
                                onLogin(finalEmail, finalName)
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                        ) {
                            Text("Sign in", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
