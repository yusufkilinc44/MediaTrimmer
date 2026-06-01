package com.yusufkilinc.mediatrimmer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufkilinc.mediatrimmer.data.local.datastore.AppSettingsDataStore
import com.yusufkilinc.mediatrimmer.domain.model.AppSettings
import com.yusufkilinc.mediatrimmer.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: AppSettingsDataStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = dataStore.appSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    fun setLanguage(lang: String) {
        viewModelScope.launch { dataStore.setLanguage(lang) }
    }

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { dataStore.setTheme(theme) }
    }
}
