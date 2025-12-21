package com.example.expencetrackerapp.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.data.database.ExpenseDatabase
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.TransactionType
import com.example.expencetrackerapp.ui.components.getCategoryIcon
import com.example.expencetrackerapp.ui.theme.ExpenceTrackerAppTheme
import com.example.expencetrackerapp.ui.theme.Primary
import com.example.expencetrackerapp.ui.theme.getCategoryColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Quick Add Activity for the home screen widget.
 * Minimal UI for fast expense entry.
 */
class QuickAddActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ExpenceTrackerAppTheme {
                QuickAddScreen(
                    onSave = { amount, merchant, category ->
                        saveExpense(amount, merchant, category)
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
    
    private fun saveExpense(amount: Double, merchant: String, category: String) {
        val database = ExpenseDatabase.getDatabase(applicationContext)
        
        CoroutineScope(Dispatchers.IO).launch {
            val expense = Expense(
                amount = amount,
                merchant = merchant,
                category = category,
                date = System.currentTimeMillis(),
                smsBody = null,
                accountNumber = "Manual",
                isAutoCategorized = false,
                transactionType = TransactionType.DEBIT
            )
            database.expenseDao().insertExpense(expense)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(
    onSave: (amount: Double, merchant: String, category: String) -> Unit,
    onCancel: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var showCategories by remember { mutableStateOf(false) }
    
    val categories = listOf(
        "Food & Dining", "Shopping", "Transport", "Fashion",
        "Entertainment", "Bills & Utilities", "Health", "Education",
        "Travel", "Investments", "Transfers", "Others"
    )
    
    val isValid = amount.toDoubleOrNull() != null && 
                  amount.toDouble() > 0 &&
                  merchant.isNotBlank() &&
                  selectedCategory.isNotBlank()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Quick Add",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Amount (large, prominent)
            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                        amount = it
                    }
                },
                label = { Text("Amount") },
                prefix = { Text("₹ ", style = MaterialTheme.typography.headlineSmall) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Merchant
            OutlinedTextField(
                value = merchant,
                onValueChange = { 
                    merchant = it
                    if (it.isNotBlank() && !showCategories) {
                        showCategories = true
                    }
                },
                label = { Text("Where did you spend?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Category grid (shows after merchant is entered)
            AnimatedVisibility(visible = showCategories || merchant.isNotBlank()) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            QuickCategoryItem(
                                category = category,
                                isSelected = selectedCategory == category,
                                onClick = { selectedCategory = category }
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
                    if (amountValue > 0 && merchant.isNotBlank() && selectedCategory.isNotBlank()) {
                        onSave(amountValue, merchant, selectedCategory)
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickCategoryItem(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(category)
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) categoryColor.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) categoryColor
                    else categoryColor.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(category),
                contentDescription = null,
                tint = if (isSelected) Color.White else categoryColor,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = category.split(" ").first(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
