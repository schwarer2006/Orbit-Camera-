package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun orbitProject_v04Defaults_areValid() {
        val project = com.example.model.OrbitProject(
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
        val squareDim = com.example.model.OrbitExportRatio.SQUARE_1_1.calculateDimensions(2048)
        assertEquals(2048, squareDim.first)
        assertEquals(2048, squareDim.second)

        val portraitDim = com.example.model.OrbitExportRatio.PORTRAIT_4_5.calculateDimensions(2048)
        assertEquals(1638, portraitDim.first)
        assertEquals(2048, portraitDim.second)

        val landscapeDim = com.example.model.OrbitExportRatio.LANDSCAPE_16_9.calculateDimensions(2048)
        assertEquals(2048, landscapeDim.first)
        assertEquals(1152, landscapeDim.second)

        // 3072px check
        val portrait3072 = com.example.model.OrbitExportRatio.PORTRAIT_4_5.calculateDimensions(3072)
        assertEquals(2458, portrait3072.first)
        assertEquals(3072, portrait3072.second)
    }

    @Test
    fun orbitProject_v08Defaults_areValid() {
        val project = com.example.model.OrbitProject(
            id = "test-v08",
            name = "Forest Canopy"
        )
        assertEquals(com.example.model.OrbitCompositionStrategy.SUBJECT_CENTERED, project.compositionStrategy)
        assertEquals(com.example.model.OrbitWorkspaceMode.SIMPLE, project.workspaceMode)
        // Defaults from OrbitArtStyle.IN_THE_ROUND
        assertEquals(85f, project.styleParams.vibrance, 0.001f)
        assertEquals(65f, project.styleParams.contrast, 0.001f)
        assertEquals(15f, project.styleParams.warmth, 0.001f)

        // Standalone defaults of OrbitStyleParameters
        val rawParams = com.example.model.OrbitStyleParameters()
        assertEquals(65f, rawParams.vibrance, 0.001f)
        assertEquals(55f, rawParams.contrast, 0.001f)
        assertEquals(0f, rawParams.warmth, 0.001f)
    }

    @Test
    fun orbitArtStyle_v08Parameters_areDifferentiated() {
        val impressionist = com.example.model.OrbitArtStyle.IMPRESSIONIST.defaultParameters
        val architectural = com.example.model.OrbitArtStyle.ARCHITECTURAL.defaultParameters
        val ethereal = com.example.model.OrbitArtStyle.ETHEREAL.defaultParameters

        // Impressionist should have warm tone and rich vibrance
        assertTrue(impressionist.warmth > 20f)
        assertTrue(impressionist.vibrance >= 60f)

        // Architectural should have higher contrast and cooler tone
        assertTrue(architectural.contrast > impressionist.contrast)
        assertTrue(architectural.warmth < impressionist.warmth)

        // Ethereal should have prominent glow and softer contrast
        assertTrue(ethereal.lightGlow > 60f)
        assertTrue(ethereal.contrast <= 50f)
    }
}
