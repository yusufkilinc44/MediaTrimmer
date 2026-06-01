package com.yusufkilinc.mediatrimmer.domain.model

data class AppSettings(
    val language: String = "tr",    // "en" | "tr"
    val themeMode: ThemeMode = ThemeMode.LIGHT
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}
