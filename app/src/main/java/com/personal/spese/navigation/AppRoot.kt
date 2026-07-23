package com.personal.spese.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.personal.spese.feature.categories.CategoriesScreen
import com.personal.spese.feature.dashboard.DashboardScreen
import com.personal.spese.feature.expenses.ExpensesScreen
import com.personal.spese.feature.expenses.addedit.AddEditExpenseScreen
import com.personal.spese.feature.installments.InstallmentsScreen
import com.personal.spese.feature.installments.addplan.AddInstallmentPlanScreen
import com.personal.spese.feature.installments.detail.InstallmentDetailScreen
import com.personal.spese.feature.recurring.RecurringListScreen
import com.personal.spese.feature.recurring.edit.RecurringEditScreen
import com.personal.spese.feature.settings.SettingsScreen

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBars = currentRoute in Routes.mainRoutes

    Scaffold(
        bottomBar = {
            if (showBars) BottomBar(navController, currentRoute)
        },
        floatingActionButton = {
            if (showBars) {
                FloatingActionButton(onClick = { navController.navigate(Routes.addEdit()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Aggiungi spesa")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(onOpenExpense = { id -> navController.navigate(Routes.addEdit(id = id)) })
            }
            composable(Routes.EXPENSES) {
                ExpensesScreen(onEdit = { id -> navController.navigate(Routes.addEdit(id = id)) })
            }
            composable(Routes.INSTALLMENTS) {
                InstallmentsScreen(
                    onAddPlan = { navController.navigate(Routes.ADD_PLAN) },
                    onOpenPlan = { id -> navController.navigate(Routes.installmentDetail(id)) }
                )
            }
            composable(Routes.ADD_PLAN) {
                AddInstallmentPlanScreen(onDone = { navController.popBackStack() })
            }
            composable(
                route = Routes.INSTALLMENT_DETAIL,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: -1L
                InstallmentDetailScreen(planId = planId, onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenRecurring = { navController.navigate(Routes.RECURRING) }
                )
            }
            composable(Routes.RECURRING) {
                RecurringListScreen(
                    onAdd = { navController.navigate(Routes.recurringEdit()) },
                    onEdit = { id -> navController.navigate(Routes.recurringEdit(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.RECURRING_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                RecurringEditScreen(recurringId = id, onDone = { navController.popBackStack() })
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.ADD_EDIT,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = "SINGLE" },
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L }
                ),
                deepLinks = listOf(navDeepLink { uriPattern = "spese://add" })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                AddEditExpenseScreen(expenseId = id, onDone = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    val items = listOf(
        NavItem(Routes.DASHBOARD, "Dashboard", Icons.Outlined.PieChart),
        NavItem(Routes.EXPENSES, "Spese", Icons.Outlined.List),
        NavItem(Routes.INSTALLMENTS, "Rate", Icons.Outlined.CalendarMonth),
        NavItem(Routes.SETTINGS, "Impostazioni", Icons.Outlined.Settings)
    )
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
