package com.yusufkilinc.mediatrimmer.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ProcessingJob(
    val id: String = UUID.randomUUID().toString(),
    val configs: List<TrimConfig>,
    val isBatch: Boolean = false
)

sealed class ProcessingState {
    data class Progress(val percent: Int, val currentFileIndex: Int = 0) : ProcessingState()
    data class Complete(val outputPath: String) : ProcessingState()
    data class Error(val message: String, val logs: String = "") : ProcessingState()
    data object Cancelled : ProcessingState()
}
