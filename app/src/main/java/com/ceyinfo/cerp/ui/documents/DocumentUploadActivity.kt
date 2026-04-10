package com.ceyinfo.cerp.ui.documents

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.model.OcrFlagRequest
import com.ceyinfo.cerp.data.remote.ApiClient
import com.ceyinfo.cerp.databinding.ActivityDocumentUploadBinding
import com.ceyinfo.cerp.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class DocumentUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentUploadBinding
    private lateinit var session: SessionManager
    private lateinit var fileAdapter: DocumentFileAdapter
    private var currentPhotoUri: Uri? = null

    // Camera
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            addFileFromUri(currentPhotoUri!!, "image/jpeg")
        }
    }

    // Gallery (images)
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri -> addFileFromUri(uri) }
    }

    // PDF file picker
    private val pdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri -> addFileFromUri(uri) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        binding.appbar.tvAppbarTitle.text = "Document Upload"
        binding.appbar.btnBack.setOnClickListener { finish() }

        fileAdapter = DocumentFileAdapter { position ->
            fileAdapter.removeAt(position)
            updateUploadButton()
        }
        binding.rvFiles.layoutManager = LinearLayoutManager(this)
        binding.rvFiles.adapter = fileAdapter

        binding.btnCamera.setOnClickListener {
            if (checkCameraPermission()) openCamera()
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnPdf.setOnClickListener {
            pdfLauncher.launch(arrayOf("application/pdf"))
        }

        binding.btnUpload.setOnClickListener { uploadAll() }
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
        val dir = File(cacheDir, "docs").apply { mkdirs() }
        val file = File(dir, "doc_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        cameraLauncher.launch(currentPhotoUri!!)
    }

    private fun addFileFromUri(uri: Uri, fallbackMime: String? = null) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        var name = "document"
        var size = 0L

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = it.getString(nameIndex) ?: "document"
                if (sizeIndex >= 0) size = it.getLong(sizeIndex)
            }
        }

        val mime = contentResolver.getType(uri) ?: fallbackMime ?: "application/octet-stream"

        fileAdapter.addFile(DocFile(uri, name, size, mime))
        updateUploadButton()
    }

    private fun updateUploadButton() {
        val count = fileAdapter.count()
        binding.btnUpload.isEnabled = count > 0
        binding.btnUpload.text = if (count > 0)
            "Upload $count file${if (count > 1) "s" else ""} for OCR"
        else
            "Upload for OCR"
    }

    private fun uploadAll() {
        val files = fileAdapter.getFiles()
        if (files.isEmpty()) return

        binding.btnUpload.isEnabled = false
        binding.btnCamera.isEnabled = false
        binding.btnGallery.isEnabled = false
        binding.btnPdf.isEnabled = false
        binding.layoutUploadProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            val api = ApiClient.getService(this@DocumentUploadActivity)
            var uploaded = 0
            var failed = 0
            val buId = session.businessUnitId ?: ""

            for ((index, docFile) in files.withIndex()) {
                binding.tvUploadProgress.text = "Uploading ${index + 1} of ${files.size}…"

                try {
                    // Read file bytes
                    val bytes = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(docFile.uri)?.readBytes()
                    } ?: throw Exception("Cannot read file")

                    val filePart = MultipartBody.Part.createFormData(
                        "file", docFile.name,
                        bytes.toRequestBody(docFile.mimeType.toMediaTypeOrNull())
                    )

                    val response = api.uploadDocument(
                        file = filePart,
                        moduleCode = "site".toRequestBody(),
                        entityCode = "document".toRequestBody(),
                        entityId = buId.toRequestBody(),
                        category = "ocr_uploads".toRequestBody()
                    )

                    if (response.isSuccessful && response.body()?.success == true) {
                        val docId = response.body()!!.data!!.id
                        // Flag for OCR
                        api.flagDocumentOcr(docId, OcrFlagRequest(toBeOcr = true))
                        uploaded++
                    } else {
                        failed++
                    }
                } catch (e: Exception) {
                    failed++
                }
            }

            binding.layoutUploadProgress.visibility = View.GONE
            binding.btnCamera.isEnabled = true
            binding.btnGallery.isEnabled = true
            binding.btnPdf.isEnabled = true

            val msg = when {
                failed == 0 -> "$uploaded document${if (uploaded > 1) "s" else ""} uploaded for OCR"
                uploaded == 0 -> "Upload failed"
                else -> "$uploaded uploaded, $failed failed"
            }
            Toast.makeText(this@DocumentUploadActivity, msg, Toast.LENGTH_SHORT).show()

            if (uploaded > 0) finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        }
    }
}
