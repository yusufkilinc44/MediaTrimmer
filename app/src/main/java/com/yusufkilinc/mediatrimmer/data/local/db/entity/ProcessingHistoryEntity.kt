package com.yusufkilinc.mediatrimmer.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processing_history")
data class ProcessingHistoryEntity(
    @PrimaryKey val id: String,
    val operationType: String,          // "TRIM" | "EXTRACT_AUDIO" | "CONVERT"
    val sourceFileName: String,
    val outputFilePath: String,
    val outputFormat: String,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val processingDurationMs: Long = 0L,
    val outputFileSizeBytes: Long = 0L,
    val sourceFileSizeBytes: Long = 0L,
    val sourceDurationMs: Long = 0L,
    val outputFileName: String = "",
    val status: String,                 // "COMPLETED" | "FAILED"
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
