package com.example.expencetrackerapp.data.sms

import android.content.Context
import com.example.expencetrackerapp.ml.HybridSmsClassifier
import java.text.SimpleDateFormat
import java.util.*

/**
 * SMS Parser that extracts transaction details from Indian bank SMS messages.
 * Uses hybrid ML + rule-based approach for spam detection.
 */
class SmartSmsParser(context: Context) {
    
    private val hybridClassifier = HybridSmsClassifier(context)
    
    // Bank sender ID patterns
    private val bankSenderPatterns = listOf(
        "ICICI", "HDFC", "SBI", "AXIS", "KOTAK", "PNB", "BOB", "IDFC", "INDUS",
        "CITI", "HSBC", "SC", "YES", "RBL", "FEDERAL", "BOI", "IOB", "CANARA",
        "UNION", "IDBI", "BANDHAN", "PAYTM", "GPAY", "PHONEPE", "AMAZON",
        "HDFCBK", "SBIBNK", "ICICIB", "AXISBK", "KOTAKB"
    )
    
    // ===== SPAM DETECTION PATTERNS =====
    private val strongSpamIndicators = listOf(
        "apply now", "claim your", "claim now", "get card", "hurry",
        "limited time", "offer ends", "act fast", "last few days",
        "check emi", "get rs", "get a loan", "lowest loan", "loan offer",
        "personal loan", "free credit card", "lifetime free", "free card",
        "massive rate", "rate drop", "special offer", "exclusive offer",
        "voucher", "reward points", "bonus points", "cashback offer",
        "pre-approved", "pre approved", "instant approval", "apply today",
        "best rates", "lowest rates", "reduce emi", "emi @ just",
        "upto rs", "up to rs", "win rs", "earn rs", "get upto",
        "flying machine", "explore our", "visit us at", "shop now",
        "use code", "promo code", "discount code", "coupon",
        "diwali", "festival offer", "festive offer", "holiday offer"
    )
    
    private val spamUrlPatterns = listOf(
        "hdfcbk.io", "icicibank.page.link", "sbicard.in", "axisb.in",
        "i.cplry.com", "bit.ly", "goo.gl", "tinyurl", "shorturl"
    )
    
    private val tncPatterns = listOf(
        "t&c", "t & c", "tnc", "terms apply", "terms and conditions",
        "*conditions apply", "conditions apply"
    )
    
    private val loanPromoPatterns = listOf(
        "@\\s*\\d+\\.\\d+%\\s*p\\.?m".toRegex(RegexOption.IGNORE_CASE),
        "\\d+\\.\\d+%\\s*(?:per month|p\\.?m|monthly)".toRegex(RegexOption.IGNORE_CASE),
        "emi\\s*of\\s*rs".toRegex(RegexOption.IGNORE_CASE),
        "loan\\s*(?:of)?\\s*rs\\.?\\s*\\d".toRegex(RegexOption.IGNORE_CASE)
    )
    
    // ===== TRANSACTION DETECTION PATTERNS =====
    private val strongTransactionIndicators = listOf(
        "debited from", "credited to", "spent on", "payment of rs",
        "your a/c", "your account", "trf to", "transferred to",
        "upi ref", "refno", "txn id", "transaction id",
        "if not you", "if not u", "not you?", "not u?",
        "available balance", "avl bal", "balance is"
    )
    
    private val amountPatterns = listOf(
        """(?:Spent|spent)\s+Rs\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE),
        """Rs\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+(?:debited|credited|spent)""".toRegex(RegexOption.IGNORE_CASE),
        """debited\s+by\s+([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE),
        """debit\s+of\s+Rs\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE),
        """(?:INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE),
        """credited\s+(?:by\s+)?(?:Rs\.?\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE)
    )
    
    private val accountPatterns = listOf(
        """Card\s+(\d{4})""".toRegex(RegexOption.IGNORE_CASE),
        """A/[cC]\s*[Xx]*(\d{4})""".toRegex(),
        """(?:ENDING|ending)\s+(?:WITH\s+)?(\d{4})""".toRegex(RegexOption.IGNORE_CASE),
        """XX(\d{4})""".toRegex(),
        """A/C\s+X(\d{4})""".toRegex(RegexOption.IGNORE_CASE)
    )
    
    private val merchantPatterns = listOf(
        """At\s+([A-Za-z0-9\s\.\-&']+?)\s+On\s+\d""".toRegex(RegexOption.IGNORE_CASE),
        """trf\s+to\s+([A-Za-z0-9\s\.\-&']+?)(?:\s+Refno|\s+If\s+not|$)""".toRegex(RegexOption.IGNORE_CASE),
        """from\s+VPA\s+([a-zA-Z0-9\.\-@]+)""".toRegex(RegexOption.IGNORE_CASE),
        """to\s+VPA\s+([a-zA-Z0-9\.\-@]+)""".toRegex(RegexOption.IGNORE_CASE),
        """AutoPay\s+for\s+([A-Za-z0-9\s\.\-&']+?)(?:\s+debit|\s+of|$)""".toRegex(RegexOption.IGNORE_CASE),
        """(?:^|\s)at\s+([A-Za-z0-9\s\.\-&']+?)(?:\s+on|\s+ref|\.|,|$)""".toRegex(RegexOption.IGNORE_CASE),
        """(?:paid\s+to|sent\s+to|transferred\s+to)\s+([A-Za-z0-9\s\.\-&']+?)(?:\s+on|\s+ref|\.|$)""".toRegex(RegexOption.IGNORE_CASE)
    )
    
    private val datePatterns = listOf(
        """On\s+(\d{4}-\d{2}-\d{2})""".toRegex(RegexOption.IGNORE_CASE),
        """on\s+(\d{1,2}-\d{1,2}-\d{2,4})""".toRegex(RegexOption.IGNORE_CASE),
        """on\s+date\s+(\d{1,2}[A-Za-z]{3}\d{2})""".toRegex(RegexOption.IGNORE_CASE),
        """ON\s+(\d{1,2}-\d{1,2}-\d{4})""".toRegex()
    )
    
    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "withdrawn", "purchase",
        "txn", "transaction", "sent", "trf to", "transferred to"
    )
    
    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposited", "refund",
        "cashback", "reversed", "payment received", "payment of"
    )
    
    private val skipKeywords = listOf(
        "is scheduled", "will be debited", "upcoming", "reminder",
        "due date", "payment due", "minimum due", "total due"
    )
    
    /**
     * Check if sender is a bank.
     */
    fun isBankSms(senderId: String): Boolean {
        val upperSender = senderId.uppercase()
        return bankSenderPatterns.any { upperSender.contains(it) }
    }
    
    /**
     * Calculate rule-based scores.
     */
    fun calculateScores(smsBody: String): Pair<Int, Int> {
        val lower = smsBody.lowercase()
        var spamScore = 0
        var transactionScore = 0
        
        // Spam indicators
        strongSpamIndicators.forEach { if (lower.contains(it)) spamScore += 3 }
        spamUrlPatterns.forEach { if (lower.contains(it)) spamScore += 5 }
        tncPatterns.forEach { if (lower.contains(it)) spamScore += 4 }
        loanPromoPatterns.forEach { if (it.containsMatchIn(smsBody)) spamScore += 4 }
        
        // Transaction indicators
        strongTransactionIndicators.forEach { if (lower.contains(it)) transactionScore += 5 }
        accountPatterns.forEach { if (it.find(smsBody) != null) transactionScore += 3 }
        
        return Pair(spamScore, transactionScore)
    }
    
    /**
     * Parse SMS with hybrid ML + rules classification.
     */
    fun parseSms(smsBody: String, smsDate: Long? = null): ParseResult {
        val lower = smsBody.lowercase()
        
        // Skip scheduled/upcoming
        if (skipKeywords.any { lower.contains(it) }) {
            return ParseResult(null, null, "Scheduled transaction (skipped)")
        }
        
        // Calculate scores and use hybrid classifier
        val (spamScore, transactionScore) = calculateScores(smsBody)
        val classificationResult = hybridClassifier.classify(smsBody, spamScore, transactionScore)
        
        if (classificationResult.isSpam) {
            return ParseResult(null, classificationResult, "Spam: ${classificationResult.reason}")
        }
        
        // Check if looks like transaction
        val hasTransactionIndicator = (debitKeywords + creditKeywords).any { lower.contains(it) }
        val hasSpentPattern = lower.contains("spent rs")
        val hasAmount = amountPatterns.any { it.find(smsBody) != null }
        val isOtp = lower.contains("otp") || lower.contains("one time password")
        
        if (!(hasTransactionIndicator || hasSpentPattern) || !hasAmount || isOtp) {
            return ParseResult(null, classificationResult, "Not a transaction SMS")
        }
        
        // Extract transaction details
        val amount = extractAmount(smsBody) ?: return ParseResult(null, classificationResult, "Could not extract amount")
        val merchant = extractMerchant(smsBody) ?: "Unknown"
        val accountNumber = extractAccountNumber(smsBody)
        val transactionType = determineTransactionType(smsBody)
        val transactionDate = extractDate(smsBody) ?: smsDate ?: System.currentTimeMillis()
        
        val transactionInfo = TransactionInfo(
            amount = amount,
            merchant = cleanMerchant(merchant),
            accountNumber = accountNumber,
            transactionType = transactionType,
            rawSms = smsBody,
            date = transactionDate
        )
        
        return ParseResult(transactionInfo, classificationResult, "Valid transaction")
    }
    
    /**
     * Train classifier with user feedback.
     */
    suspend fun trainWithFeedback(smsBody: String, isSpam: Boolean) {
        hybridClassifier.train(smsBody, isSpam)
    }
    
    /**
     * Get classifier statistics.
     */
    fun getClassifierStats() = hybridClassifier.getStats()
    
    private fun extractAmount(smsBody: String): Double? {
        for (pattern in amountPatterns) {
            val match = pattern.find(smsBody)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                try {
                    val amount = amountStr.toDouble()
                    if (amount > 0 && amount < 10_000_000) return amount
                } catch (e: NumberFormatException) { continue }
            }
        }
        return null
    }
    
    private fun extractAccountNumber(smsBody: String): String? {
        for (pattern in accountPatterns) {
            val match = pattern.find(smsBody)
            if (match != null) return "XX" + match.groupValues[1]
        }
        return null
    }
    
    private fun extractMerchant(smsBody: String): String? {
        for (pattern in merchantPatterns) {
            val match = pattern.find(smsBody)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty() && merchant.length >= 2) return merchant
            }
        }
        if (smsBody.contains("PAYMENT OF", ignoreCase = true) && 
            smsBody.contains("CREDIT CARD", ignoreCase = true)) {
            return "Credit Card Payment"
        }
        return null
    }
    
    private fun extractDate(smsBody: String): Long? {
        for (pattern in datePatterns) {
            val match = pattern.find(smsBody)
            if (match != null) {
                val dateStr = match.groupValues[1]
                val parsedDate = parseDate(dateStr)
                if (parsedDate != null) return parsedDate
            }
        }
        return null
    }
    
    private fun parseDate(dateStr: String): Long? {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yy", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
            SimpleDateFormat("d-M-yyyy", Locale.getDefault()),
            SimpleDateFormat("ddMMMyy", Locale.getDefault())
        )
        for (format in formats) {
            try {
                format.isLenient = false
                val date = format.parse(dateStr)
                if (date != null) return date.time
            } catch (e: Exception) { continue }
        }
        return null
    }
    
    private fun cleanMerchant(merchant: String): String {
        return merchant
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""[^\w\s\.\-&'@]"""), "")
            .trim().take(50).ifEmpty { "Unknown" }
    }
    
    private fun determineTransactionType(smsBody: String): TransactionInfo.TransactionType {
        val lower = smsBody.lowercase()
        if (lower.contains("spent")) return TransactionInfo.TransactionType.DEBIT
        if (lower.contains("payment") && lower.contains("received")) return TransactionInfo.TransactionType.CREDIT
        if (creditKeywords.any { lower.contains(it) }) {
            if (!lower.contains("not credited") && !lower.contains("failed")) {
                return TransactionInfo.TransactionType.CREDIT
            }
        }
        return TransactionInfo.TransactionType.DEBIT
    }
    
    data class ParseResult(
        val transaction: TransactionInfo?,
        val classification: HybridSmsClassifier.HybridResult?,
        val reason: String
    )
}
