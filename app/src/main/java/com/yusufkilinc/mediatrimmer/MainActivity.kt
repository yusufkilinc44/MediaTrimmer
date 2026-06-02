package com.yusufkilinc.mediatrimmer

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufkilinc.mediatrimmer.core.theme.MediaTrimmerTheme
import com.yusufkilinc.mediatrimmer.data.local.datastore.dataStore
import com.yusufkilinc.mediatrimmer.domain.model.ThemeMode
import com.yusufkilinc.mediatrimmer.presentation.AppRoot
import com.yusufkilinc.mediatrimmer.presentation.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // URI passed when app is opened via intent (e.g., "Open with MediaTrimmer")
    private var incomingMediaUri: Uri? = null

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language before the activity inflates any resources
        val lang = runBlocking {
            newBase.dataStore.data
                .map { prefs -> prefs[stringPreferencesKey("language")] ?: "tr" }
                .first()
        }
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(Locale(lang))
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle "Open with" and "Share to" intents
        incomingMediaUri = when (intent?.action) {
            android.content.Intent.ACTION_VIEW -> intent.data
            android.content.Intent.ACTION_SEND -> {
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM) as? Uri
            }
            else -> null
        }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            val darkTheme = when (settings.themeMode) {
                ThemeMode.DARK   -> true
                ThemeMode.LIGHT  -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MediaTrimmerTheme(darkTheme = darkTheme) {
                AppRoot(
                    incomingUri = incomingMediaUri,
                    onLanguageChanged = { recreate() }
                )
            }
        }
    }
}
