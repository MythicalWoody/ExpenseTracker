package com.example.expencetrackerapp.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.ui.components.EmptyState
import com.example.expencetrackerapp.ui.components.ExpenseCard
import com.example.expencetrackerapp.ui.theme.*
import com.example.expencetrackerapp.ui.viewmodel.ExpenseViewModel
import com.example.expencetrackerapp.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: ExpenseViewModel, onExpenseClick: (Long) -> Unit) {
    val allExpenses by viewModel.allExpenses.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val displayedExpenses = if (searchQuery.isBlank()) allExpenses else searchResults
    val groupedExpenses = groupExpensesByDate(displayedExpenses)

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background pattern
            com.example.expencetrackerapp.ui.components.BackgroundPattern()

            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Header
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Glass Search Bar
                    Box(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(
                                                    width = 1.dp,
                                                    color =
                                                            MaterialTheme.colorScheme.outline.copy(
                                                                    alpha = 0.1f
                                                            ),
                                                    shape = RoundedCornerShape(16.dp)
                                            )
                    ) {
                        OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                            "Search transactions...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                            Icons.Filled.Search,
                                            contentDescription = "Search",
                                            tint = Primary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent
                                        )
                        )
                    }
                }

                // Transaction List - NO animations
                if (displayedExpenses.isEmpty()) {
                    EmptyState(
                            icon = Icons.Filled.ReceiptLong,
                            title = if (searchQuery.isBlank()) "No transactions" else "No results",
                            description =
                                    if (searchQuery.isBlank()) "Your expenses will appear here"
                                    else "Try a different search term",
                            modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedExpenses.forEach { (dateLabel, expenses) ->
                            item(key = "header_$dateLabel") {
                                Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Primary.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                            text = dateLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Primary,
                                            modifier =
                                                    Modifier.padding(
                                                            horizontal = 14.dp,
                                                            vertical = 6.dp
                                                    )
                                    )
                                }
                            }

                            items(items = expenses, key = { expense -> expense.id }) { expense ->
                                ExpenseCard(
                                        expense = expense,
                                        onClick = { onExpenseClick(expense.id) }
                                )
                            }
                        }

                        // Bottom spacing
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

private fun groupExpensesByDate(expenses: List<Expense>): Map<String, List<Expense>> {
    return expenses.groupBy { expense -> DateUtils.getRelativeDate(expense.date) }
}
