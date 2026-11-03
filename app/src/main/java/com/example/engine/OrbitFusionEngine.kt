package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.example.model.OrbitBlendMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class FusionProgress(
    val current: Int,
    val total: Int,
    val stage: String
)

object OrbitFusionEngine {

    /**
     * Compute a fused artistic composite from a list of real image files.
     * Uses sequential memory-safe decoding with an accumulator buffer.
     */
    suspend fun renderFusion(
        imagePaths: List<String>,
        referenceIndex: Int,
        abstractionLevel: Float, // 0..100
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        blendMode: OrbitBlendMode = OrbitBlendMode.WEIGHTED_AVERAGE,
        targetWidth: Int = 1080,
        targetHeight: Int = 1080,
        onProgress: (FusionProgress) -> Unit = {}
    ): Bitmap? = withContext(Dispatchers.Default) {
        val validPaths = imagePaths.filter { File(it).exists() }
        if (validPaths.isEmpty()) return@withContext null

        val totalImages = validPaths.size
        val safeRefIndex = referenceIndex.coerceIn(0, totalImages - 1)

        onProgress(FusionProgress(0, totalImages, "Vorbereitung der Fusions-Engine..."))

        // Compute weights for each image based on abstraction level and distance from reference
        val weights = calculateWeights(totalImages, safeRefIndex, abstractionLevel, blendMode)

        // Memory-safe canvas/accumulator approach
        // We use an ARGB canvas buffer or pixel accumulator
        val outWidth = targetWidth
        val outHeight = targetHeight

        val accumR = FloatArray(outWidth * outHeight)
        val accumG = FloatArray(outWidth * outHeight)
        val accumB = FloatArray(outWidth * outHeight)
        val accumWeight = FloatArray(outWidth * outHeight)

        val tempPixels = IntArray(outWidth * outHeight)

        for (i in validPaths.indices) {
            val path = validPaths[i]
            val weight = weights[i]

            onProgress(FusionProgress(i + 1, totalImages, "Überlagere Perspektive ${i + 1} von $totalImages..."))

            // Decode this single image sampled to requested dimensions
            val sourceBitmap = BitmapUtils.decodeSampledBitmap(path, outWidth, outHeight) ?: continue

            // Scale and draw into standard dimensions
            val scaledBitmap = if (sourceBitmap.width != outWidth || sourceBitmap.height != outHeight) {
                Bitmap.createScaledBitmap(sourceBitmap, outWidth, outHeight, true).also {
                    if (it != sourceBitmap) sourceBitmap.recycle()
                }
            } else {
                sourceBitmap
            }

            // Read pixels
            scaledBitmap.getPixels(tempPixels, 0, outWidth, 0, 0, outWidth, outHeight)
            scaledBitmap.recycle()

            val isReference = (i == safeRefIndex)
            val shiftX = if (isReference) (offsetX * (outWidth / 400f)).roundToInt() else 0
            val shiftY = if (isReference) (offsetY * (outHeight / 400f)).roundToInt() else 0

            // Accumulate pixels based on blend mode
            when (blendMode) {
                OrbitBlendMode.WEIGHTED_AVERAGE, OrbitBlendMode.DRIFT_FLOW -> {
                    accumulateWeightedAverage(
                        tempPixels, accumR, accumG, accumB, accumWeight,
                        outWidth, outHeight, weight, shiftX, shiftY
                    )
                }
                OrbitBlendMode.LIGHTEN_ADDITIVE -> {
                    accumulateLighten(
                        tempPixels, accumR, accumG, accumB, accumWeight,
                        outWidth, outHeight, weight, shiftX, shiftY
                    )
                }
                OrbitBlendMode.MULTIPLY_DENSITY -> {
                    accumulateMultiplyDensity(
                        tempPixels, accumR, accumG, accumB, accumWeight,
                        outWidth, outHeight, weight, shiftX, shiftY
                    )
                }
            }
        }

        onProgress(FusionProgress(totalImages, totalImages, "Finalisiere Kunstwerk-Komposition..."))

        // Create the final output bitmap
        val outputPixels = IntArray(outWidth * outHeight)
        for (idx in outputPixels.indices) {
            val w = accumWeight[idx]
            if (w > 0f) {
                val r = (accumR[idx] / w).coerceIn(0f, 255f).roundToInt()
                val g = (accumG[idx] / w).coerceIn(0f, 255f).roundToInt()
                val b = (accumB[idx] / w).coerceIn(0f, 255f).roundToInt()
                outputPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            } else {
                outputPixels[idx] = (0xFF shl 24)
            }
        }

        val resultBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outputPixels, 0, outWidth, 0, 0, outWidth, outHeight)
        resultBitmap
    }

    private fun calculateWeights(
        total: Int,
        refIndex: Int,
        abstraction: Float,
        blendMode: OrbitBlendMode
    ): FloatArray {
        val weights = FloatArray(total)
        val normalizedAbs = (abstraction / 100f).coerceIn(0f, 1f)

        if (blendMode == OrbitBlendMode.DRIFT_FLOW) {
            // Temporal flow progression
            for (i in 0 until total) {
                val t = i.toFloat() / max(1, total - 1)
                weights[i] = 0.2f + 0.8f * t
            }
            normalize(weights)
            return weights
        }

        if (total == 1) {
            weights[0] = 1f
            return weights
        }

        // At abstraction = 0: Reference image gets 85% dominance, others share 15%
        // At abstraction = 100: All images have equal weight (uniform kinetic field)
        val refDominance = lerp(0.85f, 1f / total, normalizedAbs)
        val remainingWeight = 1f - refDominance

        // For non-reference images, falloff depends on distance from reference and abstraction
        var otherSum = 0f
        val rawOthers = FloatArray(total)
        val sigma = max(0.8f, (1f - normalizedAbs) * (total / 4f) + 1.2f)

        for (i in 0 until total) {
            if (i == refIndex) continue
            val dist = abs(i - refIndex).toFloat()
            // Gaussian-like falloff around reference when abstraction is low; uniform when abstraction is high
            val falloff = exp(-(dist * dist) / (2f * sigma * sigma))
            val blendFactor = lerp(falloff, 1f, normalizedAbs)
            rawOthers[i] = blendFactor
            otherSum += blendFactor
        }

        weights[refIndex] = refDominance
        if (otherSum > 0f) {
            for (i in 0 until total) {
                if (i != refIndex) {
                    weights[i] = (rawOthers[i] / otherSum) * remainingWeight
                }
            }
        }

        normalize(weights)
        return weights
    }

    private fun accumulateWeightedAverage(
        pixels: IntArray,
        accR: FloatArray,
        accG: FloatArray,
        accB: FloatArray,
        accW: FloatArray,
        w: Int,
        h: Int,
        weight: Float,
        shiftX: Int,
        shiftY: Int
    ) {
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                val color = pixels[srcIdx]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                accR[destIdx] += r * weight
                accG[destIdx] += g * weight
                accB[destIdx] += b * weight
                accW[destIdx] += weight
            }
        }
    }

    private fun accumulateLighten(
        pixels: IntArray,
        accR: FloatArray,
        accG: FloatArray,
        accB: FloatArray,
        accW: FloatArray,
        w: Int,
        h: Int,
        weight: Float,
        shiftX: Int,
        shiftY: Int
    ) {
        val boost = 1.25f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                val color = pixels[srcIdx]
                val r = ((color shr 16) and 0xFF) * boost
                val g = ((color shr 8) and 0xFF) * boost
                val b = (color and 0xFF) * boost

                accR[destIdx] = max(accR[destIdx], r * weight * 3.5f)
                accG[destIdx] = max(accG[destIdx], g * weight * 3.5f)
                accB[destIdx] = max(accB[destIdx], b * weight * 3.5f)
                accW[destIdx] = 1f
            }
        }
    }

    private fun accumulateMultiplyDensity(
        pixels: IntArray,
        accR: FloatArray,
        accG: FloatArray,
        accB: FloatArray,
        accW: FloatArray,
        w: Int,
        h: Int,
        weight: Float,
        shiftX: Int,
        shiftY: Int
    ) {
        val contrastFactor = 1.15f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                val color = pixels[srcIdx]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                // Enhance dark structures
                val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val densityMod = 1.4f - (luminance * 0.5f)

                accR[destIdx] += r * weight * densityMod * contrastFactor
                accG[destIdx] += g * weight * densityMod * contrastFactor
                accB[destIdx] += b * weight * densityMod * contrastFactor
                accW[destIdx] += weight * densityMod
            }
        }
    }

    private fun normalize(arr: FloatArray) {
        val sum = arr.sum()
        if (sum > 0f) {
            for (i in arr.indices) {
                arr[i] /= sum
            }
        }
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }
}
