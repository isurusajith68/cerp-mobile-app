package com.ceyinfo.cerp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val type: String,  // "photo" or "daily_report"

    val status: String = STATUS_PENDING,  // pending, uploading, completed, failed

    @ColumnInfo(name = "file_path")
    val filePath: String? = null,  // local compressed photo path

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String,  // JSON: category, description, gps, weather, etc.

    @ColumnInfo(name = "business_unit_id")
    val businessUnitId: String,

    @ColumnInfo(name = "project_id")
    val projectId: String? = null,

    val retries: Int = 0,

    @ColumnInfo(name = "max_retries")
    val maxRetries: Int = 5,

    val error: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_UPLOADING = "uploading"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"

        const val TYPE_PHOTO = "photo"
        const val TYPE_DAILY_REPORT = "daily_report"
    }
}
