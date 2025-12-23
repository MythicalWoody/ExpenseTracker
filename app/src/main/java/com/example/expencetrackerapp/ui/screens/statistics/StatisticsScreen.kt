package com.example.expencetrackerapp.ui.screens.statistics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.data.database.dao.CategorySpending
import com.example.expencetrackerapp.ui.components.EmptyState
import com.example.expencetrackerapp.ui.components.ExpenseCard
import com.example.expencetrackerapp.ui.components.getCategoryIcon
import com.example.expencetrackerapp.ui.theme.*
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Background pattern
        com.example.expencetrackerapp.ui.components.BackgroundPattern()

        // Main content
        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                            text = "Statistics",
                            style = MaterialTheme.typography.headlineLarge,
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
            }

            // Summary Cards Grid
            item {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassStatCard(
                            title = "Total Spent",
                            value = CurrencyFormatter.format(monthlyTotal),
                            color = Secondary,
                            modifier = Modifier.weight(1f)
                    )
                    GlassStatCard(
                            title = "Transactions",
                            value = expenseCount.toString(),
                            color = Primary,
                            modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                GlassStatCard(
                        title = "Average per Transaction",
                        value =
                                CurrencyFormatter.format(
                                        if (expenseCount > 0) monthlyTotal / expenseCount else 0.0
                                ),
                        color = Accent,
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
                Text(
                        text = "Category Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 8.dp)
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
                items(sortedSpending, key = { it.category }) { spending ->
                    Column {
                        GlassCategoryProgressCard(
                                spending = spending,
                                total = monthlyTotal,
                                isSelected = selectedCategory == spending.category,
                                onClick = { viewModel.selectCategory(spending.category) }
                        )

                        // Show transactions when category is selected
                        if (selectedCategory == spending.category &&
                                        categoryTransactions.isNotEmpty()
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                categoryTransactions.forEach { expense ->
                                    ExpenseCard(
                                            expense = expense,
                                            onClick = {},
                                            modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

/** Glassmorphic Stat Card */
@Composable
private fun GlassStatCard(
        title: String,
        value: String,
        color: Color,
        modifier: Modifier = Modifier
) {
    Box(
            modifier =
                    modifier.clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(18.dp)
                            )
    ) {
        // Subtle color tint
        Box(
                modifier =
                        Modifier.matchParentSize()
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        color.copy(alpha = 0.06f),
                                                                        Color.Transparent
                                                                )
                                                )
                                )
        )

        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Glassmorphic Category Progress Card */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassCategoryProgressCard(
        spending: CategorySpending,
        total: Double,
        isSelected: Boolean = false,
        onClick: () -> Unit = {}
) {
    val percentage = if (total > 0) (spending.total / total).toFloat() else 0f
    val categoryColor = getCategoryColor(spending.category)

    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                    if (isSelected) categoryColor.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surface
                            )
                            .then(
                                    if (isSelected)
                                            Modifier.border(
                                                    width = 1.5.dp,
                                                    color = categoryColor.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(18.dp)
                                            )
                                    else
                                            Modifier.border(
                                                    width = 1.dp,
                                                    color =
                                                            MaterialTheme.colorScheme.outline.copy(
                                                                    alpha = 0.08f
                                                            ),
                                                    shape = RoundedCornerShape(18.dp)
                                            )
                            )
    ) {
        Surface(onClick = onClick, color = Color.Transparent) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon
                    Box(
                            modifier =
                                    Modifier.size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(categoryColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                                imageVector = getCategoryIcon(spending.category),
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = spending.category,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = "${(percentage * 100).toInt()}% of total",
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor
                        )
                    }

                    Text(
                            text = CurrencyFormatter.format(spending.total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress bar
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(categoryColor.copy(alpha = 0.12f))
                ) {
                    Box(
                            modifier =
                                    Modifier.fillMaxWidth(percentage)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(categoryColor)
                    )
                }
            }
        }
    }
}
