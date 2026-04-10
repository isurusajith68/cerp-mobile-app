package com.ceyinfo.cerp.ui.documents

import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.databinding.ItemDocumentFileBinding
import java.text.DecimalFormat

data class DocFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String
)

class DocumentFileAdapter(
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<DocumentFileAdapter.ViewHolder>() {

    private val files = mutableListOf<DocFile>()

    fun addFile(file: DocFile) {
        files.add(file)
        notifyItemInserted(files.size - 1)
    }

    fun removeAt(position: Int) {
        if (position in files.indices) {
            files.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, files.size)
        }
    }

    fun getFiles(): List<DocFile> = files.toList()
    fun count() = files.size

    override fun getItemCount() = files.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDocumentFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files[position], position)
    }

    inner class ViewHolder(
        private val binding: ItemDocumentFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: DocFile, position: Int) {
            val ctx = binding.root.context
            val dp = ctx.resources.displayMetrics.density

            binding.tvFileName.text = file.name
            binding.tvFileSize.text = formatFileSize(file.size)

            // Icon + color by type
            val isPdf = file.mimeType.contains("pdf")
            val iconRes = if (isPdf) R.drawable.ic_document else R.drawable.ic_nav_camera
            val bgColor = if (isPdf) R.color.level_division else R.color.info

            binding.ivFileIcon.setImageResource(iconRes)
            val bg = binding.bgFileIcon.background as? GradientDrawable
                ?: GradientDrawable().apply { cornerRadius = 12f * dp }
            bg.setColor(ContextCompat.getColor(ctx, bgColor))
            binding.bgFileIcon.background = bg

            binding.btnRemove.setOnClickListener { onRemove(position) }
        }

        private fun formatFileSize(bytes: Long): String {
            val df = DecimalFormat("#.#")
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${df.format(bytes / 1024.0)} KB"
                else -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
            }
        }
    }
}
