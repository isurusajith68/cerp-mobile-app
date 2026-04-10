package com.ceyinfo.cerp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ceyinfo.cerp.data.local.SyncQueueEntity
import com.ceyinfo.cerp.data.model.DailyReportRequest
import com.ceyinfo.cerp.data.remote.ApiClient
import com.ceyinfo.cerp.data.repository.SyncRepository
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val syncRepo = SyncRepository(context)
    private val api = ApiClient.getService(context)
    private val gson = Gson()

    override suspend fun doWork(): Result {
        val pending = syncRepo.getPendingItems()
        if (pending.isEmpty()) return Result.success()

        var allSuccess = true

        for (item in pending) {
            if (item.retries >= item.maxRetries) continue

            // Mark as uploading
            syncRepo.updateItem(item.copy(
                status = SyncQueueEntity.STATUS_UPLOADING,
                lastAttemptAt = System.currentTimeMillis()
            ))

            try {
                when (item.type) {
                    SyncQueueEntity.TYPE_PHOTO -> uploadPhoto(item)
                    SyncQueueEntity.TYPE_DAILY_REPORT -> uploadReport(item)
                }

                // Success
                syncRepo.updateItem(item.copy(status = SyncQueueEntity.STATUS_COMPLETED))

            } catch (e: Exception) {
                allSuccess = false
                syncRepo.updateItem(item.copy(
                    status = SyncQueueEntity.STATUS_FAILED,
                    retries = item.retries + 1,
                    error = e.message ?: "Unknown error"
                ))
            }
        }

        return if (allSuccess) Result.success() else Result.retry()
    }

    private suspend fun uploadPhoto(item: SyncQueueEntity) {
        val file = File(item.filePath ?: throw Exception("No file path"))
        if (!file.exists()) throw Exception("File not found: ${file.name}")

        val metadata = gson.fromJson(item.metadataJson, Map::class.java) as Map<String, String>

        val filePart = MultipartBody.Part.createFormData(
            "files", file.name,
            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )

        val response = api.uploadPhotos(
            files = listOf(filePart),
            category = (metadata["category"] ?: "other").toRequestBody(),
            description = metadata["description"]?.ifEmpty { null }?.toRequestBody(),
            weather = metadata["weather"]?.ifEmpty { null }?.toRequestBody(),
            gpsLatitude = metadata["gps_latitude"]?.ifEmpty { null }?.toRequestBody(),
            gpsLongitude = metadata["gps_longitude"]?.ifEmpty { null }?.toRequestBody(),
            gpsAccuracy = metadata["gps_accuracy"]?.ifEmpty { null }?.toRequestBody(),
            capturedAt = metadata["captured_at"]?.toRequestBody()
        )

        if (!response.isSuccessful || response.body()?.success != true) {
            throw Exception(response.body()?.message ?: "Upload failed (${response.code()})")
        }

        // Clean up compressed file after successful upload
        file.delete()
    }

    private suspend fun uploadReport(item: SyncQueueEntity) {
        val request = gson.fromJson(item.metadataJson, DailyReportRequest::class.java)

        val response = api.createDailyReport(request)
        if (!response.isSuccessful || response.body()?.success != true) {
            throw Exception(response.body()?.message ?: "Report creation failed (${response.code()})")
        }
    }
}
