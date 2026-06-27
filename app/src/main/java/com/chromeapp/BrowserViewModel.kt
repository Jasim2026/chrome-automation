package com.chromeapp

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.net.Uri
import android.content.Intent
import android.app.DownloadManager
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.provider.MediaStore
import android.webkit.ValueCallback
import kotlinx.coroutines.flow.update
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "chrome://newtab",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    var webView: WebView? = null
)

enum class LogType {
    INFO, SUCCESS, ERROR, LOG
}

data class ConsoleLog(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: LogType,
    val timestamp: Long = System.currentTimeMillis()
)

data class Bookmark(
    val title: String,
    val url: String
)

data class HistoryItem(
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GoogleAccount(
    val email: String,
    val displayName: String,
    val profilePictureUrl: String? = null,
    val idToken: String? = null
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    // --- FILE UPLOAD STATE ---
    var pendingFileChooserCallback: ValueCallback<Array<Uri>>? = null
    
    private val _showFileChooser = MutableStateFlow(false)
    val showFileChooser: StateFlow<Boolean> = _showFileChooser.asStateFlow()

    fun resetFileChooserState() {
        _showFileChooser.value = false
    }

    fun onFilesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) {
            pendingFileChooserCallback?.onReceiveValue(null)
        } else {
            pendingFileChooserCallback?.onReceiveValue(uris.toTypedArray())
        }
        pendingFileChooserCallback = null
        resetFileChooserState()
    }

    // --- CUSTOM DOWNLOAD MANAGER STATE ---
    enum class DownloadState { DOWNLOADING, COMPLETED, FAILED, SCHEDULED, CANCELED }

    data class DownloadItem(
        val id: String = UUID.randomUUID().toString(),
        val url: String,
        val fileName: String,
        val mimeType: String?,
        val userAgent: String?,
        val progress: Float = 0f,
        val state: DownloadState = DownloadState.DOWNLOADING,
        val cronExpression: String? = null,
        val error: String? = null
    )

    data class PendingDownload(
        val url: String,
        val fileName: String,
        val mimetype: String?,
        val userAgent: String?
    )

    private val _pendingDownloadPrompt = MutableStateFlow<PendingDownload?>(null)
    val pendingDownloadPrompt: StateFlow<PendingDownload?> = _pendingDownloadPrompt.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private var cronDownloadCheckerJob: Job? = null

    fun clearPendingDownloadPrompt() {
        _pendingDownloadPrompt.value = null
    }

    companion object {
        const val AI_CHAT_URL = "https://www.google.com/search?q=&client=ms-android-motorola-rvo3&sourceid=chrome-mobile&ie=UTF-8&udm=50&aep=43&cud=0&qsubts=1782549516153&source=chrome.crn.obic"
    }
    private var preloadWebView: WebView? = null
    private val context = application.applicationContext
    private val prefs = application.getSharedPreferences("chrome_prefs", Context.MODE_PRIVATE)

    private val _googleAccount = MutableStateFlow<GoogleAccount?>(
        if (prefs.getBoolean("google_logged_in", false)) {
            GoogleAccount(
                email = prefs.getString("google_email", "jasimacc003@gmail.com") ?: "jasimacc003@gmail.com",
                displayName = prefs.getString("google_name", "Jasim") ?: "Jasim",
                profilePictureUrl = prefs.getString("google_pic", null)
            )
        } else {
            null
        }
    )
    val googleAccount: StateFlow<GoogleAccount?> = _googleAccount.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isAiMode = MutableStateFlow(false)
    val isAiMode: StateFlow<Boolean> = _isAiMode.asStateFlow()

    private val _isIncognito = MutableStateFlow(false)
    val isIncognito: StateFlow<Boolean> = _isIncognito.asStateFlow()

    private val _customDownloadPath = MutableStateFlow(prefs.getString("custom_download_path", "") ?: "")
    val customDownloadPath: StateFlow<String> = _customDownloadPath.asStateFlow()

    fun setCustomDownloadPath(path: String) {
        _customDownloadPath.value = path
        prefs.edit().putString("custom_download_path", path).apply()
    }

    private var schedulerJob: Job? = null
    private val _isSchedulerActive = MutableStateFlow(false)
    val isSchedulerActive: StateFlow<Boolean> = _isSchedulerActive.asStateFlow()
    
    private val _schedulerIntervalSeconds = MutableStateFlow(prefs.getInt("scheduler_interval_seconds", 60))
    val schedulerIntervalSeconds: StateFlow<Int> = _schedulerIntervalSeconds.asStateFlow()

    fun setSchedulerInterval(seconds: Int) {
        _schedulerIntervalSeconds.value = seconds
        prefs.edit().putInt("scheduler_interval_seconds", seconds).apply()
        if (_isSchedulerActive.value) {
            stopScheduler()
            startScheduler()
        }
    }

    fun setSchedulerType(type: String) {
        _schedulerType.value = type
        prefs.edit().putString("scheduler_type", type).apply()
        if (_isSchedulerActive.value) {
            stopScheduler()
            startScheduler()
        }
    }

    fun setDailyAlarmTime(time: String) {
        _dailyAlarmTime.value = time
        prefs.edit().putString("daily_alarm_time", time).apply()
        if (_isSchedulerActive.value) {
            stopScheduler()
            startScheduler()
        }
    }

    fun setCronExpression(cron: String) {
        _cronExpression.value = cron
        prefs.edit().putString("cron_expression", cron).apply()
        if (_isSchedulerActive.value) {
            stopScheduler()
            startScheduler()
        }
    }

    fun startScheduler() {
        if (_isSchedulerActive.value) return
        _isSchedulerActive.value = true
        schedulerJob = viewModelScope.launch(Dispatchers.Default) {
            log("Scheduler activated using trigger type: ${_schedulerType.value}.", LogType.INFO)
            var lastTriggerMinute = -1
            while (_isSchedulerActive.value) {
                val currentType = _schedulerType.value
                if (currentType == "periodic") {
                    withContext(Dispatchers.Main) {
                        startWorkflow()
                    }
                    delay(_schedulerIntervalSeconds.value * 1000L)
                } else if (currentType == "alarm") {
                    val calendar = java.util.Calendar.getInstance()
                    val curHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    val curMinute = calendar.get(java.util.Calendar.MINUTE)
                    
                    val alarmParts = _dailyAlarmTime.value.split(":")
                    if (alarmParts.size >= 2) {
                        val alarmHour = alarmParts[0].toIntOrNull() ?: 12
                        val alarmMinute = alarmParts[1].toIntOrNull() ?: 0
                        
                        val minuteOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR) * 1440 + curHour * 60 + curMinute
                        if (curHour == alarmHour && curMinute == alarmMinute && minuteOfYear != lastTriggerMinute) {
                            lastTriggerMinute = minuteOfYear
                            log("Daily scheduled alarm reached (${_dailyAlarmTime.value}). Starting workflow execution...", LogType.SUCCESS)
                            withContext(Dispatchers.Main) {
                                startWorkflow()
                            }
                        }
                    }
                    delay(5000L)
                } else if (currentType == "cron") {
                    val calendar = java.util.Calendar.getInstance()
                    val curMinute = calendar.get(java.util.Calendar.MINUTE)
                    val minuteOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR) * 1440 + calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + curMinute
                    
                    val cron = _cronExpression.value.trim()
                    var trigger = false
                    var interval = 5
                    
                    if (cron.startsWith("*/")) {
                        val minPart = cron.substringAfter("*/").substringBefore(" ")
                        interval = minPart.toIntOrNull() ?: 5
                        if (curMinute % interval == 0 && minuteOfYear != lastTriggerMinute) {
                            trigger = true
                        }
                    } else if (cron == "* * * * *") {
                        if (minuteOfYear != lastTriggerMinute) {
                            trigger = true
                        }
                    } else {
                        if (curMinute % 5 == 0 && minuteOfYear != lastTriggerMinute) {
                            trigger = true
                        }
                    }
                    
                    if (trigger) {
                        lastTriggerMinute = minuteOfYear
                        log("Cron trigger reached (${_cronExpression.value}). Starting workflow execution...", LogType.SUCCESS)
                        withContext(Dispatchers.Main) {
                            startWorkflow()
                        }
                    }
                    delay(5000L)
                }
            }
        }
    }

    fun stopScheduler() {
        _isSchedulerActive.value = false
        schedulerJob?.cancel()
        schedulerJob = null
        log("Scheduler deactivated.", LogType.INFO)
    }

    fun loginToGoogle(email: String, name: String) {
        val account = GoogleAccount(email, name)
        _googleAccount.value = account
        prefs.edit()
            .putBoolean("google_logged_in", true)
            .putString("google_email", email)
            .putString("google_name", name)
            .apply()
        log("Successfully logged into Google Account as $name ($email).", LogType.SUCCESS)
    }

    fun logoutFromGoogle() {
        _googleAccount.value = null
        prefs.edit()
            .putBoolean("google_logged_in", false)
            .remove("google_email")
            .remove("google_name")
            .apply()
        log("Logged out from Google Account.", LogType.INFO)
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        
        // Dynamically inject the new theme into all currently open tabs
        viewModelScope.launch(Dispatchers.Main) {
            _tabs.value.forEach { tab ->
                tab.webView?.let { webView ->
                    applyDarkModeToWebView(webView, enabled)
                }
            }
        }
    }

    fun setAiMode(enabled: Boolean) {
        _isAiMode.value = enabled
    }

    fun setIncognito(enabled: Boolean) {
        _isIncognito.value = enabled
        if (enabled) {
            log("Incognito mode turned on. Tabs and searches will not be saved in History.", LogType.INFO)
        } else {
            log("Incognito mode turned off.", LogType.INFO)
        }
    }

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _showTabSwitcher = MutableStateFlow(false)
    val showTabSwitcher: StateFlow<Boolean> = _showTabSwitcher.asStateFlow()

    private val _isScriptRunning = MutableStateFlow(false)
    val isScriptRunning: StateFlow<Boolean> = _isScriptRunning.asStateFlow()

    private val _scriptText = MutableStateFlow("")
    val scriptText: StateFlow<String> = _scriptText.asStateFlow()

    private val _workflowsList = MutableStateFlow<List<java.io.File>>(emptyList())
    val workflowsList: StateFlow<List<java.io.File>> = _workflowsList.asStateFlow()

    private val _selectedWorkflowFile = MutableStateFlow<java.io.File?>(null)
    val selectedWorkflowFile: StateFlow<java.io.File?> = _selectedWorkflowFile.asStateFlow()

    private val _schedulerType = MutableStateFlow(prefs.getString("scheduler_type", "periodic") ?: "periodic")
    val schedulerType: StateFlow<String> = _schedulerType.asStateFlow()

    private val _dailyAlarmTime = MutableStateFlow(prefs.getString("daily_alarm_time", "12:00") ?: "12:00")
    val dailyAlarmTime: StateFlow<String> = _dailyAlarmTime.asStateFlow()

    private val _cronExpression = MutableStateFlow(prefs.getString("cron_expression", "*/5 * * * *") ?: "*/5 * * * *")
    val cronExpression: StateFlow<String> = _cronExpression.asStateFlow()

    private val workflowsDir = java.io.File(context.filesDir, "workflows")

    private val _consoleLogs = MutableStateFlow<List<ConsoleLog>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLog>> = _consoleLogs.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(
        listOf(
            Bookmark("Google", "https://www.google.com"),
            Bookmark("Google Colab", "https://colab.research.google.com"),
            Bookmark("GitHub", "https://github.com"),
            Bookmark("YouTube", "https://www.youtube.com"),
            Bookmark("YAML Guide", "https://yaml.org")
        )
    )
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _isOverlayEnabled = MutableStateFlow(false)
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    private var scriptEngine: PythonAutomationEngine? = null

    init {
        // Create initial tab
        createNewTab()

        // Initialize workflows directory safely
        try {
            if (!workflowsDir.exists()) {
                workflowsDir.mkdirs()
            }
            
            // Write default presets if empty
            val files = workflowsDir.listFiles()
            if (files == null || files.isEmpty()) {
                createDefaultWorkflows()
            }
            
            refreshWorkflowsList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        startCronDownloadChecker()
        preloadAiUrl()
    }

    fun getActiveTab(): BrowserTab? {
        val activeId = _activeTabId.value
        return _tabs.value.find { it.id == activeId }
    }

    fun createNewTab(url: String = "chrome://newtab") {
        viewModelScope.launch {
            val newTab = createTabInstance(url)
            val updatedList = _tabs.value + newTab
            _tabs.value = updatedList
            _activeTabId.value = newTab.id
            _showTabSwitcher.value = false
        }
    }
    
    private fun applyDarkModeToWebView(webView: WebView, isDark: Boolean) {
        // Set the base background color to prevent white flashes while loading
        webView.setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#121212") else android.graphics.Color.WHITE)

        // Inject CSS Smart Invert filter to guarantee dark mode on all Android versions/themes
        val js = if (isDark) {
            """
            (function() {
                if (!document.getElementById('chrome-dark-mode-style')) {
                    const style = document.createElement('style');
                    style.id = 'chrome-dark-mode-style';
                    style.innerHTML = 'html { background-color: #121212 !important; filter: invert(1) hue-rotate(180deg) !important; } img, video, iframe, canvas, picture, svg { filter: invert(1) hue-rotate(180deg) !important; }';
                    document.head.appendChild(style);
                }
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                const style = document.getElementById('chrome-dark-mode-style');
                if (style) {
                    style.remove();
                }
            })();
            """.trimIndent()
        }
        webView.evaluateJavascript(js, null)
    }

    private suspend fun createTabInstance(initialUrl: String): BrowserTab = withContext(Dispatchers.Main) {
        val tabId = UUID.randomUUID().toString()
        var webView: WebView? = null
        try {
            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    supportZoom()
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    allowFileAccess = true
                    allowContentAccess = true
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = true
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                }
                
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                // Set high performance cookie management for session syncing
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                        return false
                    }

                    @Suppress("DEPRECATION")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        updateTabState(tabId) { it.copy(isLoading = true, url = url ?: "chrome://newtab", progress = 10) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        val pageTitle = view?.title ?: "New Tab"
                        val currentUrl = url ?: "chrome://newtab"
                        
                        // ADD THIS LINE: Apply dark mode dynamically the moment the page renders
                        view?.let { applyDarkModeToWebView(it, _isDarkMode.value) }

                        updateTabState(tabId) {
                            it.copy(
                                isLoading = false,
                                title = if (currentUrl.startsWith("chrome://")) "New Tab" else pageTitle,
                                url = currentUrl,
                                canGoBack = view?.canGoBack() ?: false,
                                canGoForward = view?.canGoForward() ?: false
                            )
                        }
                        if (!currentUrl.startsWith("chrome://") && !_isIncognito.value) {
                            addToHistory(pageTitle, currentUrl)
                        }
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        updateTabState(tabId) {
                            it.copy(
                                url = url ?: "chrome://newtab",
                                canGoBack = view?.canGoBack() ?: false,
                                canGoForward = view?.canGoForward() ?: false
                            )
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        updateTabState(tabId) { it.copy(progress = newProgress) }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        val currentUrl = view?.url ?: "chrome://newtab"
                        updateTabState(tabId) {
                            it.copy(title = if (currentUrl.startsWith("chrome://")) "New Tab" else (title ?: "New Tab"))
                        }
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.grant(request.resources)
                    }

                    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                        viewModelScope.launch {
                            // 1. Create a fully configured tab with your existing logic
                            val newTab = createTabInstance("about:blank")
                            
                            // 2. Add it to the UI tab array so the user can see it
                            _tabs.value = _tabs.value + newTab
                            _activeTabId.value = newTab.id
                            
                            // 3. Connect the transport to load the requested URL into this new tab
                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            transport?.webView = newTab.webView
                            resultMsg?.sendToTarget()
                        }
                        return true
                    }

                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        val message = consoleMessage?.message()
                        if (message != null) {
                            log("Browser Console: $message", LogType.INFO)
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        pendingFileChooserCallback?.onReceiveValue(null)
                        pendingFileChooserCallback = filePathCallback
                        _showFileChooser.value = true
                        return true
                    }
                }

                setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                    // Instead of downloading instantly, we show a prompt
                    val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
                    _pendingDownloadPrompt.value = PendingDownload(url, fileName, mimetype, userAgent)
                }
            }
            
            applyDarkModeToWebView(webView, _isDarkMode.value)

            if (initialUrl != "chrome://newtab") {
                webView.loadUrl(initialUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            log("Warning: Failed to initialize WebView tab: ${e.localizedMessage}", LogType.ERROR)
        }

        BrowserTab(id = tabId, title = "New Tab", url = initialUrl, webView = webView)
    }

    fun clearBrowsingData() {
        viewModelScope.launch(Dispatchers.Main) {
            val webView = _tabs.value.firstOrNull()?.webView
            if (webView != null) {
                webView.clearCache(true)
                webView.clearHistory()
                webView.clearFormData()
            }
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            _history.value = emptyList()
            prefs.edit().remove("history").apply()
            log("Browsing data cleared.", LogType.SUCCESS)
        }
    }

    fun toggleDesktopMode(tabId: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val tab = _tabs.value.find { it.id == tabId }
            tab?.webView?.let { webView ->
                val newDesktopMode = !tab.isDesktopMode
                val userAgent = if (newDesktopMode) {
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                } else {
                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                }
                webView.settings.userAgentString = userAgent
                webView.reload()
                updateTabState(tabId) { it.copy(isDesktopMode = newDesktopMode) }
            }
        }
    }

    fun findInPage(tabId: String, query: String) {
        val tab = _tabs.value.find { it.id == tabId }
        tab?.webView?.findAllAsync(query)
    }

    fun findNext(tabId: String, forward: Boolean) {
        val tab = _tabs.value.find { it.id == tabId }
        tab?.webView?.findNext(forward)
    }

    fun clearFindMatches(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId }
        tab?.webView?.clearMatches()
    }

    private fun updateTabState(id: String, transformer: (BrowserTab) -> BrowserTab) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == id) transformer(tab) else tab
        }
    }

    fun selectTab(id: String) {
        _activeTabId.value = id
        _showTabSwitcher.value = false
    }

    fun closeTab(id: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val currentList = _tabs.value
            val tabToClose = currentList.find { it.id == id }
            
            // Clean up WebView resources safely
            tabToClose?.webView?.destroy()

            val updatedList = currentList.filter { it.id != id }
            _tabs.value = updatedList

            if (updatedList.isEmpty()) {
                createNewTab()
            } else if (_activeTabId.value == id) {
                _activeTabId.value = updatedList.last().id
            }
        }
    }

    fun closeAllTabs() {
        viewModelScope.launch(Dispatchers.Main) {
            _tabs.value.forEach { it.webView?.destroy() }
            _tabs.value = emptyList()
            createNewTab()
        }
    }

    fun setShowTabSwitcher(show: Boolean) {
        _showTabSwitcher.value = show
    }

    fun loadUrlInActiveTab(url: String) {
        val activeTab = getActiveTab() ?: return
        val formattedUrl = formatUrl(url)
        updateTabState(activeTab.id) { it.copy(url = formattedUrl) }
        viewModelScope.launch(Dispatchers.Main) {
            if (formattedUrl.startsWith("chrome://")) {
                activeTab.webView?.loadUrl("about:blank")
            } else {
                activeTab.webView?.loadUrl(formattedUrl)
            }
        }
    }

    fun goBackInActiveTab() {
        val activeTab = getActiveTab() ?: return
        viewModelScope.launch(Dispatchers.Main) {
            if (activeTab.webView?.canGoBack() == true) {
                activeTab.webView?.goBack()
            }
        }
    }

    fun goForwardInActiveTab() {
        val activeTab = getActiveTab() ?: return
        viewModelScope.launch(Dispatchers.Main) {
            if (activeTab.webView?.canGoForward() == true) {
                activeTab.webView?.goForward()
            }
        }
    }

    fun reloadActiveTab() {
        val activeTab = getActiveTab() ?: return
        viewModelScope.launch(Dispatchers.Main) {
            activeTab.webView?.reload()
        }
    }

    fun toggleBookmarkActiveTab() {
        val activeTab = getActiveTab() ?: return
        if (activeTab.url.startsWith("chrome://")) return

        val existing = _bookmarks.value.find { it.url == activeTab.url }
        if (existing != null) {
            _bookmarks.value = _bookmarks.value.filter { it.url != activeTab.url }
            log("Removed Bookmark: ${activeTab.title}", LogType.INFO)
        } else {
            _bookmarks.value = _bookmarks.value + Bookmark(activeTab.title, activeTab.url)
            log("Added Bookmark: ${activeTab.title}", LogType.SUCCESS)
        }
    }

    fun setScriptText(text: String) {
        _scriptText.value = text
    }

    fun startWorkflow() {
        if (_isScriptRunning.value) return
        val currentScript = _scriptText.value
        val activeTab = getActiveTab()
        if (activeTab == null || activeTab.webView == null) {
            log("Error: No active browser tab available to run script.", LogType.ERROR)
            return
        }

        _isScriptRunning.value = true
        _consoleLogs.value = emptyList() // clear previous console
        log("Compiling YML Workflow...", LogType.INFO)

        scriptEngine = PythonAutomationEngine(
            script = currentScript,
            webView = activeTab.webView!!,
            onLog = { message, type -> log(message, type) },
            onFinished = {
                _isScriptRunning.value = false
                log("Workflow execution finished.", LogType.SUCCESS)
            },
            onOpenUrl = { url ->
                loadUrlInActiveTab(url)
            },
            onSystemButton = { button ->
                viewModelScope.launch(Dispatchers.Main) {
                    when (button.lowercase(java.util.Locale.getDefault())) {
                        "back" -> goBackInActiveTab()
                        "home" -> loadUrlInActiveTab("chrome://newtab")
                        "recent" -> setShowTabSwitcher(true)
                    }
                }
            },
            onDownload = { url ->
                downloadFile(url)
            }
        )

        scriptEngine?.start()
    }

    fun stopWorkflow() {
        if (!_isScriptRunning.value) return
        scriptEngine?.stop()
        _isScriptRunning.value = false
        log("Workflow execution stopped by user.", LogType.ERROR)
    }

    fun clearLogs() {
        _consoleLogs.value = emptyList()
    }

    fun startDownload(url: String, fileName: String, mimetype: String?, userAgent: String?, cron: String? = null) {
        val initialState = if (cron.isNullOrEmpty()) DownloadState.DOWNLOADING else DownloadState.SCHEDULED
        val newItem = DownloadItem(
            url = url,
            fileName = fileName,
            mimeType = mimetype,
            userAgent = userAgent,
            state = initialState,
            cronExpression = cron
        )
        
        _downloads.update { listOf(newItem) + it }
        
        if (initialState == DownloadState.DOWNLOADING) {
            executeDownload(newItem.id)
        } else {
            log("Scheduled download for $fileName with cron: $cron", LogType.INFO)
        }
    }
    // ADD THIS HELPER TO FIX THE COMPILE ERROR:
    fun downloadFile(
        url: String,
        contentDisposition: String? = null,
        mimetype: String? = null,
        userAgent: String? = null
    ) {
        val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
        startDownload(url, fileName, mimetype, userAgent)
    }

    private fun executeDownload(downloadId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = _downloads.value.find { it.id == downloadId } ?: return@launch
            
            try {
                // Use MediaStore for Android 10+ compatible storage without permissions
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, item.fileName)
                    put(MediaStore.Downloads.MIME_TYPE, item.mimeType ?: "application/octet-stream")
                    val customPath = _customDownloadPath.value.trim().trim('/')
                    if (customPath.isNotEmpty()) {
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$customPath")
                    }
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Failed to create file in MediaStore")

                val connection = URL(item.url).openConnection() as HttpURLConnection
                item.userAgent?.let { connection.setRequestProperty("User-Agent", it) }
                val cookie = CookieManager.getInstance().getCookie(item.url)
                if (cookie != null) connection.setRequestProperty("Cookie", cookie)
                
                connection.connect()
                val fileLength = connection.contentLength
                
                resolver.openOutputStream(uri)?.use { output ->
                    connection.inputStream.use { input ->
                        val data = ByteArray(8192)
                        var total: Long = 0
                        var count: Int
                        
                        while (input.read(data).also { count = it } != -1) {
                            // Stop if canceled
                            val currentState = _downloads.value.find { it.id == downloadId }?.state
                            if (currentState == DownloadState.CANCELED) {
                                resolver.delete(uri, null, null)
                                return@launch
                            }
                            
                            total += count
                            output.write(data, 0, count)
                            
                            if (fileLength > 0 && total % (8192 * 10) == 0L) { // update progress periodically
                                val prog = (total * 100f / fileLength) / 100f
                                updateDownloadState(downloadId) { it.copy(progress = prog) }
                            }
                        }
                    }
                }
                
                updateDownloadState(downloadId) { it.copy(state = DownloadState.COMPLETED, progress = 1f) }
                log("Download completed: ${item.fileName}", LogType.SUCCESS)
                
            } catch (e: Exception) {
                updateDownloadState(downloadId) { it.copy(state = DownloadState.FAILED, error = e.localizedMessage) }
                log("Download failed: ${item.fileName} - ${e.message}", LogType.ERROR)
            }
        }
    }

    fun cancelDownload(id: String) {
        updateDownloadState(id) { it.copy(state = DownloadState.CANCELED) }
    }

    fun removeDownload(id: String) {
        _downloads.update { list -> list.filter { it.id != id } }
    }

    private fun updateDownloadState(id: String, transformer: (DownloadItem) -> DownloadItem) {
        _downloads.update { list ->
            list.map { if (it.id == id) transformer(it) else it }
        }
    }

    private fun startCronDownloadChecker() {
        cronDownloadCheckerJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTriggerMinute = -1
            while (true) {
                val calendar = java.util.Calendar.getInstance()
                val curMinute = calendar.get(java.util.Calendar.MINUTE)
                val minuteOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR) * 1440 + calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + curMinute

                if (minuteOfYear != lastTriggerMinute) {
                    val scheduledItems = _downloads.value.filter { it.state == DownloadState.SCHEDULED && !it.cronExpression.isNullOrEmpty() }
                    
                    for (item in scheduledItems) {
                        // Simple cron parser (handles "*/5 * * * *" and "* * * * *")
                        val cron = item.cronExpression!!.trim()
                        var trigger = false
                        
                        if (cron.startsWith("*/")) {
                            val interval = cron.substringAfter("*/").substringBefore(" ").toIntOrNull() ?: 5
                            if (curMinute % interval == 0) trigger = true
                        } else if (cron == "* * * * *") {
                            trigger = true
                        } else {
                            // Default to alarm format HH:mm if it's not a standard cron
                            val timeParts = cron.split(":")
                            if (timeParts.size >= 2) {
                                val hr = timeParts[0].toIntOrNull() ?: -1
                                val min = timeParts[1].toIntOrNull() ?: -1
                                if (calendar.get(java.util.Calendar.HOUR_OF_DAY) == hr && curMinute == min) {
                                    trigger = true
                                }
                            }
                        }

                        if (trigger) {
                            updateDownloadState(item.id) { it.copy(state = DownloadState.DOWNLOADING) }
                            log("Cron triggered download: ${item.fileName}", LogType.SUCCESS)
                            executeDownload(item.id)
                        }
                    }
                    lastTriggerMinute = minuteOfYear
                }
                delay(5000L) // Check every 5 seconds
            }
        }
    }

    fun log(text: String, type: LogType = LogType.LOG) {
        viewModelScope.launch {
            val newLog = ConsoleLog(text = text, type = type)
            _consoleLogs.value = _consoleLogs.value + newLog
            
            // Send log status update to the active overlay floating monitor service
            ChromeOverlayService.serviceInstance?.updateStatus(
                logText = text,
                isError = type == LogType.ERROR
            )
        }
    }

    private fun addToHistory(title: String, url: String) {
        val newItem = HistoryItem(title = title, url = url)
        // Keep unique entries, move recent to top
        val filtered = _history.value.filter { it.url != url }
        _history.value = listOf(newItem) + filtered
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    fun deleteHistoryItem(item: HistoryItem) {
        _history.value = _history.value.filter { it != item }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        _isOverlayEnabled.value = enabled
    }

    private fun formatUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("chrome://")) return trimmed
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        
        // Check if it looks like a web address (contains dots, no spaces)
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }
        
        // Otherwise, treat as google search
        return "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
    }

    private fun createDefaultWorkflows() {
        try {
            java.io.File(workflowsDir, "colab_keep_alive.yml").writeText(
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

            java.io.File(workflowsDir, "auto_scroll.yml").writeText(
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

            java.io.File(workflowsDir, "google_search.yml").writeText(
                """# Google Search YML Workflow
steps:
  - action: log
    message: "Starting search workflow..."
  - action: open
    url: "https://www.google.com"
  - action: wait
    seconds: 4
  - action: type
    selector: "input[name='q']"
    text: "Google AI Studio"
  - action: wait
    seconds: 2
  - action: click
    target: "Google Search"
    yolo_fallback: true
  - action: wait_for_element
    selector: "#search"
    seconds: 15
  - action: log
    message: "Workflow completed successfully."
"""
            )
        } catch (e: Exception) {
            log("Error creating default workflows: ${e.message}", LogType.ERROR)
        }
    }

    fun refreshWorkflowsList() {
        val files = workflowsDir.listFiles()?.toList() ?: emptyList()
        _workflowsList.value = files.sortedBy { it.name }
        
        val currentSelected = _selectedWorkflowFile.value
        if (currentSelected == null || !currentSelected.exists()) {
            if (files.isNotEmpty()) {
                selectWorkflowFile(files.first())
            } else {
                _selectedWorkflowFile.value = null
                _scriptText.value = ""
            }
        }
    }

    fun selectWorkflowFile(file: java.io.File) {
        _selectedWorkflowFile.value = file
        if (file.exists()) {
            _scriptText.value = file.readText()
        }
    }

    fun createNewWorkflowFile(name: String) {
        var fileName = name.trim().replace(" ", "_")
        if (!fileName.endsWith(".yml", ignoreCase = true) && !fileName.endsWith(".yaml", ignoreCase = true)) {
            fileName += ".yml"
        }
        val file = java.io.File(workflowsDir, fileName)
        if (!file.exists()) {
            file.writeText(
                """# $name Custom Workflow
steps:
  - action: log
    message: "New workflow started!"
  - action: wait
    seconds: 2
"""
            )
        }
        refreshWorkflowsList()
        selectWorkflowFile(file)
    }

    fun deleteWorkflowFile(file: java.io.File) {
        if (file.exists()) {
            file.delete()
        }
        _selectedWorkflowFile.value = null
        refreshWorkflowsList()
    }

    fun saveCurrentWorkflowText(text: String) {
        _scriptText.value = text
        val file = _selectedWorkflowFile.value
        if (file != null) {
            try {
                file.writeText(text)
            } catch (e: Exception) {
                log("Error saving file: ${e.message}", LogType.ERROR)
            }
        }
    }

    fun startWorkflow(file: java.io.File) {
        if (_isScriptRunning.value) return
        if (!file.exists()) {
            log("Error: Workflow file does not exist: ${file.name}", LogType.ERROR)
            return
        }
        selectWorkflowFile(file)
        startWorkflow()
    }

    override fun onCleared() {
        super.onCleared()
        preloadWebView?.destroy()
        preloadWebView = null
        // Destroy all other WebViews
        _tabs.value.forEach { it.webView?.destroy() }
    }
    private fun preloadAiUrl() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                preloadWebView = WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT // Instructs Chromium to respect & populate caches
                    }
                    loadUrl(AI_CHAT_URL)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
