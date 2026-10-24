package com.clarity.finance.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clarity.finance.ui.screens.DashboardScreen
import com.clarity.finance.ui.screens.LoginScreen
import com.clarity.finance.ui.screens.RegisterScreen
import com.clarity.finance.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Dashboard : Screen("dashboard")
    data object ExpensesHistory : Screen("expenses_history")
    data object Insights : Screen("insights")
}

@Composable
fun ClarityNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var loginNoticeMessage by remember { mutableStateOf<String?>(null) }

    val startDestination = if (authViewModel.isUserLoggedIn) {
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                initialMessage = loginNoticeMessage,
                onNavigateToRegister = {
                    loginNoticeMessage = null
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    loginNoticeMessage = null
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = { successMessage ->
                    // Set notice message and redirect to login screen
                    loginNoticeMessage = successMessage
                    authViewModel.resetState()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                authViewModel = authViewModel,
                onSignOutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToExpensesHistory = {
                    navController.navigate(Screen.ExpensesHistory.route)
                },
                onNavigateToInsights = {
                    navController.navigate(Screen.Insights.route)
                }
            )
        }

        composable(Screen.ExpensesHistory.route) {
            com.clarity.finance.ui.screens.ExpensesHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Insights.route) {
            com.clarity.finance.ui.screens.InsightsScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
