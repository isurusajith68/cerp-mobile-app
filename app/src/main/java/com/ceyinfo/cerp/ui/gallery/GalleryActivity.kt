package com.ceyinfo.cerp.ui.gallery

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

        binding.appbar.tvAppbarTitle.text = getString(R.string.gallery_title)
        binding.appbar.btnBack.setOnClickListener { finish() }

        setupCategoryChips()
        loadPhotos()
    }

    private fun setupCategoryChips() {
        categories.forEach { cat ->
            val label = if (cat == "All") cat else cat.replace("_", " ").replaceFirstChar { it.uppercase() }
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = cat == "All"
                chipCornerRadius = 20f * resources.displayMetrics.density
                chipStrokeWidth = 1f * resources.displayMetrics.density
                chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.divider)
                )
                chipBackgroundColor = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(
                        ContextCompat.getColor(context, R.color.primary),
                        ContextCompat.getColor(context, R.color.white)
                    )
                )
                setTextColor(android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(
                        ContextCompat.getColor(context, R.color.white),
                        ContextCompat.getColor(context, R.color.on_surface)
                    )
                ))
                isCheckedIconVisible = false
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
