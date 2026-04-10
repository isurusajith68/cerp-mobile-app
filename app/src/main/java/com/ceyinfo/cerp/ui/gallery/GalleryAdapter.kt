package com.ceyinfo.cerp.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.model.ProgressPhoto
import com.ceyinfo.cerp.databinding.ItemGalleryPhotoBinding
import java.text.SimpleDateFormat
import java.util.*

class GalleryAdapter : ListAdapter<ProgressPhoto, GalleryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryPhotoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemGalleryPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: ProgressPhoto) {
            // Load thumbnail or full image
            val url = photo.thumbnailUrl ?: photo.fileUrl
            if (url != null) {
                binding.ivPhoto.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.bg_icon_circle)
                }
            }

            binding.tvCategory.text = photo.category
                .replace("_", " ")
                .replaceFirstChar { it.uppercase() }

            // Format date
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK)
                val date = inputFormat.parse(photo.capturedAt.take(19))
                binding.tvDate.text = date?.let { outputFormat.format(it) } ?: photo.capturedAt
            } catch (e: Exception) {
                binding.tvDate.text = photo.capturedAt.take(10)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ProgressPhoto>() {
        override fun areItemsTheSame(old: ProgressPhoto, new: ProgressPhoto) = old.id == new.id
        override fun areContentsTheSame(old: ProgressPhoto, new: ProgressPhoto) = old == new
    }
}
