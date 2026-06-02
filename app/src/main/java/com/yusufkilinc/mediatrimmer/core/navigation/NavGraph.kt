package com.yusufkilinc.mediatrimmer.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yusufkilinc.mediatrimmer.presentation.about.AboutScreen
import com.yusufkilinc.mediatrimmer.presentation.batch.BatchScreen
import com.yusufkilinc.mediatrimmer.presentation.history.HistoryScreen
import com.yusufkilinc.mediatrimmer.presentation.home.HomeScreen
import com.yusufkilinc.mediatrimmer.presentation.settings.SettingsScreen
import com.yusufkilinc.mediatrimmer.presentation.trim.TrimScreen

@Composable
fun MediaTrimmerNavGraph(
    navController: NavHostController,
    incomingUri: android.net.Uri? = null,
    onLanguageChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                incomingUri = incomingUri,
                onFileReady = { path ->
                    navController.navigate(Screen.Trim.createRoute(path))
                }
            )
        }

        composable(
            route = Screen.Trim.route,
            arguments = listOf(navArgument("encodedPath") { type = NavType.StringType })
        ) { backStack ->
            val encodedPath = backStack.arguments?.getString("encodedPath") ?: ""
            val filePath = try {
                java.net.URLDecoder.decode(encodedPath, "UTF-8")
            } catch (_: Exception) { encodedPath }

            TrimScreen(
                filePath = filePath,
                onBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Batch.route) {
            BatchScreen()
        }

        composable(Screen.History.route) {
            HistoryScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onLanguageChanged = onLanguageChanged
            )
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
