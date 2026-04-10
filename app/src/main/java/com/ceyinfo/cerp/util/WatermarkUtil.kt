package com.ceyinfo.cerp.util

import android.graphics.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object WatermarkUtil {

    fun apply(
        photoFile: File,
        projectName: String,
        category: String,
        latitude: Double?,
        longitude: Double?,
        outputFile: File? = null
    ): File {
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val width = result.width.toFloat()
        val height = result.height.toFloat()

        // Font size scales with image width
        val fontSize = maxOf(14f, width / 50f)
        val lineHeight = fontSize * 1.4f
        val padding = fontSize * 0.8f

        // Build text lines
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK)
        val lines = mutableListOf<String>()
        lines.add(projectName)
        lines.add(category.replace("_", " ").uppercase())
        lines.add(dateFormat.format(Date()))
        if (latitude != null && longitude != null) {
            lines.add("%.6f, %.6f".format(latitude, longitude))
        }

        // Background strip
        val stripHeight = (lines.size * lineHeight) + (padding * 2)
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 128  // 50% opacity
        }
        canvas.drawRect(0f, height - stripHeight, width, height, bgPaint)

        // Text
        val textPaint = Paint().apply {
            color = Color.WHITE
            alpha = 242  // 95% opacity
            textSize = fontSize
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        var y = height - stripHeight + padding + fontSize
        for (line in lines) {
            canvas.drawText(line, padding, y, textPaint)
            y += lineHeight
        }

        // Save
        val out = outputFile ?: photoFile
        FileOutputStream(out).use { fos ->
            result.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }

        bitmap.recycle()
        result.recycle()

        return out
    }
}
