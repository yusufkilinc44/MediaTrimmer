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
        OperationType.EXTRACT_AUDIO -> MediaFormat.M4A
        else -> {
            when {
                sourceFormat != null && sourceFormat in MediaFormat.videoFormats -> sourceFormat
                sourceFormat != null && sourceFormat in MediaFormat.audioFormats -> sourceFormat
                // Source format not Transformer-supported — pick best default
                sourceFormat != null && sourceFormat.isAudioOnly -> MediaFormat.M4A
                sourceFormat != null -> MediaFormat.MP4
                else -> MediaFormat.MP4
            }
        }
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
