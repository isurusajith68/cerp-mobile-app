package com.ceyinfo.cerp.data.model

import com.google.gson.annotations.SerializedName

// ── API Response wrapper ──

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
    val count: Int? = null
)

// ── Auth ──

data class LoginRequest(
    val email: String,
    val password: String,
    val organizationId: String? = null
)

data class LoginData(
    val userId: String,
    val email: String,
    val organizationId: String,
    val isOwner: Boolean,
    val isGypsy: Boolean,
    val permittedBusinessUnits: List<BusinessUnit>,
    val selectOrgRequired: Boolean? = null,
    val organizations: List<Organization>? = null
)

data class Organization(
    val id: String,
    val name: String
)

data class BusinessUnit(
    val id: String,
    val name: String,
    val code: String? = null,
    @SerializedName("parent_id") val parentId: String? = null,
    val level: String? = null,
    @SerializedName("level_order") val levelOrder: Int? = null,
    val path: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)

data class SelectUnitRequest(
    val businessUnitId: String
)

data class SelectUnitData(
    val businessUnitId: String,
    val businessUnitName: String,
    val roleLabel: String? = null,
    val roleName: String? = null,
    val isOwner: Boolean
)

data class VerifyModuleData(
    val userId: String,
    val email: String,
    val organizationId: String,
    val organizationName: String,
    val employeeName: String? = null,
    val roleLabel: String? = null,
    val isOwner: Boolean,
    val isGypsy: Boolean,
    val businessUnitId: String? = null,
    val businessUnitName: String? = null,
    val businessUnitLevel: String? = null
)

// ── Photos ──

data class ProgressPhoto(
    val id: String,
    val category: String,
    val description: String? = null,
    @SerializedName("gps_latitude") val gpsLatitude: Double? = null,
    @SerializedName("gps_longitude") val gpsLongitude: Double? = null,
    @SerializedName("captured_at") val capturedAt: String,
    val weather: String? = null,
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// ── Daily Report ──

data class DailyReportRequest(
    @SerializedName("report_date") val reportDate: String,
    @SerializedName("work_summary") val workSummary: String,
    val weather: String? = null,
    @SerializedName("temperature_high") val temperatureHigh: Double? = null,
    @SerializedName("temperature_low") val temperatureLow: Double? = null,
    @SerializedName("workforce_count") val workforceCount: Int? = null,
    @SerializedName("subcontractor_count") val subcontractorCount: Int? = null,
    val issues: String? = null,
    @SerializedName("safety_incidents") val safetyIncidents: Int? = null,
    @SerializedName("safety_notes") val safetyNotes: String? = null,
    @SerializedName("material_notes") val materialNotes: String? = null,
    @SerializedName("equipment_notes") val equipmentNotes: String? = null,
    @SerializedName("project_id") val projectId: String? = null
)

data class DailyReport(
    val id: String,
    @SerializedName("report_date") val reportDate: String,
    val status: String,
    @SerializedName("work_summary") val workSummary: String,
    val weather: String? = null,
    @SerializedName("temperature_high") val temperatureHigh: Double? = null,
    @SerializedName("temperature_low") val temperatureLow: Double? = null,
    @SerializedName("workforce_count") val workforceCount: Int? = null,
    @SerializedName("subcontractor_count") val subcontractorCount: Int? = null,
    val issues: String? = null,
    @SerializedName("safety_incidents") val safetyIncidents: Int? = null,
    @SerializedName("safety_notes") val safetyNotes: String? = null,
    @SerializedName("material_notes") val materialNotes: String? = null,
    @SerializedName("equipment_notes") val equipmentNotes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
