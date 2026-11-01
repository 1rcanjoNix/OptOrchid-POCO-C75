package com.optorchid.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.optorchid.core.di.AppContainer
import com.optorchid.core.theme.LocalReduceMotion
import com.optorchid.presentation.dashboard.DashboardScreen
import com.optorchid.presentation.dashboard.DashboardViewModel
import com.optorchid.presentation.diagnostic.DisplayDiagnosticScreen
import com.optorchid.presentation.diagnostic.DisplayDiagnosticViewModel
import com.optorchid.presentation.settings.SettingsScreen
import com.optorchid.presentation.settings.SettingsViewModel
import com.optorchid.presentation.splash.SplashScreen
import com.optorchid.presentation.splash.SplashViewModel

object Routes {
    const val SPLASH = "splash"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val DISPLAY_DIAGNOSTIC = "display_diagnostic"
}

private const val TRANSITION_MS = 380

@Composable
fun OrchidNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val reduce = LocalReduceMotion.current

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            if (reduce) EnterTransition.None
            else fadeIn(tween(TRANSITION_MS)) + slideInHorizontally(tween(TRANSITION_MS)) { it / 10 }
        },
        exitTransition = {
            if (reduce) ExitTransition.None else fadeOut(tween(TRANSITION_MS / 2))
        },
        popEnterTransition = {
            if (reduce) EnterTransition.None else fadeIn(tween(TRANSITION_MS))
        },
        popExitTransition = {
            if (reduce) ExitTransition.None
            else fadeOut(tween(TRANSITION_MS / 2)) + slideOutHorizontally(tween(TRANSITION_MS)) { it / 10 }
        },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                viewModel = viewModel(factory = SplashViewModel.factory(container)),
                onFinished = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel(factory = DashboardViewModel.factory(container)),
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel(factory = SettingsViewModel.factory(container)),
                onBack = { navController.popBackStack() },
                onOpenDisplayDiagnostic = { navController.navigate(Routes.DISPLAY_DIAGNOSTIC) },
            )
        }
        composable(Routes.DISPLAY_DIAGNOSTIC) {
            DisplayDiagnosticScreen(
                viewModel = viewModel(factory = DisplayDiagnosticViewModel.factory(container)),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
