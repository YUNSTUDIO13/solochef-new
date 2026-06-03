package com.example.solochef.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    private const val MAX_WIDTH = 1200
    private const val MAX_HEIGHT = 1200
    private const val JPEG_QUALITY = 75

    /**
     * Compress an image from Uri and save to app's private storage.
     * Returns the file path of the compressed image.
     */
    fun compressAndSave(context: Context, uri: Uri, prefix: String = "img"): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val compressed = compressBitmap(bitmap)
        bitmap.recycle()

        val file = File(context.filesDir, "images/${prefix}_${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs()

        FileOutputStream(file).use { out ->
            compressed.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        compressed.recycle()

        return file.absolutePath
    }

    /**
     * Compress a base64 string and save as file.
     * Returns the file path.
     */
    fun compressBase64AndSave(context: Context, base64: String, prefix: String = "img"): String {
        val cleanBase64 = base64.substringAfter("base64,").trim()
        val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: run {
            // If decode fails, save raw base64 as fallback
            val file = File(context.filesDir, "images/${prefix}_${System.currentTimeMillis()}.txt")
            file.parentFile?.mkdirs()
            file.writeText(base64)
            return file.absolutePath
        }

        val compressed = compressBitmap(bitmap)
        bitmap.recycle()

        val file = File(context.filesDir, "images/${prefix}_${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs()

        FileOutputStream(file).use { out ->
            compressed.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        compressed.recycle()

        return file.absolutePath
    }

    private fun compressBitmap(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height

        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) return source

        val ratio = minOf(MAX_WIDTH.toFloat() / width, MAX_HEIGHT.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    /**
     * Read an image file as base64 for in-memory use.
     */
    fun fileToBase64(path: String): String {
        val bytes = File(path).readBytes()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
}
