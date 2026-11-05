package com.example.engine

import android.graphics.Bitmap
import com.example.model.OrbitArtStyle
import com.example.model.OrbitBlendMode
import com.example.model.OrbitStyleParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
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
        artStyle: OrbitArtStyle = OrbitArtStyle.IN_THE_ROUND,
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

        // Secondary accumulator for high-contrast perspective contours/echoes
        val echoAccumR = FloatArray(outWidth * outHeight)
        val echoAccumG = FloatArray(outWidth * outHeight)
        val echoAccumB = FloatArray(outWidth * outHeight)
        val echoAccumWeight = FloatArray(outWidth * outHeight)

        val tempPixels = IntArray(outWidth * outHeight)

        // Determine discrete echo frame indices across the series (for distinct Pepa Ventosa perspective outlines)
        val echoCountInt = styleParams.echoCount.roundToInt().coerceIn(3, 12)
        val echoIndices = selectEchoIndices(totalImages, safeRefIndex, echoCountInt)

        for (i in validPaths.indices) {
            coroutineContext.ensureActive()
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

            // Calculate shift: combine global offset, individual shift, and perspective spread
            val isReference = (i == safeRefIndex)
            val indShift = individualShifts[i] ?: Pair(0f, 0f)

            // Perspective spread calculations: angular spread around the subject
            val spreadFactor = (styleParams.perspectiveSpread / 100f) * (outWidth / 180f)
            val angleRad = (i.toFloat() / max(1, totalImages)) * (2.0 * Math.PI)
            val spreadOffsetX = if (!isReference) (cos(angleRad) * spreadFactor).toFloat() else 0f
            val spreadOffsetY = if (!isReference) (sin(angleRad) * spreadFactor * 0.65f).toFloat() else 0f

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

            // Accumulate pixels based on art style specific fusion strategy
            when (artStyle) {
                OrbitArtStyle.IN_THE_ROUND -> {
                    accumulateInTheRound(
                        tempPixels, accumR, accumG, accumB, accumWeight,
                        outWidth, outHeight, weight, shiftX, shiftY, isReference, styleParams
                    )
                }
                OrbitArtStyle.IMPRESSIONIST -> {
                    accumulateImpressionist(
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

            // If this is one of the chosen key perspective echo frames, capture its contour layer
            if (i in echoIndices && !isReference && styleParams.echoStrength > 10f) {
                accumulateEchoContours(
                    tempPixels, echoAccumR, echoAccumG, echoAccumB, echoAccumWeight,
                    outWidth, outHeight, shiftX, shiftY, styleParams
                )
            }
        }

        onProgress(FusionProgress(totalImages, totalImages, "Prägnante Konturen & Leuchtkraft veredeln..."))

        // Create base fused pixel array
        val outputPixels = IntArray(outWidth * outHeight)
        val echoWeightNormalized = (styleParams.echoStrength / 100f) * 0.45f

        for (idx in outputPixels.indices) {
            val w = accumWeight[idx]
            var r = if (w > 0f) (accumR[idx] / w) else 0f
            var g = if (w > 0f) (accumG[idx] / w) else 0f
            var b = if (w > 0f) (accumB[idx] / w) else 0f

            // Blend discrete perspective echoes into the composition
            val ew = echoAccumWeight[idx]
            if (ew > 0f && echoWeightNormalized > 0f) {
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
        // 1. Background dissolve (radial depth falloff)
        // 2. Vibrance & Color Intensity
        // 3. Warmth tone shaping
        // 4. Shadow Depth & micro-contrast
        // 5. Light Glow / Highlight Bloom
        // 6. Contour Embossing / Edge Emphasis
        coroutineContext.ensureActive()
        applyCreativePostProcessing(outputPixels, outWidth, outHeight, artStyle, styleParams)

        coroutineContext.ensureActive()
        val resultBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outputPixels, 0, outWidth, 0, 0, outWidth, outHeight)
        resultBitmap
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
        params: OrbitStyleParameters
    ): FloatArray {
        val weights = FloatArray(total)
        val abstractionNorm = (params.abstraction / 100f).coerceIn(0f, 1f)
        val stabilityNorm = (params.subjectStability / 100f).coerceIn(0f, 1f)

        if (total == 1) {
            weights[0] = 1f
            return weights
        }

        // Reference dominance: high stability anchors the subject; abstraction disperses
        val refBaseDominance = lerp(0.35f, 0.92f, stabilityNorm)
        val refDominance = lerp(refBaseDominance, 1f / total, abstractionNorm * 0.70f)
        val remainingWeight = 1f - refDominance

        var otherSum = 0f
        val rawOthers = FloatArray(total)
        val sigma = max(0.5f, (1f - abstractionNorm) * (total / 3f) + (stabilityNorm * 2.2f))

        for (i in 0 until total) {
            if (i == refIndex) continue
            val dist = abs(i - refIndex).toFloat()
            val falloff = exp(-(dist * dist) / (2f * sigma * sigma))
            val weightFactor = when (artStyle) {
                OrbitArtStyle.IN_THE_ROUND -> {
                    // Staggered rhythmic distribution around the orbit
                    val harmonic = 1.0f + 0.35f * cos((dist / total) * 4.0 * Math.PI).toFloat()
                    lerp(falloff, 1f, abstractionNorm) * harmonic
                }
                OrbitArtStyle.MOTION_FLOW -> {
                    val seqT = i.toFloat() / max(1, total - 1)
                    (0.35f + 0.65f * seqT) * lerp(falloff, 1f, abstractionNorm)
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

    // --- Style-Specific Accumulation Functions ---

    private fun accumulateInTheRound(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters
    ) {
        val centerX = w / 2f
        val centerY = h / 2f
        val maxRadius = hypot(centerX, centerY)
        val stability = (params.subjectStability / 100f)
        val dissolveStrength = (params.backgroundDissolve / 100f)
        val colorDensityMod = 1.0f + (params.colorDensity / 100f) * 0.45f

        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                // Radial distance from motif center
                val dist = hypot(x - centerX, y - centerY)
                val normRadius = (dist / maxRadius).coerceIn(0f, 1f)

                // Central motif stays anchored by reference; background perspectives dissolve
                val spatialWeight = if (isReference) {
                    weight * (1.2f + stability * (1f - normRadius) * 2.8f)
                } else {
                    weight * (0.8f + (1f - stability + dissolveStrength * 0.5f) * normRadius)
                }

                val color = pixels[srcIdx]
                var r = ((color shr 16) and 0xFF).toFloat()
                var g = ((color shr 8) and 0xFF).toFloat()
                var b = (color and 0xFF).toFloat()

                // Preserve vivid color saturation
                val mean = (r + g + b) / 3f
                r = (mean + (r - mean) * colorDensityMod).coerceIn(0f, 255f)
                g = (mean + (g - mean) * colorDensityMod).coerceIn(0f, 255f)
                b = (mean + (b - mean) * colorDensityMod).coerceIn(0f, 255f)

                accR[destIdx] += r * spatialWeight
                accG[destIdx] += g * spatialWeight
                accB[destIdx] += b * spatialWeight
                accW[destIdx] += spatialWeight
            }
        }
    }

    private fun accumulateImpressionist(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters
    ) {
        val colorDensityFactor = 1.0f + (params.colorDensity / 100f) * 0.4f
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

                // Painterly color density & warm golden hour boost
                val mean = (r + g + b) / 3f
                r = (mean + (r - mean) * colorDensityFactor).coerceIn(0f, 255f)
                g = (mean + (g - mean) * colorDensityFactor).coerceIn(0f, 255f)
                b = (mean + (b - mean) * colorDensityFactor).coerceIn(0f, 255f)

                val effectiveWeight = if (isReference) weight * 1.35f else weight
                accR[destIdx] += r * effectiveWeight
                accG[destIdx] += g * effectiveWeight
                accB[destIdx] += b * effectiveWeight
                accW[destIdx] += effectiveWeight
            }
        }
    }

    private fun accumulateArchitectural(
        pixels: IntArray,
        accR: FloatArray, accG: FloatArray, accB: FloatArray, accW: FloatArray,
        w: Int, h: Int, weight: Float, shiftX: Int, shiftY: Int,
        isReference: Boolean, params: OrbitStyleParameters
    ) {
        val edgeBoost = 1.0f + (params.edgeEmphasis / 100f) * 0.5f
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

                // Geometric structural microcontrast
                val lum = (0.299f * r + 0.587f * g + 0.114f * b)
                val contrastMod = if (lum < 128f) 0.85f else 1.15f * edgeBoost

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
        val highlightLift = 1.0f + (params.lightGlow / 100f) * 0.35f
        for (y in 0 until h) {
            val srcY = y - shiftY
            if (srcY !in 0 until h) continue
            for (x in 0 until w) {
                val srcX = x - shiftX
                if (srcX !in 0 until w) continue

                val srcIdx = srcY * w + srcX
                val destIdx = y * w + x

                val color = pixels[srcIdx]
                val r = ((color shr 16) and 0xFF).toFloat() * highlightLift
                val g = ((color shr 8) and 0xFF).toFloat() * highlightLift
                val b = (color and 0xFF).toFloat() * highlightLift

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
        val motionMult = 1.0f + (params.motionStrength / 100f) * 0.4f
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
        val density = 1.0f + (params.colorDensity / 100f) * 0.5f
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

    private fun accumulateEchoContours(
        pixels: IntArray,
        echoR: FloatArray, echoG: FloatArray, echoB: FloatArray, echoW: FloatArray,
        w: Int, h: Int, shiftX: Int, shiftY: Int, params: OrbitStyleParameters
    ) {
        // High frequency edge accumulator for perspective outlines
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

                // Simple gradient magnitude for contour edge
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

    // --- Comprehensive Creative Engine V0.3 Post-Processing ---

    private fun applyCreativePostProcessing(
        pixels: IntArray,
        w: Int,
        h: Int,
        artStyle: OrbitArtStyle,
        params: OrbitStyleParameters
    ) {
        val centerX = w / 2f
        val centerY = h / 2f
        val maxRadius = hypot(centerX, centerY)

        // 1. Color Intensity / Vibrance & Warmth (Prevent flat/gray multi-exposures!)
        val vibranceBoost = (params.colorIntensity / 100f) * 1.4f
        val warmFactor = (params.warmth - 50f) / 100f // -0.5 .. +0.5
        val shadowDepthMod = (params.shadowDepth / 100f) * 0.45f

        for (i in pixels.indices) {
            val c = pixels[i]
            var r = ((c shr 16) and 0xFF).toFloat()
            var g = ((c shr 8) and 0xFF).toFloat()
            var b = (c and 0xFF).toFloat()

            // Calculate saturation and boost lower saturated pixels more (Vibrance)
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val sat = if (maxC > 0f) (maxC - minC) / maxC else 0f
            val satMultiplier = 1.0f + (1.0f - sat) * vibranceBoost

            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            r = (lum + (r - lum) * satMultiplier).coerceIn(0f, 255f)
            g = (lum + (g - lum) * satMultiplier).coerceIn(0f, 255f)
            b = (lum + (b - lum) * satMultiplier).coerceIn(0f, 255f)

            // Warmth shift: subtle golden hour amber glow
            if (warmFactor != 0f) {
                r = (r * (1f + warmFactor * 0.24f)).coerceIn(0f, 255f)
                b = (b * (1f - warmFactor * 0.22f)).coerceIn(0f, 255f)
            }

            // Shadow Depth: S-curve to anchor blacks and prevent washed-out milky gray tones
            if (shadowDepthMod > 0f) {
                val normLum = lum / 255f
                val sCurve = (normLum * normLum * (3f - 2f * normLum))
                val curveFactor = lerp(1f, sCurve / max(0.01f, normLum), shadowDepthMod)
                r = (r * curveFactor).coerceIn(0f, 255f)
                g = (g * curveFactor).coerceIn(0f, 255f)
                b = (b * curveFactor).coerceIn(0f, 255f)
            }

            pixels[i] = (0xFF shl 24) or (r.roundToInt() shl 16) or (g.roundToInt() shl 8) or b.roundToInt()
        }

        // 2. Background Dissolve & Radial Softness
        if (params.backgroundDissolve > 20f || params.softness > 20f) {
            applyBackgroundDissolveAndGlow(pixels, w, h, centerX, centerY, maxRadius, params)
        }

        // 3. Konturprägung / Edge Emphasis (Sharp, musical perspective lines)
        if (params.edgeEmphasis > 25f) {
            applyEdgeEmphasis(pixels, w, h, (params.edgeEmphasis - 20f) / 80f)
        }
    }

    private fun applyBackgroundDissolveAndGlow(
        pixels: IntArray,
        w: Int,
        h: Int,
        cx: Float,
        cy: Float,
        maxRadius: Float,
        params: OrbitStyleParameters
    ) {
        val copy = pixels.clone()
        val dissolveStrength = (params.backgroundDissolve / 100f)
        val stability = (params.subjectStability / 100f)
        val coreRadius = maxRadius * 0.32f * (stability + 0.3f)
        val glowStrength = (params.lightGlow / 100f) * 0.4f

        for (y in 1 until h - 1) {
            val yOffset = y * w
            for (x in 1 until w - 1) {
                val idx = yOffset + x

                val dist = hypot(x - cx, y - cy)
                val dissolveFactor = if (dist > coreRadius) {
                    ((dist - coreRadius) / (maxRadius - coreRadius)).coerceIn(0f, 1f) * dissolveStrength
                } else 0f

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

                // Blend background with soft dissolve
                var rFinal = lerp(rOrig, rAvg, dissolveFactor * 0.7f)
                var gFinal = lerp(gOrig, gAvg, dissolveFactor * 0.7f)
                var bFinal = lerp(bOrig, bAvg, dissolveFactor * 0.7f)

                // Light Glow on bright highlights
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
