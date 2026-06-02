package com.yusufkilinc.mediatrimmer.domain.usecase

import com.yusufkilinc.mediatrimmer.domain.model.MediaFormat
import com.yusufkilinc.mediatrimmer.domain.model.OperationType

/**
 * Provides output-format suggestions for media operations.
 * Media3 Transformer only supports MP4, M4A, and WebM containers.
 */
object FFmpegCommandBuilder {

    private val TRANSFORMER_VIDEO_FORMATS = listOf(MediaFormat.MP4, MediaFormat.WEBM)
    private val TRANSFORMER_AUDIO_FORMATS = listOf(MediaFormat.M4A, MediaFormat.MP4)

    fun suggestOutputFormat(
        sourceFormat: MediaFormat?,
        operation: OperationType
    ): MediaFormat = when (operation) {
        OperationType.EXTRACT_AUDIO -> MediaFormat.M4A
        OperationType.CONVERT -> {
            if (sourceFormat != null && !sourceFormat.isAudioOnly) MediaFormat.MP4
            else MediaFormat.M4A
        }
        else -> sourceFormat?.let {
            if (it in TRANSFORMER_VIDEO_FORMATS || it in TRANSFORMER_AUDIO_FORMATS) it
            else if (it.isAudioOnly) MediaFormat.M4A else MediaFormat.MP4
        } ?: MediaFormat.MP4
    }

    fun availableOutputFormats(
        sourceIsVideo: Boolean,
        operation: OperationType,
        sourceFormat: MediaFormat? = null
    ): List<MediaFormat> = when (operation) {
        OperationType.TRIM -> {
            if (sourceIsVideo) TRANSFORMER_VIDEO_FORMATS else TRANSFORMER_AUDIO_FORMATS
        }
        OperationType.EXTRACT_AUDIO -> TRANSFORMER_AUDIO_FORMATS
        OperationType.CONVERT -> {
            if (sourceIsVideo) TRANSFORMER_VIDEO_FORMATS + TRANSFORMER_AUDIO_FORMATS
            else TRANSFORMER_AUDIO_FORMATS
        }
    }
}
