package com.example.expencetrackerapp.ui.screens.addexpense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.expencetrackerapp.ui.components.GlassRefractiveBox
import com.example.expencetrackerapp.ui.components.LocalHazeState
import com.example.expencetrackerapp.ui.components.getCategoryIcon
import com.example.expencetrackerapp.ui.theme.Primary
import com.example.expencetrackerapp.ui.theme.Secondary
import com.example.expencetrackerapp.ui.theme.getCategoryColor
import com.example.expencetrackerapp.ui.viewmodel.ExpenseViewModel
import dev.chrisbanes.haze.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
        viewModel: ExpenseViewModel,
        expenseId: Long? = null,
        onNavigateBack: () -> Unit
) {
        val categories by viewModel.allCategories.collectAsState()

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
                if (merchant.isNotBlank() && selectedCategory.isEmpty() && existingExpense == null
                ) {
                        val suggested = viewModel.suggestCategory(merchant)
                        if (suggested != "Others") {
                                selectedCategory = suggested
                        }
                }
        }

        val isValid =
                amount.toDoubleOrNull() != null &&
                        amount.toDouble() > 0 &&
                        merchant.isNotBlank() &&
                        selectedCategory.isNotBlank()

        val hazeState = LocalHazeState.current ?: remember { HazeState() }

        Scaffold(topBar = { /* Overlay handled manually */}) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize()) {
                        // Unified App Background
                        com.example.expencetrackerapp.ui.components.AppBackground(
                                modifier = Modifier.fillMaxSize().haze(state = hazeState)
                        )
                        if (isLoading) {
                                Box(
                                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                                        contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator() }
                        } else {
                                Column(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .padding(top = 40.dp, bottom = 24.dp)
                                                        .verticalScroll(rememberScrollState())
                                                        .padding(horizontal = 16.dp)
                                                        .padding(top = 40.dp, bottom = 100.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                                        leadingIcon =
                                                                if (isExpense) {
                                                                        {
                                                                                Icon(
                                                                                        Icons.Filled
                                                                                                .Check,
                                                                                        null,
                                                                                        Modifier.size(
                                                                                                18.dp
                                                                                        )
                                                                                )
                                                                        }
                                                                } else null,
                                                        colors =
                                                                FilterChipDefaults.filterChipColors(
                                                                        selectedContainerColor =
                                                                                Secondary.copy(
                                                                                        alpha = 0.2f
                                                                                ),
                                                                        selectedLabelColor =
                                                                                Secondary
                                                                )
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                FilterChip(
                                                        selected = !isExpense,
                                                        onClick = { isExpense = false },
                                                        label = { Text("Income") },
                                                        leadingIcon =
                                                                if (!isExpense) {
                                                                        {
                                                                                Icon(
                                                                                        Icons.Filled
                                                                                                .Check,
                                                                                        null,
                                                                                        Modifier.size(
                                                                                                18.dp
                                                                                        )
                                                                                )
                                                                        }
                                                                } else null,
                                                        colors =
                                                                FilterChipDefaults.filterChipColors(
                                                                        selectedContainerColor =
                                                                                Primary.copy(
                                                                                        alpha = 0.2f
                                                                                ),
                                                                        selectedLabelColor = Primary
                                                                )
                                                )
                                        }

                                        // Amount Input
                                        GlassRefractiveBox(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .clip(RoundedCornerShape(16.dp)),
                                                shape = RoundedCornerShape(16.dp)
                                        ) {
                                                OutlinedTextField(
                                                        value = amount,
                                                        onValueChange = { newValue ->
                                                                if (newValue.isEmpty() ||
                                                                                newValue.matches(
                                                                                        Regex(
                                                                                                "^\\d*\\.?\\d{0,2}$"
                                                                                        )
                                                                                )
                                                                ) {
                                                                        amount = newValue
                                                                }
                                                        },
                                                        label = { Text("Amount") },
                                                        prefix = { Text("₹ ") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        keyboardOptions =
                                                                KeyboardOptions(
                                                                        keyboardType =
                                                                                KeyboardType.Decimal
                                                                ),
                                                        singleLine = true,
                                                        textStyle =
                                                                MaterialTheme.typography
                                                                        .headlineSmall.copy(
                                                                        fontWeight = FontWeight.Bold
                                                                ),
                                                        colors =
                                                                OutlinedTextFieldDefaults.colors(
                                                                        focusedBorderColor =
                                                                                Color.Transparent,
                                                                        unfocusedBorderColor =
                                                                                Color.Transparent,
                                                                        focusedContainerColor =
                                                                                Color.Transparent,
                                                                        unfocusedContainerColor =
                                                                                Color.Transparent
                                                                )
                                                )
                                        }

                                        // Merchant Input
                                        GlassRefractiveBox(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .clip(RoundedCornerShape(16.dp)),
                                                shape = RoundedCornerShape(16.dp)
                                        ) {
                                                OutlinedTextField(
                                                        value = merchant,
                                                        onValueChange = { merchant = it },
                                                        label = { Text("Merchant / Payee") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        colors =
                                                                OutlinedTextFieldDefaults.colors(
                                                                        focusedBorderColor =
                                                                                Color.Transparent,
                                                                        unfocusedBorderColor =
                                                                                Color.Transparent,
                                                                        focusedContainerColor =
                                                                                Color.Transparent,
                                                                        unfocusedContainerColor =
                                                                                Color.Transparent
                                                                )
                                                )
                                        }

                                        // Category Selection
                                        Text(
                                                text = "Category",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Category Selection
                                        categories.chunked(3).forEach { rowCategories ->
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        rowCategories.forEach { category ->
                                                                Box(
                                                                        modifier =
                                                                                Modifier.weight(1f)
                                                                ) {
                                                                        CategorySelectionItem(
                                                                                category = category,
                                                                                isSelected =
                                                                                        selectedCategory ==
                                                                                                category.name,
                                                                                onClick = {
                                                                                        selectedCategory =
                                                                                                category.name
                                                                                }
                                                                        )
                                                                }
                                                        }
                                                        // Add spacers to fill the row if it has
                                                        // fewer than 3 items
                                                        if (rowCategories.size < 3) {
                                                                repeat(3 - rowCategories.size) {
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        // Note Input
                                        GlassRefractiveBox(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .clip(RoundedCornerShape(16.dp)),
                                                shape = RoundedCornerShape(16.dp)
                                        ) {
                                                OutlinedTextField(
                                                        value = note,
                                                        onValueChange = { note = it },
                                                        label = { Text("Note (optional)") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        maxLines = 3,
                                                        colors =
                                                                OutlinedTextFieldDefaults.colors(
                                                                        focusedBorderColor =
                                                                                Color.Transparent,
                                                                        unfocusedBorderColor =
                                                                                Color.Transparent,
                                                                        focusedContainerColor =
                                                                                Color.Transparent,
                                                                        unfocusedContainerColor =
                                                                                Color.Transparent
                                                                )
                                                )
                                        }

                                        // Original SMS Message (if exists)
                                        existingExpense?.smsBody?.let { smsBody ->
                                                GlassRefractiveBox(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp)
                                                ) {
                                                        Column(modifier = Modifier.padding(16.dp)) {
                                                                Row(
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Text(
                                                                                text = "📱",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .titleMedium
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                text =
                                                                                        "Original SMS",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .titleSmall,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .SemiBold,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                        )
                                                                }
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        8.dp
                                                                                )
                                                                )
                                                                Text(
                                                                        text = smsBody,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.8f
                                                                                        ),
                                                                        lineHeight =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall
                                                                                        .lineHeight *
                                                                                        1.3
                                                                )

                                                                // Show account number if available
                                                                existingExpense?.accountNumber
                                                                        ?.let { account ->
                                                                                Spacer(
                                                                                        modifier =
                                                                                                Modifier.height(
                                                                                                        8.dp
                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                "Account: $account",
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .labelSmall,
                                                                                        color =
                                                                                                Primary
                                                                                )
                                                                        }
                                                        }
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                }
                        }

                        // Glass Top Bar Overlay
                        GlassRefractiveBox(
                                modifier =
                                        Modifier.align(Alignment.TopCenter)
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .padding(top = 8.dp),
                                shape = RoundedCornerShape(24.dp)
                        ) {
                                TopAppBar(
                                        title = {
                                                Text(
                                                        if (existingExpense != null) "Edit Expense"
                                                        else "Add Expense"
                                                )
                                        },
                                        navigationIcon = {
                                                IconButton(onClick = onNavigateBack) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.AutoMirrored.Filled
                                                                                .ArrowBack,
                                                                contentDescription = "Back"
                                                        )
                                                }
                                        },
                                        actions = {
                                                IconButton(
                                                        onClick = {
                                                                if (isValid) {
                                                                        val amountValue =
                                                                                amount.toDouble()
                                                                        if (existingExpense != null
                                                                        ) {
                                                                                viewModel
                                                                                        .updateExpense(
                                                                                                existingExpense!!
                                                                                                        .copy(
                                                                                                                amount =
                                                                                                                        amountValue,
                                                                                                                merchant =
                                                                                                                        merchant,
                                                                                                                category =
                                                                                                                        selectedCategory,
                                                                                                                note =
                                                                                                                        note
                                                                                                                                .ifBlank {
                                                                                                                                        null
                                                                                                                                },
                                                                                                                transactionType =
                                                                                                                        if (isExpense
                                                                                                                        )
                                                                                                                                TransactionType
                                                                                                                                        .DEBIT
                                                                                                                        else
                                                                                                                                TransactionType
                                                                                                                                        .CREDIT,
                                                                                                                isAutoCategorized =
                                                                                                                        false
                                                                                                        )
                                                                                        )
                                                                        } else {
                                                                                viewModel
                                                                                        .addExpense(
                                                                                                amount =
                                                                                                        amountValue,
                                                                                                merchant =
                                                                                                        merchant,
                                                                                                category =
                                                                                                        selectedCategory,
                                                                                                note =
                                                                                                        note
                                                                                                                .ifBlank {
                                                                                                                        null
                                                                                                                },
                                                                                                transactionType =
                                                                                                        if (isExpense
                                                                                                        )
                                                                                                                TransactionType
                                                                                                                        .DEBIT
                                                                                                        else
                                                                                                                TransactionType
                                                                                                                        .CREDIT
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
                                                                tint =
                                                                        if (isValid) Primary
                                                                        else
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                        )
                                                }
                                        },
                                        windowInsets = WindowInsets(0.dp),
                                        colors =
                                                TopAppBarDefaults.topAppBarColors(
                                                        containerColor = Color.Transparent
                                                )
                                )
                        }
                }
        }
}

@Composable
private fun CategorySelectionItem(category: Category, isSelected: Boolean, onClick: () -> Unit) {
        val categoryColor = getCategoryColor(category.name)

        GlassRefractiveBox(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                shape = RoundedCornerShape(12.dp)
        ) {
                // Color tint for selection
                Box(
                        modifier =
                                Modifier.matchParentSize()
                                        .background(
                                                if (isSelected) categoryColor.copy(alpha = 0.15f)
                                                else Color.Transparent
                                        )
                )

                Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Box(
                                modifier =
                                        Modifier.size(40.dp)
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
                                color =
                                        if (isSelected) categoryColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                        )
                }
        }
}
