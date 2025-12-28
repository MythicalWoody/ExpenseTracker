package com.example.expencetrackerapp.ui.screens.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.ui.components.EmptyState
import com.example.expencetrackerapp.ui.components.ExpenseCard
import com.example.expencetrackerapp.ui.components.GlassRefractiveBox
import com.example.expencetrackerapp.ui.components.LocalHazeState
import com.example.expencetrackerapp.ui.theme.*
import com.example.expencetrackerapp.ui.viewmodel.ExpenseViewModel
import com.example.expencetrackerapp.util.DateUtils
import dev.chrisbanes.haze.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: ExpenseViewModel, onExpenseClick: (Long) -> Unit) {
        val allExpenses by viewModel.allExpenses.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        val searchResults by viewModel.searchResults.collectAsState()

        val displayedExpenses = if (searchQuery.isBlank()) allExpenses else searchResults
        val groupedExpenses = groupExpensesByDate(displayedExpenses)

        val listState = rememberLazyListState()
        val hazeState = LocalHazeState.current ?: remember { HazeState() }

        Scaffold { paddingValues ->
                Box(modifier = Modifier.fillMaxSize()) {
                        // Unified App Background
                        com.example.expencetrackerapp.ui.components.AppBackground(
                                modifier = Modifier.fillMaxSize().haze(state = hazeState)
                        )

                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                                // Content Layer (Bottom)
                                if (displayedExpenses.isEmpty()) {
                                        EmptyState(
                                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                                title =
                                                        if (searchQuery.isBlank()) "No transactions"
                                                        else "No results",
                                                description =
                                                        if (searchQuery.isBlank())
                                                                "Your expenses will appear here"
                                                        else "Try a different search term",
                                                modifier =
                                                        Modifier.align(Alignment.Center)
                                                                .padding(top = 160.dp)
                                        )
                                } else {
                                        LazyColumn(
                                                state = listState,
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .padding(
                                                                        top = 92.dp
                                                                ) // Align scroll area with middle
                                                                // of glass header
                                                                .padding(bottom = 24.dp),
                                                contentPadding =
                                                        PaddingValues(
                                                                top = 98.dp,
                                                                bottom = 100.dp,
                                                                start = 20.dp,
                                                                end = 20.dp
                                                        ),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                groupedExpenses.forEach { (dateLabel, expenses) ->
                                                        item(key = "header_$dateLabel") {
                                                                val offset =
                                                                        remember(listState) {
                                                                                derivedStateOf {
                                                                                        val layoutInfo =
                                                                                                listState
                                                                                                        .layoutInfo
                                                                                        val itemInfo =
                                                                                                layoutInfo
                                                                                                        .visibleItemsInfo
                                                                                                        .find {
                                                                                                                it.key ==
                                                                                                                        "header_$dateLabel"
                                                                                                        }
                                                                                        if (itemInfo !=
                                                                                                        null
                                                                                        ) {
                                                                                                val center =
                                                                                                        itemInfo.offset +
                                                                                                                (itemInfo.size /
                                                                                                                        2f)
                                                                                                val viewportCenter =
                                                                                                        layoutInfo
                                                                                                                .viewportEndOffset /
                                                                                                                2f
                                                                                                (center -
                                                                                                        viewportCenter) /
                                                                                                        viewportCenter
                                                                                        } else 0f
                                                                                }
                                                                        }
                                                                GlassRefractiveBox(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        vertical =
                                                                                                8.dp
                                                                                ),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        10.dp
                                                                                ),
                                                                        verticalOffset =
                                                                                offset.value
                                                                ) {
                                                                        Text(
                                                                                text = dateLabel,
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .labelLarge,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .SemiBold,
                                                                                color = Primary,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                horizontal =
                                                                                                        14.dp,
                                                                                                vertical =
                                                                                                        6.dp
                                                                                        )
                                                                        )
                                                                }
                                                        }

                                                        items(
                                                                items = expenses,
                                                                key = { expense -> expense.id }
                                                        ) { expense ->
                                                                val offset =
                                                                        remember(listState) {
                                                                                derivedStateOf {
                                                                                        val layoutInfo =
                                                                                                listState
                                                                                                        .layoutInfo
                                                                                        val itemInfo =
                                                                                                layoutInfo
                                                                                                        .visibleItemsInfo
                                                                                                        .find {
                                                                                                                it.key ==
                                                                                                                        expense.id
                                                                                                        }
                                                                                        if (itemInfo !=
                                                                                                        null
                                                                                        ) {
                                                                                                val center =
                                                                                                        itemInfo.offset +
                                                                                                                (itemInfo.size /
                                                                                                                        2f)
                                                                                                val viewportCenter =
                                                                                                        layoutInfo
                                                                                                                .viewportEndOffset /
                                                                                                                2f
                                                                                                (center -
                                                                                                        viewportCenter) /
                                                                                                        viewportCenter
                                                                                        } else 0f
                                                                                }
                                                                        }
                                                                ExpenseCard(
                                                                        expense = expense,
                                                                        onClick = {
                                                                                onExpenseClick(
                                                                                        expense.id
                                                                                )
                                                                        },
                                                                        useGlass = true,
                                                                        verticalOffset =
                                                                                offset.value
                                                                )
                                                        }
                                                }
                                        }
                                }

                                // Glass Header Overlay (Top Layer)
                                GlassRefractiveBox(
                                        modifier =
                                                Modifier.align(Alignment.TopCenter)
                                                        .padding(16.dp)
                                                        .fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp)
                                ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                                Text(
                                                        text = "Transactions",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .headlineLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                )

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Search Bar
                                                OutlinedTextField(
                                                        value = searchQuery,
                                                        onValueChange = {
                                                                viewModel.updateSearchQuery(it)
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        placeholder = {
                                                                Text(
                                                                        "Search transactions...",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                        },
                                                        leadingIcon = {
                                                                Icon(
                                                                        Icons.Filled.Search,
                                                                        contentDescription =
                                                                                "Search",
                                                                        tint = Primary
                                                                )
                                                        },
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors =
                                                                OutlinedTextFieldDefaults.colors(
                                                                        focusedBorderColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .outline
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        ),
                                                                        unfocusedBorderColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .outline
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.3f
                                                                                        ),
                                                                        focusedContainerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surface
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.1f
                                                                                        ),
                                                                        unfocusedContainerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surface
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.1f
                                                                                        ),
                                                                        focusedTextColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurface,
                                                                        unfocusedTextColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurface
                                                                )
                                                )
                                        }
                                }
                        }
                }
        }
}

private fun groupExpensesByDate(expenses: List<Expense>): Map<String, List<Expense>> {
        return expenses.groupBy { expense -> DateUtils.getRelativeDate(expense.date) }
}
