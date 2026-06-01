package com.yusufkilinc.mediatrimmer.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufkilinc.mediatrimmer.data.local.db.entity.ProcessingHistoryEntity
import com.yusufkilinc.mediatrimmer.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val history: StateFlow<List<ProcessingHistoryEntity>> = historyRepository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deleteEntry(id: String) {
        viewModelScope.launch { historyRepository.deleteById(id) }
    }

    fun clearAll() {
        viewModelScope.launch { historyRepository.clearAll() }
    }
}
