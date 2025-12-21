package com.example.expencetrackerapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single expense transaction.
 * Can be auto-detected from SMS or manually entered.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Transaction amount in INR */
    val amount: Double,
    
    /** Merchant/payee name extracted from SMS or manually entered */
    val merchant: String,
    
    /** Category name (e.g., "Food", "Transport") */
    val category: String,
    
    /** Timestamp of the transaction in milliseconds */
    val date: Long,
    
    /** Original SMS body if auto-detected, null for manual entries */
    val smsBody: String? = null,
    
    /** Last 4 digits of account/card number if available */
    val accountNumber: String? = null,
    
    /** Whether this expense was auto-categorized (true) or manually set (false) */
    val isAutoCategorized: Boolean = false,
    
    /** Optional note added by user */
    val note: String? = null,
    
    /** Transaction type: DEBIT or CREDIT */
    val transactionType: TransactionType = TransactionType.DEBIT
)

enum class TransactionType {
    DEBIT,
    CREDIT
}
