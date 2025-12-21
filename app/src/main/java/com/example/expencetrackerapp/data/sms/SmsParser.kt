package com.example.expencetrackerapp.data.sms

import java.text.SimpleDateFormat
import java.util.*

/**
 * SMS Parser that extracts transaction details from Indian bank SMS messages.
 * Supports multiple banks including ICICI, HDFC, SBI, Axis, Kotak, and more.
 * 
 * Includes intelligent spam/promotional message filtering.
 */
object SmsParser {
    
    // Bank sender ID patterns (common prefixes)
    private val bankSenderPatterns = listOf(
        "ICICI", "HDFC", "SBI", "AXIS", "KOTAK", "PNB", "BOB", "IDFC", "INDUS",
        "CITI", "HSBC", "SC", "YES", "RBL", "FEDERAL", "BOI", "IOB", "CANARA",
        "UNION", "IDBI", "BANDHAN", "PAYTM", "GPAY", "PHONEPE", "AMAZON",
        "HDFCBK", "SBIBNK", "ICICIB", "AXISBK", "KOTAKB"
    )
    
    // ===== SPAM DETECTION PATTERNS =====
    
    // Strong spam indicators - if found, almost certainly spam
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
    
    // URL patterns in spam
    private val spamUrlPatterns = listOf(
        "hdfcbk.io", "icicibank.page.link", "sbicard.in", "axisb.in",
        "i.cplry.com", "bit.ly", "goo.gl", "tinyurl", "shorturl",
        "click here", "tap here", "visit:", "link:"
    )
    
    // T&C patterns (promotional messages always have T&C)
    private val tncPatterns = listOf(
        "t&c", "t & c", "tnc", "terms apply", "terms and conditions",
        "*conditions apply", "conditions apply", "*t&c"
    )
    
    // Loan/EMI promotional patterns
    private val loanPromoPatterns = listOf(
        "@\\s*\\d+\\.\\d+%\\s*p\\.?m".toRegex(RegexOption.IGNORE_CASE),  // @1.4% p.m
        "\\d+\\.\\d+%\\s*(?:per month|p\\.?m|monthly)".toRegex(RegexOption.IGNORE_CASE),
        "emi\\s*of\\s*rs".toRegex(RegexOption.IGNORE_CASE),
        "loan\\s*(?:of)?\\s*rs\\.?\\s*\\d".toRegex(RegexOption.IGNORE_CASE)
    )
    
    // ===== TRANSACTION DETECTION PATTERNS =====
    
    // Strong transaction indicators - confirms it's a real transaction
    private val strongTransactionIndicators = listOf(
        "debited from", "credited to", "spent on", "payment of rs",
        "your a/c", "your account", "trf to", "transferred to",
        "upi ref", "refno", "txn id", "transaction id",
        "if not you", "if not u", "not you?", "not u?",
        "available balance", "avl bal", "balance is"
    )
    
    // ===== AMOUNT EXTRACTION PATTERNS =====
    private val amountPatterns = listOf(
        """(?:Spent|spent)\s+Rs\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE),
        """Rs\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)\s+(?:debited|credited|spent)""".toRegex(RegexOption.IGNORE_CASE),
        """debited\s+by\s+([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE),
        """debit\s+of\s+Rs\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE),
        """(?:INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE),
        """credited\s+(?:by\s+)?(?:Rs\.?\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""".toRegex(RegexOption.IGNORE_CASE)
    )
    
    // ===== ACCOUNT/CARD NUMBER PATTERNS =====
    private val accountPatterns = listOf(
        """Card\s+(\d{4})""".toRegex(RegexOption.IGNORE_CASE),
        """A/[cC]\s*[Xx]*(\d{4})""".toRegex(),
        """(?:ENDING|ending)\s+(?:WITH\s+)?(\d{4})""".toRegex(RegexOption.IGNORE_CASE),
        """XX(\d{4})""".toRegex(),
        """A/C\s+X(\d{4})""".toRegex(RegexOption.IGNORE_CASE)
    )
    
    // ===== MERCHANT EXTRACTION PATTERNS =====
    private val merchantPatterns = listOf(
        """At\s+([A-Za-z0-9\s\.\-&']+?)\s+On\s+\d""".toRegex(RegexOption.IGNORE_CASE),
        """trf\s+to\s+([A-Za-z0-9\s\.\-&']+?)(?:\s+Refno|\s+If\s+not|$)""".toRegex(RegexOption.IGNORE_CASE),
        """from\s+VPA\s+([a-zA-Z0-9\.\-@]+)""".toRegex(RegexOption.IGNORE_CASE),
        """to\s+VPA\s+([a-zA-Z0-9\.\-@]+)""".toRegex(RegexOption.IGNORE_CASE),
        """AutoPay\s+for\s+([A-Za-z0-9\s\.\-&']+?)(?:\s+debit|\s+of|$)""".toRegex(RegexOption.IGNORE_CASE),
        """(?:^|\s)at\s+([A-Za-z0-9\s\.\-&']+?)(?:\s+on|\s+ref|\.|,|$)""".toRegex(RegexOption.IGNORE_CASE),
        """(?:paid\s+to|sent\s+to|transferred\s+to)\s+([A-Za-z0-9\s\.\-&']+?)(?:\s+on|\s+ref|\.|$)""".toRegex(RegexOption.IGNORE_CASE)
    )
    
    // ===== DATE EXTRACTION PATTERNS =====
    private val datePatterns = listOf(
        """On\s+(\d{4}-\d{2}-\d{2})""".toRegex(RegexOption.IGNORE_CASE),
        """on\s+(\d{1,2}-\d{1,2}-\d{2,4})""".toRegex(RegexOption.IGNORE_CASE),
        """on\s+date\s+(\d{1,2}[A-Za-z]{3}\d{2})""".toRegex(RegexOption.IGNORE_CASE),
        """ON\s+(\d{1,2}-\d{1,2}-\d{4})""".toRegex()
    )
    
    // ===== TRANSACTION TYPE KEYWORDS =====
    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "withdrawn", "purchase",
        "txn", "transaction", "sent", "trf to", "transferred to"
    )
    
    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposited", "refund",
        "cashback", "reversed", "payment received", "payment of"
    )
    
    // Skip these - they are scheduled/upcoming, not actual transactions
    private val skipKeywords = listOf(
        "is scheduled", "will be debited", "upcoming", "reminder",
        "due date", "payment due", "minimum due", "total due"
    )
    
    /**
     * Checks if the sender ID looks like a bank SMS source.
     */
    fun isBankSms(senderId: String): Boolean {
        val upperSender = senderId.uppercase()
        return bankSenderPatterns.any { pattern ->
            upperSender.contains(pattern)
        }
    }
    
    /**
     * Comprehensive spam detection with scoring system.
     * Returns true if the message appears to be spam/promotional.
     */
    fun isSpamOrPromotional(smsBody: String): Boolean {
        val lower = smsBody.lowercase()
        var spamScore = 0
        var transactionScore = 0
        
        // Check strong spam indicators (+3 each)
        strongSpamIndicators.forEach { indicator ->
            if (lower.contains(indicator)) {
                spamScore += 3
            }
        }
        
        // Check spam URLs (+5 each - very strong indicator)
        spamUrlPatterns.forEach { urlPattern ->
            if (lower.contains(urlPattern)) {
                spamScore += 5
            }
        }
        
        // Check T&C patterns (+4 - promotional messages always have T&C)
        tncPatterns.forEach { tnc ->
            if (lower.contains(tnc)) {
                spamScore += 4
            }
        }
        
        // Check loan promo patterns (+4 each)
        loanPromoPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(smsBody)) {
                spamScore += 4
            }
        }
        
        // Check strong transaction indicators (-5 each - reduces spam score)
        strongTransactionIndicators.forEach { indicator ->
            if (lower.contains(indicator)) {
                transactionScore += 5
            }
        }
        
        // Check if message contains an actual account reference (-3)
        accountPatterns.forEach { pattern ->
            if (pattern.find(smsBody) != null) {
                transactionScore += 3
            }
        }
        
        // Final decision: spam if spam score exceeds transaction score significantly
        // A genuine transaction with spam score 0 and transaction score 10+ is clearly valid
        // A promo with spam score 15+ and transaction score 0 is clearly spam
        return spamScore > transactionScore + 5
    }
    
    /**
     * Parses a bank SMS and extracts transaction details.
     * Returns null if the SMS doesn't appear to be a valid transaction message.
     */
    fun parseSms(smsBody: String, smsDate: Long? = null): TransactionInfo? {
        val lower = smsBody.lowercase()
        
        // Skip scheduled/upcoming transactions
        if (skipKeywords.any { lower.contains(it) }) {
            return null
        }
        
        // Skip spam/promotional messages
        if (isSpamOrPromotional(smsBody)) {
            return null
        }
        
        // Skip if doesn't look like a transaction SMS
        if (!looksLikeTransactionSms(smsBody)) {
            return null
        }
        
        val amount = extractAmount(smsBody) ?: return null
        val merchant = extractMerchant(smsBody) ?: "Unknown"
        val accountNumber = extractAccountNumber(smsBody)
        val transactionType = determineTransactionType(smsBody)
        val transactionDate = extractDate(smsBody) ?: smsDate ?: System.currentTimeMillis()
        
        return TransactionInfo(
            amount = amount,
            merchant = cleanMerchant(merchant),
            accountNumber = accountNumber,
            transactionType = transactionType,
            rawSms = smsBody,
            date = transactionDate
        )
    }
    
    private fun looksLikeTransactionSms(smsBody: String): Boolean {
        val lower = smsBody.lowercase()
        
        // Must have some transaction indicator
        val hasTransactionIndicator = (debitKeywords + creditKeywords).any { lower.contains(it) }
        
        // Or "Spent Rs" pattern (HDFC cards)
        val hasSpentPattern = lower.contains("spent rs")
        
        // Must have an amount
        val hasAmount = amountPatterns.any { it.find(smsBody) != null }
        
        // Exclude OTP messages
        val isOtp = lower.contains("otp") || lower.contains("one time password") || lower.contains("cvv")
        
        return (hasTransactionIndicator || hasSpentPattern) && hasAmount && !isOtp
    }
    
    private fun extractAmount(smsBody: String): Double? {
        for (pattern in amountPatterns) {
            val match = pattern.find(smsBody)
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
    
    private fun extractAccountNumber(smsBody: String): String? {
        for (pattern in accountPatterns) {
            val match = pattern.find(smsBody)
            if (match != null) {
                return "XX" + match.groupValues[1]
            }
        }
        return null
    }
    
    private fun extractMerchant(smsBody: String): String? {
        for (pattern in merchantPatterns) {
            val match = pattern.find(smsBody)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty() && merchant.length >= 2) {
                    return merchant
                }
            }
        }
        
        // Special case: HDFC payment received
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
                if (parsedDate != null) {
                    return parsedDate
                }
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
            SimpleDateFormat("ddMMMyy", Locale.getDefault()),
            SimpleDateFormat("dd-M-yyyy", Locale.getDefault())
        )
        
        for (format in formats) {
            try {
                format.isLenient = false
                val date = format.parse(dateStr)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }
    
    private fun cleanMerchant(merchant: String): String {
        return merchant
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""[^\w\s\.\-&'@]"""), "")
            .trim()
            .take(50)
            .ifEmpty { "Unknown" }
    }
    
    private fun determineTransactionType(smsBody: String): TransactionInfo.TransactionType {
        val lower = smsBody.lowercase()
        
        if (lower.contains("spent")) {
            return TransactionInfo.TransactionType.DEBIT
        }
        
        if (lower.contains("payment") && lower.contains("received")) {
            return TransactionInfo.TransactionType.CREDIT
        }
        
        if (creditKeywords.any { lower.contains(it) }) {
            if (!lower.contains("not credited") && !lower.contains("failed")) {
                return TransactionInfo.TransactionType.CREDIT
            }
        }
        
        return TransactionInfo.TransactionType.DEBIT
    }
}
