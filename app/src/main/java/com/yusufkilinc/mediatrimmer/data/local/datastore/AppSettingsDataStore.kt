package com.yusufkilinc.mediatrimmer.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yusufkilinc.mediatrimmer.domain.model.AppSettings
import com.yusufkilinc.mediatrimmer.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_LANGUAGE       = stringPreferencesKey("language")
        val KEY_THEME          = stringPreferencesKey("theme")
        val KEY_OUTPUT_DIR_URI = stringPreferencesKey("output_dir_uri")
    }

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            language     = prefs[KEY_LANGUAGE] ?: "tr",
            themeMode    = runCatching {
                ThemeMode.valueOf(prefs[KEY_THEME] ?: "LIGHT")
            }.getOrDefault(ThemeMode.LIGHT),
            outputDirUri = prefs[KEY_OUTPUT_DIR_URI]
        )
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setTheme(theme: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun setOutputDirUri(uri: String?) {
        context.dataStore.edit {
            if (uri != null) it[KEY_OUTPUT_DIR_URI] = uri
            else it.remove(KEY_OUTPUT_DIR_URI)
        }
    }

    suspend fun getOutputDirUri(): String? {
        return context.dataStore.data.first()[KEY_OUTPUT_DIR_URI]
    }
}
