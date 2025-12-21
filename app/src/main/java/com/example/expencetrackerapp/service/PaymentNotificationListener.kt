package com.example.expencetrackerapp.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.expencetrackerapp.data.database.ExpenseDatabase
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.TransactionType
import com.example.expencetrackerapp.domain.categorization.CategoryMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Notification Listener Service that captures payment notifications from UPI apps.
 * 
 * Supported apps:
 * - PhonePe
 * - Google Pay (GPay)
 * - Paytm
 * - Amazon Pay
 * - BHIM
 * - WhatsApp Pay
 * 
 * This catches payments even when bank SMS is not received.
 */
class PaymentNotificationListener : NotificationListenerService() {
    
    companion object {
        private const val TAG = "PaymentNotificationListener"
        
        // Package names of UPI apps
        private val UPI_PACKAGES = setOf(
            "com.phonepe.app",           // PhonePe
            "com.google.android.apps.nbu.paisa.user", // GPay
            "net.one97.paytm",           // Paytm
            "in.amazon.mShop.android.shopping", // Amazon
            "com.amazon.pay.ui.android", // Amazon Pay
            "in.org.npci.upiapp",        // BHIM
            "com.whatsapp",              // WhatsApp (for WhatsApp Pay)
        )
        
        // Keywords indicating successful payment
        private val PAYMENT_KEYWORDS = listOf(
            "paid", "sent", "debited", "payment successful", 
            "transfer successful", "money sent", "paid to",
            "payment of", "₹", "rs.", "rs "
        )
        
        // Keywords to ignore (not actual payments)
        private val IGNORE_KEYWORDS = listOf(
            "received", "credited", "cashback", "reward",
            "offer", "discount", "failed", "pending", "request"
        )
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: ExpenseDatabase
    private val categoryMatcher = CategoryMatcher()
    
    override fun onCreate() {
        super.onCreate()
        database = ExpenseDatabase.getDatabase(applicationContext)
        Log.d(TAG, "PaymentNotificationListener started")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        
        val packageName = sbn.packageName
        
        // Only process UPI app notifications
        if (packageName !in UPI_PACKAGES) return
        
        val notification = sbn.notification
        val extras = notification.extras
        
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        
        val fullText = "$title $text $bigText"
        
        Log.d(TAG, "UPI Notification from $packageName: $fullText")
        
        // Check if this is a payment notification
        if (!isPaymentNotification(fullText)) {
            Log.d(TAG, "Not a payment notification, ignoring")
            return
        }
        
        // Parse payment details
        val paymentInfo = parsePaymentNotification(fullText, packageName)
        
        if (paymentInfo != null) {
            Log.d(TAG, "Parsed payment: ${paymentInfo.amount} to ${paymentInfo.merchant}")
            savePayment(paymentInfo, fullText)
        }
    }
    
    private fun isPaymentNotification(text: String): Boolean {
        val lower = text.lowercase()
        
        // Must contain payment keywords
        val hasPaymentKeyword = PAYMENT_KEYWORDS.any { lower.contains(it) }
        
        // Must not be a received/incoming payment or offer
        val hasIgnoreKeyword = IGNORE_KEYWORDS.any { lower.contains(it) }
        
        // Must contain an amount
        val hasAmount = Regex("""[₹₨]?\s?\d{1,3}(?:,?\d{3})*(?:\.\d{2})?""").containsMatchIn(text)
        
        return hasPaymentKeyword && !hasIgnoreKeyword && hasAmount
    }
    
    private fun parsePaymentNotification(text: String, packageName: String): PaymentInfo? {
        // Extract amount
        val amount = extractAmount(text) ?: return null
        
        // Extract merchant/recipient
        val merchant = extractMerchant(text, packageName)
        
        // Determine source app
        val source = getSourceName(packageName)
        
        return PaymentInfo(
            amount = amount,
            merchant = merchant,
            source = source
        )
    }
    
    private fun extractAmount(text: String): Double? {
        // Patterns for amount extraction
        val patterns = listOf(
            Regex("""[₹₨]\s?(\d{1,3}(?:,?\d{3})*(?:\.\d{2})?)"""),
            Regex("""Rs\.?\s?(\d{1,3}(?:,?\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""(\d{1,3}(?:,?\d{3})*(?:\.\d{2})?)\s?(?:paid|sent|debited)""", RegexOption.IGNORE_CASE),
            Regex("""(?:paid|sent|debited)\s?[₹₨]?\s?(\d{1,3}(?:,?\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                try {
                    val amount = amountStr.toDouble()
                    if (amount > 0 && amount < 10_000_000) {
                        return amount
                    }
                } catch (e: NumberFormatException) {
                    continue
                }
            }
        }
        return null
    }
    
    private fun extractMerchant(text: String, packageName: String): String {
        val lower = text.lowercase()
        
        // Patterns to extract merchant name
        val patterns = listOf(
            Regex("""(?:paid to|sent to|to)\s+(.+?)(?:\s+on|\s+via|\s+from|\.|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:payment to|transferred to)\s+(.+?)(?:\s+|$)""", RegexOption.IGNORE_CASE),
            Regex("""(.+?)\s+(?:paid|completed|successful)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                if (merchant.length in 2..50) {
                    return cleanMerchant(merchant)
                }
            }
        }
        
        // Default based on app
        return when (packageName) {
            "com.phonepe.app" -> "PhonePe Payment"
            "com.google.android.apps.nbu.paisa.user" -> "GPay Payment"
            "net.one97.paytm" -> "Paytm Payment"
            else -> "UPI Payment"
        }
    }
    
    private fun cleanMerchant(merchant: String): String {
        return merchant
            .replace(Regex("""[^\w\s\.\-&'@]"""), "")
            .trim()
            .take(50)
            .ifEmpty { "Unknown" }
    }
    
    private fun getSourceName(packageName: String): String {
        return when (packageName) {
            "com.phonepe.app" -> "PhonePe"
            "com.google.android.apps.nbu.paisa.user" -> "GPay"
            "net.one97.paytm" -> "Paytm"
            "in.amazon.mShop.android.shopping", "com.amazon.pay.ui.android" -> "Amazon Pay"
            "in.org.npci.upiapp" -> "BHIM"
            "com.whatsapp" -> "WhatsApp Pay"
            else -> "UPI"
        }
    }
    
    private fun savePayment(paymentInfo: PaymentInfo, rawNotification: String) {
        serviceScope.launch {
            try {
                // Check for duplicates (same amount and merchant within 5 minutes)
                val recentExpenses = database.expenseDao().getRecentExpensesSync(10)
                val isDuplicate = recentExpenses.any { expense ->
                    expense.amount == paymentInfo.amount &&
                    expense.merchant.equals(paymentInfo.merchant, ignoreCase = true) &&
                    (System.currentTimeMillis() - expense.date) < 5 * 60 * 1000 // 5 minutes
                }
                
                if (isDuplicate) {
                    Log.d(TAG, "Duplicate payment detected, skipping")
                    return@launch
                }
                
                // Get categories and mappings from database
                val categories = database.categoryDao().getAllCategoriesSync()
                val merchantMappings = database.merchantDao().getAllMappingsSync()
                
                // Get category suggestion
                val matchResult = categoryMatcher.matchCategory(
                    paymentInfo.merchant,
                    categories,
                    merchantMappings
                )
                val category = matchResult.categoryName
                val isAutoCategorized = matchResult.confidence > 0.5f
                
                // Create expense
                val expense = Expense(
                    amount = paymentInfo.amount,
                    merchant = paymentInfo.merchant,
                    category = category,
                    date = System.currentTimeMillis(),
                    smsBody = "[${paymentInfo.source}] $rawNotification",
                    accountNumber = paymentInfo.source,
                    isAutoCategorized = isAutoCategorized,
                    transactionType = TransactionType.DEBIT
                )
                
                database.expenseDao().insertExpense(expense)
                Log.d(TAG, "Payment saved: ${paymentInfo.amount} to ${paymentInfo.merchant}")
                
                // Update merchant mapping if high confidence
                if (isAutoCategorized) {
                    database.merchantDao().incrementUsage(paymentInfo.merchant, category)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving payment", e)
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed for our use case
    }
    
    data class PaymentInfo(
        val amount: Double,
        val merchant: String,
        val source: String
    )
}
