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
        assertEquals("LAST", project.ghostReferenceMode)
        assertFalse(project.isIntervalMode)
        assertEquals(3, project.intervalSeconds)
    }
}
