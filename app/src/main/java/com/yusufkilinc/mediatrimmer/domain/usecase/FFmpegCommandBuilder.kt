package com.yusufkilinc.mediatrimmer.domain.usecase

import com.yusufkilinc.mediatrimmer.domain.model.MediaFormat
import com.yusufkilinc.mediatrimmer.domain.model.OperationType
import com.yusufkilinc.mediatrimmer.domain.model.TrimConfig
import java.util.Locale

object FFmpegCommandBuilder {

    /**
     * Build the FFmpeg command string for a given TrimConfig.
     *
     * Strategy:
     *  - TRIM + same container family → stream copy (-c copy), fastest
     *  - TRIM + different format      → re-encode with target codec args
     *  - EXTRACT_AUDIO               → -vn, audio codec only
     *  - CONVERT                     → full file, re-encode to target format
     */
    fun build(config: TrimConfig): String {
        val inputPath = config.sourceFilePath.escapeForFFmpeg()
        val outputPath = config.outputPath.escapeForFFmpeg()
        val startSec = msToSec(config.startMs)
        val endSec = msToSec(config.endMs)
        val fmt = config.outputMediaFormat

        return when (config.operation) {
            OperationType.TRIM -> buildTrimCommand(
                inputPath, outputPath, startSec, endSec, config
            )
            OperationType.EXTRACT_AUDIO -> buildExtractAudioCommand(
                inputPath, outputPath, startSec, endSec, fmt.ffmpegCodecArgs
            )
            OperationType.CONVERT -> buildConvertCommand(
                inputPath, outputPath, fmt.ffmpegCodecArgs
            )
        }
    }

    // ── Trim ─────────────────────────────────────────────────────────────────

    private fun buildTrimCommand(
        inputPath: String,
        outputPath: String,
        startSec: String,
        endSec: String,
        config: TrimConfig
    ): String {
        val sourceExt = config.sourceFilePath.substringAfterLast('.', "").lowercase(Locale.US)
        val destExt = config.outputMediaFormat.extension.lowercase(Locale.US)
        val sameContainer = sourceExt == destExt ||
                // Treat m4v/mp4 as compatible, etc.
                (sourceExt in setOf("mp4", "m4v") && destExt in setOf("mp4", "m4v"))

        return if (sameContainer && !config.outputMediaFormat.isAudioOnly) {
            // Fast path: stream copy — no re-encode, handles most common case
            "-ss $startSec -to $endSec -i $inputPath -c copy -avoid_negative_ts make_zero $outputPath"
        } else {
            // Re-encode path — format conversion + trim
            "-ss $startSec -to $endSec -i $inputPath ${config.outputMediaFormat.ffmpegCodecArgs} -map_metadata 0 $outputPath"
        }
    }

    // ── Extract audio ────────────────────────────────────────────────────────

    private fun buildExtractAudioCommand(
        inputPath: String,
        outputPath: String,
        startSec: String,
        endSec: String,
        codecArgs: String
    ): String {
        // -vn = no video; trim range is included when start != 0 or end != duration
        return "-ss $startSec -to $endSec -i $inputPath -vn -map 0:a $codecArgs $outputPath"
    }

    fun buildExtractAudioFull(inputPath: String, outputPath: String, codecArgs: String): String {
        // Full file extraction (no time range)
        return "-i \"$inputPath\" -vn -map 0:a $codecArgs \"$outputPath\""
    }

    // ── Convert ──────────────────────────────────────────────────────────────

    private fun buildConvertCommand(
        inputPath: String,
        outputPath: String,
        codecArgs: String
    ): String {
        return "-i $inputPath $codecArgs -map_metadata 0 $outputPath"
    }

    // ── Suggest formats ───────────────────────────────────────────────────────

    fun suggestOutputFormat(
        sourceFormat: MediaFormat?,
        operation: OperationType
    ): MediaFormat = when (operation) {
        OperationType.TRIM          -> sourceFormat ?: MediaFormat.MP4
        OperationType.EXTRACT_AUDIO -> MediaFormat.MP3
        OperationType.CONVERT       -> MediaFormat.MP4
    }

    fun availableOutputFormats(
        sourceIsVideo: Boolean,
        operation: OperationType
    ): List<MediaFormat> = when (operation) {
        OperationType.TRIM          -> if (sourceIsVideo) MediaFormat.videoFormats
                                       else MediaFormat.audioFormats
        OperationType.EXTRACT_AUDIO -> MediaFormat.audioFormats
        OperationType.CONVERT       -> if (sourceIsVideo)
                                           MediaFormat.videoFormats + MediaFormat.audioFormats
                                       else MediaFormat.audioFormats
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun msToSec(ms: Long): String =
        String.format(Locale.US, "%.3f", ms / 1000.0)

    private fun String.escapeForFFmpeg(): String = "\"${this.replace("\"", "\\\"")}\""
}
