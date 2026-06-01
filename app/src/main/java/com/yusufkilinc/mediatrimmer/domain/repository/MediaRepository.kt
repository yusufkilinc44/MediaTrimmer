package com.yusufkilinc.mediatrimmer.domain.repository

import com.yusufkilinc.mediatrimmer.domain.model.ProcessingState
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun executeFFmpegCommand(command: String, totalDurationMs: Long): Flow<ProcessingState>
    suspend fun probeMediaDurationMs(path: String): Long
    fun cancelCurrentJob()
}
