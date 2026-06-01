package com.yusufkilinc.mediatrimmer.domain.model

import android.net.Uri

data class MediaFile(
    val uri: Uri,
    val resolvedPath: String,   // Absolute path resolved for FFmpegKit
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int = 0,
    val height: Int = 0,
    val isVideo: Boolean
) {
    val isAudio: Boolean get() = !isVideo
    val format: MediaFormat? get() = MediaFormat.fromPath(resolvedPath)
}
