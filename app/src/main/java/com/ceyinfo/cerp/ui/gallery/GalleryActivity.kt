package com.ceyinfo.cerp.ui.gallery

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.model.ProgressPhoto
import com.ceyinfo.cerp.data.remote.ApiClient
import com.ceyinfo.cerp.databinding.ActivityGalleryBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private var currentCategory: String? = null
    private var currentPage = 1

    private val categories = arrayOf(
        "All", "foundation", "structure", "electrical", "plumbing", "roofing",
        "finishing", "landscaping", "safety", "equipment", "material_delivery", "other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = GalleryAdapter()
        binding.rvPhotos.layoutManager = GridLayoutManager(this, 2)
        binding.rvPhotos.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        setupCategoryChips()
        loadPhotos()
    }

    private fun setupCategoryChips() {
        categories.forEach { cat ->
            val chip = Chip(this).apply {
                text = if (cat == "All") cat else cat.replace("_", " ").replaceFirstChar { it.uppercase() }
                isCheckable = true
                isChecked = cat == "All"
                setOnClickListener {
                    currentCategory = if (cat == "All") null else cat
                    currentPage = 1
                    loadPhotos()
                }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun loadPhotos() {
        binding.progress.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val api = ApiClient.getService(this@GalleryActivity)
                val response = api.getPhotos(
                    page = currentPage,
                    limit = 20,
                    category = currentCategory
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val photos = response.body()!!.data ?: emptyList()
                    if (photos.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                    adapter.submitList(photos)
                } else {
                    Toast.makeText(this@GalleryActivity, "Failed to load photos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GalleryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }
}
