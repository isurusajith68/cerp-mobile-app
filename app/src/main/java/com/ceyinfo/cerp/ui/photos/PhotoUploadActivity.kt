package com.ceyinfo.cerp.ui.photos

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.local.SyncQueueEntity
import com.ceyinfo.cerp.data.remote.ApiClient
import com.ceyinfo.cerp.data.repository.SyncRepository
import com.ceyinfo.cerp.databinding.ActivityPhotoUploadBinding
import com.ceyinfo.cerp.util.*
import com.ceyinfo.cerp.worker.SyncWorkerUtil
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoUploadBinding
    private lateinit var session: SessionManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var syncRepo: SyncRepository
    private lateinit var thumbAdapter: PhotoThumbAdapter

    private var currentPhotoUri: Uri? = null
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentAccuracy: Float? = null
    private var isUploading = false

    private val categories = arrayOf(
        "foundation", "structure", "electrical", "plumbing", "roofing",
        "finishing", "landscaping", "safety", "equipment", "material_delivery", "other"
    )
    private val weatherOptions = arrayOf("sunny", "cloudy", "rainy", "windy", "stormy")

    // Camera — single photo
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            processImages(listOf(currentPhotoUri!!))
        }
    }

    // Gallery — multiple photos
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            processImages(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        networkMonitor = NetworkMonitor(this)
        syncRepo = SyncRepository(this)

        thumbAdapter = PhotoThumbAdapter { position ->
            thumbAdapter.removeAt(position)
            updatePhotoCount()
        }
        binding.rvPhotos.layoutManager = GridLayoutManager(this, 3)
        binding.rvPhotos.adapter = thumbAdapter

        setupDropdowns()
        setupButtons()
        requestLocation()
    }

    private fun setupDropdowns() {
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            categories.map { it.replace("_", " ").replaceFirstChar { c -> c.uppercase() } })
        binding.dropdownCategory.setAdapter(categoryAdapter)

        val weatherAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            weatherOptions.map { it.replaceFirstChar { c -> c.uppercase() } })
        binding.dropdownWeather.setAdapter(weatherAdapter)
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnCamera.setOnClickListener {
            if (checkCameraPermission()) openCamera()
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnUpload.setOnClickListener { uploadAllPhotos() }
    }

    private fun checkCameraPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            return false
        }
        return true
    }

    private fun openCamera() {
        val dir = File(cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        cameraLauncher.launch(currentPhotoUri!!)
    }

    private fun processImages(uris: List<Uri>) {
        binding.layoutProcessing.visibility = View.VISIBLE
        binding.btnCamera.isEnabled = false
        binding.btnGallery.isEnabled = false

        lifecycleScope.launch {
            var processed = 0
            for (uri in uris) {
                try {
                    binding.tvProcessing.text = "Processing ${processed + 1} of ${uris.size}…"

                    val compressed = withContext(Dispatchers.IO) {
                        ImageCompressor.compress(this@PhotoUploadActivity, uri)
                    }

                    val watermarked = withContext(Dispatchers.IO) {
                        WatermarkUtil.apply(
                            compressed,
                            session.businessUnitName ?: "CERP",
                            binding.dropdownCategory.text?.toString() ?: "photo",
                            currentLatitude,
                            currentLongitude
                        )
                    }

                    thumbAdapter.addFile(watermarked)
                    processed++
                } catch (e: Exception) {
                    Toast.makeText(this@PhotoUploadActivity,
                        "Failed to process photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            binding.layoutProcessing.visibility = View.GONE
            binding.btnCamera.isEnabled = true
            binding.btnGallery.isEnabled = true
            updatePhotoCount()
        }
    }

    private fun updatePhotoCount() {
        val count = thumbAdapter.count()
        if (count > 0) {
            binding.tvPhotoCount.text = "$count"
            binding.tvPhotoCount.visibility = View.VISIBLE
            binding.btnUpload.isEnabled = !isUploading
            binding.btnUpload.text = "Upload $count Photo${if (count > 1) "s" else ""}"
        } else {
            binding.tvPhotoCount.visibility = View.GONE
            binding.btnUpload.isEnabled = false
            binding.btnUpload.text = getString(R.string.btn_upload)
        }
    }

    private fun uploadAllPhotos() {
        val files = thumbAdapter.getFiles()
        if (files.isEmpty()) return

        val categoryIndex = categories.indexOfFirst {
            it.replace("_", " ").replaceFirstChar { c -> c.uppercase() } ==
                binding.dropdownCategory.text?.toString()
        }
        val category = if (categoryIndex >= 0) categories[categoryIndex] else "other"
        val weather = binding.dropdownWeather.text?.toString()?.lowercase()?.ifEmpty { null }
        val description = binding.etDescription.text?.toString()?.trim()?.ifEmpty { null }

        isUploading = true
        binding.btnUpload.isEnabled = false
        binding.btnCamera.isEnabled = false
        binding.btnGallery.isEnabled = false

        if (networkMonitor.checkNetwork()) {
            uploadDirect(files, category, weather, description)
        } else {
            queueAll(files, category, weather, description)
        }
    }

    private fun uploadDirect(files: List<File>, category: String, weather: String?, description: String?) {
        binding.layoutUploadProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            var uploaded = 0
            var queued = 0

            for ((index, file) in files.withIndex()) {
                binding.tvUploadProgress.text = "Uploading ${index + 1} of ${files.size}…"

                try {
                    val api = ApiClient.getService(this@PhotoUploadActivity)
                    val filePart = MultipartBody.Part.createFormData(
                        "files", file.name,
                        file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    val capturedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                        .format(Date())

                    val response = api.uploadPhotos(
                        files = listOf(filePart),
                        category = category.toRequestBody(),
                        description = description?.toRequestBody(),
                        weather = weather?.toRequestBody(),
                        gpsLatitude = currentLatitude?.toString()?.toRequestBody(),
                        gpsLongitude = currentLongitude?.toString()?.toRequestBody(),
                        gpsAccuracy = currentAccuracy?.toString()?.toRequestBody(),
                        capturedAt = capturedAt.toRequestBody()
                    )

                    if (response.isSuccessful && response.body()?.success == true) {
                        uploaded++
                        file.delete()
                    } else {
                        queueSingle(file, category, weather, description)
                        queued++
                    }
                } catch (e: Exception) {
                    queueSingle(file, category, weather, description)
                    queued++
                }
            }

            binding.layoutUploadProgress.visibility = View.GONE

            val msg = when {
                queued == 0 -> "$uploaded photo${if (uploaded > 1) "s" else ""} uploaded"
                uploaded == 0 -> "$queued photo${if (queued > 1) "s" else ""} queued for sync"
                else -> "$uploaded uploaded, $queued queued"
            }
            Toast.makeText(this@PhotoUploadActivity, msg, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun queueAll(files: List<File>, category: String, weather: String?, description: String?) {
        lifecycleScope.launch {
            for (file in files) {
                queueSingle(file, category, weather, description)
            }
            SyncWorkerUtil.enqueue(this@PhotoUploadActivity)
            Toast.makeText(this@PhotoUploadActivity,
                "${files.size} photo${if (files.size > 1) "s" else ""} queued for sync",
                Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private suspend fun queueSingle(file: File, category: String, weather: String?, description: String?) {
        val metadata = mapOf(
            "category" to category,
            "weather" to (weather ?: ""),
            "description" to (description ?: ""),
            "gps_latitude" to (currentLatitude?.toString() ?: ""),
            "gps_longitude" to (currentLongitude?.toString() ?: ""),
            "gps_accuracy" to (currentAccuracy?.toString() ?: ""),
            "captured_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .format(Date())
        )

        val item = SyncQueueEntity(
            type = SyncQueueEntity.TYPE_PHOTO,
            filePath = file.absolutePath,
            metadataJson = Gson().toJson(metadata),
            businessUnitId = session.businessUnitId ?: ""
        )
        syncRepo.addToQueue(item)
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                101
            )
            return
        }
        fetchLocation()
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val client = LocationServices.getFusedLocationProviderClient(this)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude
                    currentAccuracy = it.accuracy
                    binding.tvGps.text = "GPS: %.6f, %.6f (±%.0fm)".format(it.latitude, it.longitude, it.accuracy)
                    binding.tvGps.visibility = View.VISIBLE
                }
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) openCamera()
            101 -> if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) fetchLocation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.unregister()
    }
}
