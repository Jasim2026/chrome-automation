package com.chromeapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class ChromeOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: FrameLayout? = null
    private var collapsedView: FrameLayout? = null
    private var expandedView: LinearLayout? = null
    private var webViewContainer: FrameLayout? = null
    private var statusText: TextView? = null
    private var titleText: TextView? = null

    private var activeWebView: WebView? = null

    private var paramsCollapsed: WindowManager.LayoutParams? = null
    private var paramsExpanded: WindowManager.LayoutParams? = null

    companion object {
        private const val CHANNEL_ID = "chrome_automation_background_channel"
        private const val NOTIFICATION_ID = 2655

        var serviceInstance: ChromeOverlayService? = null
            private set

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        private val _isOverlayExpanded = MutableStateFlow(false)
        val isOverlayExpanded = _isOverlayExpanded.asStateFlow()

        // Reference to BrowserViewModel (assigned by MainActivity)
        var viewModel: BrowserViewModel? = null
    }

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        _isServiceRunning.value = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                buildNotification(), 
                1024 // ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && android.provider.Settings.canDrawOverlays(this)) {
            setupOverlayWindow()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chrome Automation Active Task",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Chrome tabs active and running for background scripting and automations."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ChromeOverlayService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Chrome Automation Browser Active")
            .setContentText("Background task and floating monitor overlay is active.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingOpenIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Background Service", pendingStopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun setupOverlayWindow() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Collapsed parameters (circular bubble)
        paramsCollapsed = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // 2. Expanded parameters (dashboard monitor)
        paramsExpanded = WindowManager.LayoutParams(
            dpToPx(330),
            dpToPx(440),
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 150
        }

        // Create main overlay layout container
        overlayView = FrameLayout(this)

        createCollapsedView()
        createExpandedView()

        // Initial state: show collapsed
        overlayView?.addView(collapsedView)
        windowManager.addView(overlayView, paramsCollapsed)
        _isOverlayExpanded.value = false
    }

    private fun createCollapsedView() {
        collapsedView = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dpToPx(56), dpToPx(56))
            
            // Circular background with elegant gradient
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#1A73E8"), Color.parseColor("#1557B0"))
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dpToPx(2), Color.WHITE)
            }

            // Central robot/compass icon
            val icon = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_compass)
                setColorFilter(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(dpToPx(28), dpToPx(28), Gravity.CENTER)
            }
            addView(icon)

            // Setup drag-and-drop listener + click trigger
            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var lastActionTime = 0L

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = paramsCollapsed!!.x
                            initialY = paramsCollapsed!!.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            lastActionTime = System.currentTimeMillis()
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            paramsCollapsed!!.x = initialX + (event.rawX - initialTouchX).toInt()
                            paramsCollapsed!!.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager.updateViewLayout(overlayView, paramsCollapsed)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            val clickDuration = System.currentTimeMillis() - lastActionTime
                            val deltaX = Math.abs(event.rawX - initialTouchX)
                            val deltaY = Math.abs(event.rawY - initialTouchY)
                            if (clickDuration < 200 && deltaX < 10 && deltaY < 10) {
                                expandOverlay()
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }
    }

    private fun createExpandedView() {
        expandedView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#202124")) // Chrome dark theme background
                cornerRadius = dpToPx(16).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#3C4043"))
            }
            elevation = dpToPx(8).toFloat()

            // Header Row
            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#2D2E30"))
                    cornerRadii = floatArrayOf(
                        dpToPx(16).toFloat(), dpToPx(16).toFloat(),
                        dpToPx(16).toFloat(), dpToPx(16).toFloat(),
                        0f, 0f, 0f, 0f
                    )
                }

                // Chrome mini logo
                val chromeIcon = ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_menu_compass)
                    setColorFilter(Color.parseColor("#4285F4"))
                    layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18)).apply {
                        rightMargin = dpToPx(8)
                    }
                }
                addView(chromeIcon)

                // Title
                titleText = TextView(context).apply {
                    text = "Chrome Automation Monitor"
                    textColor = Color.WHITE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setSingleLine()
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
                }
                addView(titleText)

                // Fullscreen button
                val fullScreenBtn = ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_menu_view)
                    setColorFilter(Color.WHITE)
                    setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                    setOnClickListener {
                        openMainActivityAndCollapse()
                    }
                    layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)).apply {
                        rightMargin = dpToPx(4)
                    }
                }
                addView(fullScreenBtn)

                // Close/Collapse button
                val collapseBtn = ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    setColorFilter(Color.WHITE)
                    setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                    setOnClickListener {
                        collapseOverlay()
                    }
                    layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28))
                }
                addView(collapseBtn)
            }
            addView(header)

            // Live WebView Host Area
            webViewContainer = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dpToPx(280)
                )
                // Default placeholder layout when WebView isn't attached
                val placeholderText = TextView(context).apply {
                    text = "Background Session Monitor Active\nGoogle Colab & Scripts keeping alive..."
                    textColor = Color.parseColor("#9AA0A6")
                    gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                addView(placeholderText)
            }
            addView(webViewContainer)

            // Real-time workflow status console line
            statusText = TextView(context).apply {
                text = "Console: Idle"
                textColor = Color.parseColor("#34A853")
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#17181A"))
                }
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setSingleLine()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            addView(statusText)

            // Bottom control navigation row
            val controls = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#2D2E30"))
                    cornerRadii = floatArrayOf(
                        0f, 0f, 0f, 0f,
                        0f, 0f,
                        dpToPx(16).toFloat(), dpToPx(16).toFloat(),
                        dpToPx(16).toFloat(), dpToPx(16).toFloat()
                    )
                }

                // Quick buttons
                val btnBack = createControlBtn(android.R.drawable.ic_media_previous, "Back") {
                    viewModel?.goBackInActiveTab()
                }
                val btnReload = createControlBtn(android.R.drawable.ic_popup_sync, "Reload") {
                    viewModel?.reloadActiveTab()
                }
                val btnForward = createControlBtn(android.R.drawable.ic_media_next, "Forward") {
                    viewModel?.goForwardInActiveTab()
                }
                val btnScript = createControlBtn(android.R.drawable.ic_media_play, "Run YML") {
                    val running = viewModel?.isScriptRunning?.value ?: false
                    if (running) {
                        viewModel?.stopWorkflow()
                    } else {
                        viewModel?.startWorkflow()
                    }
                }

                addView(btnBack)
                addView(btnReload)
                addView(btnForward)
                addView(btnScript)
            }
            addView(controls)

            // Simple header click drag
            header.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = paramsExpanded!!.x
                            initialY = paramsExpanded!!.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            paramsExpanded!!.x = initialX + (event.rawX - initialTouchX).toInt()
                            paramsExpanded!!.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager.updateViewLayout(overlayView, paramsExpanded)
                            return true
                        }
                    }
                    return false
                }
            })
        }
    }

    private fun createControlBtn(iconRes: Int, contentDesc: String, onClick: () -> Unit): ImageView {
        return ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(Color.WHITE)
            contentDescription = contentDesc
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3C4043"))
                shape = GradientDrawable.OVAL
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                leftMargin = dpToPx(12)
                rightMargin = dpToPx(12)
            }
        }
    }

    private fun expandOverlay() {
        overlayView?.removeAllViews()
        overlayView?.addView(expandedView)
        windowManager.updateViewLayout(overlayView, paramsExpanded)
        _isOverlayExpanded.value = true

        // Transfer live WebView inside overlay container
        val activeTab = viewModel?.getActiveTab()
        if (activeTab?.webView != null) {
            attachWebView(activeTab.webView!!)
        }
    }

    private fun collapseOverlay() {
        // Safe remove of WebView
        detachWebView()

        overlayView?.removeAllViews()
        overlayView?.addView(collapsedView)
        windowManager.updateViewLayout(overlayView, paramsCollapsed)
        _isOverlayExpanded.value = false
    }

    private fun openMainActivityAndCollapse() {
        collapseOverlay()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    fun attachWebView(view: WebView) {
        val parent = view.parent as? ViewGroup
        parent?.removeView(view) // Detach from previous parent

        activeWebView = view
        webViewContainer?.removeAllViews()
        webViewContainer?.addView(view, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        titleText?.text = view.title ?: "Chrome Tab"
    }

    fun detachWebView(): WebView? {
        val view = activeWebView
        if (view != null) {
            webViewContainer?.removeView(view)
            activeWebView = null
            // Re-inflate placeholder text in the overlay
            val placeholderText = TextView(this).apply {
                text = "Background Session Monitor Active\nGoogle Colab & Scripts keeping alive..."
                textColor = Color.parseColor("#9AA0A6")
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            webViewContainer?.addView(placeholderText)
        }
        return view
    }

    fun updateStatus(logText: String, isError: Boolean = false) {
        statusText?.text = logText
        statusText?.textColor = if (isError) Color.parseColor("#EA4335") else Color.parseColor("#34A853")
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    // Direct helper to set text color easily
    private var TextView.textColor: Int
        get() = currentTextColor
        set(value) = setTextColor(value)

    override fun onDestroy() {
        super.onDestroy()
        detachWebView()
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                // View might not be attached
            }
        }
        _isServiceRunning.value = false
        serviceInstance = null
    }
}
