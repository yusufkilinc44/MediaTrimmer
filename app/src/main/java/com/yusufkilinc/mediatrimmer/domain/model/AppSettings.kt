package com.yusufkilinc.mediatrimmer.domain.model

data class AppSettings(
    val language: String = "en",    // "en" | "tr"
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}
