package com.example.expencetrackerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expencetrackerapp.notification.NotificationHelper
import com.example.expencetrackerapp.ui.navigation.Screen
import com.example.expencetrackerapp.ui.screens.addexpense.AddExpenseScreen
import com.example.expencetrackerapp.ui.screens.dashboard.DashboardScreen
import com.example.expencetrackerapp.ui.screens.settings.SettingsScreen
import com.example.expencetrackerapp.ui.screens.statistics.StatisticsScreen
import com.example.expencetrackerapp.ui.screens.transactions.TransactionsScreen
import com.example.expencetrackerapp.ui.theme.ExpenceTrackerAppTheme
import com.example.expencetrackerapp.ui.theme.Primary
import com.example.expencetrackerapp.ui.theme.ThemeState
import com.example.expencetrackerapp.ui.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Handle deep link from notification
        val expenseIdFromNotification = intent.getLongExtra(NotificationHelper.EXTRA_EXPENSE_ID, -1)
        val navigateTo = intent.getStringExtra(NotificationHelper.EXTRA_NAVIGATE_TO)

        enableEdgeToEdge()
        setContent {
            val currentThemeMode = ThemeState.themeMode
            ExpenceTrackerAppTheme(themeMode = currentThemeMode) {
                ExpenseTrackerApp(
                        initialExpenseId =
                                if (navigateTo == NotificationHelper.NAV_EDIT_EXPENSE &&
                                                expenseIdFromNotification > 0
                                )
                                        expenseIdFromNotification
                                else null
                )
            }
        }
    }
}

// Simple data class for bottom nav items
private data class BottomNavItem(
        val route: String,
        val title: String,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(initialExpenseId: Long? = null) {
    val navController = rememberNavController()
    val viewModel: ExpenseViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define bottom nav items explicitly here
    val bottomNavItems = remember {
        listOf(
                BottomNavItem("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home),
                BottomNavItem(
                        "transactions",
                        "Transactions",
                        Icons.Filled.Receipt,
                        Icons.Outlined.Receipt
                ),
                BottomNavItem("add_expense", "Add", Icons.Filled.Add, Icons.Filled.Add),
                BottomNavItem(
                        "statistics",
                        "Stats",
                        Icons.Filled.BarChart,
                        Icons.Outlined.BarChart
                ),
                BottomNavItem(
                        "settings",
                        "Settings",
                        Icons.Filled.Settings,
                        Icons.Outlined.Settings
                )
        )
    }

    // Determine if bottom bar should be shown
    val showBottomBar =
            currentRoute in
                    listOf("dashboard", "transactions", "statistics", "settings", "add_expense")

    // Navigate to edit expense if launched from notification
    LaunchedEffect(initialExpenseId) {
        if (initialExpenseId != null && initialExpenseId > 0) {
            navController.navigate(Screen.EditExpense.createRoute(initialExpenseId))
        }
    }

    Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                AnimatedVisibility(
                        visible = showBottomBar,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route

                            NavigationBarItem(
                                    icon = {
                                        Icon(
                                                imageVector =
                                                        if (selected) item.selectedIcon
                                                        else item.unselectedIcon,
                                                contentDescription = item.title
                                        )
                                    },
                                    label = { Text(item.title) },
                                    selected = selected,
                                    onClick = {
                                        if (item.route == "add_expense") {
                                            navController.navigate(item.route)
                                        } else {
                                            navController.navigate(item.route) {
                                                popUpTo(
                                                        navController.graph.findStartDestination()
                                                                .id
                                                ) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    colors =
                                            NavigationBarItemDefaults.colors(
                                                    selectedIconColor = Primary,
                                                    selectedTextColor = Primary,
                                                    indicatorColor = Primary.copy(alpha = 0.1f),
                                                    unselectedIconColor =
                                                            MaterialTheme.colorScheme
                                                                    .onSurfaceVariant,
                                                    unselectedTextColor =
                                                            MaterialTheme.colorScheme
                                                                    .onSurfaceVariant
                                            )
                            )
                        }
                    }
                }
            }
    ) { innerPadding ->
        NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                        viewModel = viewModel,
                        onExpenseClick = { expenseId ->
                            navController.navigate(Screen.EditExpense.createRoute(expenseId))
                        },
                        onAddClick = { navController.navigate(Screen.AddExpense.route) }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                        viewModel = viewModel,
                        onExpenseClick = { expenseId ->
                            navController.navigate(Screen.EditExpense.createRoute(expenseId))
                        }
                )
            }

            composable(Screen.AddExpense.route) {
                AddExpenseScreen(
                        viewModel = viewModel,
                        expenseId = null,
                        onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                    route = Screen.EditExpense.route,
                    arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getLong("expenseId")
                AddExpenseScreen(
                        viewModel = viewModel,
                        expenseId = expenseId,
                        onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Statistics.route) { StatisticsScreen(viewModel = viewModel) }

            composable(Screen.Settings.route) {
                SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToCategories = { navController.navigate(Screen.Categories.route) }
                )
            }

            composable(Screen.Categories.route) {
                // TODO: Categories screen
                Text("Categories Management")
            }
        }
    }
}
