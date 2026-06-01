package com.yusufkilinc.mediatrimmer.domain.usecase

import com.yusufkilinc.mediatrimmer.domain.model.MediaFormat
import com.yusufkilinc.mediatrimmer.domain.model.OperationType

/**
 * Provides output-format suggestions for media operations.
 * Previously built FFmpeg command strings; now only serves as
 * a format-selection helper (actual processing via Media3 Transformer).
 */
object FFmpegCommandBuilder {

    fun suggestOutputFormat(
        sourceFormat: MediaFormat?,
        operation: OperationType
    ): MediaFormat = when (operation) {
        OperationType.EXTRACT_AUDIO -> sourceFormat?.takeIf { it.isAudioOnly } ?: MediaFormat.M4A
        else -> sourceFormat ?: MediaFormat.MP4
    }

    fun availableOutputFormats(
        sourceIsVideo: Boolean,
        operation: OperationType,
        sourceFormat: MediaFormat? = null
    ): List<MediaFormat> = when (operation) {
        OperationType.TRIM -> MediaFormat.orderedFormats(sourceIsVideo, sourceFormat)
        OperationType.EXTRACT_AUDIO -> MediaFormat.orderedFormats(false, sourceFormat?.takeIf { it.isAudioOnly })
        OperationType.CONVERT -> {
            if (sourceIsVideo) {
                MediaFormat.orderedFormats(true, sourceFormat) +
                    MediaFormat.orderedFormats(false, null)
            } else {
                MediaFormat.orderedFormats(false, sourceFormat)
            }
        }
    }
}
