package com.example.engine

import android.graphics.Bitmap
import com.example.model.OrbitArtStyle
import com.example.model.OrbitBlendMode
import com.example.model.OrbitStyleParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class FusionProgress(
    val current: Int,
    val total: Int,
    val stage: String
)

object OrbitFusionEngine {

    /**
     * Compute a fused artistic composite from a list of real image files
     * according to the chosen artistic style preset and parameters.
     * Uses sequential memory-safe decoding with an accumulator buffer.
     */
    suspend fun renderFusion(
        imagePaths: List<String>,
        referenceIndex: Int,
        artStyle: OrbitArtStyle = OrbitArtStyle.IMPRESSIONIST,
        styleParams: OrbitStyleParameters = artStyle.defaultParameters,
        individualShifts: Map<Int, Pair<Float, Float>> = emptyMap(),
        globalOffsetX: Float = 0f,
        globalOffsetY: Float = 0f,
        blendMode: OrbitBlendMode = OrbitBlendMode.WEIGHTED_AVERAGE,
        targetWidth: Int = 1080,
        targetHeight: Int = 1080,
        onProgress: (FusionProgress) -> Unit = {}
    ): Bitmap? = withContext(Dispatchers.Default) {
        val validPaths = imagePaths.filter { File(it).exists() }
        if (validPaths.isEmpty()) return@withContext null

        val totalImages = validPaths.size
        val safeRefIndex = referenceIndex.coerceIn(0, totalImages - 1)

        onProgress(FusionProgress(0, totalImages, "Stil '${artStyle.title}' initialisieren..."))

        // Compute weights based on art style, subject stability, and abstraction
        val weights = calculateStyleWeights(totalImages, safeRefIndex, artStyle, styleParams)

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

            onProgress(FusionProgress(i + 1, totalImages, "${artStyle.title}: Überlagere Perspektive ${i + 1}/$totalImages..."))

            // Decode single image sampled to requested dimensions
            val sourceBitmap = BitmapUtils.decodeSampledBitmap(path, outWidth, outHeight) ?: continue

            val scaledBitmap = if (sourceBitmap.width != outWidth || sourceBitmap.height != outHeight) {
                Bitmap.createScaledBitmap(sourceBitmap, outWidth, outHeight, true).also {
                    if (it != sourceBitmap) sourceBitmap.recycle()
                }
            } else {
                sourceBitmap
            }

            scaledBitmap.getPixels(tempPixels, 0, outWidth, 0, 0, outWidth, outHeight)
            scaledBitmap.recycle()

            // Calculate shift: combine global offset, individual shift, and style motion offset
            val isReference = (i == safeRefIndex)
            val indShift = individualShifts[i] ?: Pair(0f, 0f)

            // Motion flow creates directional shift along series sequence
            val motionOffsetX = if (artStyle == OrbitArtStyle.MOTION_FLOW) {
                (i - safeRefIndex).toFloat() * (styleParams.motionStrength / 100f) * 4.5f
            } else 0f

            val motionOffsetY = if (artStyle == OrbitArtStyle.MOTION_FLOW) {
                (i - safeRefIndex).toFloat() * (styleParams.motionStrength / 100f) * 1.5f
            } else 0f

            val totalShiftX = ((if (isReference) globalOffsetX else 0f) + indShift.first + motionOffsetX) * (outWidth / 400f)
            val totalShiftY = ((if (isReference) globalOffsetY else 0f) + indShift.second + motionOffsetY) * (outHeight / 400f)

            val shiftX = totalShiftX.roundToInt()
            val shiftY = totalShiftY.roundToInt()

            // Accumulate pixels based on art style specific fusion strategy
            when (artStyle) {
                OrbitArtStyle.IMPRESSIONIST -> {
                    accumulateImpressionist(
                        tempPixels, accumR, accumG, accumB, accumWeight,
                        outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams
                    )
                }
                OrbitArtStyle.IN_THE_ROUND -> {
                    accumulateInTheRound(
                        tempPixels, accumR, accumG, accumB, accumWeight,
                        outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams
                    )
                }
                OrbitArtStyle.ARCHITECTURAL -> {
                    accumulateArchitectural(
                        tempPixels, accumR, accumG, accumB, accumWeight,
                        outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams
                    )
                }
                OrbitArtStyle.ETHEREAL -> {
                    accumulateEthereal(
                        tempPixels, accumR, accumG, accumB, accumWeight,
                        outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams
                    )
                }
                OrbitArtStyle.MOTION_FLOW -> {
                    accumulateMotionFlow(
                        tempPixels, accumR, accumG, accumB, accumWeight,
                        outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams
                    )
                }
                OrbitArtStyle.URBAN_LAYERS -> {
                    accumulateUrbanLayers(
                        tempPixels, accumR, accumG, accumB, accumWeight,
                        outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams
                    )
                }
            }
        }

        onProgress(FusionProgress(totalImages, totalImages, "Stilistische Veredelung (${artStyle.title})..."))

        // Create base fused pixel array
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

        // Apply style post-processing: Edge Emphasis, Softness/Glow, Color Density
        applyStylePostProcessing(outputPixels, outWidth, outHeight, artStyle, styleParams)

        val resultBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outputPixels, 0, outWidth, 0, 0, outWidth, outHeight)
        resultBitmap
    }

    private fun calculateStyleWeights(
        total: Int,
        refIndex: Int,
        artStyle: OrbitArtStyle,
        params: OrbitStyleParameters
    ): FloatArray {
        val weights = FloatArray(total)
        val abstractionNorm = (params.abstraction / 100f).coerceIn(0f, 1f)
        val stabilityNorm = (params.subjectStability / 100f).coerceIn(0f, 1f)

        if (total == 1) {
            weights[0] = 1f
            return weights
        }

        // High subject stability gives strong dominance to reference frame
        // Low abstraction also keeps reference dominant; high abstraction disperses weight
        val refBaseDominance = lerp(0.35f, 0.90f, stabilityNorm)
        val refDominance = lerp(refBaseDominance, 1f / total, abstractionNorm * 0.75f)
        val remainingWeight = 1f - refDominance

        var otherSum = 0f
        val rawOthers = FloatArray(total)
        val sigma = max(0.6f, (1f - abstractionNorm) * (total / 3f) + (stabilityNorm * 2f))

        for (i in 0 until total) {
            if (i == refIndex) continue
            val dist = abs(i - refIndex).toFloat()
            val falloff = exp(-(dist * dist) / (2f * sigma * sigma))
            val weightFactor = when (artStyle) {
                OrbitArtStyle.MOTION_FLOW -> {
                    // Temporal rhythm gradient
                    val seqT = i.toFloat() / max(1, total - 1)
                    (0.3f + 0.7f * seqT) * lerp(falloff, 1f, abstractionNorm)
                }
                OrbitArtStyle.URBAN_LAYERS -> {
                    // Alternating rhythmic pulse
                    val pulse = if (i % 2 == 0) 1.25f else 0.85f
                    lerp(falloff, 1f, abstractionNorm) * pulse
                }
                else -> {
                    lerp(falloff, 1f, abstractionNorm)
                }
            }
            rawOthers[i] = weightFactor
            otherSum += weightFactor
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

    // --- Style-Specific Accumulation Functions ---

    private fun accumulateImpressionist(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters
    ) {
        val colorDensityFactor = 1.0f + (params.colorDensity / 100f) * 0.35f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                val color = pixels[srcIdx]
                var r = ((color shr 16) and 0xFF).toFloat()
                var g = ((color shr 8) and 0xFF).toFloat()
                var b = (color and 0xFF).toFloat()

                // Painterly color density boost
                val mean = (r + g + b) / 3f
                r = (mean + (r - mean) * colorDensityFactor).coerceIn(0f, 255f)
                g = (mean + (g - mean) * colorDensityFactor).coerceIn(0f, 255f)
                b = (mean + (b - mean) * colorDensityFactor).coerceIn(0f, 255f)

                val effectiveWeight = if (isReference) weight * 1.25f else weight
                accR[destIdx] += r * effectiveWeight
                accG[destIdx] += g * effectiveWeight
                accB[destIdx] += b * effectiveWeight
                accW[destIdx] += effectiveWeight
            }
        }
    }

    private fun accumulateInTheRound(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters
    ) {
        val centerX = w / 2f
        val centerY = h / 2f
        val maxRadius = min(w, h) / 2f
        val stability = (params.subjectStability / 100f)

        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                // Radial falloff: Center subject is anchored, perimeter allows multiple silhouette angles
                val distFromCenter = kotlin.math.hypot(x - centerX, y - centerY)
                val normRadius = (distFromCenter / maxRadius).coerceIn(0f, 1f)

                val spatialWeight = if (isReference) {
                    weight * (1f + stability * (1f - normRadius) * 2.0f)
                } else {
                    weight * (1f + (1f - stability) * normRadius)
                }

                val color = pixels[srcIdx]
                accR[destIdx] += ((color shr 16) and 0xFF) * spatialWeight
                accG[destIdx] += ((color shr 8) and 0xFF) * spatialWeight
                accB[destIdx] += (color and 0xFF) * spatialWeight
                accW[destIdx] += spatialWeight
            }
        }
    }

    private fun accumulateArchitectural(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters
    ) {
        // High geometric structure accumulation: accentuates dark contrast and linear edges
        val edgeBoost = 1.0f + (params.edgeEmphasis / 100f) * 0.4f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                val color = pixels[srcIdx]
                var r = ((color shr 16) and 0xFF).toFloat()
                var g = ((color shr 8) and 0xFF).toFloat()
                var b = (color and 0xFF).toFloat()

                // Enhance structural microcontrast
                val lum = (0.299f * r + 0.587f * g + 0.114f * b)
                val contrastMod = if (lum < 128f) 0.88f else 1.12f * edgeBoost

                r = (r * contrastMod).coerceIn(0f, 255f)
                g = (g * contrastMod).coerceIn(0f, 255f)
                b = (b * contrastMod).coerceIn(0f, 255f)

                accR[destIdx] += r * weight
                accG[destIdx] += g * weight
                accB[destIdx] += b * weight
                accW[destIdx] += weight
            }
        }
    }

    private fun accumulateEthereal(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters
    ) {
        // High-key ethereal transparency and highlight lift
        val highlightLift = 1.0f + (params.softness / 100f) * 0.25f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                val color = pixels[srcIdx]
                var r = ((color shr 16) and 0xFF).toFloat() * highlightLift
                var g = ((color shr 8) and 0xFF).toFloat() * highlightLift
                var b = (color and 0xFF).toFloat() * highlightLift

                // Soft transparency accumulation
                accR[destIdx] += r * weight
                accG[destIdx] += g * weight
                accB[destIdx] += b * weight
                accW[destIdx] += weight
            }
        }
    }

    private fun accumulateMotionFlow(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters
    ) {
        val motionMult = 1.0f + (params.motionStrength / 100f) * 0.3f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                val color = pixels[srcIdx]
                val r = ((color shr 16) and 0xFF).toFloat()
                val g = ((color shr 8) and 0xFF).toFloat()
                val b = (color and 0xFF).toFloat()

                accR[destIdx] += r * weight * motionMult
                accG[destIdx] += g * weight * motionMult
                accB[destIdx] += b * weight * motionMult
                accW[destIdx] += weight * motionMult
            }
        }
    }

    private fun accumulateUrbanLayers(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters
    ) {
        val density = 1.0f + (params.colorDensity / 100f) * 0.45f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                val color = pixels[srcIdx]
                var r = ((color shr 16) and 0xFF).toFloat()
                var g = ((color shr 8) and 0xFF).toFloat()
                var b = (color and 0xFF).toFloat()

                // Gritty street saturation & contrast
                val avg = (r + g + b) / 3f
                r = (avg + (r - avg) * density).coerceIn(0f, 255f)
                g = (avg + (g - avg) * density).coerceIn(0f, 255f)
                b = (avg + (b - avg) * density).coerceIn(0f, 255f)

                accR[destIdx] += r * weight
                accG[destIdx] += g * weight
                accB[destIdx] += b * weight
                accW[destIdx] += weight
            }
        }
    }

    // --- Post-Processing Finishing Passes ---

    private fun applyStylePostProcessing(
        pixels: IntArray,
        w: Int,
        h: Int,
        artStyle: OrbitArtStyle,
        params: OrbitStyleParameters
    ) {
        // 1. Edge Emphasis (Unsharp/Laplacian sharpening)
        if (params.edgeEmphasis > 25f) {
            applyEdgeEmphasis(pixels, w, h, (params.edgeEmphasis - 25f) / 75f)
        }

        // 2. Softness pass (Gentle 3x3 box blend)
        if (params.softness > 25f) {
            applySoftnessGlow(pixels, w, h, (params.softness - 25f) / 75f)
        }
    }

    private fun applyEdgeEmphasis(pixels: IntArray, w: Int, h: Int, strength: Float) {
        val copy = pixels.clone()
        val factor = strength * 0.6f

        for (y in 1 until h - 1) {
            val yOffset = y * w
            for (x in 1 until w - 1) {
                val idx = yOffset + x

                val cCenter = copy[idx]
                val cUp = copy[idx - w]
                val cDown = copy[idx + w]
                val cLeft = copy[idx - 1]
                val cRight = copy[idx + 1]

                val rCenter = (cCenter shr 16) and 0xFF
                val gCenter = (cCenter shr 8) and 0xFF
                val bCenter = cCenter and 0xFF

                val rSurround = (((cUp shr 16) and 0xFF) + ((cDown shr 16) and 0xFF) + ((cLeft shr 16) and 0xFF) + ((cRight shr 16) and 0xFF)) / 4
                val gSurround = (((cUp shr 8) and 0xFF) + ((cDown shr 8) and 0xFF) + ((cLeft shr 8) and 0xFF) + ((cRight shr 8) and 0xFF)) / 4
                val bSurround = ((cUp and 0xFF) + (cDown and 0xFF) + (cLeft and 0xFF) + (cRight and 0xFF)) / 4

                val rNew = (rCenter + (rCenter - rSurround) * factor).roundToInt().coerceIn(0, 255)
                val gNew = (gCenter + (gCenter - gSurround) * factor).roundToInt().coerceIn(0, 255)
                val bNew = (bCenter + (bCenter - bSurround) * factor).roundToInt().coerceIn(0, 255)

                pixels[idx] = (0xFF shl 24) or (rNew shl 16) or (gNew shl 8) or bNew
            }
        }
    }

    private fun applySoftnessGlow(pixels: IntArray, w: Int, h: Int, strength: Float) {
        val copy = pixels.clone()
        val blendFactor = strength * 0.45f

        for (y in 1 until h - 1) {
            val yOffset = y * w
            for (x in 1 until w - 1) {
                val idx = yOffset + x

                val cCenter = copy[idx]
                val cUp = copy[idx - w]
                val cDown = copy[idx + w]
                val cLeft = copy[idx - 1]
                val cRight = copy[idx + 1]

                val rAvg = (((cCenter shr 16) and 0xFF) + ((cUp shr 16) and 0xFF) + ((cDown shr 16) and 0xFF) + ((cLeft shr 16) and 0xFF) + ((cRight shr 16) and 0xFF)) / 5f
                val gAvg = (((cCenter shr 8) and 0xFF) + ((cUp shr 8) and 0xFF) + ((cDown shr 8) and 0xFF) + ((cLeft shr 8) and 0xFF) + ((cRight shr 8) and 0xFF)) / 5f
                val bAvg = ((cCenter and 0xFF) + (cUp and 0xFF) + (cDown and 0xFF) + (cLeft and 0xFF) + (cRight and 0xFF)) / 5f

                val rOrig = ((cCenter shr 16) and 0xFF).toFloat()
                val gOrig = ((cCenter shr 8) and 0xFF).toFloat()
                val bOrig = (cCenter and 0xFF).toFloat()

                val rFinal = (rOrig * (1f - blendFactor) + rAvg * blendFactor).roundToInt().coerceIn(0, 255)
                val gFinal = (gOrig * (1f - blendFactor) + gAvg * blendFactor).roundToInt().coerceIn(0, 255)
                val bFinal = (bOrig * (1f - blendFactor) + bAvg * blendFactor).roundToInt().coerceIn(0, 255)

                pixels[idx] = (0xFF shl 24) or (rFinal shl 16) or (gFinal shl 8) or bFinal
            }
        }
    }

    /**
     * Optional coarse 2D translation alignment via fast downsampled cross-correlation
     * against the chosen reference image. Deterministic, fast, offline.
     */
    suspend fun autoAlignImages(
        imagePaths: List<String>,
        referenceIndex: Int
    ): Map<Int, Pair<Float, Float>> = withContext(Dispatchers.Default) {
        val result = mutableMapOf<Int, Pair<Float, Float>>()
        val total = imagePaths.size
        val safeRef = referenceIndex.coerceIn(0, total - 1)

        val refPath = imagePaths[safeRef]
        val refBmp = BitmapUtils.decodeSampledBitmap(refPath, 128, 128) ?: return@withContext emptyMap()
        val rw = refBmp.width
        val rh = refBmp.height
        val refPixels = IntArray(rw * rh)
        refBmp.getPixels(refPixels, 0, rw, 0, 0, rw, rh)
        refBmp.recycle()

        // Calculate center of mass / centroid for reference luminance
        var refSumX = 0.0
        var refSumY = 0.0
        var refSumLum = 0.0
        for (y in 0 until rh) {
            for (x in 0 until rw) {
                val c = refPixels[y * rw + x]
                val lum = (0.299 * ((c shr 16) and 0xFF) + 0.587 * ((c shr 8) and 0xFF) + 0.114 * (c and 0xFF))
                refSumX += x * lum
                refSumY += y * lum
                refSumLum += lum
            }
        }
        val refCenterX = if (refSumLum > 0) refSumX / refSumLum else rw / 2.0
        val refCenterY = if (refSumLum > 0) refSumY / refSumLum else rh / 2.0

        for (i in imagePaths.indices) {
            if (i == safeRef) {
                result[i] = Pair(0f, 0f)
                continue
            }
            val path = imagePaths[i]
            val bmp = BitmapUtils.decodeSampledBitmap(path, 128, 128) ?: continue
            val bw = bmp.width
            val bh = bmp.height
            val pixels = IntArray(bw * bh)
            bmp.getPixels(pixels, 0, bw, 0, 0, bw, bh)
            bmp.recycle()

            var sumX = 0.0
            var sumY = 0.0
            var sumLum = 0.0
            for (y in 0 until bh) {
                for (x in 0 until bw) {
                    val c = pixels[y * bw + x]
                    val lum = (0.299 * ((c shr 16) and 0xFF) + 0.587 * ((c shr 8) and 0xFF) + 0.114 * (c and 0xFF))
                    sumX += x * lum
                    sumY += y * lum
                    sumLum += lum
                }
            }
            val cx = if (sumLum > 0) sumX / sumLum else bw / 2.0
            val cy = if (sumLum > 0) sumY / sumLum else bh / 2.0

            // Delta in normalized space (-10..+10 range)
            val deltaX = ((refCenterX - cx) / bw * 20.0).toFloat().coerceIn(-15f, 15f)
            val deltaY = ((refCenterY - cy) / bh * 20.0).toFloat().coerceIn(-15f, 15f)
            result[i] = Pair(deltaX, deltaY)
        }

        result
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
