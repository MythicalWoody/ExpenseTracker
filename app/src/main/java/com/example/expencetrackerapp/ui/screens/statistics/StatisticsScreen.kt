package com.example.expencetrackerapp.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.data.database.dao.CategorySpending
import com.example.expencetrackerapp.ui.components.EmptyState
import com.example.expencetrackerapp.ui.components.ExpenseCard
import com.example.expencetrackerapp.ui.components.getCategoryIcon
import com.example.expencetrackerapp.ui.theme.getCategoryColor
import com.example.expencetrackerapp.ui.viewmodel.ExpenseViewModel
import com.example.expencetrackerapp.util.CurrencyFormatter
import com.example.expencetrackerapp.util.DateUtils

@Composable
fun StatisticsScreen(viewModel: ExpenseViewModel) {
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val spendingByCategory by viewModel.monthlySpendingByCategory.collectAsState()
    val expenseCount by viewModel.expenseCount.collectAsState()

    val selectedCategory by viewModel.selectedCategoryName.collectAsState()
    val categoryTransactions by viewModel.categoryTransactions.collectAsState()

    val sortedSpending = spendingByCategory.sortedByDescending { it.total }

    LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text = DateUtils.formatMonthYear(System.currentTimeMillis()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary Cards
        item {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                        title = "Total Spent",
                        value = CurrencyFormatter.format(monthlyTotal),
                        modifier = Modifier.weight(1f)
                )
                StatCard(
                        title = "Transactions",
                        value = expenseCount.toString(),
                        modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            StatCard(
                    title = "Average per Transaction",
                    value =
                            CurrencyFormatter.format(
                                    if (expenseCount > 0) monthlyTotal / expenseCount else 0.0
                            ),
                    modifier = Modifier.fillMaxWidth()
            )
        }

        // Interactive Pie Chart
        if (sortedSpending.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                com.example.expencetrackerapp.ui.components.InteractivePieChart(
                        data = sortedSpending,
                        totalAmount = monthlyTotal
                )
            }
        }

        // Spending by Category Breakdown
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "Category Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (sortedSpending.isEmpty()) {
            item {
                EmptyState(
                        icon = Icons.Filled.TrendingDown,
                        title = "No spending data",
                        description = "Add some expenses to see your statistics"
                )
            }
        } else {
            sortedSpending.forEach { spending ->
                item(key = spending.category) {
                    CategoryProgressCard(
                            spending = spending,
                            total = monthlyTotal,
                            isSelected = selectedCategory == spending.category,
                            onClick = { viewModel.selectCategory(spending.category) }
                    )
                }

                if (selectedCategory == spending.category) {
                    if (categoryTransactions.isEmpty()) {
                        item {
                            Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                            ) {
                                Text(
                                        text = "No transactions found",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(categoryTransactions, key = { it.id }) { expense ->
                            ExpenseCard(
                                    expense = expense,
                                    onClick = { /* Navigate to detail if needed */},
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom spacing
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryProgressCard(
        spending: CategorySpending,
        total: Double,
        isSelected: Boolean = false,
        onClick: () -> Unit = {}
) {
    val percentage = if (total > 0) (spending.total / total).toFloat() else 0f
    val categoryColor = getCategoryColor(spending.category)

    Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = 0.5f
                                            )
                                    else MaterialTheme.colorScheme.surface
                    ),
            border =
                    if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, categoryColor)
                    else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                        modifier =
                                Modifier.size(40.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            imageVector = getCategoryIcon(spending.category),
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = spending.category,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                            text = CurrencyFormatter.format(spending.total),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text = "${(percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = categoryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = categoryColor,
                    trackColor = categoryColor.copy(alpha = 0.15f)
            )
        }
    }
}
