package com.example.expencetrackerapp.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.data.database.ExpenseDatabase
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.TransactionType
import com.example.expencetrackerapp.domain.categorization.CategoryMatcher
import com.example.expencetrackerapp.ui.theme.ExpenceTrackerAppTheme
import com.example.expencetrackerapp.ui.theme.Primary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Activity to handle shared text/images from other apps.
 * 
 * Allows quick expense entry when user shares:
 * - Payment confirmation text
 * - Screenshot text (via Share > Copy to Expense Tracker)
 * - Any text containing amount
 */
class ShareReceiverActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedText = extractSharedText(intent)
        val extractedAmount = extractAmountFromText(sharedText)
        val extractedMerchant = extractMerchantFromText(sharedText)
        
        setContent {
            ExpenceTrackerAppTheme {
                ShareReceiverScreen(
                    sharedText = sharedText,
                    initialAmount = extractedAmount,
                    initialMerchant = extractedMerchant,
                    onSave = { amount, merchant, category ->
                        saveExpense(amount, merchant, category, sharedText)
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
    
    private fun extractSharedText(intent: Intent): String {
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                } else {
                    ""
                }
            }
            else -> ""
        }
    }
    
    private fun extractAmountFromText(text: String): String {
        val patterns = listOf(
            Regex("""[₹₨]\s?(\d{1,3}(?:,?\d{3})*(?:\.\d{2})?)"""),
            Regex("""Rs\.?\s?(\d{1,3}(?:,?\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""(\d{1,3}(?:,?\d{3})*(?:\.\d{2})?)\s?(?:paid|sent|debited)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].replace(",", "")
            }
        }
        return ""
    }
    
    private fun extractMerchantFromText(text: String): String {
        val patterns = listOf(
            Regex("""(?:paid to|sent to|to)\s+(.+?)(?:\s+on|\s+via|\.|$)""", RegexOption.IGNORE_CASE),
            Regex("""at\s+(.+?)(?:\s+on|\s+using|\.|$)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                if (merchant.length in 2..40) {
                    return merchant
                }
            }
        }
        return ""
    }
    
    private fun saveExpense(amount: Double, merchant: String, category: String, sharedText: String) {
        val database = ExpenseDatabase.getDatabase(applicationContext)
        
        CoroutineScope(Dispatchers.IO).launch {
            val expense = Expense(
                amount = amount,
                merchant = merchant,
                category = category,
                date = System.currentTimeMillis(),
                smsBody = "[Shared] $sharedText",
                accountNumber = "Shared",
                isAutoCategorized = false,
                transactionType = TransactionType.DEBIT
            )
            database.expenseDao().insertExpense(expense)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReceiverScreen(
    sharedText: String,
    initialAmount: String,
    initialMerchant: String,
    onSave: (amount: Double, merchant: String, category: String) -> Unit,
    onCancel: () -> Unit
) {
    var amount by remember { mutableStateOf(initialAmount) }
    var merchant by remember { mutableStateOf(initialMerchant) }
    var selectedCategory by remember { mutableStateOf("Others") }
    
    val categories = listOf(
        "Food & Dining", "Shopping", "Transport", "Fashion",
        "Entertainment", "Bills & Utilities", "Health", "Others"
    )
    
    val isValid = amount.toDoubleOrNull() != null && 
                  amount.toDouble() > 0 &&
                  merchant.isNotBlank()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Add Expense",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Shared text preview
            if (sharedText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Shared Content",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sharedText.take(150) + if (sharedText.length > 150) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                        amount = it
                    }
                },
                label = { Text("Amount") },
                prefix = { Text("₹ ") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Merchant
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Payee") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Category chips in a flow
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.chunked(4).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { 
                                    Text(
                                        category.split(" ").first(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save button
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (amountValue > 0 && merchant.isNotBlank()) {
                        onSave(amountValue, merchant, selectedCategory)
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Expense", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
