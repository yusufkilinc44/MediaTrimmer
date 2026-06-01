package com.yusufkilinc.mediatrimmer.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.yusufkilinc.mediatrimmer.domain.model.OperationType
import com.yusufkilinc.mediatrimmer.domain.model.ProcessingState
import com.yusufkilinc.mediatrimmer.domain.model.TrimConfig
import com.yusufkilinc.mediatrimmer.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaRepository {

    @Volatile private var activeTransformer: Transformer? = null

    /**
     * Simple suspend function that runs Transformer and returns the output path.
     * Throws on failure with a descriptive message.
     */
    suspend fun transformMedia(config: TrimConfig): String {
        // Ensure output directory exists
        File(config.outputPath).parentFile?.mkdirs()

        // Verify source file
        val sourceFile = File(config.sourceFilePath)
        if (!sourceFile.exists()) {
            throw IllegalStateException("Source file not found: ${sourceFile.name}")
        }
        if (sourceFile.length() == 0L) {
            throw IllegalStateException("Source file is empty: ${sourceFile.name}")
        }

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                try {
                    val transformer = Transformer.Builder(context)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(
                                composition: Composition,
                                exportResult: ExportResult
                            ) {
                                activeTransformer = null
                                if (cont.isActive) cont.resume(config.outputPath)
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException
                            ) {
                                activeTransformer = null
                                if (cont.isActive) {
                                    val cause = exportException.cause?.message ?: ""
                                    val detail = exportException.message ?: "Export failed"
                                    val msg = if (cause.isNotEmpty() && cause != detail) {
                                        "$detail — $cause"
                                    } else {
                                        detail
                                    }
                                    cont.resumeWithException(RuntimeException(msg))
                                }
                            }
                        })
                        .build()

                    activeTransformer = transformer

                    val mediaItemBuilder = MediaItem.Builder()
                        .setUri(Uri.fromFile(sourceFile))

                    if (config.operation != OperationType.CONVERT) {
                        mediaItemBuilder.setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(config.startMs)
                                .setEndPositionMs(config.endMs)
                                .build()
                        )
                    }

                    val editedMediaItem = EditedMediaItem.Builder(mediaItemBuilder.build())
                        .setRemoveVideo(config.operation == OperationType.EXTRACT_AUDIO)
                        .build()

                    transformer.start(editedMediaItem, config.outputPath)

                    cont.invokeOnCancellation {
                        transformer.cancel()
                        activeTransformer = null
                    }
                } catch (e: Exception) {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            RuntimeException("Failed to start transformer: ${e.message}", e)
                        )
                    }
                }
            }
        }
    }

    override fun executeTransformation(config: TrimConfig): Flow<ProcessingState> = callbackFlow {
        // Ensure output directory exists
        File(config.outputPath).parentFile?.mkdirs()

        val sourceFile = File(config.sourceFilePath)
        if (!sourceFile.exists()) {
            trySend(ProcessingState.Error("Source file not found: ${sourceFile.name}"))
            close()
            return@callbackFlow
        }

        var transformer: Transformer? = null

        withContext(Dispatchers.Main) {
            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    trySend(ProcessingState.Complete(config.outputPath))
                    close()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    val cause = exportException.cause?.message ?: ""
                    val detail = exportException.message ?: "Export failed"
                    val msg = if (cause.isNotEmpty() && cause != detail) {
                        "$detail — $cause"
                    } else {
                        detail
                    }
                    trySend(ProcessingState.Error(msg))
                    close()
                }
            }

            try {
                transformer = Transformer.Builder(context)
                    .addListener(listener)
                    .build()
                    .also { activeTransformer = it }

                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(Uri.fromFile(sourceFile))

                if (config.operation != OperationType.CONVERT) {
                    mediaItemBuilder.setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(config.startMs)
                            .setEndPositionMs(config.endMs)
                            .build()
                    )
                }

                val editedMediaItem = EditedMediaItem.Builder(mediaItemBuilder.build())
                    .setRemoveVideo(config.operation == OperationType.EXTRACT_AUDIO)
                    .build()

                transformer?.start(editedMediaItem, config.outputPath)
            } catch (e: Exception) {
                trySend(ProcessingState.Error("Failed to start: ${e.message}"))
                close()
            }
        }

        launch(Dispatchers.Main) {
            while (true) {
                delay(250)
                val holder = ProgressHolder()
                val state = transformer?.getProgress(holder) ?: break
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    trySend(ProcessingState.Progress(holder.progress))
                }
            }
        }

        awaitClose {
            activeTransformer?.let { t ->
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    t.cancel()
                } else {
                    android.os.Handler(Looper.getMainLooper()).post { t.cancel() }
                }
            }
            activeTransformer = null
        }
    }

    override suspend fun probeMediaDurationMs(path: String): Long = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L
            retriever.release()
            duration
        } catch (_: Exception) {
            0L
        }
    }

    override fun cancelCurrentJob() {
        activeTransformer?.let { t ->
            android.os.Handler(Looper.getMainLooper()).post { t.cancel() }
        }
    }
}
