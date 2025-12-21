package com.example.expencetrackerapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Navigation destinations for the app. */
sealed class Screen(
        val route: String,
        val title: String,
        val selectedIcon: ImageVector? = null,
        val unselectedIcon: ImageVector? = null
) {
        object Dashboard :
                Screen(
                        route = "dashboard",
                        title = "Home",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home
                )

        object Transactions :
                Screen(
                        route = "transactions",
                        title = "Transactions",
                        selectedIcon = Icons.Filled.Receipt,
                        unselectedIcon = Icons.Outlined.Receipt
                )

        object AddExpense :
                Screen(
                        route = "add_expense",
                        title = "Add",
                        selectedIcon = Icons.Filled.Add,
                        unselectedIcon = Icons.Filled.Add
                )

        object EditExpense : Screen(route = "edit_expense/{expenseId}", title = "Edit Expense") {
                fun createRoute(expenseId: Long) = "edit_expense/$expenseId"
        }

        object Statistics :
                Screen(
                        route = "statistics",
                        title = "Stats",
                        selectedIcon = Icons.Filled.BarChart,
                        unselectedIcon = Icons.Outlined.BarChart
                )

        object Settings :
                Screen(
                        route = "settings",
                        title = "Settings",
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings
                )

        object Categories : Screen(route = "categories", title = "Categories")

        companion object {
                val bottomNavItems =
                        listOf(Dashboard, Transactions, AddExpense, Statistics, Settings)
        }
}
