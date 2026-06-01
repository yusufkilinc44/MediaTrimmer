package com.yusufkilinc.mediatrimmer.presentation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yusufkilinc.mediatrimmer.core.navigation.MediaTrimmerNavGraph
import com.yusufkilinc.mediatrimmer.core.navigation.Screen
import com.yusufkilinc.mediatrimmer.presentation.components.BottomNavBar

@Composable
fun AppRoot(
    incomingUri: Uri? = null,
    onLanguageChanged: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Hide bottom nav on TrimScreen and AboutScreen
    val showBottomBar = currentRoute !in listOf(Screen.Trim.route, Screen.About.route) &&
            !currentRoute.orEmpty().startsWith("trim/")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        MediaTrimmerNavGraph(
            navController = navController,
            incomingUri = incomingUri,
            onLanguageChanged = onLanguageChanged,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
