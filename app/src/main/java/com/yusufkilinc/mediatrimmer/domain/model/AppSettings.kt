package com.yusufkilinc.mediatrimmer.domain.model

data class AppSettings(
    val language: String = "tr",    // "en" | "tr"
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val outputDirUri: String? = null // SAF tree URI for custom output directory
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}
