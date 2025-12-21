package com.example.expencetrackerapp.data.sms

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.example.expencetrackerapp.data.database.ExpenseDatabase
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.TransactionType
import com.example.expencetrackerapp.data.repository.ExpenseRepository
import com.example.expencetrackerapp.domain.categorization.CategoryMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Service to import historical SMS messages and parse them for transactions.
 */
class SmsImporter(private val context: Context) {
    
    private val categoryMatcher = CategoryMatcher()
    
    data class ImportResult(
        val totalSmsRead: Int,
        val bankSmsFound: Int,
        val transactionsImported: Int,
        val duplicatesSkipped: Int
    )
    
    /**
     * Imports all historical bank SMS messages.
     * 
     * @param daysBack Number of days to look back (default 365 = 1 year)
     * @return ImportResult with statistics
     */
    suspend fun importHistoricalSms(daysBack: Int = 365): ImportResult = withContext(Dispatchers.IO) {
        val database = ExpenseDatabase.getDatabase(context)
        val repository = ExpenseRepository(
            database.expenseDao(),
            database.categoryDao(),
            database.merchantDao()
        )
        
        // Get existing SMS bodies to avoid duplicates
        val existingExpenses = repository.allExpenses.first()
        val existingSmsBodies = existingExpenses.mapNotNull { it.smsBody }.toSet()
        
        // Get categories and mappings for categorization
        val categories = repository.allCategories.first()
        val merchantMappings = repository.allMerchantMappings.first()
        
        var totalSmsRead = 0
        var bankSmsFound = 0
        var transactionsImported = 0
        var duplicatesSkipped = 0
        
        // Calculate date threshold
        val cutoffDate = System.currentTimeMillis() - (daysBack.toLong() * 24 * 60 * 60 * 1000)
        
        // Query SMS inbox
        val cursor = querySmsInbox(cutoffDate)
        
        cursor?.use {
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            
            while (it.moveToNext()) {
                totalSmsRead++
                
                val body = it.getString(bodyIndex) ?: continue
                val address = it.getString(addressIndex) ?: continue
                val smsDate = it.getLong(dateIndex)
                
                // Check if it's a bank SMS
                if (!SmsParser.isBankSms(address)) {
                    continue
                }
                
                bankSmsFound++
                
                // Skip if already imported
                if (existingSmsBodies.contains(body)) {
                    duplicatesSkipped++
                    continue
                }
                
                // Try to parse the SMS
                val transaction = SmsParser.parseSms(body, smsDate) ?: continue
                
                // Categorize
                val matchResult = categoryMatcher.matchCategory(
                    transaction.merchant,
                    categories,
                    merchantMappings
                )
                
                // Create and save expense
                val expense = Expense(
                    amount = transaction.amount,
                    merchant = transaction.merchant,
                    category = matchResult.categoryName,
                    date = transaction.date,
                    smsBody = transaction.rawSms,
                    accountNumber = transaction.accountNumber,
                    isAutoCategorized = matchResult.isConfident,
                    transactionType = when (transaction.transactionType) {
                        TransactionInfo.TransactionType.DEBIT -> TransactionType.DEBIT
                        TransactionInfo.TransactionType.CREDIT -> TransactionType.CREDIT
                    }
                )
                
                repository.insertExpense(expense)
                transactionsImported++
            }
        }
        
        ImportResult(
            totalSmsRead = totalSmsRead,
            bankSmsFound = bankSmsFound,
            transactionsImported = transactionsImported,
            duplicatesSkipped = duplicatesSkipped
        )
    }
    
    private fun querySmsInbox(cutoffDate: Long): Cursor? {
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.BODY,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(cutoffDate.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"
        
        return try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
        } catch (e: SecurityException) {
            // Permission not granted
            null
        }
    }
}
