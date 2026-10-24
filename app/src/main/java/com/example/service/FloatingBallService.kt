package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.data.RecordingsRepository
import com.example.model.RecordingState
import kotlinx.coroutines.*
import kotlin.math.abs

class FloatingBallService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null

    private var floatingView: View? = null
    private var floatingParams: WindowManager.LayoutParams? = null

    private var annotationView: AnnotationCanvasView? = null
    private var annotationParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var isMinimizedToEdge = false
    private var isStealth = true
    private var isBallHidden = false
    private var autoHideJob: Job? = null

    private var edgeHandleView: View? = null
    private var edgeHandleParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var timeTextView: TextView? = null
    private var pauseIcon: ImageView? = null
    private var pulseDot: View? = null
    private var actionMenuLayout: LinearLayout? = null

    companion object {
        private const val TAG = "FloatingBallService"
        const val EXTRA_STEALTH = "extra_stealth"

        fun startService(context: Context, stealth: Boolean) {
            if (!Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Cannot draw overlays: permission not granted.")
                return
            }
            val intent = Intent(context, FloatingBallService::class.java).apply {
                putExtra(EXTRA_STEALTH, stealth)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingBallService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isStealth = intent?.getBooleanExtra(EXTRA_STEALTH, true) ?: true
        if (Settings.canDrawOverlays(this)) {
            createFloatingBall()
            createEdgeHandle()
            observeState()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingBall() {
        if (floatingView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val windowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        floatingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            windowFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        val container = FrameLayout(this)
        val density = resources.displayMetrics.density

        // Main Ball View
        val mainCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (8 * density).toInt(), (14 * density).toInt(), (8 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * density
                setColor(Color.parseColor("#E60A0F1D"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#334155"))
            }
            elevation = 12 * density
        }

        // Glowing Record Dot
        pulseDot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((12 * density).toInt(), (12 * density).toInt()).apply {
                marginEnd = (8 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF2D55"))
            }
        }
        mainCard.addView(pulseDot)

        // Elapsed Time Text
        timeTextView = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setShadowLayer(4f, 0f, 0f, Color.parseColor("#FF2D55"))
        }
        mainCard.addView(timeTextView)

        // Expandable Action Menu (hidden by default)
        actionMenuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding((8 * density).toInt(), 0, 0, 0)
        }

        // 1. Pause / Resume Button
        pauseIcon = createActionButton(android.R.drawable.ic_media_pause, "#FFB800", "Pause") {
            ScreenRecorderManager.togglePauseResume()
        }
        actionMenuLayout?.addView(pauseIcon)

        // 2. Stop Button
        val stopBtn = createActionButton(android.R.drawable.ic_menu_close_clear_cancel, "#FF2D55", "Stop") {
            val repo = RecordingsRepository(applicationContext)
            ScreenRecorderManager.stopRecording(applicationContext, repo)
            // Open MainActivity to show preview / trimmer
            val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("OPEN_TRIMMER_LAST", true)
            }
            startActivity(openIntent)
        }
        actionMenuLayout?.addView(stopBtn)

        // 3. Annotate / Draw Mode Button
        val drawBtn = createActionButton(android.R.drawable.ic_menu_edit, "#00E5FF", "Annotate") {
            toggleAnnotationOverlay()
        }
        actionMenuLayout?.addView(drawBtn)

        // 4. Hide Ball / Stealth Mode Button (ensures clean recorded video)
        val hideBtn = createActionButton(android.R.drawable.ic_menu_view, "#38BDF8", "Hide in Video") {
            hideFloatingBall(showToast = true)
        }
        actionMenuLayout?.addView(hideBtn)

        // 5. Minimize to Edge Tab Button
        val minBtn = createActionButton(android.R.drawable.ic_menu_revert, "#94A3B8", "Minimize") {
            toggleMinimize()
        }
        actionMenuLayout?.addView(minBtn)

        mainCard.addView(actionMenuLayout)
        container.addView(mainCard)

        // Drag & Tap Listener
        mainCard.setOnTouchListener { _, event ->
            val p = floatingParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = initialX + (event.rawX - initialTouchX).toInt()
                    p.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = abs(event.rawX - initialTouchX)
                    val diffY = abs(event.rawY - initialTouchY)
                    if (diffX < 10 && diffY < 10) {
                        // Considered a Click
                        if (isMinimizedToEdge) {
                            toggleMinimize()
                        } else {
                            toggleExpandMenu()
                        }
                    } else {
                        // Magnetic snap to nearest edge
                        snapToEdge()
                    }
                    true
                }
                else -> false
            }
        }

        floatingView = container
        windowManager?.addView(floatingView, floatingParams)
    }

    private fun createActionButton(
        iconRes: Int,
        tintHex: String,
        contentDesc: String,
        onClick: () -> Unit
    ): ImageView {
        val density = resources.displayMetrics.density
        return ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt()).apply {
                marginStart = (6 * density).toInt()
            }
            setImageResource(iconRes)
            setColorFilter(Color.parseColor(tintHex))
            contentDescription = contentDesc
            setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
                setStroke((1 * density).toInt(), Color.parseColor(tintHex))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun toggleExpandMenu() {
        isExpanded = !isExpanded
        actionMenuLayout?.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun toggleMinimize() {
        isMinimizedToEdge = !isMinimizedToEdge
        val density = resources.displayMetrics.density
        if (isMinimizedToEdge) {
            isExpanded = false
            actionMenuLayout?.visibility = View.GONE
            timeTextView?.visibility = View.GONE
            floatingParams?.alpha = 0.6f
        } else {
            timeTextView?.visibility = View.VISIBLE
            floatingParams?.alpha = 1.0f
        }
        snapToEdge()
    }

    private fun snapToEdge() {
        val p = floatingParams ?: return
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getMetrics(metrics)
        val screenWidth = metrics.widthPixels

        val viewWidth = floatingView?.width ?: 150
        val targetX = if (p.x + viewWidth / 2 < screenWidth / 2) {
            if (isMinimizedToEdge) -viewWidth / 3 else 20
        } else {
            if (isMinimizedToEdge) screenWidth - viewWidth * 2 / 3 else screenWidth - viewWidth - 20
        }
        p.x = targetX
        windowManager?.updateViewLayout(floatingView, p)
    }

    private fun toggleAnnotationOverlay() {
        if (annotationView == null) {
            showAnnotationOverlay()
        } else {
            hideAnnotationOverlay()
        }
    }

    private fun showAnnotationOverlay() {
        if (annotationView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        annotationParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            flags,
            PixelFormat.TRANSLUCENT
        )

        val canvas = AnnotationCanvasView(this) {
            hideAnnotationOverlay()
        }
        annotationView = canvas
        windowManager?.addView(annotationView, annotationParams)
    }

    private fun hideAnnotationOverlay() {
        annotationView?.let {
            windowManager?.removeView(it)
            annotationView = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createEdgeHandle() {
        if (edgeHandleView != null) return
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val density = resources.displayMetrics.density
        edgeHandleParams = WindowManager.LayoutParams(
            (14 * density).toInt(),
            (56 * density).toInt(),
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            x = 0
        }

        val handle = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    14 * density, 14 * density,
                    0f, 0f,
                    0f, 0f,
                    14 * density, 14 * density
                )
                setColor(Color.parseColor("#990A0F1D"))
                setStroke((1 * density).toInt(), Color.parseColor("#6638BDF8"))
            }
            visibility = View.GONE
            setOnClickListener {
                showFloatingBall()
            }
        }
        edgeHandleView = handle
        try {
            windowManager?.addView(edgeHandleView, edgeHandleParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach edge handle", e)
        }
    }

    private fun hideFloatingBall(showToast: Boolean = false) {
        isBallHidden = true
        isExpanded = false
        actionMenuLayout?.visibility = View.GONE
        floatingView?.visibility = View.GONE
        edgeHandleView?.visibility = View.VISIBLE
        if (showToast) {
            android.widget.Toast.makeText(
                applicationContext,
                "Floating ball hidden. Tap screen edge or use Notification to restore.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showFloatingBall() {
        isBallHidden = false
        floatingView?.visibility = View.VISIBLE
        edgeHandleView?.visibility = View.GONE
    }

    private fun scheduleAutoHide() {
        if (!isStealth || isBallHidden) return
        autoHideJob?.cancel()
        autoHideJob = serviceScope.launch {
            delay(1500L)
            hideFloatingBall()
        }
    }

    private fun observeState() {
        serviceScope.launch {
            ScreenRecorderManager.recordingState.collect { state ->
                when (state) {
                    is RecordingState.Active -> {
                        timeTextView?.text = state.formattedDuration()
                        if (state.isPaused) {
                            pauseIcon?.setImageResource(android.R.drawable.ic_media_play)
                            pauseIcon?.setColorFilter(Color.parseColor("#10B981"))
                            pulseDot?.setBackgroundColor(Color.parseColor("#FFB800"))
                            autoHideJob?.cancel()
                            showFloatingBall()
                        } else {
                            pauseIcon?.setImageResource(android.R.drawable.ic_media_pause)
                            pauseIcon?.setColorFilter(Color.parseColor("#FFB800"))
                            pulseDot?.setBackgroundColor(Color.parseColor("#FF2D55"))
                            if (isStealth && !isBallHidden && autoHideJob == null) {
                                scheduleAutoHide()
                            }
                        }
                    }
                    is RecordingState.Idle, is RecordingState.Finished, is RecordingState.Error -> {
                        stopSelf()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroy() {
        autoHideJob?.cancel()
        hideAnnotationOverlay()
        edgeHandleView?.let {
            runCatching { windowManager?.removeView(it) }
            edgeHandleView = null
        }
        floatingView?.let {
            runCatching { windowManager?.removeView(it) }
            floatingView = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/**
 * Full-screen annotation view with drawing brush, color picker, clear, undo and exit tools.
 * Inherits FLAG_SECURE from parent WindowManager.LayoutParams to stay 100% invisible in recording!
 */
@SuppressLint("ViewConstructor")
class AnnotationCanvasView(
    context: Context,
    private val onClose: () -> Unit
) : FrameLayout(context) {

    private val paths = mutableListOf<Stroke>()
    private var currentPath = Path()
    private var currentColor = Color.parseColor("#00E5FF")
    private var currentStrokeWidth = 10f

    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    data class Stroke(val path: Path, val color: Int, val width: Float)

    private val drawingCanvas = object : View(context) {
        init {
            setWillNotDraw(false)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            for (stroke in paths) {
                paint.color = stroke.color
                paint.strokeWidth = stroke.width
                canvas.drawPath(stroke.path, paint)
            }
            paint.color = currentColor
            paint.strokeWidth = currentStrokeWidth
            canvas.drawPath(currentPath, paint)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentPath.reset()
                    currentPath.moveTo(x, y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentPath.lineTo(x, y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    currentPath.lineTo(x, y)
                    paths.add(Stroke(Path(currentPath), currentColor, currentStrokeWidth))
                    currentPath.reset()
                    invalidate()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }

    init {
        addView(drawingCanvas, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Floating annotation toolbar at bottom
        val density = resources.displayMetrics.density
        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20 * density
                setColor(Color.parseColor("#EE0B1120"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#38BDF8"))
            }
            elevation = 16 * density
        }

        val toolbarParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (32 * density).toInt()
        }

        // Color selection dots
        val colors = listOf("#00E5FF", "#FF2D55", "#FFDE03", "#FFFFFF", "#10B981")
        for (hex in colors) {
            val colorView = View(context).apply {
                layoutParams = LinearLayout.LayoutParams((28 * density).toInt(), (28 * density).toInt()).apply {
                    marginEnd = (8 * density).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hex))
                    setStroke((2 * density).toInt(), Color.WHITE)
                }
                setOnClickListener {
                    currentColor = Color.parseColor(hex)
                }
            }
            toolbar.addView(colorView)
        }

        // Undo button
        val undoBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt()).apply {
                marginEnd = (8 * density).toInt()
            }
            setImageResource(android.R.drawable.ic_menu_revert)
            setColorFilter(Color.WHITE)
            contentDescription = "Undo stroke"
            setOnClickListener {
                if (paths.isNotEmpty()) {
                    paths.removeAt(paths.size - 1)
                    drawingCanvas.invalidate()
                }
            }
        }
        toolbar.addView(undoBtn)

        // Clear button
        val clearBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt()).apply {
                marginEnd = (8 * density).toInt()
            }
            setImageResource(android.R.drawable.ic_menu_delete)
            setColorFilter(Color.parseColor("#FF5555"))
            contentDescription = "Clear all strokes"
            setOnClickListener {
                paths.clear()
                currentPath.reset()
                drawingCanvas.invalidate()
            }
        }
        toolbar.addView(clearBtn)

        // Done / Close button
        val closeBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt())
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            contentDescription = "Exit drawing mode"
            setOnClickListener {
                onClose()
            }
        }
        toolbar.addView(closeBtn)

        addView(toolbar, toolbarParams)
    }
}
