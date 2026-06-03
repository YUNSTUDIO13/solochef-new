package com.example.solochef

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * High-fidelity image compression pipeline.
 *
 * Pipeline:  decode with inSampleSize (max 1200px edge)
 *          → correct EXIF rotation
 *          → quality loop 85→60 (target 400-600KB)
 *          → write to outputFile
 */
object ImageUtils {

    /** Maximum edge length (width or height) after resize. */
    const val MAX_EDGE = 1200

    /** JPEG quality range (inclusive). */
    private const val Q_START = 85
    private const val Q_MIN = 60
    private const val Q_STEP = 5

    /** Target volume range in bytes. */
    private const val TARGET_MAX = 600L * 1024
    private const val TARGET_MIN = 400L * 1024

    /**
     * Compress and save the image at [inputUri] to [outputFile] on [Dispatchers.IO].
     *
     * @return `true` on success, `false` on any failure.
     */
    suspend fun compressAndSaveImage(
        context: Context,
        inputUri: Uri,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // ── 1. Read raw bytes ──
            val rawBytes = context.contentResolver.openInputStream(inputUri)
                ?.use { it.readBytes() } ?: return@withContext false
            if (rawBytes.isEmpty()) return@withContext false

            // ── 2. Decode dimensions (no pixel allocation yet) ──
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)
            val origW = opts.outWidth; val origH = opts.outHeight
            if (origW <= 0 || origH <= 0) return@withContext false

            // ── 3. Calculate inSampleSize (power-of-2) ──
            val maxDim = maxOf(origW, origH)
            val inSampleSize = if (maxDim > MAX_EDGE) {
                var sample = 1
                while (maxDim / sample > MAX_EDGE) sample *= 2
                sample
            } else 1
            opts.inSampleSize = inSampleSize
            opts.inJustDecodeBounds = false

            val sampled = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)
                ?: return@withContext false

            // ── 4. Precise scale to MAX_EDGE if still oversize ──
            var bitmap = sampled
            if (maxOf(bitmap.width, bitmap.height) > MAX_EDGE) {
                val ratio = MAX_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height)
                val scaled = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
                bitmap.recycle()
                bitmap = scaled
            }

            // ── 5. EXIF orientation correction ──
            val exif = try {
                context.contentResolver.openInputStream(inputUri)
                    ?.use { ExifInterface(it) }
            } catch (_: Exception) { null }

            val rotation = when (exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                val rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }

            // ── 6. Quality loop (85 → 60, target 400-600KB) ──
            var quality = Q_START
            var bytes: ByteArray
            val bos = ByteArrayOutputStream()
            do {
                bos.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                bytes = bos.toByteArray()
                if (bytes.size <= TARGET_MAX) break        // hit target
                if (quality <= Q_MIN) break                // hit minimum quality floor
                quality -= Q_STEP
            } while (true)

            bitmap.recycle()

            // ── 7. Write to disk ──
            outputFile.parentFile?.mkdirs()
            outputFile.outputStream().use { it.write(bytes) }

            true
        } catch (e: Exception) {
            android.util.Log.e("SoloChef.ImageUtils", "compressAndSaveImage failed", e)
            false
        }
    }

    // ═══════════════════════════════════════════════════
    //  Zip Export / Import with compressed images
    // ═══════════════════════════════════════════════════

    private const val ZIP_TARGET_MAX = 300L * 1024

    private suspend fun compressFileToJpegBytes(file: File): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            val maxDim = maxOf(opts.outWidth, opts.outHeight)
            if (maxDim <= 0) return@withContext null
            val inSampleSize = if (maxDim > MAX_EDGE) {
                var s = 1; while (maxDim / s > MAX_EDGE) s *= 2; s
            } else 1
            opts.inSampleSize = inSampleSize; opts.inJustDecodeBounds = false
            val sampled = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return@withContext null
            var bitmap = sampled
            if (maxOf(bitmap.width, bitmap.height) > MAX_EDGE) {
                val r = MAX_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height)
                val s = Bitmap.createScaledBitmap(bitmap, (bitmap.width * r).toInt(), (bitmap.height * r).toInt(), true)
                bitmap.recycle(); bitmap = s
            }
            var q = 85; var bytes: ByteArray; val bos = ByteArrayOutputStream()
            do {
                bos.reset(); bitmap.compress(Bitmap.CompressFormat.JPEG, q, bos); bytes = bos.toByteArray()
                if (bytes.size <= ZIP_TARGET_MAX || q <= 45) break; q -= 5
            } while (true)
            bitmap.recycle(); bytes
        } catch (e: Exception) { android.util.Log.e("SoloChef", "compressFileToJpeg", e); null }
    }

    suspend fun exportRecipeToZip(
        context: Context,
        recipe: com.example.solochef.model.Recipe,
        outputStream: OutputStream
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            data class Img(val rel: String, val file: File)
            val entries = mutableListOf<Img>()
            var coverRel = "./images/cover.jpg"
            var bomRel: String? = null
            val coverFile = File(recipe.cover_image)
            if (coverFile.exists()) entries.add(Img("images/cover.jpg", coverFile)) else coverRel = recipe.cover_image
            recipe.bom_snapshot?.let { b ->
                val bf = File(b)
                if (bf.exists()) { bomRel = "./images/bom.jpg"; entries.add(Img("images/bom.jpg", bf)) } else bomRel = b
            }
            val newTimeline = recipe.timeline.mapIndexed { si, step ->
                val newImgs = step.images?.mapIndexed { ii, p ->
                    val f = File(p)
                    if (f.exists()) { entries.add(Img("images/step_${si}_${ii}.jpg", f)); "./images/step_${si}_${ii}.jpg" } else p
                }
                step.copy(images = newImgs ?: step.images)
            }
            ZipOutputStream(outputStream).use { zos ->
                entries.forEach { e ->
                    compressFileToJpegBytes(e.file)?.let { b ->
                        zos.putNextEntry(ZipEntry(e.rel)); zos.write(b); zos.closeEntry()
                    }
                }
                val exported = recipe.copy(cover_image = coverRel, bom_snapshot = bomRel, timeline = newTimeline)
                zos.putNextEntry(ZipEntry("recipe.json"))
                zos.write(kotlinx.serialization.json.Json { prettyPrint = true; encodeDefaults = true }.encodeToString(exported).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            true
        } catch (e: Exception) { android.util.Log.e("SoloChef", "exportRecipeToZip", e); false }
    }

    suspend fun importRecipeFromZip(
        context: Context,
        inputStream: InputStream
    ): com.example.solochef.model.Recipe? = withContext(Dispatchers.IO) {
        try {
            var recipe: com.example.solochef.model.Recipe? = null
            val pathMap = mutableMapOf<String, String>()
            val importDir = File(context.filesDir, "imported").also { it.mkdirs() }
            val buf = ByteArray(8192)
            ZipInputStream(inputStream).use { zis ->
                var e = zis.nextEntry
                while (e != null) {
                    when {
                        e.name.startsWith("images/") -> {
                            val t = File(importDir, "import_${System.currentTimeMillis()}_${e.name.substringAfterLast("/")}")
                            t.outputStream().use { o -> var l: Int; while (zis.read(buf).also { l = it } > 0) o.write(buf, 0, l) }
                            pathMap["./${e.name}"] = t.absolutePath
                        }
                        e.name == "recipe.json" -> {
                            recipe = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                                .decodeFromString<com.example.solochef.model.Recipe>(zis.bufferedReader().readText())
                        }
                    }
                    zis.closeEntry(); e = zis.nextEntry
                }
            }
            val r = recipe ?: return@withContext null
            r.copy(
                id = System.currentTimeMillis().toString(),
                cover_image = pathMap[r.cover_image] ?: r.cover_image,
                bom_snapshot = r.bom_snapshot?.let { pathMap[it] ?: it },
                timeline = r.timeline.map { s -> s.copy(images = s.images?.map { pathMap[it] ?: it } ?: s.images) },
                updated_at = System.currentTimeMillis().toString()
            )
        } catch (e: Exception) { android.util.Log.e("SoloChef", "importRecipeFromZip", e); null }
    }
}
