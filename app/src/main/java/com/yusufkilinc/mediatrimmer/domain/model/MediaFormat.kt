package com.yusufkilinc.mediatrimmer.domain.model

enum class MediaFormat(
    val extension: String,
    val displayName: String,
    val mimeType: String,
    val isAudioOnly: Boolean,
    val ffmpegCodecArgs: String
) {
    // --- Video formats ---
    MP4(
        extension = "mp4",
        displayName = "MP4",
        mimeType = "video/mp4",
        isAudioOnly = false,
        ffmpegCodecArgs = "-c:v libx264 -preset fast -crf 23 -c:a aac -b:a 192k"
    ),
    MKV(
        extension = "mkv",
        displayName = "MKV",
        mimeType = "video/x-matroska",
        isAudioOnly = false,
        ffmpegCodecArgs = "-c:v libx264 -preset fast -crf 23 -c:a aac -b:a 192k"
    ),
    AVI(
        extension = "avi",
        displayName = "AVI",
        mimeType = "video/x-msvideo",
        isAudioOnly = false,
        ffmpegCodecArgs = "-c:v libxvid -q:v 4 -c:a libmp3lame -q:a 2"
    ),
    MOV(
        extension = "mov",
        displayName = "MOV",
        mimeType = "video/quicktime",
        isAudioOnly = false,
        ffmpegCodecArgs = "-c:v libx264 -preset fast -crf 23 -c:a aac -b:a 192k"
    ),
    WEBM(
        extension = "webm",
        displayName = "WebM",
        mimeType = "video/webm",
        isAudioOnly = false,
        ffmpegCodecArgs = "-c:v libvpx-vp9 -crf 30 -b:v 0 -c:a libopus -b:a 128k"
    ),
    FLV(
        extension = "flv",
        displayName = "FLV",
        mimeType = "video/x-flv",
        isAudioOnly = false,
        ffmpegCodecArgs = "-c:v libx264 -preset fast -crf 23 -c:a aac -b:a 192k"
    ),
    WMV(
        extension = "wmv",
        displayName = "WMV",
        mimeType = "video/x-ms-wmv",
        isAudioOnly = false,
        ffmpegCodecArgs = "-c:v wmv2 -b:v 1500k -c:a wmav2 -b:a 192k"
    ),
    GP3(
        extension = "3gp",
        displayName = "3GP",
        mimeType = "video/3gpp",
        isAudioOnly = false,
        ffmpegCodecArgs = "-c:v libx264 -preset fast -crf 28 -c:a aac -b:a 128k"
    ),
    TS(
        extension = "ts",
        displayName = "TS",
        mimeType = "video/mp2t",
        isAudioOnly = false,
        ffmpegCodecArgs = "-c:v libx264 -preset fast -crf 23 -c:a aac -b:a 192k"
    ),

    // --- Audio formats ---
    MP3(
        extension = "mp3",
        displayName = "MP3",
        mimeType = "audio/mpeg",
        isAudioOnly = true,
        ffmpegCodecArgs = "-c:a libmp3lame -q:a 2"
    ),
    AAC(
        extension = "aac",
        displayName = "AAC",
        mimeType = "audio/aac",
        isAudioOnly = true,
        ffmpegCodecArgs = "-c:a aac -b:a 192k"
    ),
    OGG(
        extension = "ogg",
        displayName = "OGG",
        mimeType = "audio/ogg",
        isAudioOnly = true,
        ffmpegCodecArgs = "-c:a libvorbis -q:a 6"
    ),
    FLAC(
        extension = "flac",
        displayName = "FLAC",
        mimeType = "audio/flac",
        isAudioOnly = true,
        ffmpegCodecArgs = "-c:a flac"
    ),
    WAV(
        extension = "wav",
        displayName = "WAV",
        mimeType = "audio/wav",
        isAudioOnly = true,
        ffmpegCodecArgs = "-c:a pcm_s16le"
    ),
    M4A(
        extension = "m4a",
        displayName = "M4A",
        mimeType = "audio/mp4",
        isAudioOnly = true,
        ffmpegCodecArgs = "-c:a aac -b:a 192k"
    ),
    OPUS(
        extension = "opus",
        displayName = "OPUS",
        mimeType = "audio/opus",
        isAudioOnly = true,
        ffmpegCodecArgs = "-c:a libopus -b:a 128k"
    ),
    WMA(
        extension = "wma",
        displayName = "WMA",
        mimeType = "audio/x-ms-wma",
        isAudioOnly = true,
        ffmpegCodecArgs = "-c:a wmav2 -b:a 192k"
    );

    companion object {
        val videoFormats: List<MediaFormat> get() = entries.filter { !it.isAudioOnly }
        val audioFormats: List<MediaFormat> get() = entries.filter { it.isAudioOnly }

        fun fromExtension(ext: String): MediaFormat? =
            entries.firstOrNull { it.extension.equals(ext, ignoreCase = true) }

        fun fromPath(path: String): MediaFormat? =
            fromExtension(path.substringAfterLast('.', ""))
    }
}
