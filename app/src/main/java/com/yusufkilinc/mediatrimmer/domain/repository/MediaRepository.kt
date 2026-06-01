package com.yusufkilinc.mediatrimmer.domain.repository

import com.yusufkilinc.mediatrimmer.domain.model.ProcessingState
import com.yusufkilinc.mediatrimmer.domain.model.TrimConfig
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun executeTransformation(config: TrimConfig): Flow<ProcessingState>
    suspend fun probeMediaDurationMs(path: String): Long
    fun cancelCurrentJob()
}
