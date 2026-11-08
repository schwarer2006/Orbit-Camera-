package com.example

import com.example.engine.RegistrationConfidence
import com.example.engine.RegistrationReport
import com.example.model.OrbitBlendMode
import com.example.model.OrbitCompositionStrategy
import com.example.model.OrbitExportRatio
import com.example.model.OrbitProject
import com.example.model.OrbitStyleParameters
import com.example.model.OrbitWorkspaceMode
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying ORBIT core logic, models, blend modes, and V0.9.0 data structures.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun orbitProject_v04Defaults_areValid() {
        val project = OrbitProject(
            id = "test-1",
            name = "Test Series"
        )
        assertFalse(project.ghostOverlayEnabled)
        assertEquals(0.45f, project.ghostOverlayAlpha, 0.001f)
        assertEquals("PREVIOUS", project.ghostReferenceMode)
        assertFalse(project.isIntervalMode)
        assertEquals(3, project.intervalSeconds)
    }

    @Test
    fun orbitExportConfig_aspectRatioCalculations_areAccurate() {
        val squareDim = OrbitExportRatio.SQUARE_1_1.calculateDimensions(2048)
        assertEquals(2048, squareDim.first)
        assertEquals(2048, squareDim.second)

        val portraitDim = OrbitExportRatio.PORTRAIT_4_5.calculateDimensions(2048)
        assertEquals(1638, portraitDim.first)
        assertEquals(2048, portraitDim.second)

        val landscapeDim = OrbitExportRatio.LANDSCAPE_16_9.calculateDimensions(2048)
        assertEquals(2048, landscapeDim.first)
        assertEquals(1152, landscapeDim.second)

        // 3072px check
        val portrait3072 = OrbitExportRatio.PORTRAIT_4_5.calculateDimensions(3072)
        assertEquals(2458, portrait3072.first)
        assertEquals(3072, portrait3072.second)
    }

    @Test
    fun orbitProject_v08Defaults_areValid() {
        val project = OrbitProject(
            id = "test-v08",
            name = "Forest Canopy"
        )
        assertEquals(OrbitCompositionStrategy.SUBJECT_CENTERED, project.compositionStrategy)
        assertEquals(OrbitWorkspaceMode.SIMPLE, project.workspaceMode)
        // Defaults from OrbitArtStyle.IN_THE_ROUND
        assertEquals(85f, project.styleParams.vibrance, 0.001f)
        assertEquals(65f, project.styleParams.contrast, 0.001f)
        assertEquals(15f, project.styleParams.warmth, 0.001f)

        // Standalone defaults of OrbitStyleParameters
        val rawParams = OrbitStyleParameters()
        assertEquals(65f, rawParams.vibrance, 0.001f)
        assertEquals(55f, rawParams.contrast, 0.001f)
        assertEquals(0f, rawParams.warmth, 0.001f)
    }

    @Test
    fun orbitProject_v09Defaults_areValid() {
        val project = OrbitProject(
            id = "test-v09",
            name = "In the Round Series",
            imagePaths = listOf("/tmp/img1.jpg", "/tmp/img2.jpg", "/tmp/img3.jpg")
        )

        // V0.9.0 Anchor positioning
        assertEquals(0.5f, project.anchorX, 0.001f)
        assertEquals(0.5f, project.anchorY, 0.001f)

        // V0.9.0 Natural perspective vs artistic spread
        assertFalse(project.artisticSpreadEnabled)

        // V0.9.0 Intelligent image selection
        assertTrue(project.disabledImagePaths.isEmpty())
        assertEquals(3, project.activeCount)
        assertEquals(listOf("/tmp/img1.jpg", "/tmp/img2.jpg", "/tmp/img3.jpg"), project.activeImagePaths)

        // When an image is disabled
        val withDisabled = project.copy(disabledImagePaths = setOf("/tmp/img2.jpg"))
        assertEquals(2, withDisabled.activeCount)
        assertEquals(listOf("/tmp/img1.jpg", "/tmp/img3.jpg"), withDisabled.activeImagePaths)

        // V0.9.0 Selective subject weighting (mask)
        assertFalse(project.maskEnabled)
        assertEquals(45f, project.maskRadius, 0.001f)
        assertEquals(30f, project.maskFeather, 0.001f)
        assertEquals(60f, project.maskIntensity, 0.001f)
    }

    @Test
    fun orbitBlendMode_parsingAndFallback_works() {
        assertEquals(OrbitBlendMode.LIGHTEN, OrbitBlendMode.fromString("LIGHTEN"))
        assertEquals(OrbitBlendMode.LIGHTEN, OrbitBlendMode.fromString("LIGHTEN_ADDITIVE"))
        assertEquals(OrbitBlendMode.MULTIPLY, OrbitBlendMode.fromString("MULTIPLY"))
        assertEquals(OrbitBlendMode.MULTIPLY, OrbitBlendMode.fromString("MULTIPLY_DENSITY"))
        assertEquals(OrbitBlendMode.SCREEN, OrbitBlendMode.fromString("SCREEN"))
        assertEquals(OrbitBlendMode.DRIFT_FLOW, OrbitBlendMode.fromString("DRIFT_FLOW"))
        assertEquals(OrbitBlendMode.WEIGHTED_AVERAGE, OrbitBlendMode.fromString("WEIGHTED_AVERAGE"))
        assertEquals(OrbitBlendMode.WEIGHTED_AVERAGE, OrbitBlendMode.fromString("UNKNOWN_MODE"))
    }

    @Test
    fun orbitRegistration_reportAggregation_isAccurate() {
        val confidences = mapOf(
            0 to RegistrationConfidence(0, 0f, 0f, 1.0f, true),
            1 to RegistrationConfidence(1, 2.5f, -1.2f, 0.82f, true),
            2 to RegistrationConfidence(2, 0.5f, 0.5f, 0.22f, false)
        )
        val successful = confidences.values.count { it.isReliable }
        val lowConf = confidences.values.count { !it.isReliable }
        val avgConf = confidences.values.map { it.confidence }.average().toFloat()

        val report = RegistrationReport(
            totalImages = 3,
            successfulCount = successful,
            lowConfidenceCount = lowConf,
            confidences = confidences,
            averageConfidence = avgConf
        )

        assertEquals(3, report.totalImages)
        assertEquals(2, report.successfulCount)
        assertEquals(1, report.lowConfidenceCount)
        assertEquals(0.68f, report.averageConfidence, 0.01f)
    }
}
