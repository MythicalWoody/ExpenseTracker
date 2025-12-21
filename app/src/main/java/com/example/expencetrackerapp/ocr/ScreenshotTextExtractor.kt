package com.example.expencetrackerapp.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Advanced OCR Extractor with App-Specific Rules. Supports: Samsung Wallet, PhonePe, Google Pay,
 * CRED, Paytm.
 *
 * V2 Improvements:
 * - "7-Glitch" Fix: Detects when '₹' is misread as '7'
 * - Redundancy Check: Compares multiple amount occurrences
 */
object ScreenshotTextExtractor {

    private const val TAG = "ScreenshotOCR"

    data class ExtractedPayment(
            val amount: Double?,
            val merchant: String?,
            val rawText: String,
            val confidence: Float
    )

    /** Extract payment details using specific strategies for the known app package. */
    suspend fun extractFromBitmap(bitmap: Bitmap, packageName: String?): ExtractedPayment {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer
                    .process(image)
                    .addOnSuccessListener { result ->
                        // Log full structure for debugging
                        logStructure(result)

                        val (amount, merchant) =
                                when (packageName) {
                                    "com.samsung.android.spay", "com.samsung.android.samsungpay" ->
                                            parseSamsungWallet(result)
                                    "com.phonepe.app" -> parsePhonePe(result)
                                    "com.google.android.apps.nbu.paisa.user" -> parseGPay(result)
                                    "com.dreamplug.androidapp" -> parseCred(result)
                                    else -> parseSamsungWallet(result)
                                }

                        // Final sanity check for the "7" glitch
                        val sanitizedAmount = sanitizeAmount(amount, result.text)

                        Log.i(
                                TAG,
                                "🏁 FINAL RESULT (${packageName}): Amount=₹$sanitizedAmount, Merchant='$merchant'"
                        )

                        continuation.resume(
                                ExtractedPayment(
                                        amount = sanitizedAmount,
                                        merchant = merchant,
                                        rawText = result.text,
                                        confidence =
                                                if (sanitizedAmount != null && merchant != null)
                                                        0.9f
                                                else 0.5f
                                )
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "OCR failed", e)
                        continuation.resume(ExtractedPayment(null, null, "", 0f))
                    }
        }
    }

    // ============================================================================================
    // 📱 APP SPECIFIC STRATEGIES
    // ============================================================================================

    private fun parseSamsungWallet(result: Text): Pair<Double?, String?> {
        Log.d(TAG, "--- Parsing Samsung Wallet Strategy ---")

        val sortedLines =
                result.textBlocks.flatMap { it.lines }.sortedBy { it.boundingBox?.top ?: 0 }

        Log.d(TAG, "Total Lines: ${sortedLines.size}")
        sortedLines.forEachIndexed { index, line ->
            Log.d(TAG, "Line $index: '${line.text}' at Y=${line.boundingBox?.top}")
        }

        // Use the "Fixer" logic to find the best amount candidate
        val (amount, amountIndex) = detectAndFixAmountInLines(sortedLines)

        if (amount == null) {
            Log.d(TAG, "No amount found after applying all strategies and fixes")
            return Pair(null, null)
        }

        Log.d(TAG, "🏁 FINAL DETECTED AMOUNT: ₹$amount (from line $amountIndex)")

        // Find merchant: Look backwards from amount line and collect all valid lines
        // Enhanced to handle multi-line merchant names like "Khansamas Tuckshop EY 2nd Floor"
        val merchantLines = mutableListOf<String>()

        // Scan backwards from just before the amount line
        val startScanIndex = if (amountIndex != -1) amountIndex - 1 else sortedLines.size - 1

        for (i in startScanIndex downTo maxOf(0, startScanIndex - 6)) {
            val candidateText = sortedLines[i].text.trim()

            // Skip very short lines
            if (candidateText.length < 2) continue

            // If we hit a header/status label, stop scanning
            val lower = candidateText.lowercase()
            if (lower == "transaction status" || lower == "transaction" || lower == "status") {
                Log.d(TAG, "Hit header at line $i: '$candidateText', stopping merchant scan")
                break
            }

            // Skip UI labels
            if (isUILabelSamsung(candidateText)) {
                Log.d(TAG, "Skipping UI label at line $i: '$candidateText'")
                if (merchantLines.isNotEmpty()) break
                continue
            }

            // Skip patterns that look like amounts (to avoid picking up 'T65' as a merchant)
            if (looksLikeAmountPattern(candidateText)) {
                Log.d(TAG, "Skipping amount pattern at line $i: '$candidateText'")
                continue
            }

            merchantLines.add(candidateText)
            Log.d(TAG, "Added merchant line $i: '$candidateText'")
        }

        val merchant =
                if (merchantLines.isNotEmpty()) {
                    merchantLines.reversed().joinToString(" ")
                } else {
                    null
                }

        Log.d(TAG, "Final merchant: '$merchant' (combined from ${merchantLines.size} lines)")
        return Pair(amount, cleanMerchantNameSamsung(merchant ?: ""))
    }

    /**
     * ADVANCED "FIXER" LOGIC: Scans all lines to identify the most likely amount.
     * 1. Identifies strings with actual ₹ sign.
     * 2. If missing, identifies strings that likely replaced ₹ with 'T', '7', or '2'.
     * 3. Uses context (anchors like "Sent") as a tie-breaker.
     */
    private fun detectAndFixAmountInLines(lines: List<Text.Line>): Pair<Double?, Int> {
        // --- STEP 1: Look for explicit currency symbols (Highest Confidence) ---
        for (index in lines.indices) {
            val text = lines[index].text.trim()
            if (text.contains("₹")) {
                val amt = parseAmountSamsung(text)
                if (amt != null) {
                    Log.d(TAG, "Fixer: Found explicit ₹ at line $index: '$text'")
                    return Pair(amt, index)
                }
            }
        }

        // --- STEP 2: Handle OCR Misreads (T, 7, 2) in prominent positions ---
        // We look for patterns like "T65", "t 65", "7 65.00", etc.
        val misreadRegex = Regex("""^[Tt72]\s?\d+""", RegexOption.IGNORE_CASE)

        // Priority 2a: Candidates with misread prefix near "Sent"/"Received"
        val sentIndex =
                lines.indexOfFirst {
                    it.text.equals("Sent", true) || it.text.equals("Received", true)
                }
        if (sentIndex > 0) {
            for (i in sentIndex - 1 downTo maxOf(0, sentIndex - 2)) {
                val text = lines[i].text.trim()
                if (misreadRegex.containsMatchIn(text) || looksLikeAmountPattern(text)) {
                    val amt = parseAmountSamsung(text)
                    if (amt != null) {
                        Log.d(
                                TAG,
                                "Fixer: Found misread amount near 'Sent/Received' anchor at line $i: '$text'"
                        )
                        return Pair(amt, i)
                    }
                }
            }
        }

        // Priority 2b: Global scan for misreads (e.g., 'T65' appearing anywhere)
        for (index in lines.indices) {
            val text = lines[index].text.trim()
            if (misreadRegex.containsMatchIn(text)) {
                val amt = parseAmountSamsung(text)
                if (amt != null) {
                    Log.d(TAG, "Fixer: Found potential misread amount at line $index: '$text'")
                    // We only take this if it's reasonably isolated or looks like a standalone line
                    return Pair(amt, index)
                }
            }
        }

        // --- STEP 3: Fallback to generic number patterns in anchor positions ---
        if (sentIndex > 0) {
            for (i in sentIndex - 1 downTo maxOf(0, sentIndex - 2)) {
                val text = lines[i].text.trim()
                if (looksLikeAmountPattern(text)) {
                    val amt = parseAmountSamsung(text)
                    if (amt != null) {
                        return Pair(amt, i)
                    }
                }
            }
        }

        return Pair(null, -1)
    }

    private fun looksLikeAmountPattern(text: String): Boolean {
        val cleaned = text.trim()

        // Pattern 1: Contains explicit ₹/Rs/INR with digits
        if (cleaned.contains("₹") && cleaned.any { it.isDigit() }) return true
        if (cleaned.contains("Rs", true) || cleaned.contains("INR", true)) return true

        // Pattern 2: Starts with digit or likely misread (T, 7, 2)
        // Examples: "750", "T65", "T 65", "2 50" (misread ₹50)
        val withoutMisread =
                cleaned.replace(Regex("""^[Tt72₹]\s?"""), "")
                        .replace("Rs", "", true)
                        .replace("INR", "", true)
                        .trim()

        if (!withoutMisread.any { it.isDigit() }) return false

        // Pattern: starts with digit, may have spaces/dots/commas
        val amountPattern = Regex("""^\d[\d\s.,]*\d?$""")
        if (withoutMisread.matches(amountPattern)) {
            val digitsOnly = withoutMisread.replace(Regex("""[^\d]"""), "")
            // Transaction amounts typically have 2-7 digits
            return digitsOnly.length in 2..7
        }

        return false
    }

    /** The core "fixer" that strips OCR noise and misreads from a string. */
    private fun parseAmountSamsung(text: String): Double? {
        var cleaned =
                text.trim()
                        .replace("₹", "")
                        .replace("Rs", "", true)
                        .replace("INR", "", true)
                        .replace(Regex("""^[Tt]\s?"""), "") // Aggressive 'T' stripping

        // Handle common misreads starting with 7 or 2 only if followed by space or a logical number
        // e.g., "7 65.00" -> "65.00"
        val spaceMisread = Regex("""^[72]\s+(\d)""")
        if (cleaned.contains(spaceMisread)) {
            cleaned = cleaned.replace(Regex("""^[72]\s+"""), "")
        } else if (cleaned.startsWith("7") && cleaned.length > 4) {
            // If it's a huge number starting with 7 (like 71000 for 1000), strip it
            cleaned = cleaned.substring(1)
        } else if (cleaned.startsWith("2") && cleaned.length > 3 && !cleaned.contains(".")) {
            // Heuristic for '2' (often read instead of ₹)
            // If we suspect '2' is ₹, we strip it. Only if the result is still a number.
            val candidate = cleaned.substring(1)
            if (candidate.isNotEmpty() && candidate[0].isDigit()) {
                cleaned = candidate
            }
        }

        // Final sanity wash: remove all non-numeric except . and ,
        cleaned =
                cleaned.replace(Regex("""[^\d.,]"""), "")
                        .replace(",", "")
                        .replace(Regex("""\s+"""), "")

        return try {
            val value = cleaned.toDoubleOrNull()
            if (value != null && value > 0 && value < 100000) value else null
        } catch (e: Exception) {
            null
        }
    }

    private fun isUILabelSamsung(text: String): Boolean {
        val labels =
                setOf(
                        "transaction",
                        "status",
                        "share",
                        "receipt",
                        "sent",
                        "received",
                        "paid",
                        "payment",
                        "successful",
                        "success",
                        "completed",
                        "view",
                        "details",
                        "done",
                        "back",
                        "raise",
                        "query",
                        "help",
                        "upi",
                        "lite",
                        "balance",
                        "add",
                        "powered",
                        "by"
                )

        val normalized = text.lowercase().trim()

        return labels.any { label ->
            normalized == label ||
                    normalized.startsWith(label) && normalized.length <= label.length + 2
        }
    }

    private fun cleanMerchantNameSamsung(name: String): String {
        return name.trim()
                .replace(Regex("""[^\w\s-]"""), "")
                .replace(Regex("""\s+"""), " ")
                .take(100)
    }

    private fun parsePhonePe(result: Text): Pair<Double?, String?> {
        Log.d(TAG, "--- Parsing PhonePe Strategy ---")
        val lines = result.textBlocks.flatMap { it.lines }

        // 1. Find "Paid to" anchor
        val paidToLine = lines.find { it.text.contains("Paid to", ignoreCase = true) }

        var merchant: String? = null
        var primaryAmount: Double? = null
        var debitedAmount: Double? = null

        if (paidToLine != null) {
            val sortedLines = lines.sortedBy { it.boundingBox?.top ?: 0 }
            val paidToIndex = sortedLines.indexOf(paidToLine)

            // Scan down for Merchant & Primary Amount
            for (i in paidToIndex + 1 until sortedLines.size) {
                val line = sortedLines[i]
                val text = line.text.trim()

                if (primaryAmount == null) {
                    val parsed = parseAmount(text)
                    if (parsed != null) primaryAmount = parsed
                }

                if (merchant == null && !isLabelOrCommonWord(text) && parseAmount(text) == null) {
                    if (!text.contains("@")) {
                        merchant = text
                    } else if (merchant == null) {
                        merchant = text.substringBefore("@")
                    }
                }

                if (text.contains("Payment details", ignoreCase = true) ||
                                text.contains("Transfer Details", ignoreCase = true)
                ) {
                    break
                }
                if (primaryAmount != null && merchant != null && i > paidToIndex + 5) break
            }
        }

        // 2. Find "Debited from" Amount (The Truth Source)
        // Usually "Debited from ... ₹2,095.20"
        val debitedLine = lines.find { it.text.contains("Debited from", ignoreCase = true) }
        if (debitedLine != null) {
            val debitedBlock = debitedLine.text
            debitedAmount = parseAmount(debitedBlock)

            if (debitedAmount == null) {
                // Check next line
                val sortedLines = lines.sortedBy { it.boundingBox?.top ?: 0 }
                val debitedIndex = sortedLines.indexOf(debitedLine)
                if (debitedIndex + 1 < sortedLines.size) {
                    debitedAmount = parseAmount(sortedLines[debitedIndex + 1].text)
                }
            }
        }

        Log.d(TAG, "PhonePe: Primary=$primaryAmount, Debited=$debitedAmount")

        // Intelligent Selection
        val finalAmount =
                if (debitedAmount != null) {
                    // If we have a debited amount, it's usually the most reliable
                    debitedAmount
                } else {
                    primaryAmount
                }

        // Fallbacks
        val safeAmount = finalAmount ?: extractAmountGeneric(result.text)
        val safeMerchant = merchant ?: extractMerchantGeneric(result.text)

        return Pair(safeAmount, cleanMerchantName(safeMerchant ?: ""))
    }

    private fun parseGPay(result: Text): Pair<Double?, String?> {
        val text = result.text
        var amount = extractAmountGeneric(text)

        // 1. Try Prefix Strategy "To ..." or "Payment to ..."
        var merchant: String? = null
        val toLine =
                result.textBlocks.flatMap { it.lines }.find {
                    it.text.matches(Regex("""^(To|Payment to)\s+.*""", RegexOption.IGNORE_CASE))
                }

        if (toLine != null) {
            val nameCandidate =
                    toLine.text
                            .replace(Regex("""^(To|Payment to)\s+""", RegexOption.IGNORE_CASE), "")
                            .trim()
            if (nameCandidate.length > 2) merchant = nameCandidate
        }

        // 2. Fallback: Spatial Strategy (for "Transaction status" screens)
        if (merchant == null) {
            merchant = findMerchantByLayout(result)
        }

        // If we still found nothing, check for generic patterns
        if (merchant == null) {
            merchant = extractMerchantGeneric(text)
        }

        return Pair(amount, merchant)
    }

    private fun parseCred(result: Text): Pair<Double?, String?> {
        Log.d(TAG, "--- Parsing CRED Strategy ---")
        val sortedLines =
                result.textBlocks.flatMap { it.lines }.sortedBy { it.boundingBox?.top ?: 0 }

        var merchant: String? = null

        // Check first 5 lines
        for (i in 0..minOf(5, sortedLines.size - 1)) {
            val text = sortedLines[i].text.trim()
            if (!isLabelOrCommonWord(text) && text.length > 3 && parseAmount(text) == null) {
                // Ensure it's not "Receipt" or "Success"
                if (!text.equals("RECEIPT", true) && !text.contains("Success", true)) {
                    merchant = text
                    break
                }
            }
        }

        val amount = extractAmountGeneric(result.text)
        return Pair(amount, cleanMerchantName(merchant ?: ""))
    }

    private fun parseGeneric(result: Text): Pair<Double?, String?> {
        val amount = extractAmountGeneric(result.text)
        // Try spatial layout first as it's often more accurate for "receipt" style screens
        val merchant = findMerchantByLayout(result) ?: extractMerchantGeneric(result.text)
        return Pair(amount, merchant)
    }

    // ============================================================================================
    // 🧩 SPATIAL ANALYSIS (The "Look Above" Strategy)
    // ============================================================================================

    /**
     * Finds Merchant Name by looking physically ABOVE the detected amount. Works well for GPay/UPI
     * Lite "Transaction Status" screens. Enhanced to handle multi-line merchant names.
     */
    private fun findMerchantByLayout(result: Text): String? {
        // 1. Sort all lines vertically (Top to Bottom)
        val sortedLines =
                result.textBlocks.flatMap { it.lines }.sortedBy { it.boundingBox?.top ?: 0 }

        // 2. Identify the likely Amount Line
        // It usually has "₹" or is the confirmed amount value
        val amountLine =
                sortedLines.firstOrNull {
                    val text = it.text.trim()
                    (text.contains("₹") || text.contains("Rs", true)) && parseAmount(text) != null
                }
                        ?: return null

        val amountIndex = sortedLines.indexOf(amountLine)
        Log.d(
                TAG,
                "findMerchantByLayout: Found amount line at index $amountIndex: '${amountLine.text}'"
        )

        if (amountIndex <= 0) return null

        // 3. Scan upwards from the amount and collect valid merchant lines
        // We might have a multi-line merchant name, so collect all consecutive valid lines
        val merchantLines = mutableListOf<String>()

        for (i in amountIndex - 1 downTo maxOf(0, amountIndex - 5)) {
            val line = sortedLines[i]
            val text = line.text.trim()

            Log.d(TAG, "findMerchantByLayout: Checking line $i: '$text'")

            // If we hit a header/label, stop scanning
            if (text.lowercase() == "transaction status" ||
                            text.lowercase() == "transaction" ||
                            text.lowercase() == "status"
            ) {
                Log.d(TAG, "findMerchantByLayout: Hit header, stopping scan")
                break
            }

            // Check if this line looks like part of a merchant name
            if (isValidMerchantLine(text)) {
                merchantLines.add(text)
                Log.d(TAG, "findMerchantByLayout: Added valid line to merchant name")
            } else {
                // If we already have merchant lines and hit an invalid line, stop
                if (merchantLines.isNotEmpty()) {
                    Log.d(TAG, "findMerchantByLayout: Already have merchant lines, stopping")
                    break
                }
            }
        }

        // 4. Combine the merchant lines (they're in reverse order)
        if (merchantLines.isNotEmpty()) {
            val combinedMerchant = merchantLines.reversed().joinToString(" ")
            val cleanedName = cleanMerchantName(combinedMerchant)
            Log.d(
                    TAG,
                    "findMerchantByLayout: Combined merchant: '$combinedMerchant' -> Cleaned: '$cleanedName'"
            )
            return cleanedName
        }

        Log.d(TAG, "findMerchantByLayout: No merchant found")
        return null
    }

    private fun isValidMerchantLine(text: String): Boolean {
        if (text.length < 2) {
            Log.d(TAG, "isValidMerchantLine: '$text' rejected - too short")
            return false
        }

        if (isLabelOrCommonWord(text)) {
            Log.d(TAG, "isValidMerchantLine: '$text' rejected - is label or common word")
            return false
        }

        // Specific checks for Transaction Status screens
        val lower = text.lowercase()
        if (lower == "transaction status") {
            Log.d(TAG, "isValidMerchantLine: '$text' rejected - is 'transaction status'")
            return false
        }
        if (lower == "sent") {
            Log.d(TAG, "isValidMerchantLine: '$text' rejected - is 'sent'")
            return false
        }
        if (lower.startsWith("paid to")) {
            Log.d(TAG, "isValidMerchantLine: '$text' rejected - starts with 'paid to'")
            return false
        }

        // It shouldn't be another amount
        if (parseAmount(text) != null) {
            Log.d(TAG, "isValidMerchantLine: '$text' rejected - looks like an amount")
            return false
        }

        Log.d(TAG, "isValidMerchantLine: '$text' ACCEPTED as valid merchant line")
        return true
    }

    // ============================================================================================
    // 🛠️ UTILITIES
    // ============================================================================================

    /**
     * Fixes the "7" glitch where '₹' is read as '7'. Example: 72095.0 -> 2095.0
     *
     * SAFE VERSION: Only corrects if:
     * 1. The '7' is separated by space in raw text (handled by regex previously)
     * 2. OR The corrected amount (2095) is found explicitly elsewhere in the text
     */
    private fun sanitizeAmount(amount: Double?, fullRawText: String): Double? {
        if (amount == null) return null

        // Glitch Type 1: Huge Number starting with 7 (e.g. 72095)
        val str = amount.toLong().toString()
        if (amount > 10000 && str.startsWith("7")) {
            val fixedStr = str.substring(1)
            val fixedVal = fixedStr.toDoubleOrNull()

            // If the stripped amount is plausible (>0)
            if (fixedVal != null && fixedVal > 0) {
                // CORROBORATION CHECK:
                // Does "2095" or "2,095" appear independently in the text?
                // We look for the fixed string surrounded by non-digits
                val fixedPattern =
                        Regex("""(^|\D)${fixedStr}(\D|$)""") // e.g. " 2095 " or "Rs.2095"

                // We also check formatted version "2,095"
                val formattedFixed =
                        try {
                            java.text.NumberFormat.getIntegerInstance().format(fixedVal.toLong())
                        } catch (e: Exception) {
                            fixedStr
                        }

                if (fixedPattern.containsMatchIn(fullRawText) ||
                                fullRawText.contains(formattedFixed)
                ) {
                    Log.w(TAG, "⚠️ '7' Glitch detected & VERIFIED! Correcting $amount -> $fixedVal")
                    return fixedVal + (amount - amount.toLong())
                } else {
                    Log.d(
                            TAG,
                            "❓ Potential '7' glitch ($amount) but could not verify '$fixedVal' elsewhere. Keeping original."
                    )
                }
            }
        }

        return amount
    }

    private fun extractAmountGeneric(text: String): Double? {
        // Pre-process: Replace "7 " at start of lines with "₹ " to fix the glitch proactively
        // This handles "7 2095.00" -> "₹ 2095.00"
        val fixedText = text.replace(Regex("""(^|\n)7\s+(\d)"""), "$1₹ $2")

        // High confidence: "₹ 123"
        val rsPattern = Regex("""[₹Rs]\s?([\d,]+\.?\d{0,2})""", RegexOption.IGNORE_CASE)
        val match = rsPattern.find(fixedText)
        if (match != null) {
            return parseAmount(match.groupValues[1])
        }

        // Low confidence: Just large numbers
        val numberPattern = Regex("""(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)""")
        return numberPattern.findAll(fixedText).mapNotNull { parseAmount(it.value) }.maxByOrNull {
            it
        }
    }

    private fun extractMerchantGeneric(text: String): String? {
        val patterns =
                listOf(
                        Regex("""(?:Paid to|To)[:\s]+([^\n]+)""", RegexOption.IGNORE_CASE),
                        Regex("""(?:Sent to)[:\s]+([^\n]+)""", RegexOption.IGNORE_CASE),
                        Regex("""([a-zA-Z0-9\.\-_]+)@[a-zA-Z]+""")
                )
        for (pattern in patterns) {
            pattern.find(text)?.let {
                val val1 = it.groupValues[1].trim()
                if (val1.contains("@")) return val1.substringBefore("@")
                return val1
            }
        }
        return null
    }

    private fun hasPotentialAmount(text: String): Boolean {
        // Must contain digit
        if (!text.any { it.isDigit() }) return false

        // Must NOT look like an account mask (XXXX1234)
        if (text.contains("X", ignoreCase = true) || text.contains("*")) return false

        return (text.contains(".") ||
                text.contains(",") ||
                text.contains("₹") ||
                text.contains("Rs", true))
    }

    private fun parseAmount(text: String): Double? {
        // Critical: Ignore strings that look like account numbers (e.g. XXXXXX6532)
        if (text.contains("X", ignoreCase = true) || text.contains("*")) {
            Log.d(TAG, "Ignoring masked account number: $text")
            return null
        }

        // Strip non-numeric except dot and comma
        val clean = text.replace(Regex("""[^0-9.,]"""), "")
        return try {
            val value = clean.replace(",", "").toDouble()
            // Filter out:
            // 1. Zero or negative
            // 2. Unusually large (> 10 Lakhs is suspicious for this context)
            // 3. Simple 4-digit integers that might be years (2025) or PINs, unless they have
            // decimals
            if (value > 0 && value < 1000000) {
                // If it's an integer between 1990 and 2030, it's likely a year unless context
                // proves otherwise
                if (value % 1.0 == 0.0 && value >= 2020 && value <= 2030) return null
                value
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanMerchantName(name: String): String? {
        val banned =
                listOf(
                        "receipt",
                        "transaction",
                        "successful",
                        "share",
                        "close",
                        "paid",
                        "to",
                        "sent",
                        "from",
                        "upi",
                        "ref",
                        "date",
                        "time",
                        "powered",
                        "by",
                        "details",
                        "payment",
                        "bank",
                        "wallet",
                        "transfer",
                        "money"
                )
        val clean = name.trim().replace(Regex("""[^\w\s&]"""), "")
        Log.d(TAG, "cleanMerchantName: Input='$name' -> Cleaned='$clean' (length=${clean.length})")

        if (clean.length < 2) {
            Log.d(TAG, "cleanMerchantName: Rejected - too short")
            return null
        }

        if (banned.any { clean.equals(it, ignoreCase = true) }) {
            Log.d(TAG, "cleanMerchantName: Rejected - matches banned word")
            return null
        }

        return clean
    }

    private fun isLabelOrCommonWord(text: String): Boolean {
        val commons =
                setOf(
                        "receipt",
                        "share",
                        "transaction",
                        "successful",
                        "id",
                        "date",
                        "time",
                        "paid",
                        "sent",
                        "from",
                        "to",
                        "notes",
                        "upi",
                        "ref",
                        "no",
                        "status",
                        "confirm",
                        "repeat",
                        "close",
                        "done",
                        "details",
                        "payment",
                        "history",
                        "view",
                        "split",
                        "expense",
                        "raise",
                        "query",
                        "help",
                        "support",
                        "powered",
                        "by",
                        "bank",
                        "wallet",
                        "credit",
                        "card",
                        "total",
                        "amount",
                        "transfer",
                        "debited",
                        "completed",
                        "success"
                )
        val normalized = text.lowercase().replace(Regex("""\W"""), "")
        return commons.contains(normalized) ||
                commons.any { normalized.startsWith(it) && normalized.length < it.length + 3 }
    }

    private fun logStructure(result: Text) {
        val sb = StringBuilder()
        sb.append("\n--- OCR STRUCTURE ---\n")
        result.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                sb.append("[${line.boundingBox?.toShortString()}] ${line.text}\n")
            }
        }
        sb.append("---------------------\n")
        Log.d(TAG, sb.toString())
    }
}
