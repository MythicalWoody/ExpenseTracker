package com.example.expencetrackerapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.expencetrackerapp.MainActivity
import com.example.expencetrackerapp.R

/**
 * Helper class for managing expense-related notifications.
 */
object NotificationHelper {
    
    private const val CHANNEL_ID = "expense_tracker_channel"
    private const val CHANNEL_NAME = "Expense Tracker"
    private const val CHANNEL_DESCRIPTION = "Notifications for new expenses that need categorization"
    
    const val EXTRA_EXPENSE_ID = "expense_id"
    const val EXTRA_NAVIGATE_TO = "navigate_to"
    const val NAV_EDIT_EXPENSE = "edit_expense"
    
    /**
     * Creates the notification channel (required for Android 8.0+).
     * Should be called when the app starts.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Posts a notification asking the user to categorize an expense.
     * 
     * @param context Application context
     * @param expenseId ID of the expense that needs categorization
     * @param merchant Merchant name to show in notification
     * @param amount Transaction amount
     */
    fun showCategorizationNeeded(
        context: Context,
        expenseId: Long,
        merchant: String,
        amount: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_EXPENSE_ID, expenseId)
            putExtra(EXTRA_NAVIGATE_TO, NAV_EDIT_EXPENSE)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            expenseId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val formattedAmount = "₹%.2f".format(amount)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Categorize Expense")
            .setContentText("$formattedAmount at $merchant - tap to set category")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("We couldn't automatically categorize a $formattedAmount expense at $merchant. Tap to choose the right category."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Categorize",
                pendingIntent
            )
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(expenseId.toInt(), notification)
    }
    
    /**
     * Cancels a specific expense notification.
     */
    fun cancelNotification(context: Context, expenseId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(expenseId.toInt())
    }
}
