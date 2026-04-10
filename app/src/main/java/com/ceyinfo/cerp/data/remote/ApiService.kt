package com.ceyinfo.cerp.data.remote

import com.ceyinfo.cerp.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginData>>

    @GET("auth/verify_module")
    suspend fun verifyModule(): Response<ApiResponse<VerifyModuleData>>

    @POST("auth/select-unit")
    suspend fun selectUnit(@Body request: SelectUnitRequest): Response<ApiResponse<SelectUnitData>>

    @POST("site/select-unit")
    suspend fun selectUnitMobile(@Body request: SelectUnitRequest): Response<ApiResponse<SelectUnitData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    // ── Business Units (projects & sites only, with descendants) ──

    @GET("site/permitted-units")
    suspend fun getPermittedUnits(): Response<ApiResponse<List<BusinessUnit>>>

    // ── Photos ──

    @Multipart
    @POST("site/photos")
    suspend fun uploadPhotos(
        @Part files: List<MultipartBody.Part>,
        @Part("category") category: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("weather") weather: RequestBody?,
        @Part("gps_latitude") gpsLatitude: RequestBody?,
        @Part("gps_longitude") gpsLongitude: RequestBody?,
        @Part("gps_accuracy") gpsAccuracy: RequestBody?,
        @Part("captured_at") capturedAt: RequestBody?
    ): Response<ApiResponse<List<ProgressPhoto>>>

    @GET("site/photos")
    suspend fun getPhotos(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null
    ): Response<ApiResponse<List<ProgressPhoto>>>

    // ── Daily Reports ──

    @POST("site/daily-reports")
    suspend fun createDailyReport(@Body request: DailyReportRequest): Response<ApiResponse<DailyReport>>

    @PATCH("site/daily-reports/{id}")
    suspend fun updateDailyReport(
        @Path("id") id: String,
        @Body request: DailyReportRequest
    ): Response<ApiResponse<DailyReport>>

    @POST("site/daily-reports/{id}/submit")
    suspend fun submitDailyReport(@Path("id") id: String): Response<ApiResponse<DailyReport>>

    @GET("site/daily-reports")
    suspend fun getDailyReports(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<DailyReport>>>
}
