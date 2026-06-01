package com.yusufkilinc.mediatrimmer.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TrimConfig(
    val sourceFilePath: String,
    val sourceFileName: String,
    val sourceDurationMs: Long,
    val startMs: Long,
    val endMs: Long,
    val outputFormat: String,           // MediaFormat.name()
    val outputPath: String,
    val operation: OperationType = OperationType.TRIM,
    val preserveMetadata: Boolean = true
) {
    val outputMediaFormat: MediaFormat
        get() = MediaFormat.valueOf(outputFormat)

    val durationMs: Long get() = endMs - startMs
}

@Serializable
enum class OperationType {
    TRIM,           // Cut video/audio to time range (keep original media type)
    EXTRACT_AUDIO,  // Extract audio only from video
    CONVERT         // Full file format conversion (no trim)
}
