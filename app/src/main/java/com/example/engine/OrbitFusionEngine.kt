package com.example.engine

import android.graphics.Bitmap
import com.example.model.OrbitArtStyle
import com.example.model.OrbitBlendMode
import com.example.model.OrbitCompositionStrategy
import com.example.model.OrbitStyleParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class FusionProgress(
    val current: Int,
    val total: Int,
    val stage: String
)

data class RegistrationConfidence(
    val imageIndex: Int,
    val shiftX: Float,
    val shiftY: Float,
    val confidence: Float, // 0.0f .. 1.0f
    val isReliable: Boolean
)

data class RegistrationReport(
    val totalImages: Int,
    val successfulCount: Int,
    val lowConfidenceCount: Int,
    val confidences: Map<Int, RegistrationConfidence>,
    val averageConfidence: Float
)

data class AutoAlignResult(
    val shifts: Map<Int, Pair<Float, Float>>,
    val report: RegistrationReport
)

object OrbitFusionEngine {

    /**
     * Compute a fused artistic composite from a list of real image files
     * according to the chosen artistic style preset, composition strategy, and parameters.
     * Uses sequential memory-safe decoding with an accumulator buffer.
     */
    suspend fun renderFusion(
        imagePaths: List<String>,
        referenceIndex: Int,
        artStyle: OrbitArtStyle = OrbitArtStyle.IN_THE_ROUND,
        compositionStrategy: OrbitCompositionStrategy = OrbitCompositionStrategy.SUBJECT_CENTERED,
        styleParams: OrbitStyleParameters = artStyle.defaultParameters,
        individualShifts: Map<Int, Pair<Float, Float>> = emptyMap(),
        globalOffsetX: Float = 0f,
        globalOffsetY: Float = 0f,
        blendMode: OrbitBlendMode = OrbitBlendMode.WEIGHTED_AVERAGE,
        targetWidth: Int = 1080,
        targetHeight: Int = 1080,
        anchorX: Float = 0.5f,
        anchorY: Float = 0.5f,
        artisticSpreadEnabled: Boolean = false,
        disabledImagePaths: Set<String> = emptySet(),
        maskEnabled: Boolean = false,
        maskRadius: Float = 45f,
        maskFeather: Float = 30f,
        maskIntensity: Float = 60f,
        onProgress: (FusionProgress) -> Unit = {}
    ): Bitmap? = withContext(Dispatchers.Default) {
        val validPaths = imagePaths.filter { File(it).exists() && it !in disabledImagePaths }
        if (validPaths.isEmpty()) return@withContext null

        val totalImages = validPaths.size
        val refPath = if (referenceIndex in imagePaths.indices) imagePaths[referenceIndex] else validPaths.first()
        val safeRefIndex = validPaths.indexOf(refPath).let { if (it >= 0) it else 0 }

        val strategyLabel = if (compositionStrategy == OrbitCompositionStrategy.STRUCTURAL_FIELD) "Flächenfeld" else "Motivzentriert"
        onProgress(FusionProgress(0, totalImages, "Stil '${artStyle.title}' ($strategyLabel) vorbereiten..."))

        // Compute weights based on art style, composition strategy, subject stability, and abstraction
        val weights = calculateStyleWeights(totalImages, safeRefIndex, artStyle, compositionStrategy, styleParams)

        val outWidth = targetWidth
        val outHeight = targetHeight

        return@withContext try {
            val accumR = FloatArray(outWidth * outHeight)
            val accumG = FloatArray(outWidth * outHeight)
            val accumB = FloatArray(outWidth * outHeight)
            val accumWeight = FloatArray(outWidth * outHeight)

            val tempPixels = IntArray(outWidth * outHeight)

            // Determine discrete echo frame indices across the series
            val echoCountInt = styleParams.echoCount.roundToInt().coerceIn(3, 12)
            val echoIndices = selectEchoIndices(totalImages, safeRefIndex, echoCountInt)
            val hasEchoes = (styleParams.echoStrength > 10f && echoIndices.isNotEmpty())

            // Allocate secondary echo accumulator only if echo contours are enabled to conserve memory
            val echoAccumR = if (hasEchoes) FloatArray(outWidth * outHeight) else null
            val echoAccumG = if (hasEchoes) FloatArray(outWidth * outHeight) else null
            val echoAccumB = if (hasEchoes) FloatArray(outWidth * outHeight) else null
            val echoAccumWeight = if (hasEchoes) FloatArray(outWidth * outHeight) else null

            for (i in validPaths.indices) {
                coroutineContext.ensureActive()
                val path = validPaths[i]
                val weight = weights[i]

                onProgress(FusionProgress(i + 1, totalImages, "${artStyle.title}: Überlagere Perspektive ${i + 1}/$totalImages..."))

                // Decode single image sampled to requested dimensions
                val sourceBitmap = BitmapUtils.decodeSampledBitmap(path, outWidth, outHeight) ?: continue

                // Center-fit crop to exact aspect ratio (1:1, 4:5, 16:9) without distortion
                val fittedBitmap = BitmapUtils.cropCenterFit(sourceBitmap, outWidth, outHeight)
                fittedBitmap.getPixels(tempPixels, 0, outWidth, 0, 0, outWidth, outHeight)
                fittedBitmap.recycle()

                // Calculate shift: combine global offset, individual shift, and perspective spread
                val isReference = (i == safeRefIndex)
                // Map original image index if shifts are indexed by original list
                val origIndex = imagePaths.indexOf(path)
                val indShift = individualShifts[origIndex] ?: individualShifts[i] ?: Pair(0f, 0f)

                // Perspective spread:
                // When artisticSpreadEnabled is FALSE: strictly use genuine photographic displacement
                // When artisticSpreadEnabled is TRUE: apply algorithmic trigonometric spread around the anchor
                val spreadFactor = if (artisticSpreadEnabled) (styleParams.perspectiveSpread / 100f) * (outWidth / 180f) else 0f
                val spreadOffsetX: Float
                val spreadOffsetY: Float
                if (spreadFactor > 0f) {
                    if (compositionStrategy == OrbitCompositionStrategy.STRUCTURAL_FIELD) {
                        val phaseX = (i * 1.618f)
                        val phaseY = (i * 2.414f)
                        spreadOffsetX = if (!isReference) (cos(phaseX) * spreadFactor * 0.85f).toFloat() else 0f
                        spreadOffsetY = if (!isReference) (sin(phaseY) * spreadFactor * 0.70f).toFloat() else 0f
                    } else {
                        val angleRad = (i.toFloat() / max(1, totalImages)) * (2.0 * Math.PI)
                        spreadOffsetX = if (!isReference) (cos(angleRad) * spreadFactor).toFloat() else 0f
                        spreadOffsetY = if (!isReference) (sin(angleRad) * spreadFactor * 0.65f).toFloat() else 0f
                    }
                } else {
                    spreadOffsetX = 0f
                    spreadOffsetY = 0f
                }

                // Motion flow creates directional shift along series sequence
                val motionOffsetX = if (artStyle == OrbitArtStyle.MOTION_FLOW) {
                    (i - safeRefIndex).toFloat() * (styleParams.motionStrength / 100f) * 5.0f
                } else 0f

                val motionOffsetY = if (artStyle == OrbitArtStyle.MOTION_FLOW) {
                    (i - safeRefIndex).toFloat() * (styleParams.motionStrength / 100f) * 1.8f
                } else 0f

                val totalShiftX = ((if (isReference) globalOffsetX else 0f) + indShift.first + spreadOffsetX + motionOffsetX) * (outWidth / 400f)
                val totalShiftY = ((if (isReference) globalOffsetY else 0f) + indShift.second + spreadOffsetY + motionOffsetY) * (outHeight / 400f)

                val shiftX = totalShiftX.roundToInt()
                val shiftY = totalShiftY.roundToInt()

                // Accumulate pixels based on art style specific fusion strategy and blend mode
                when (artStyle) {
                    OrbitArtStyle.IN_THE_ROUND -> {
                        accumulateInTheRound(
                            tempPixels, accumR, accumG, accumB, accumWeight,
                            outWidth, outHeight, weight, shiftX, shiftY, isReference, compositionStrategy, styleParams,
                            blendMode, anchorX, anchorY, maskEnabled, maskRadius, maskFeather, maskIntensity
                        )
                    }
                    OrbitArtStyle.IMPRESSIONIST -> {
                        accumulateImpressionist(
                            tempPixels, accumR, accumG, accumB, accumWeight,
                            outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams, blendMode
                        )
                    }
                    OrbitArtStyle.ARCHITECTURAL -> {
                        accumulateArchitectural(
                            tempPixels, accumR, accumG, accumB, accumWeight,
                            outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams, blendMode
                        )
                    }
                    OrbitArtStyle.ETHEREAL -> {
                        accumulateEthereal(
                            tempPixels, accumR, accumG, accumB, accumWeight,
                            outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams, blendMode
                        )
                    }
                    OrbitArtStyle.MOTION_FLOW -> {
                        accumulateMotionFlow(
                            tempPixels, accumR, accumG, accumB, accumWeight,
                            outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams, blendMode
                        )
                    }
                    OrbitArtStyle.URBAN_LAYERS -> {
                        accumulateUrbanLayers(
                            tempPixels, accumR, accumG, accumB, accumWeight,
                            outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams, blendMode
                        )
                    }
                }

                // If this is one of the chosen key perspective echo frames, capture its contour layer
                if (i in echoIndices && !isReference && hasEchoes && echoAccumR != null && echoAccumG != null && echoAccumB != null && echoAccumWeight != null) {
                    accumulateEchoContours(
                        tempPixels, echoAccumR, echoAccumG, echoAccumB, echoAccumWeight,
                        outWidth, outHeight, shiftX, shiftY, styleParams
                    )
                }
            }

            onProgress(FusionProgress(totalImages, totalImages, "Künstlerische Farbkurven & Konturen veredeln..."))

            // Create base fused pixel array with blend mode resolution
            val outputPixels = IntArray(outWidth * outHeight)
            val echoWeightNormalized = (styleParams.echoStrength / 100f) * 0.48f

            for (idx in outputPixels.indices) {
                val w = accumWeight[idx]
                var r = 0f
                var g = 0f
                var b = 0f

                if (w > 0f) {
                    when (blendMode) {
                        OrbitBlendMode.LIGHTEN, OrbitBlendMode.LIGHTEN_ADDITIVE -> {
                            r = accumR[idx]
                            g = accumG[idx]
                            b = accumB[idx]
                        }
                        OrbitBlendMode.MULTIPLY, OrbitBlendMode.MULTIPLY_DENSITY -> {
                            // Exposure-compensated logarithmic product (geometric mean)
                            r = (exp(accumR[idx] / w) * 0.95f).coerceIn(0f, 255f)
                            g = (exp(accumG[idx] / w) * 0.95f).coerceIn(0f, 255f)
                            b = (exp(accumB[idx] / w) * 0.95f).coerceIn(0f, 255f)
                        }
                        OrbitBlendMode.SCREEN -> {
                            // Exposure-compensated screen inversion
                            r = ((1f - exp(accumR[idx] / w)).coerceIn(0f, 1f) * 255f).coerceIn(0f, 255f)
                            g = ((1f - exp(accumG[idx] / w)).coerceIn(0f, 1f) * 255f).coerceIn(0f, 255f)
                            b = ((1f - exp(accumB[idx] / w)).coerceIn(0f, 1f) * 255f).coerceIn(0f, 255f)
                        }
                        OrbitBlendMode.WEIGHTED_AVERAGE, OrbitBlendMode.DRIFT_FLOW -> {
                            r = (accumR[idx] / w).coerceIn(0f, 255f)
                            g = (accumG[idx] / w).coerceIn(0f, 255f)
                            b = (accumB[idx] / w).coerceIn(0f, 255f)
                        }
                    }
                }

                // Blend discrete perspective echoes into the composition
                val ew = echoAccumWeight?.get(idx) ?: 0f
                if (ew > 0f && echoWeightNormalized > 0f && echoAccumR != null && echoAccumG != null && echoAccumB != null) {
                    val er = echoAccumR[idx] / ew
                    val eg = echoAccumG[idx] / ew
                    val eb = echoAccumB[idx] / ew

                    // Overlay high-frequency perspective contours
                    r = (r * (1f - echoWeightNormalized) + er * echoWeightNormalized).coerceIn(0f, 255f)
                    g = (g * (1f - echoWeightNormalized) + eg * echoWeightNormalized).coerceIn(0f, 255f)
                    b = (b * (1f - echoWeightNormalized) + eb * echoWeightNormalized).coerceIn(0f, 255f)
                }

                outputPixels[idx] = (0xFF shl 24) or (r.roundToInt() shl 16) or (g.roundToInt() shl 8) or b.roundToInt()
            }

            // Apply advanced post-processing:
            // Centered on the user-selected subject anchor point (anchorX, anchorY)
            coroutineContext.ensureActive()
            applyCreativePostProcessing(outputPixels, outWidth, outHeight, artStyle, compositionStrategy, styleParams, anchorX, anchorY)

            coroutineContext.ensureActive()
            val resultBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            resultBitmap.setPixels(outputPixels, 0, outWidth, 0, 0, outWidth, outHeight)
            resultBitmap
        } catch (oom: OutOfMemoryError) {
            System.gc()
            oom.printStackTrace()
            null
        }
    }

    private fun selectEchoIndices(total: Int, refIndex: Int, count: Int): Set<Int> {
        if (total <= 2) return emptySet()
        val step = max(1, total / count)
        val indices = mutableSetOf<Int>()
        var idx = 0
        while (idx < total && indices.size < count) {
            if (idx != refIndex) indices.add(idx)
            idx += step
        }
        return indices
    }

    private fun calculateStyleWeights(
        total: Int,
        refIndex: Int,
        artStyle: OrbitArtStyle,
        compositionStrategy: OrbitCompositionStrategy,
        params: OrbitStyleParameters
    ): FloatArray {
        val weights = FloatArray(total)
        val abstractionNorm = (params.abstraction / 100f).coerceIn(0f, 1f)
        val stabilityNorm = (params.subjectStability / 100f).coerceIn(0f, 1f)

        if (total == 1) {
            weights[0] = 1f
            return weights
        }

        if (compositionStrategy == OrbitCompositionStrategy.STRUCTURAL_FIELD) {
            // STRUCTURAL FIELD: balanced distribution for planar textures
            val base = 1.0f / total
            val refBoost = 1.0f + (stabilityNorm * 0.75f)
            for (i in 0 until total) {
                weights[i] = if (i == refIndex) base * refBoost else base
            }
            normalize(weights)
            return weights
        }

        // SUBJECT CENTERED
        val refDominance = lerp(0.12f, 0.45f, stabilityNorm * (1.0f - abstractionNorm * 0.6f))
        val remainingWeight = 1.0f - refDominance

        val rawOthers = FloatArray(total)
        var otherSum = 0f
        for (i in 0 until total) {
            if (i == refIndex) continue
            val dist = abs(i - refIndex).toFloat()
            val normDist = dist / total
            val falloff = exp(-normDist * (3.0f - abstractionNorm * 2.2f))

            val weightFactor = when (artStyle) {
                OrbitArtStyle.MOTION_FLOW -> {
                    val timeFactor = (i.toFloat() / total)
                    lerp(falloff, timeFactor, (params.motionStrength / 100f) * 0.75f)
                }
                OrbitArtStyle.ETHEREAL -> {
                    falloff * 0.7f + 0.3f
                }
                OrbitArtStyle.URBAN_LAYERS -> {
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

    private fun blendSample(
        blendMode: OrbitBlendMode,
        destIdx: Int,
        r: Float, g: Float, b: Float,
        w: Float,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray
    ) {
        when (blendMode) {
            OrbitBlendMode.LIGHTEN, OrbitBlendMode.LIGHTEN_ADDITIVE -> {
                accR[destIdx] = max(accR[destIdx], r)
                accG[destIdx] = max(accG[destIdx], g)
                accB[destIdx] = max(accB[destIdx], b)
                accW[destIdx] = 1f
            }
            OrbitBlendMode.MULTIPLY, OrbitBlendMode.MULTIPLY_DENSITY -> {
                accR[destIdx] += ln(max(1f, r)) * w
                accG[destIdx] += ln(max(1f, g)) * w
                accB[destIdx] += ln(max(1f, b)) * w
                accW[destIdx] += w
            }
            OrbitBlendMode.SCREEN -> {
                accR[destIdx] += ln(max(0.001f, 1f - (r / 255f).coerceIn(0f, 0.999f))) * w
                accG[destIdx] += ln(max(0.001f, 1f - (g / 255f).coerceIn(0f, 0.999f))) * w
                accB[destIdx] += ln(max(0.001f, 1f - (b / 255f).coerceIn(0f, 0.999f))) * w
                accW[destIdx] += w
            }
            OrbitBlendMode.WEIGHTED_AVERAGE, OrbitBlendMode.DRIFT_FLOW -> {
                accR[destIdx] += r * w
                accG[destIdx] += g * w
                accB[destIdx] += b * w
                accW[destIdx] += w
            }
        }
    }

    // --- Style-Specific Accumulation Functions ---

    private fun accumulateInTheRound(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, compositionStrategy: OrbitCompositionStrategy, params: OrbitStyleParameters,
        blendMode: OrbitBlendMode,
        anchorX: Float = 0.5f, anchorY: Float = 0.5f,
        maskEnabled: Boolean = false, maskRadius: Float = 45f, maskFeather: Float = 30f, maskIntensity: Float = 60f
    ) {
        val centerX = if (compositionStrategy == OrbitCompositionStrategy.SUBJECT_CENTERED) anchorX * w else w / 2f
        val centerY = if (compositionStrategy == OrbitCompositionStrategy.SUBJECT_CENTERED) anchorY * h else h / 2f
        val maxRadius = hypot(max(centerX, w - centerX), max(centerY, h - centerY))
        val stability = (params.subjectStability / 100f)
        val dissolveStrength = (params.backgroundDissolve / 100f)
        val colorDensityMod = 1.0f + (params.colorDensity / 100f) * 0.45f
        val isStructural = (compositionStrategy == OrbitCompositionStrategy.STRUCTURAL_FIELD)

        val rCore = (maskRadius / 100f) * min(w, h) * 0.5f
        val rFeatherDist = (maskFeather / 100f) * rCore + 1f
        val maskBoost = (maskIntensity / 50f)

        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            val yOffset = y * w
            val srcYOffset = srcY * w

            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcYOffset + srcX
                val destIdx = yOffset + x

                val spatialWeight = if (isStructural) {
                    weight * (1.0f + (if (isReference) stability * 0.4f else 0f))
                } else {
                    val dist = hypot(x - centerX, y - centerY)
                    val normRadius = (dist / maxRadius).coerceIn(0f, 1f)
                    var baseW = if (isReference) {
                        weight * (1.2f + stability * (1f - normRadius) * 2.8f)
                    } else {
                        weight * (0.8f + (1f - stability + dissolveStrength * 0.5f) * normRadius)
                    }

                    if (maskEnabled) {
                        val maskWeight = if (dist <= rCore) {
                            1f
                        } else if (dist < rCore + rFeatherDist) {
                            val t = (dist - rCore) / rFeatherDist
                            1f - (3f * t * t - 2f * t * t * t)
                        } else {
                            0f
                        }
                        if (isReference) {
                            baseW *= (1f + maskWeight * maskBoost * 1.5f)
                        } else {
                            baseW *= (1f - maskWeight * (maskBoost * 0.4f).coerceAtMost(0.8f))
                        }
                    }
                    baseW
                }

                val color = pixels[srcIdx]
                var r = ((color shr 16) and 0xFF).toFloat()
                var g = ((color shr 8) and 0xFF).toFloat()
                var b = (color and 0xFF).toFloat()

                val mean = (r + g + b) / 3f
                r = (mean + (r - mean) * colorDensityMod).coerceIn(0f, 255f)
                g = (mean + (g - mean) * colorDensityMod).coerceIn(0f, 255f)
                b = (mean + (b - mean) * colorDensityMod).coerceIn(0f, 255f)

                blendSample(blendMode, destIdx, r, g, b, spatialWeight, accR, accG, accB, accW)
            }
        }
    }

    private fun accumulateImpressionist(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters,
        blendMode: OrbitBlendMode
    ) {
        val colorDensityFactor = 1.0f + (params.colorDensity / 100f) * 0.4f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            val yOffset = y * w
            val srcYOffset = srcY * w

            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcYOffset + srcX
                val destIdx = yOffset + x

                val color = pixels[srcIdx]
                var r = ((color shr 16) and 0xFF).toFloat()
                var g = ((color shr 8) and 0xFF).toFloat()
                var b = (color and 0xFF).toFloat()

                val mean = (r + g + b) / 3f
                r = (mean + (r - mean) * colorDensityFactor).coerceIn(0f, 255f)
                g = (mean + (g - mean) * colorDensityFactor).coerceIn(0f, 255f)
                b = (mean + (b - mean) * colorDensityFactor).coerceIn(0f, 255f)

                val effectiveWeight = if (isReference) weight * 1.35f else weight
                blendSample(blendMode, destIdx, r, g, b, effectiveWeight, accR, accG, accB, accW)
            }
        }
    }

    private fun accumulateArchitectural(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters,
        blendMode: OrbitBlendMode
    ) {
        val edgeBoost = 1.0f + (params.edgeEmphasis / 100f) * 0.5f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            val yOffset = y * w
            val srcYOffset = srcY * w

            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcYOffset + srcX
                val destIdx = yOffset + x

                val color = pixels[srcIdx]
                var r = ((color shr 16) and 0xFF).toFloat()
                var g = ((color shr 8) and 0xFF).toFloat()
                var b = (color and 0xFF).toFloat()

                val lum = (0.299f * r + 0.587f * g + 0.114f * b)
                val contrastMod = if (lum < 128f) 0.85f else 1.15f * edgeBoost

                r = (r * contrastMod).coerceIn(0f, 255f)
                g = (g * contrastMod).coerceIn(0f, 255f)
                b = (b * contrastMod).coerceIn(0f, 255f)

                blendSample(blendMode, destIdx, r, g, b, weight, accR, accG, accB, accW)
            }
        }
    }

    private fun accumulateEthereal(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters,
        blendMode: OrbitBlendMode
    ) {
        val highlightLift = 1.0f + (params.lightGlow / 100f) * 0.35f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            val yOffset = y * w
            val srcYOffset = srcY * w

            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcYOffset + srcX
                val destIdx = yOffset + x

                val color = pixels[srcIdx]
                val r = (((color shr 16) and 0xFF).toFloat() * highlightLift).coerceIn(0f, 255f)
                val g = (((color shr 8) and 0xFF).toFloat() * highlightLift).coerceIn(0f, 255f)
                val b = ((color and 0xFF).toFloat() * highlightLift).coerceIn(0f, 255f)

                blendSample(blendMode, destIdx, r, g, b, weight, accR, accG, accB, accW)
            }
        }
    }

    private fun accumulateMotionFlow(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters,
        blendMode: OrbitBlendMode
    ) {
        val motionMult = 1.0f + (params.motionStrength / 100f) * 0.4f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            val yOffset = y * w
            val srcYOffset = srcY * w

            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcYOffset + srcX
                val destIdx = yOffset + x

                val color = pixels[srcIdx]
                val r = ((color shr 16) and 0xFF).toFloat()
                val g = ((color shr 8) and 0xFF).toFloat()
                val b = (color and 0xFF).toFloat()

                blendSample(blendMode, destIdx, r, g, b, weight * motionMult, accR, accG, accB, accW)
            }
        }
    }

    private fun accumulateUrbanLayers(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters,
        blendMode: OrbitBlendMode
    ) {
        val density = 1.0f + (params.colorDensity / 100f) * 0.5f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            val yOffset = y * w
            val srcYOffset = srcY * w

            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcYOffset + srcX
                val destIdx = yOffset + x

                val color = pixels[srcIdx]
                var r = ((color shr 16) and 0xFF).toFloat()
                var g = ((color shr 8) and 0xFF).toFloat()
                var b = (color and 0xFF).toFloat()

                val avg = (r + g + b) / 3f
                r = (avg + (r - avg) * density).coerceIn(0f, 255f)
                g = (avg + (g - avg) * density).coerceIn(0f, 255f)
                b = (avg + (b - avg) * density).coerceIn(0f, 255f)

                blendSample(blendMode, destIdx, r, g, b, weight, accR, accG, accB, accW)
            }
        }
    }

    private fun accumulateEchoContours(
        pixels: IntArray,
        echoR: FloatArray, echoG: FloatArray, echoB: FloatArray, echoW: FloatArray,
        w: Int, h: Int, shiftX: Int, shiftY: Int, params: OrbitStyleParameters
    ) {
        for (y in 1 until h - 1) {
            val srcY = y - shiftY
            if (srcY !in 1 until h - 1) continue
            for (x in 1 until w - 1) {
                val srcX = x - shiftX
                if (srcX !in 1 until w - 1) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                val cCenter = pixels[srcIdx]
                val cRight = pixels[srcIdx + 1]
                val cDown = pixels[srcIdx + w]

                val diffR = abs(((cCenter shr 16) and 0xFF) - ((cRight shr 16) and 0xFF)) + abs(((cCenter shr 16) and 0xFF) - ((cDown shr 16) and 0xFF))
                val diffG = abs(((cCenter shr 8) and 0xFF) - ((cRight shr 8) and 0xFF)) + abs(((cCenter shr 8) and 0xFF) - ((cDown shr 8) and 0xFF))
                val diffB = abs((cCenter and 0xFF) - (cRight and 0xFF)) + abs((cCenter and 0xFF) - (cDown and 0xFF))

                if (diffR + diffG + diffB > 28) {
                    val r = ((cCenter shr 16) and 0xFF).toFloat()
                    val g = ((cCenter shr 8) and 0xFF).toFloat()
                    val b = (cCenter and 0xFF).toFloat()

                    echoR[destIdx] += r
                    echoG[destIdx] += g
                    echoB[destIdx] += b
                    echoW[destIdx] += 1f
                }
            }
        }
    }

    // --- Post-Processing with Positionable Anchor ---

    private fun applyCreativePostProcessing(
        pixels: IntArray,
        w: Int,
        h: Int,
        artStyle: OrbitArtStyle,
        compositionStrategy: OrbitCompositionStrategy,
        params: OrbitStyleParameters,
        anchorX: Float = 0.5f,
        anchorY: Float = 0.5f
    ) {
        val centerX = if (compositionStrategy == OrbitCompositionStrategy.SUBJECT_CENTERED) anchorX * w else w / 2f
        val centerY = if (compositionStrategy == OrbitCompositionStrategy.SUBJECT_CENTERED) anchorY * h else h / 2f
        val maxRadius = hypot(max(centerX, w - centerX), max(centerY, h - centerY))

        val vibranceFactor = (params.vibrance / 100f) * 1.5f
        val intensityFactor = (params.colorIntensity / 50f)
        val contrastFactor = (params.contrast - 50f) / 50f
        val warmFactor = (params.warmth / 100f).coerceIn(-1f, 1f)
        val densityFactor = 1.0f + (params.colorDensity / 100f) * 0.40f

        for (i in pixels.indices) {
            val c = pixels[i]
            var r = ((c shr 16) and 0xFF).toFloat()
            var g = ((c shr 8) and 0xFF).toFloat()
            var b = (c and 0xFF).toFloat()

            val mean = (r + g + b) / 3f
            r = (mean + (r - mean) * densityFactor).coerceIn(0f, 255f)
            g = (mean + (g - mean) * densityFactor).coerceIn(0f, 255f)
            b = (mean + (b - mean) * densityFactor).coerceIn(0f, 255f)

            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val sat = if (maxC > 0f) (maxC - minC) / maxC else 0f
            val vibranceMultiplier = 1.0f + (1.0f - sat) * vibranceFactor
            val totalSatMultiplier = vibranceMultiplier * intensityFactor

            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            r = (lum + (r - lum) * totalSatMultiplier).coerceIn(0f, 255f)
            g = (lum + (g - lum) * totalSatMultiplier).coerceIn(0f, 255f)
            b = (lum + (b - lum) * totalSatMultiplier).coerceIn(0f, 255f)

            if (warmFactor > 0f) {
                r = (r * (1f + warmFactor * 0.28f)).coerceIn(0f, 255f)
                g = (g * (1f + warmFactor * 0.08f)).coerceIn(0f, 255f)
                b = (b * (1f - warmFactor * 0.25f)).coerceIn(0f, 255f)
            } else if (warmFactor < 0f) {
                val cool = -warmFactor
                r = (r * (1f - cool * 0.22f)).coerceIn(0f, 255f)
                b = (b * (1f + cool * 0.26f)).coerceIn(0f, 255f)
            }

            if (contrastFactor != 0f) {
                val normLum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val sCurve = (normLum * normLum * (3f - 2f * normLum))
                val targetLum = if (contrastFactor > 0f) {
                    lerp(normLum, sCurve, contrastFactor)
                } else {
                    lerp(normLum, (normLum + 0.5f) / 2f, -contrastFactor * 0.5f)
                }
                val lumScale = if (normLum > 0.01f) (targetLum / normLum) else 1f
                r = (r * lumScale).coerceIn(0f, 255f)
                g = (g * lumScale).coerceIn(0f, 255f)
                b = (b * lumScale).coerceIn(0f, 255f)
            }

            pixels[i] = (0xFF shl 24) or (r.roundToInt() shl 16) or (g.roundToInt() shl 8) or b.roundToInt()
        }

        if (params.backgroundDissolve > 15f || params.softness > 20f) {
            applyBackgroundDissolveAndGlow(pixels, w, h, centerX, centerY, maxRadius, compositionStrategy, params)
        }

        if (params.edgeEmphasis > 20f) {
            applyEdgeEmphasis(pixels, w, h, (params.edgeEmphasis - 15f) / 85f)
        }
    }

    private fun applyBackgroundDissolveAndGlow(
        pixels: IntArray,
        w: Int,
        h: Int,
        cx: Float,
        cy: Float,
        maxRadius: Float,
        compositionStrategy: OrbitCompositionStrategy,
        params: OrbitStyleParameters
    ) {
        val copy = pixels.clone()
        val dissolveStrength = (params.backgroundDissolve / 100f)
        val stability = (params.subjectStability / 100f)
        val coreRadius = maxRadius * 0.32f * (stability + 0.3f)
        val glowStrength = (params.lightGlow / 100f) * 0.4f
        val isStructural = (compositionStrategy == OrbitCompositionStrategy.STRUCTURAL_FIELD)

        for (y in 1 until h - 1) {
            val yOffset = y * w
            for (x in 1 until w - 1) {
                val idx = yOffset + x

                val dissolveFactor = if (isStructural) {
                    dissolveStrength * 0.65f
                } else {
                    val dist = hypot(x - cx, y - cy)
                    if (dist > coreRadius) {
                        ((dist - coreRadius) / (maxRadius - coreRadius)).coerceIn(0f, 1f) * dissolveStrength
                    } else 0f
                }

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

                var rFinal = lerp(rOrig, rAvg, dissolveFactor * 0.7f)
                var gFinal = lerp(gOrig, gAvg, dissolveFactor * 0.7f)
                var bFinal = lerp(bOrig, bAvg, dissolveFactor * 0.7f)

                val lum = (0.299f * rFinal + 0.587f * gFinal + 0.114f * bFinal)
                if (lum > 175f && glowStrength > 0f) {
                    val bloom = (lum - 175f) / 80f * glowStrength
                    rFinal = (rFinal + 255f * bloom * 0.4f).coerceIn(0f, 255f)
                    gFinal = (gFinal + 235f * bloom * 0.35f).coerceIn(0f, 255f)
                    bFinal = (bFinal + 200f * bloom * 0.3f).coerceIn(0f, 255f)
                }

                pixels[idx] = (0xFF shl 24) or (rFinal.roundToInt() shl 16) or (gFinal.roundToInt() shl 8) or bFinal.roundToInt()
            }
        }
    }

    private fun applyEdgeEmphasis(pixels: IntArray, w: Int, h: Int, strength: Float) {
        val copy = pixels.clone()
        val factor = strength * 0.75f

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

    /**
     * Authentic 2D Image Registration:
     * Computes Sobel edge feature maps and 2D normalized cross-correlation (NCC)
     * against the chosen reference image.
     * Produces accurate shifts and a detailed RegistrationReport with per-image confidence.
     */
    suspend fun autoAlignImages(
        imagePaths: List<String>,
        referenceIndex: Int
    ): AutoAlignResult = withContext(Dispatchers.Default) {
        val total = imagePaths.size
        if (total == 0) {
            return@withContext AutoAlignResult(
                emptyMap(),
                RegistrationReport(0, 0, 0, emptyMap(), 0f)
            )
        }
        val safeRef = referenceIndex.coerceIn(0, total - 1)
        val shifts = mutableMapOf<Int, Pair<Float, Float>>()
        val confidences = mutableMapOf<Int, RegistrationConfidence>()

        // Sample dimension for fast yet feature-rich Sobel edge NCC
        val sampleSize = 128
        val refBmp = BitmapUtils.decodeSampledBitmap(imagePaths[safeRef], sampleSize, sampleSize)
        if (refBmp == null) {
            val fallbackMap = imagePaths.indices.associateWith { Pair(0f, 0f) }
            val fallbackReport = RegistrationReport(
                totalImages = total,
                successfulCount = 0,
                lowConfidenceCount = total,
                confidences = emptyMap(),
                averageConfidence = 0f
            )
            return@withContext AutoAlignResult(fallbackMap, fallbackReport)
        }

        val rw = refBmp.width
        val rh = refBmp.height
        val refPixels = IntArray(rw * rh)
        refBmp.getPixels(refPixels, 0, rw, 0, 0, rw, rh)
        refBmp.recycle()

        // 1. Compute Sobel Edge Magnitude for Reference
        val refEdges = computeSobelEdges(refPixels, rw, rh)

        // Extract centered template patch (size 48x48)
        val patchSize = 48
        val halfPatch = patchSize / 2
        val refCenterX = rw / 2
        val refCenterY = rh / 2

        var tSum = 0.0
        var tSumSq = 0.0
        val tCount = patchSize * patchSize
        val template = FloatArray(tCount)

        for (py in 0 until patchSize) {
            val ty = refCenterY - halfPatch + py
            val rowOffset = ty * rw
            for (px in 0 until patchSize) {
                val tx = refCenterX - halfPatch + px
                val v = refEdges[rowOffset + tx]
                template[py * patchSize + px] = v
                tSum += v
                tSumSq += v * v
            }
        }
        val tMean = (tSum / tCount).toFloat()
        val tVar = (tSumSq / tCount - tMean * tMean).coerceAtLeast(1e-5).toFloat()
        val tStd = sqrt(tVar)

        // Reference self-confidence
        shifts[safeRef] = Pair(0f, 0f)
        confidences[safeRef] = RegistrationConfidence(
            imageIndex = safeRef,
            shiftX = 0f,
            shiftY = 0f,
            confidence = 1.0f,
            isReliable = true
        )

        // 2. Cross-correlate each non-reference image
        val maxSearchRadius = 14 // +/- 14 pixels in 128x128 space
        val scaleToStudio = 400f / sampleSize.toFloat()

        for (i in imagePaths.indices) {
            if (i == safeRef) continue
            val path = imagePaths[i]
            val bmp = BitmapUtils.decodeSampledBitmap(path, sampleSize, sampleSize)
            if (bmp == null) {
                shifts[i] = Pair(0f, 0f)
                confidences[i] = RegistrationConfidence(i, 0f, 0f, 0f, false)
                continue
            }
            val bw = bmp.width
            val bh = bmp.height
            val pixels = IntArray(bw * bh)
            bmp.getPixels(pixels, 0, bw, 0, 0, bw, bh)
            bmp.recycle()

            val targetEdges = computeSobelEdges(pixels, bw, bh)

            var bestNcc = -1f
            var bestDx = 0
            var bestDy = 0

            // Search 2D displacement grid for peak NCC
            for (dy in -maxSearchRadius..maxSearchRadius step 2) {
                val targetCenterY = bh / 2 + dy
                if (targetCenterY - halfPatch < 0 || targetCenterY + halfPatch >= bh) continue

                for (dx in -maxSearchRadius..maxSearchRadius step 2) {
                    val targetCenterX = bw / 2 + dx
                    if (targetCenterX - halfPatch < 0 || targetCenterX + halfPatch >= bw) continue

                    var pSum = 0.0
                    var pSumSq = 0.0
                    var prodSum = 0.0

                    for (py in 0 until patchSize) {
                        val ty = targetCenterY - halfPatch + py
                        val rowOffset = ty * bw
                        for (px in 0 until patchSize) {
                            val tx = targetCenterX - halfPatch + px
                            val pv = targetEdges[rowOffset + tx]
                            val tv = template[py * patchSize + px]

                            pSum += pv
                            pSumSq += pv * pv
                            prodSum += pv * tv
                        }
                    }

                    val pMean = (pSum / tCount).toFloat()
                    val pVar = (pSumSq / tCount - pMean * pMean).coerceAtLeast(1e-5).toFloat()
                    val pStd = sqrt(pVar)

                    val cov = (prodSum / tCount - tMean * pMean).toFloat()
                    val ncc = if (tStd > 0.001f && pStd > 0.001f) (cov / (tStd * pStd)).coerceIn(-1f, 1f) else 0f

                    if (ncc > bestNcc) {
                        bestNcc = ncc
                        bestDx = dx
                        bestDy = dy
                    }
                }
            }

            // Fine-grained refinement step (+/- 1 pixel around coarse peak)
            val fineRadius = 1
            var refinedNcc = bestNcc
            var refinedDx = bestDx
            var refinedDy = bestDy

            for (fdy in (bestDy - fineRadius)..(bestDy + fineRadius)) {
                val targetCenterY = bh / 2 + fdy
                if (targetCenterY - halfPatch < 0 || targetCenterY + halfPatch >= bh) continue
                for (fdx in (bestDx - fineRadius)..(bestDx + fineRadius)) {
                    val targetCenterX = bw / 2 + fdx
                    if (targetCenterX - halfPatch < 0 || targetCenterX + halfPatch >= bw) continue

                    var pSum = 0.0
                    var pSumSq = 0.0
                    var prodSum = 0.0

                    for (py in 0 until patchSize) {
                        val ty = targetCenterY - halfPatch + py
                        val rowOffset = ty * bw
                        for (px in 0 until patchSize) {
                            val tx = targetCenterX - halfPatch + px
                            val pv = targetEdges[rowOffset + tx]
                            val tv = template[py * patchSize + px]

                            pSum += pv
                            pSumSq += pv * pv
                            prodSum += pv * tv
                        }
                    }

                    val pMean = (pSum / tCount).toFloat()
                    val pVar = (pSumSq / tCount - pMean * pMean).coerceAtLeast(1e-5).toFloat()
                    val pStd = sqrt(pVar)
                    val cov = (prodSum / tCount - tMean * pMean).toFloat()
                    val ncc = if (tStd > 0.001f && pStd > 0.001f) (cov / (tStd * pStd)).coerceIn(-1f, 1f) else 0f

                    if (ncc > refinedNcc) {
                        refinedNcc = ncc
                        refinedDx = fdx
                        refinedDy = fdy
                    }
                }
            }

            val finalShiftX = -refinedDx * scaleToStudio
            val finalShiftY = -refinedDy * scaleToStudio
            val confidenceNorm = ((refinedNcc + 1f) / 2f).coerceIn(0f, 1f)
            val isReliable = (refinedNcc >= 0.28f)

            shifts[i] = Pair(finalShiftX, finalShiftY)
            confidences[i] = RegistrationConfidence(
                imageIndex = i,
                shiftX = finalShiftX,
                shiftY = finalShiftY,
                confidence = confidenceNorm,
                isReliable = isReliable
            )
        }

        val successCount = confidences.values.count { it.isReliable }
        val lowConfCount = total - successCount
        val avgConf = if (confidences.isNotEmpty()) {
            confidences.values.map { it.confidence }.average().toFloat()
        } else 0f

        val report = RegistrationReport(
            totalImages = total,
            successfulCount = successCount,
            lowConfidenceCount = lowConfCount,
            confidences = confidences,
            averageConfidence = avgConf
        )

        AutoAlignResult(shifts, report)
    }

    private fun computeSobelEdges(pixels: IntArray, w: Int, h: Int): FloatArray {
        val gray = FloatArray(w * h)
        for (i in pixels.indices) {
            val c = pixels[i]
            gray[i] = (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF))
        }

        val edges = FloatArray(w * h)
        for (y in 1 until h - 1) {
            val yOffset = y * w
            val upOffset = (y - 1) * w
            val downOffset = (y + 1) * w

            for (x in 1 until w - 1) {
                val gx = (gray[upOffset + x + 1] + 2f * gray[yOffset + x + 1] + gray[downOffset + x + 1]) -
                        (gray[upOffset + x - 1] + 2f * gray[yOffset + x - 1] + gray[downOffset + x - 1])
                val gy = (gray[downOffset + x - 1] + 2f * gray[downOffset + x] + gray[downOffset + x + 1]) -
                        (gray[upOffset + x - 1] + 2f * gray[upOffset + x] + gray[upOffset + x + 1])

                edges[yOffset + x] = hypot(gx, gy)
            }
        }
        return edges
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
