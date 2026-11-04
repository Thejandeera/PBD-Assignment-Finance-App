package com.clarity.finance.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.ui.screens.ComingSoonScreen
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
    data object Income : Screen("income")
    data object Goal : Screen("goal")
}

private val TabRoutes = setOf(
    Screen.Dashboard.route,
    Screen.Income.route,
    Screen.Insights.route,
    Screen.Goal.route
)
private val AuthRoutes = setOf(Screen.Login.route, Screen.Register.route)

// iOS navigation pushes ease out quickly and settle gently.
private const val PUSH_DURATION_MS = 420
private const val FADE_DURATION_MS = 260
private val PushEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private fun ClarityNavTab.route(): String = when (this) {
    ClarityNavTab.HOME -> Screen.Dashboard.route
    ClarityNavTab.INCOME -> Screen.Income.route
    ClarityNavTab.INSIGHTS -> Screen.Insights.route
    ClarityNavTab.GOAL -> Screen.Goal.route
}

/** Switches tabs while keeping Home as the single root, so Back always returns Home. */
private fun NavHostController.navigateToTab(tab: ClarityNavTab) {
    navigate(tab.route()) {
        popUpTo(Screen.Dashboard.route)
        launchSingleTop = true
    }
}

private val NavBackStackEntry.route: String? get() = destination.route

/**
 * Runs a user-initiated navigation only while this destination is fully resumed, so a double
 * tap during a transition can't push a screen twice or pop the root and leave a blank screen.
 */
private inline fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.route in TabRoutes && targetState.route in TabRoutes

private fun AnimatedContentTransitionScope<NavBackStackEntry>.crossesAuthBoundary(): Boolean =
    (initialState.route in AuthRoutes) != (targetState.route in AuthRoutes)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isReturnToLogin(): Boolean =
    initialState.route == Screen.Register.route && targetState.route == Screen.Login.route

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushEnter(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(PUSH_DURATION_MS, easing = PushEasing)
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushExit(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(PUSH_DURATION_MS, easing = PushEasing),
        targetOffset = { fullWidth -> fullWidth / 4 }
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(PUSH_DURATION_MS, easing = PushEasing),
        initialOffset = { fullWidth -> fullWidth / 4 }
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(PUSH_DURATION_MS, easing = PushEasing)
    )

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
            loginNoticeMessage = "Your session expired. Sign in again to continue."
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
        modifier = modifier,
        enterTransition = {
            when {
                isTabSwitch() -> EnterTransition.None
                crossesAuthBoundary() -> fadeIn(tween(FADE_DURATION_MS))
                isReturnToLogin() -> popEnter()
                else -> pushEnter()
            }
        },
        exitTransition = {
            when {
                isTabSwitch() -> ExitTransition.None
                crossesAuthBoundary() -> fadeOut(tween(FADE_DURATION_MS))
                isReturnToLogin() -> popExit()
                else -> pushExit()
            }
        },
        popEnterTransition = {
            when {
                isTabSwitch() -> EnterTransition.None
                crossesAuthBoundary() -> fadeIn(tween(FADE_DURATION_MS))
                else -> popEnter()
            }
        },
        popExitTransition = {
            when {
                isTabSwitch() -> ExitTransition.None
                crossesAuthBoundary() -> fadeOut(tween(FADE_DURATION_MS))
                else -> popExit()
            }
        }
    ) {
        composable(Screen.Login.route) { entry ->
            LoginScreen(
                authViewModel = authViewModel,
                initialMessage = loginNoticeMessage,
                onNavigateToRegister = {
                    entry.ifResumed {
                        loginNoticeMessage = null
                        navController.navigate(Screen.Register.route)
                    }
                },
                onLoginSuccess = {
                    loginNoticeMessage = null
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) { entry ->
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    entry.ifResumed { navController.popBackStack() }
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

        composable(Screen.Dashboard.route) { entry ->
            DashboardScreen(
                authViewModel = authViewModel,
                onSignOutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToExpensesHistory = {
                    entry.ifResumed { navController.navigate(Screen.ExpensesHistory.route) }
                },
                onNavigateToInsights = {
                    entry.ifResumed { navController.navigateToTab(ClarityNavTab.INSIGHTS) }
                },
                onNavigateToSettings = {
                    entry.ifResumed { navController.navigate(Screen.Settings.route) }
                },
                onNavigateToDrafts = {
                    entry.ifResumed { navController.navigate(Screen.Drafts.route) }
                },
                onNavigateToIncome = {
                    entry.ifResumed { navController.navigateToTab(ClarityNavTab.INCOME) }
                },
                onNavigateToGoal = {
                    entry.ifResumed { navController.navigateToTab(ClarityNavTab.GOAL) }
                }
            )
        }

        composable(Screen.ExpensesHistory.route) { entry ->
            ExpensesHistoryScreen(
                onNavigateBack = {
                    entry.ifResumed { navController.popBackStack() }
                },
                onNavigateToDrafts = {
                    entry.ifResumed { navController.navigate(Screen.Drafts.route) }
                }
            )
        }

        composable(Screen.Insights.route) { entry ->
            InsightsScreen(
                onNavigateToDashboard = {
                    entry.ifResumed { navController.navigateToTab(ClarityNavTab.HOME) }
                },
                onNavigateToIncome = {
                    entry.ifResumed { navController.navigateToTab(ClarityNavTab.INCOME) }
                },
                onNavigateToGoal = {
                    entry.ifResumed { navController.navigateToTab(ClarityNavTab.GOAL) }
                }
            )
        }

        composable(Screen.Income.route) { entry ->
            ComingSoonScreen(
                tab = ClarityNavTab.INCOME,
                title = "Income Tracking Is Coming",
                message = "Salary, freelance, AdSense and crypto income will be tracked here and converted to LKR.",
                onTabSelected = { tab -> entry.ifResumed { navController.navigateToTab(tab) } }
            )
        }

        composable(Screen.Goal.route) { entry ->
            ComingSoonScreen(
                tab = ClarityNavTab.GOAL,
                title = "Goals Are Coming",
                message = "Follow your MacBook Pro M4 savings goal and see how much to set aside each month.",
                onTabSelected = { tab -> entry.ifResumed { navController.navigateToTab(tab) } }
            )
        }

        composable(Screen.Settings.route) { entry ->
            SettingsScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    entry.ifResumed { navController.popBackStack() }
                },
                onSignOutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToDrafts = {
                    entry.ifResumed { navController.navigate(Screen.Drafts.route) }
                }
            )
        }

        composable(Screen.Drafts.route) { entry ->
            DraftsScreen(
                onNavigateBack = {
                    entry.ifResumed { navController.popBackStack() }
                }
            )
        }
    }
}
