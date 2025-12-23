package com.example.expencetrackerapp.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
        viewModel: ExpenseViewModel,
        onExpenseClick: (Long) -> Unit,
        onAddClick: () -> Unit
) {
        val recentExpenses by viewModel.recentExpenses.collectAsState()
        val monthlyTotal by viewModel.monthlyTotal.collectAsState()
        val spendingByCategory by viewModel.monthlySpendingByCategory.collectAsState()

        // Haze state for glassmorphism
        val hazeState = remember { HazeState() }

        Scaffold(
                floatingActionButton = {
                        FloatingActionButton(
                                onClick = onAddClick,
                                containerColor = Primary,
                                contentColor = Color.White,
                                shape = CircleShape,
                                elevation =
                                        FloatingActionButtonDefaults.elevation(
                                                defaultElevation = 6.dp
                                        )
                        ) {
                                Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add expense",
                                        modifier = Modifier.size(26.dp)
                                )
                        }
                }
        ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize()) {
                        // Background pattern with haze source
                        Box(modifier = Modifier.haze(state = hazeState)) {
                                com.example.expencetrackerapp.ui.components.BackgroundPattern()
                        }

                        // Main content
                        LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                                contentPadding = PaddingValues(20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                                // Header
                                item {
                                        Column {
                                                Text(
                                                        text = "Dashboard",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .headlineLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onBackground
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                        text =
                                                                DateUtils.formatMonthYear(
                                                                        System.currentTimeMillis()
                                                                ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }

                                // Glassmorphic Hero Card
                                item {
                                        GlassmorphicHeroCard(
                                                amount = monthlyTotal,
                                                month =
                                                        DateUtils.formatMonthYear(
                                                                System.currentTimeMillis()
                                                        ),
                                                hazeState = hazeState
                                        )
                                }

                                // Category Breakdown
                                if (spendingByCategory.isNotEmpty()) {
                                        item {
                                                Text(
                                                        text = "Spending by Category",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onBackground
                                                )
                                        }

                                        item {
                                                LazyRow(
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(12.dp)
                                                ) {
                                                        items(spendingByCategory) { spending ->
                                                                GlassCategoryCard(
                                                                        spending = spending,
                                                                        total = monthlyTotal,
                                                                        hazeState = hazeState
                                                                )
                                                        }
                                                }
                                        }
                                }

                                // Recent Transactions Header
                                item {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        text = "Recent Transactions",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onBackground
                                                )
                                        }
                                }

                                // Transactions - NO animations, instant load
                                if (recentExpenses.isEmpty()) {
                                        item {
                                                EmptyState(
                                                        icon = Icons.Filled.ReceiptLong,
                                                        title = "No expenses yet",
                                                        description =
                                                                "Your transactions will appear here"
                                                )
                                        }
                                } else {
                                        items(recentExpenses, key = { it.id }) { expense ->
                                                ExpenseCard(
                                                        expense = expense,
                                                        onClick = { onExpenseClick(expense.id) }
                                                )
                                        }
                                }

                                // Bottom spacing for FAB
                                item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                }
        }
}

/** Apple-style Glassmorphic Hero Card */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun GlassmorphicHeroCard(amount: Double, month: String, hazeState: HazeState) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .hazeChild(
                                        state = hazeState,
                                        shape = RoundedCornerShape(24.dp),
                                        style = HazeMaterials.thin()
                                )
                                .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(24.dp)
                                )
        ) {
                // Gradient background - made more transparent for glass effect
                Box(
                        modifier =
                                Modifier.matchParentSize()
                                        .background(
                                                brush =
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                Primary.copy(
                                                                                        alpha = 0.4f
                                                                                ),
                                                                                PrimaryDark.copy(
                                                                                        alpha = 0.5f
                                                                                )
                                                                        )
                                                        )
                                        )
                )

                // Glass overlay - light tint
                Box(
                        modifier =
                                Modifier.matchParentSize()
                                        .background(Color.White.copy(alpha = 0.05f))
                )

                // Content
                Column(modifier = Modifier.padding(28.dp)) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                Box(
                                        modifier =
                                                Modifier.size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.2f))
                                                        .border(
                                                                width = 1.dp,
                                                                color =
                                                                        Color.White.copy(
                                                                                alpha = 0.3f
                                                                        ),
                                                                shape = CircleShape
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Filled.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                        )
                                }
                                Column {
                                        Text(
                                                text = "Total Spending",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White.copy(alpha = 0.9f)
                                        )
                                        Text(
                                                text = month,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.7f)
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                                text = CurrencyFormatter.format(amount),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                        )
                }

                // Top shine effect
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(80.dp)
                                        .background(
                                                brush =
                                                        Brush.verticalGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color.White.copy(
                                                                                        alpha =
                                                                                                0.15f
                                                                                ),
                                                                                Color.Transparent
                                                                        )
                                                        )
                                        )
                )
        }
}

/** Glassmorphic Category Card */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun GlassCategoryCard(spending: CategorySpending, total: Double, hazeState: HazeState) {
        val percentage = if (total > 0) (spending.total / total * 100).toInt() else 0
        val categoryColor = getCategoryColor(spending.category)

        Box(
                modifier =
                        Modifier.width(150.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .hazeChild(
                                        state = hazeState,
                                        shape = RoundedCornerShape(20.dp),
                                        style = HazeMaterials.ultraThin()
                                )
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                .border(
                                        width = 1.dp,
                                        color =
                                                MaterialTheme.colorScheme.outline.copy(
                                                        alpha = 0.1f
                                                ),
                                        shape = RoundedCornerShape(20.dp)
                                )
        ) {
                // Subtle category tint
                Box(
                        modifier =
                                Modifier.matchParentSize()
                                        .background(
                                                brush =
                                                        Brush.verticalGradient(
                                                                colors =
                                                                        listOf(
                                                                                categoryColor.copy(
                                                                                        alpha =
                                                                                                0.08f
                                                                                ),
                                                                                Color.Transparent
                                                                        )
                                                        )
                                        )
                )

                Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                                modifier =
                                        Modifier.size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(categoryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = getCategoryIcon(spending.category),
                                        contentDescription = null,
                                        tint = categoryColor,
                                        modifier = Modifier.size(22.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                                text = spending.category,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                                text = CurrencyFormatter.formatCompact(spending.total),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Glass progress bar
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(categoryColor.copy(alpha = 0.15f))
                        ) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth(percentage / 100f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(categoryColor)
                                )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor
                        )
                }
        }
}
