package com.example.expencetrackerapp.data.sms

/**
 * Represents a parsed bank transaction from SMS.
 */
data class TransactionInfo(
    val amount: Double,
    val merchant: String,
    val accountNumber: String?,
    val transactionType: TransactionType,
    val rawSms: String,
    val date: Long = System.currentTimeMillis()
) {
    enum class TransactionType {
        DEBIT,
        CREDIT
    }
}
