package com.example.expencetrackerapp.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for formatting currency amounts.
 */
object CurrencyFormatter {
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    
    /**
     * Format amount as Indian Rupee.
     * Example: 1500.50 -> "₹1,500.50"
     */
    fun format(amount: Double): String {
        return currencyFormat.format(amount)
    }
    
    /**
     * Format amount with sign for display.
     * Positive amounts get + prefix, negative get - prefix.
     */
    fun formatWithSign(amount: Double, isExpense: Boolean = true): String {
        val formatted = format(kotlin.math.abs(amount))
        return if (isExpense) "-$formatted" else "+$formatted"
    }
    
    /**
     * Format amount in compact form for charts.
     * Example: 150000 -> "₹1.5L"
     */
    fun formatCompact(amount: Double): String {
        return when {
            amount >= 10_000_000 -> "₹${String.format("%.1f", amount / 10_000_000)}Cr"
            amount >= 100_000 -> "₹${String.format("%.1f", amount / 100_000)}L"
            amount >= 1_000 -> "₹${String.format("%.1f", amount / 1_000)}K"
            else -> format(amount)
        }
    }
}

/**
 * Utility functions for date formatting.
 */
object DateUtils {
    
    private val dayMonthFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    
    /**
     * Format timestamp to "dd MMM" format.
     * Example: "08 Dec"
     */
    fun formatDayMonth(timestamp: Long): String {
        return dayMonthFormat.format(Date(timestamp))
    }
    
    /**
     * Format timestamp to full date format.
     * Example: "08 Dec 2025"
     */
    fun formatFullDate(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }
    
    /**
     * Format timestamp to time format.
     * Example: "10:30 AM"
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    /**
     * Format timestamp to month and year.
     * Example: "December 2025"
     */
    fun formatMonthYear(timestamp: Long): String {
        return monthYearFormat.format(Date(timestamp))
    }
    
    /**
     * Get relative date string.
     * Example: "Today", "Yesterday", "08 Dec"
     */
    fun getRelativeDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        
        calendar.timeInMillis = timestamp
        val targetDay = calendar.get(Calendar.DAY_OF_YEAR)
        val targetYear = calendar.get(Calendar.YEAR)
        
        return when {
            year == targetYear && today == targetDay -> "Today"
            year == targetYear && today - targetDay == 1 -> "Yesterday"
            year == targetYear && today - targetDay < 7 -> dayOfWeekFormat.format(Date(timestamp))
            else -> formatDayMonth(timestamp)
        }
    }
    
    /**
     * Get start of current month in milliseconds.
     */
    fun getStartOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get end of current month in milliseconds.
     */
    fun getEndOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * Get start of day in milliseconds.
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get end of day in milliseconds.
     */
    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
