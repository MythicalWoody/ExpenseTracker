package com.example.expencetrackerapp.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.expencetrackerapp.data.database.ExpenseDatabase
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.TransactionType
import com.example.expencetrackerapp.data.repository.ExpenseRepository
import com.example.expencetrackerapp.domain.categorization.CategoryMatcher
import com.example.expencetrackerapp.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that listens for incoming SMS messages.
 * When a bank SMS is detected, it parses and saves the transaction.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {
    
    private val categoryMatcher = CategoryMatcher()
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }
        
        for (smsMessage in messages) {
            val sender = smsMessage.originatingAddress ?: continue
            val body = smsMessage.messageBody ?: continue
            
            // Only process if it looks like a bank SMS
            if (!SmsParser.isBankSms(sender)) {
                continue
            }
            
            // Try to parse the transaction
            val transaction = SmsParser.parseSms(body) ?: continue
            
            // Process in background
            CoroutineScope(Dispatchers.IO).launch {
                processTransaction(context, transaction)
            }
        }
    }
    
    private suspend fun processTransaction(context: Context, transaction: TransactionInfo) {
        val database = ExpenseDatabase.getDatabase(context)
        val repository = ExpenseRepository(
            database.expenseDao(),
            database.categoryDao(),
            database.merchantDao()
        )
        
        // Get categories and merchant mappings for categorization
        val categories = repository.allCategories.first()
        val merchantMappings = repository.allMerchantMappings.first()
        
        // Match category
        val matchResult = categoryMatcher.matchCategory(
            transaction.merchant,
            categories,
            merchantMappings
        )
        
        // Create expense
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
        
        // Save to database
        val expenseId = repository.insertExpense(expense)
        
        // If categorization confidence is low, show notification
        if (!matchResult.isConfident) {
            NotificationHelper.showCategorizationNeeded(
                context = context,
                expenseId = expenseId,
                merchant = transaction.merchant,
                amount = transaction.amount
            )
        }
    }
}
