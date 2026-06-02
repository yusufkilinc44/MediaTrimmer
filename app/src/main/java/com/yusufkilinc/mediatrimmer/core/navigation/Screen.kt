package com.yusufkilinc.mediatrimmer.core.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Trim : Screen("trim/{encodedPath}?origUri={origUri}") {
        fun createRoute(path: String, originalUri: String = ""): String {
            val enc = URLEncoder.encode(path, "UTF-8")
            val encUri = URLEncoder.encode(originalUri, "UTF-8")
            return "trim/$enc?origUri=$encUri"
        }
    }
    data object Batch : Screen("batch")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object About : Screen("about")
}
