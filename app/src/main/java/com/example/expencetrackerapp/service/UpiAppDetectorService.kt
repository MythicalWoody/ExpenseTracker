package com.example.expencetrackerapp.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor

/**
 * Accessibility Service that:
 * 1. Detects when UPI apps are launched
 * 2. Shows floating overlay button
 * 3. Takes screenshots using accessibility API (no permission dialog!)
 * 
 * Supported apps: Samsung Wallet, PhonePe, Google Pay, CRED
 */
class UpiAppDetectorService : AccessibilityService() {
    
    companion object {
        private const val TAG = "UpiAppDetectorService"
        
        // Package names of UPI apps to monitor
        val MONITORED_APPS = setOf(
            "com.samsung.android.spay",          // Samsung Wallet/Pay
            "com.samsung.android.samsungpay",    // Samsung Pay (alt)
            "com.phonepe.app",                   // PhonePe
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.dreamplug.androidapp",          // CRED
        )
        
        var isRunning = false
            private set
            
        var isButtonVisible = false
            private set
            
        // Reference to the service for screenshot capture
        private var instance: UpiAppDetectorService? = null
        
        fun takeScreenshot(callback: (String?) -> Unit) {
            instance?.captureScreen(callback) ?: callback(null)
        }
    }
    
    private var lastDetectedPackage: String? = null
    private var currentUpiApp: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        isButtonVisible = false
        instance = this
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
        
        Log.d(TAG, "UPI App Detector Service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: "unknown"
        
        // IGNORE our own app - the floating button overlay triggers events!
        if (packageName == "com.example.expencetrackerapp") {
            Log.d(TAG, "⏭️ Ignoring our own overlay: $className")
            return
        }
        
        // Log ALL detected apps with full details
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "📱 APP DETECTED: $packageName")
        Log.i(TAG, "   Is UPI App: ${packageName in MONITORED_APPS}")
        Log.i(TAG, "   Button Visible: $isButtonVisible")
        
        // Skip if same package
        if (packageName == lastDetectedPackage) {
            Log.d(TAG, "   ↳ Same as last, skipping")
            return
        }
        
        lastDetectedPackage = packageName
        
        // Check if this is a UPI app
        val isUpiApp = packageName in MONITORED_APPS
        
        if (isUpiApp && !isButtonVisible) {
            // Show button
            Log.i(TAG, "✅ UPI APP OPENED: $packageName")
            currentUpiApp = packageName
            showFloatingButton(packageName)
        } else if (!isUpiApp && isButtonVisible) {
            // Hide button - user left UPI app
            Log.i(TAG, "❌ LEFT UPI APP - now in: $packageName")
            hideFloatingButton()
        }
    }
    
    /**
     * Capture screenshot using Accessibility API (Android R+).
     * This is INSTANT - no permission dialog needed!
     */
    fun captureScreen(callback: (String?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.e(TAG, "Screenshot API requires Android 11+")
            mainHandler.post {
                Toast.makeText(this, "Requires Android 11+", Toast.LENGTH_SHORT).show()
            }
            callback(null)
            return
        }
        
        Log.d(TAG, "📸 Taking screenshot...")
        
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        Log.d(TAG, "✅ Screenshot captured!")
                        
                        try {
                            val bitmap = Bitmap.wrapHardwareBuffer(
                                screenshot.hardwareBuffer,
                                screenshot.colorSpace
                            )
                            
                            if (bitmap != null) {
                                // Save to cache
                                val file = File(cacheDir, "upi_screenshot_${System.currentTimeMillis()}.png")
                                FileOutputStream(file).use { out ->
                                    // Convert hardware bitmap if needed
                                    val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    softwareBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    softwareBitmap.recycle()
                                }
                                bitmap.recycle()
                                screenshot.hardwareBuffer.close()
                                
                                Log.d(TAG, "📁 Screenshot saved: ${file.absolutePath}")
                                callback(file.absolutePath)
                            } else {
                                Log.e(TAG, "Failed to create bitmap from screenshot")
                                callback(null)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing screenshot", e)
                            callback(null)
                        }
                    }
                    
                    override fun onFailure(errorCode: Int) {
                        Log.e(TAG, "❌ Screenshot failed with error code: $errorCode")
                        mainHandler.post {
                            Toast.makeText(
                                this@UpiAppDetectorService,
                                "Screenshot failed (code: $errorCode)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        callback(null)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error taking screenshot", e)
            callback(null)
        }
    }
    
    override fun onInterrupt() {
        isRunning = false
        Log.d(TAG, "UPI App Detector Service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        instance = null
        hideFloatingButton()
    }
    
    private fun showFloatingButton(appPackage: String) {
        try {
            val intent = Intent(this, FloatingButtonService::class.java).apply {
                action = FloatingButtonService.ACTION_SHOW
                putExtra(FloatingButtonService.EXTRA_APP_PACKAGE, appPackage)
            }
            startService(intent)
            isButtonVisible = true
            Log.d(TAG, "Floating button SHOW command sent")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating button", e)
        }
    }
    
    private fun hideFloatingButton() {
        try {
            val intent = Intent(this, FloatingButtonService::class.java).apply {
                action = FloatingButtonService.ACTION_SHOW
            }
            startService(intent)
            isButtonVisible = false
            Log.d(TAG, "Floating button HIDE command sent")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating button", e)
        }
    }
}
