package com.ceyinfo.cerp.ui.photos

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ceyinfo.cerp.databinding.ItemPhotoThumbBinding
import java.io.File

class PhotoThumbAdapter(
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<PhotoThumbAdapter.ViewHolder>() {

    private val files = mutableListOf<File>()

    fun addFile(file: File) {
        files.add(file)
        notifyItemInserted(files.size - 1)
    }

    fun removeAt(position: Int) {
        if (position in files.indices) {
            files[position].delete()
            files.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, files.size)
        }
    }

    fun getFiles(): List<File> = files.toList()

    fun count() = files.size

    fun clear() {
        val size = files.size
        files.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun getItemCount() = files.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhotoThumbBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files[position], position)
    }

    inner class ViewHolder(
        private val binding: ItemPhotoThumbBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File, position: Int) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            binding.ivThumb.setImageBitmap(bitmap)
            binding.btnRemove.setOnClickListener { onRemove(position) }
        }
    }
}
