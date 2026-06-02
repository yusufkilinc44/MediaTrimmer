package com.yusufkilinc.mediatrimmer.domain.usecase

import com.yusufkilinc.mediatrimmer.domain.model.MediaFormat
import com.yusufkilinc.mediatrimmer.domain.model.OperationType

/**
 * Provides output-format suggestions for media operations.
 * Actual processing is via Media3 Transformer which only supports MP4, M4A, and WebM.
 */
object FFmpegCommandBuilder {

    fun suggestOutputFormat(
        sourceFormat: MediaFormat?,
        operation: OperationType
    ): MediaFormat = when (operation) {
        OperationType.EXTRACT_AUDIO -> sourceFormat?.takeIf { it.isAudioOnly } ?: MediaFormat.MP3
        else -> sourceFormat ?: MediaFormat.MP4
    }

    fun availableOutputFormats(
        sourceIsVideo: Boolean,
        operation: OperationType,
        sourceFormat: MediaFormat? = null
    ): List<MediaFormat> = when (operation) {
        OperationType.TRIM -> MediaFormat.orderedFormats(sourceIsVideo, sourceFormat)
        OperationType.EXTRACT_AUDIO -> MediaFormat.orderedFormats(false, null)
        OperationType.CONVERT -> {
            if (sourceIsVideo) {
                // Video can convert to video or audio formats
                MediaFormat.videoFormats + MediaFormat.audioFormats
            } else {
                MediaFormat.orderedFormats(false, sourceFormat)
            }
        }
    }
}
