package com.yusufkilinc.mediatrimmer.core.navigation

import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Trim : Screen("trim/{encodedPath}") {
        fun createRoute(path: String): String =
            "trim/${URLEncoder.encode(path, "UTF-8")}"
    }
    data object Batch : Screen("batch")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object About : Screen("about")
}
