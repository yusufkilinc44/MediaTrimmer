package com.yusufkilinc.mediatrimmer.domain.usecase

import com.yusufkilinc.mediatrimmer.domain.model.MediaFormat
import com.yusufkilinc.mediatrimmer.domain.model.OperationType

object FFmpegCommandBuilder {

    fun suggestOutputFormat(
        sourceFormat: MediaFormat?,
        operation: OperationType
    ): MediaFormat = when (operation) {
        OperationType.EXTRACT_AUDIO -> sourceFormat?.takeIf { it.isAudioOnly } ?: MediaFormat.MP3
        OperationType.CONVERT -> sourceFormat ?: MediaFormat.MP4
        OperationType.TRIM -> sourceFormat ?: MediaFormat.MP4
    }

    fun availableOutputFormats(
        sourceIsVideo: Boolean,
        operation: OperationType,
        sourceFormat: MediaFormat? = null
    ): List<MediaFormat> = when (operation) {
        OperationType.TRIM -> {
            if (sourceIsVideo) MediaFormat.orderedFormats(true, sourceFormat)
            else MediaFormat.orderedFormats(false, sourceFormat)
        }
        OperationType.EXTRACT_AUDIO -> MediaFormat.orderedFormats(false, null)
        OperationType.CONVERT -> {
            if (sourceIsVideo) {
                MediaFormat.orderedFormats(true, sourceFormat) + MediaFormat.audioFormats
            } else {
                MediaFormat.orderedFormats(false, sourceFormat)
            }
        }
    }
}
