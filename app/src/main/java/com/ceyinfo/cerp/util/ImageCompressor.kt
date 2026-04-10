package com.ceyinfo.cerp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    private const val MAX_WIDTH = 1920
    private const val MAX_HEIGHT = 1920
    private const val QUALITY = 92
    private const val MAX_FILE_SIZE = 1024 * 1024 // 1MB

    fun compress(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open URI")

        // Decode bounds first
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        // Calculate sample size
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight)
        options.inJustDecodeBounds = false

        // Decode with sample size
        val input2 = context.contentResolver.openInputStream(uri)!!
        val bitmap = BitmapFactory.decodeStream(input2, null, options)!!
        input2.close()

        // Scale if still too large
        val scaled = scaleBitmap(bitmap)

        // Write to cache file
        val dir = File(context.cacheDir, "compressed").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")

        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        }

        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()

        return file
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w > MAX_WIDTH * 2 || h > MAX_HEIGHT * 2) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }
        return sampleSize
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) return bitmap

        val ratio = minOf(MAX_WIDTH.toFloat() / width, MAX_HEIGHT.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
