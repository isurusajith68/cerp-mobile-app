package com.ceyinfo.cerp.ui.queue

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.local.SyncQueueEntity
import com.ceyinfo.cerp.databinding.ItemQueueBinding
import java.text.SimpleDateFormat
import java.util.*

class QueueAdapter : ListAdapter<SyncQueueEntity, QueueAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQueueBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemQueueBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SyncQueueEntity) {
            val ctx = binding.root.context
            val dp = ctx.resources.displayMetrics.density

            // Type
            val isPhoto = item.type == SyncQueueEntity.TYPE_PHOTO
            binding.tvType.text = if (isPhoto) "Photo Upload" else "Daily Report"

            // Type icon + bg color
            binding.ivTypeIcon.setImageResource(
                if (isPhoto) R.drawable.ic_nav_camera
                else R.drawable.ic_nav_report
            )
            val iconTint = if (isPhoto) R.color.info else R.color.success
            binding.ivTypeIcon.setColorFilter(ContextCompat.getColor(ctx, iconTint))
            val iconBgColor = if (isPhoto) "#DBEAFE" else "#D1FAE5"
            (binding.bgTypeIcon.background as? GradientDrawable)?.setColor(
                android.graphics.Color.parseColor(iconBgColor)
            )

            // Status badge
            binding.tvStatus.text = item.status.replaceFirstChar { it.uppercase() }
            val statusColor = when (item.status) {
                SyncQueueEntity.STATUS_PENDING -> R.color.status_pending
                SyncQueueEntity.STATUS_UPLOADING -> R.color.status_uploading
                SyncQueueEntity.STATUS_COMPLETED -> R.color.status_completed
                SyncQueueEntity.STATUS_FAILED -> R.color.status_failed
                else -> R.color.text_secondary
            }
            val bg = binding.tvStatus.background as? GradientDrawable
                ?: GradientDrawable().apply { cornerRadius = 6f * dp }
            bg.setColor(ContextCompat.getColor(ctx, statusColor))
            binding.tvStatus.background = bg

            // Created date
            val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK)
            binding.tvCreated.text = dateFormat.format(Date(item.createdAt))

            // Error
            if (item.error != null && item.status == SyncQueueEntity.STATUS_FAILED) {
                binding.tvError.text = item.error
                binding.tvError.visibility = View.VISIBLE
            } else {
                binding.tvError.visibility = View.GONE
            }

            // Retries
            if (item.retries > 0) {
                binding.tvRetries.text = "Retries: ${item.retries}/${item.maxRetries}"
                binding.tvRetries.visibility = View.VISIBLE
            } else {
                binding.tvRetries.visibility = View.GONE
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SyncQueueEntity>() {
        override fun areItemsTheSame(old: SyncQueueEntity, new: SyncQueueEntity) = old.id == new.id
        override fun areContentsTheSame(old: SyncQueueEntity, new: SyncQueueEntity) = old == new
    }
}
