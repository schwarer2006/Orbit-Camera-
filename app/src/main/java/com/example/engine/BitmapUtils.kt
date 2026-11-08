package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BitmapUtils {

    /**
     * Decode a file to Bitmap with memory-safe downsampling.
     */
    fun decodeSampledBitmap(
        filePath: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        val decoded = BitmapFactory.decodeFile(filePath, options) ?: return null
        return applyExifOrientation(filePath, decoded)
    }

    /**
     * Decode an InputStream from a ContentUri (for Photo Picker).
     */
    fun decodeSampledStream(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        var input: InputStream? = null
        try {
            input = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            input.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            input = context.contentResolver.openInputStream(uri) ?: return null
            return BitmapFactory.decodeStream(input, null, options)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            input?.close()
        }
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun applyExifOrientation(filePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    fun saveBitmapToFile(bitmap: Bitmap, targetFile: File, quality: Int = 92): Boolean {
        return try {
            targetFile.parentFile?.mkdirs()
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Export final artwork into Android MediaStore Pictures/ORBIT_Camera gallery.
     */
    fun exportToMediaStore(
        context: Context,
        bitmap: Bitmap,
        title: String,
        quality: Int = 95
    ): Uri? {
        val filename = "${title}_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ORBIT_Camera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                    throw IllegalStateException("Failed to compress bitmap to MediaStore")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(uri, null, null)
            return null
        }
    }

    /**
     * Center-crops and scales source bitmap to exactly targetWidth x targetHeight
     * without image distortion (aspect-fill). Recycles source bitmap if a new bitmap is created.
     */
    fun cropCenterFit(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        if (source.width == targetW && source.height == targetH) return source

        val scale = max(targetW.toFloat() / source.width, targetH.toFloat() / source.height)
        val scaledW = (source.width * scale).roundToInt()
        val scaledH = (source.height * scale).roundToInt()

        val result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val dx = (targetW - scaledW) / 2f
        val dy = (targetH - scaledH) / 2f
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        if (source != result) {
            source.recycle()
        }
        return result
    }

    /**
     * Launch the system Android Sharesheet with the given image URI.
     */
    fun shareImage(context: Context, uri: Uri, title: String = "ORBIT Kunstwerk") {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Erstellt mit ORBIT Camera – Computational Art Photography")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "ORBIT Kunstwerk teilen").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Fast local quality assessment for a series of images (sharpness, exposure, similarity).
     * Non-destructive, advisory indicators only.
     */
    fun analyzeImageQuality(imagePaths: List<String>): Map<String, ImageQualityReport> {
        val result = mutableMapOf<String, ImageQualityReport>()
        if (imagePaths.isEmpty()) return result

        val sampleDim = 64
        val luminances = mutableListOf<Float>()
        val sharpnesses = mutableListOf<Float>()
        val pixelMaps = mutableListOf<FloatArray?>()

        for (path in imagePaths) {
            val bmp = decodeSampledBitmap(path, sampleDim, sampleDim)
            if (bmp == null) {
                pixelMaps.add(null)
                luminances.add(128f)
                sharpnesses.add(0f)
                continue
            }
            val w = bmp.width
            val h = bmp.height
            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            bmp.recycle()

            val lums = FloatArray(w * h)
            var sumLum = 0.0
            var sumGrad = 0.0

            for (y in 0 until h) {
                for (x in 0 until w) {
                    val idx = y * w + x
                    val c = pixels[idx]
                    val l = (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF))
                    lums[idx] = l
                    sumLum += l

                    if (x > 0 && y > 0) {
                        val gx = kotlin.math.abs(l - lums[idx - 1])
                        val gy = kotlin.math.abs(l - lums[idx - w])
                        sumGrad += (gx + gy)
                    }
                }
            }

            val avgL = (sumLum / (w * h)).toFloat()
            val sharp = (sumGrad / (w * h)).toFloat()
            luminances.add(avgL)
            sharpnesses.add(sharp)
            pixelMaps.add(lums)
        }

        val avgSharpness = if (sharpnesses.isNotEmpty()) sharpnesses.average().toFloat() else 10f
        val sortedLums = luminances.sorted()
        val medianLum = if (sortedLums.isNotEmpty()) sortedLums[sortedLums.size / 2] else 128f

        for (i in imagePaths.indices) {
            val path = imagePaths[i]
            val sharp = sharpnesses[i]
            val lum = luminances[i]
            val currentPix = pixelMaps[i]

            val isBlurry = (sharp < avgSharpness * 0.40f && sharp < 12.0f)
            val isExposureOutlier = kotlin.math.abs(lum - medianLum) > 60f

            var isNearDuplicate = false
            if (i > 0) {
                val prevPix = pixelMaps[i - 1]
                if (currentPix != null && prevPix != null && currentPix.size == prevPix.size) {
                    var diffSum = 0.0
                    for (k in currentPix.indices) {
                        diffSum += kotlin.math.abs(currentPix[k] - prevPix[k])
                    }
                    val avgDiff = (diffSum / currentPix.size).toFloat()
                    if (avgDiff < 3.8f) {
                        isNearDuplicate = true
                    }
                }
            }

            val warnings = mutableListOf<String>()
            if (isBlurry) warnings.add("Möglicherweise unscharf")
            if (isExposureOutlier) warnings.add("Starke Belichtungsabweichung")
            if (isNearDuplicate) warnings.add("Sehr ähnlich zu vorherigem Bild")

            result[path] = ImageQualityReport(
                path = path,
                sharpnessScore = sharp,
                avgLuminance = lum,
                isBlurry = isBlurry,
                isExposureOutlier = isExposureOutlier,
                isNearDuplicate = isNearDuplicate,
                warnings = warnings
            )
        }

        return result
    }
}

data class ImageQualityReport(
    val path: String,
    val sharpnessScore: Float,
    val avgLuminance: Float,
    val isBlurry: Boolean,
    val isExposureOutlier: Boolean,
    val isNearDuplicate: Boolean,
    val warnings: List<String>
) {
    val isDuplicate: Boolean
        get() = isNearDuplicate
}
