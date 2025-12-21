package com.example.expencetrackerapp.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.Toast
import com.example.expencetrackerapp.R

/**
 * Floating button service that appears over UPI apps.
 * When clicked, triggers screenshot via AccessibilityService and opens parser.
 */
class FloatingButtonService : Service() {
    
    // Consolidating constants in the single companion object at bottom of file

    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    // Store current params to update them later
    private var currentLayoutParams: WindowManager.LayoutParams? = null
    private var currentAppPackage: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                currentAppPackage = intent.getStringExtra(EXTRA_APP_PACKAGE)
                showFloatingButton()
            }
            ACTION_HIDE -> {
                hideFloatingButton()
            }
        }
        return START_STICKY
    }
    
    @SuppressLint("InflateParams")
    private fun showFloatingButton() {
        if (floatingView != null) return
        
        val layoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = layoutInflater.inflate(R.layout.floating_button, null)
        
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Initial position: Bottom-Right area roughly
            val metrics = applicationContext.resources.displayMetrics
            x = metrics.widthPixels - 200 
            y = metrics.heightPixels / 2
        }
        
        // Save refernece
        currentLayoutParams = params
        
        // Setup button click
        floatingView?.findViewById<ImageButton>(R.id.floating_capture_button)?.setOnClickListener {
            Log.d(TAG, "📸 Floating button clicked - taking screenshot")
            captureAndProcess()
        }
        
        // Setup drag
        setupDragListener(params)
        
        try {
            windowManager?.addView(floatingView, params)
            isButtonVisible = true
            Log.d(TAG, "Floating button displayed")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating button", e)
        }
    }
    
    private fun setupDragListener(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        
        floatingView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false 
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    
                    if (!isDragging && diffX < 10 && diffY < 10) {
                        // It was a click
                        floatingView?.findViewById<View>(R.id.floating_capture_button)?.performClick()
                    } else {
                        // Snap to edge logic
                        snapToEdge()
                    }
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    // Determine if we are dragging
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isDragging = true
                    }
                    
                    params.x = initialX + deltaX.toInt()
                    params.y = initialY + deltaY.toInt()
                    
                    try {
                        windowManager?.updateViewLayout(floatingView, params)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating layout", e)
                    }
                    return@setOnTouchListener true
                }
                else -> false
            }
        }
    }
    
    private fun snapToEdge() {
        val wm = windowManager ?: return
        val p = currentLayoutParams ?: return
        val fv = floatingView ?: return
        val metrics = applicationContext.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        
        // Calculate nearest edge
        // Left is 0, Right is screenWidth - width
        val middle = screenWidth / 2
        val targetX = if (p.x + (fv.width / 2) < middle) {
             0 
        } else {
            screenWidth - fv.width
        }
        
        // Animate
        val animator = ValueAnimator.ofInt(p.x, targetX)
        animator.duration = 250 
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            p.x = animation.animatedValue as Int
            try {
                wm.updateViewLayout(fv, p)
            } catch (e: Exception) {
                // Ignore
            }
        }
        animator.start()
    }
    
    private fun captureAndProcess() {
        // Hide button temporarily during capture
        floatingView?.visibility = View.INVISIBLE
        
        // Small delay to let button hide
        mainHandler.postDelayed({
            // Use AccessibilityService to take screenshot - NO DIALOGS!
            UpiAppDetectorService.takeScreenshot { screenshotPath ->
                mainHandler.post {
                    if (screenshotPath != null) {
                        Log.d(TAG, "✅ Got screenshot: $screenshotPath")
                        openParser(screenshotPath)
                    } else {
                        Log.e(TAG, "❌ Screenshot failed")
                        Toast.makeText(this, "Screenshot failed", Toast.LENGTH_SHORT).show()
                        floatingView?.visibility = View.VISIBLE
                    }
                }
            }
        }, 200)
    }
    
    private fun openParser(screenshotPath: String) {
        // Open the parser activity with the screenshot
        val intent = Intent(this, ScreenshotParserActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("screenshot_path", screenshotPath)
            putExtra("app_package", currentAppPackage)
        }
        startActivity(intent)
        
        // Show button again after a delay
        mainHandler.postDelayed({
            floatingView?.visibility = View.VISIBLE
        }, 500)
    }
    
    private fun hideFloatingButton() {
        floatingView?.let {
            try {
                windowManager?.removeView(it)
                Log.d(TAG, "Floating view removed from window")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating button", e)
            }
        }
        floatingView = null
        isButtonVisible = false // Reset visibility flag if you have one, or just rely on view nullity
        stopSelf()
        Log.d(TAG, "FloatingButtonService stopped")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideFloatingButton()
    }

    // Static flag to track visibility helper for other services if needed
    companion object {
        var isButtonVisible = false
        private const val TAG = "FloatingButtonService"
        const val ACTION_SHOW = "com.example.expencetrackerapp.SHOW_FLOATING"
        const val ACTION_HIDE = "com.example.expencetrackerapp.HIDE_FLOATING"
        const val EXTRA_APP_PACKAGE = "app_package"
    }
}
