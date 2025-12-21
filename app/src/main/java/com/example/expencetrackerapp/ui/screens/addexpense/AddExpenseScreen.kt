package com.example.expencetrackerapp.ui.screens.addexpense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.data.database.entities.Category
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.TransactionType
import com.example.expencetrackerapp.ui.components.getCategoryIcon
import com.example.expencetrackerapp.ui.theme.Primary
import com.example.expencetrackerapp.ui.theme.Secondary
import com.example.expencetrackerapp.ui.theme.getCategoryColor
import com.example.expencetrackerapp.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    expenseId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val categories by viewModel.allCategories.collectAsState()
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(expenseId != null) }
    var existingExpense by remember { mutableStateOf<Expense?>(null) }
    
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    
    // Load existing expense if editing
    LaunchedEffect(expenseId) {
        if (expenseId != null && expenseId > 0) {
            val expense = viewModel.getExpenseById(expenseId)
            expense?.let {
                existingExpense = it
                amount = it.amount.toString()
                merchant = it.merchant
                selectedCategory = it.category
                note = it.note ?: ""
                isExpense = it.transactionType == TransactionType.DEBIT
            }
            isLoading = false
        } else {
            isLoading = false
        }
    }
    
    // Auto-suggest category when merchant changes
    LaunchedEffect(merchant) {
        if (merchant.isNotBlank() && selectedCategory.isEmpty() && existingExpense == null) {
            val suggested = viewModel.suggestCategory(merchant)
            if (suggested != "Others") {
                selectedCategory = suggested
            }
        }
    }
    
    val isValid = amount.toDoubleOrNull() != null && 
                  amount.toDouble() > 0 && 
                  merchant.isNotBlank() && 
                  selectedCategory.isNotBlank()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (existingExpense != null) "Edit Expense" else "Add Expense") 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isValid) {
                                val amountValue = amount.toDouble()
                                if (existingExpense != null) {
                                    viewModel.updateExpense(
                                        existingExpense!!.copy(
                                            amount = amountValue,
                                            merchant = merchant,
                                            category = selectedCategory,
                                            note = note.ifBlank { null },
                                            transactionType = if (isExpense) TransactionType.DEBIT else TransactionType.CREDIT,
                                            isAutoCategorized = false
                                        )
                                    )
                                } else {
                                    viewModel.addExpense(
                                        amount = amountValue,
                                        merchant = merchant,
                                        category = selectedCategory,
                                        note = note.ifBlank { null },
                                        transactionType = if (isExpense) TransactionType.DEBIT else TransactionType.CREDIT
                                    )
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = isValid
                    ) {
                        Icon(
                            Icons.Filled.Check, 
                            contentDescription = "Save",
                            tint = if (isValid) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Transaction Type Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = { Text("Expense") },
                        leadingIcon = if (isExpense) {
                            { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Secondary.copy(alpha = 0.2f),
                            selectedLabelColor = Secondary
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = { Text("Income") },
                        leadingIcon = if (!isExpense) {
                            { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary.copy(alpha = 0.2f),
                            selectedLabelColor = Primary
                        )
                    )
                }
                
                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amount = newValue
                        }
                    },
                    label = { Text("Amount") },
                    prefix = { Text("₹ ") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                
                // Merchant Input
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Payee") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        CategorySelectionItem(
                            category = category,
                            isSelected = selectedCategory == category.name,
                            onClick = { selectedCategory = category.name }
                        )
                    }
                }
                
                // Note Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                // Original SMS Message (if exists)
                existingExpense?.smsBody?.let { smsBody ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📱",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Original SMS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = smsBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                            )
                            
                            // Show account number if available
                            existingExpense?.accountNumber?.let { account ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Account: $account",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CategorySelectionItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(category.name)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                categoryColor.copy(alpha = 0.15f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            CardDefaults.outlinedCardBorder().copy(width = 2.dp)
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) categoryColor 
                        else categoryColor.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.name),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) 
                    categoryColor 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
