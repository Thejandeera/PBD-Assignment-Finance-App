package com.clarity.finance.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.clarity.finance.ui.screens.DraftsScreen
import com.clarity.finance.ui.screens.ExpensesHistoryScreen
import com.clarity.finance.ui.screens.InsightsScreen
import com.clarity.finance.ui.screens.LoginScreen
import com.clarity.finance.ui.screens.RegisterScreen
import com.clarity.finance.ui.screens.SettingsScreen
import com.clarity.finance.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Dashboard : Screen("dashboard")
    data object ExpensesHistory : Screen("expenses_history")
    data object Insights : Screen("insights")
    data object Settings : Screen("settings")
    data object Drafts : Screen("drafts")
}

@Composable
fun ClarityNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var loginNoticeMessage by remember { mutableStateOf<String?>(null) }

    // 1-Hour Session Expiry Check
    LaunchedEffect(Unit) {
        if (authViewModel.isUserLoggedIn && authViewModel.isSessionExpired()) {
            authViewModel.signOut()
            loginNoticeMessage = "Session expired due to inactivity. Please log in again."
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (authViewModel.isUserLoggedIn) {
            authViewModel.updateSessionActivity()
        }
    }

    val startDestination = if (authViewModel.isUserLoggedIn && !authViewModel.isSessionExpired()) {
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
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToDrafts = {
                    navController.navigate(Screen.Drafts.route)
                }
            )
        }

        composable(Screen.ExpensesHistory.route) {
            ExpensesHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDrafts = {
                    navController.navigate(Screen.Drafts.route)
                }
            )
        }

        composable(Screen.Insights.route) {
            InsightsScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Drafts.route) {
            DraftsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
