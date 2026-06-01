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
        OperationType.TRIM          -> if (sourceFormat?.isAudioOnly == true) MediaFormat.M4A else MediaFormat.MP4
        OperationType.EXTRACT_AUDIO -> MediaFormat.M4A
        OperationType.CONVERT       -> if (sourceFormat?.isAudioOnly == true) MediaFormat.M4A else MediaFormat.MP4
    }

    fun availableOutputFormats(
        sourceIsVideo: Boolean,
        operation: OperationType
    ): List<MediaFormat> = when (operation) {
        OperationType.TRIM          -> if (sourceIsVideo) MediaFormat.videoFormats else MediaFormat.audioFormats
        OperationType.EXTRACT_AUDIO -> MediaFormat.audioFormats
        OperationType.CONVERT       -> if (sourceIsVideo) MediaFormat.videoFormats + MediaFormat.audioFormats
                                       else MediaFormat.audioFormats
    }
}
