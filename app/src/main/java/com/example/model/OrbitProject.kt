package com.example.model

import java.util.UUID

enum class OrbitCaptureMode(
    val title: String,
    val description: String,
    val guideHint: String
) {
    STATIONARY(
        title = "Stationär",
        description = "Fester Standpunkt, leichte Variationen von Bildwinkel & Ausschnitt",
        guideHint = "Halte den Standort und variiere leicht Blickwinkel oder Brennpunkt."
    ),
    LINEAR(
        title = "Linear",
        description = "Gleichmäßige Bewegung entlang oder seitlich des Motivs",
        guideHint = "Bewege dich seitlich in gleichmäßigen Schritten entlang des Objekts."
    ),
    ORBIT(
        title = "Orbit",
        description = "Bogenförmige oder kreisförmige Umrundung des Motivs",
        guideHint = "Umrunde das Motiv auf einer Kreisbahn und halte den Motivanker zentriert."
    )
}

enum class OrbitBlendMode(
    val title: String,
    val description: String
) {
    WEIGHTED_AVERAGE(
        title = "Ausgewogen",
        description = "Harmonische Mittelung mit dynamischem Referenzfokus"
    ),
    SEQUENTIAL_LAYERS(
        title = "Sequenzielle Ebenen",
        description = "Schrittweise Alpha-Überlagerung mit individueller Ebenenreihenfolge und Deckkraft"
    ),
    LIGHTEN(
        title = "Lichtspuren (Lighten)",
        description = "Bevorzugt hellere Tonwerte und Lichtstrukturen"
    ),
    MULTIPLY(
        title = "Dichte (Multiply)",
        description = "Multiplikative Verdichtung dunkler Texturen und Linien"
    ),
    SCREEN(
        title = "Leuchten (Screen)",
        description = "Aufhellende Überlagerung mit kontrollierter Tonwertverteilung"
    ),
    DRIFT_FLOW(
        title = "Fließende Dynamik",
        description = "Verstärkt die Bewegungsrichtung der Bildserie"
    ),
    LIGHTEN_ADDITIVE(
        title = "Lichtspuren",
        description = "Betont Glanzlichter und bewegte Lichtkanten"
    ),
    MULTIPLY_DENSITY(
        title = "Dichte & Tiefe",
        description = "Verdichtet Texturen und grafische Konturen"
    );

    val label: String
        get() = title

    companion object {
        fun fromString(value: String?): OrbitBlendMode {
            return when (value) {
                "SEQUENTIAL", "SEQUENTIAL_LAYERS" -> SEQUENTIAL_LAYERS
                "LIGHTEN", "LIGHTEN_ADDITIVE" -> LIGHTEN
                "MULTIPLY", "MULTIPLY_DENSITY" -> MULTIPLY
                "SCREEN" -> SCREEN
                "DRIFT_FLOW" -> DRIFT_FLOW
                else -> WEIGHTED_AVERAGE
            }
        }
    }
}

data class OrbitVariant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val artStyle: OrbitArtStyle = OrbitArtStyle.IN_THE_ROUND,
    val compositionStrategy: OrbitCompositionStrategy = OrbitCompositionStrategy.SUBJECT_CENTERED,
    val blendMode: OrbitBlendMode = OrbitBlendMode.WEIGHTED_AVERAGE,
    val styleParams: OrbitStyleParameters = OrbitArtStyle.IN_THE_ROUND.defaultParameters,
    val referenceImageIndex: Int = 0,
    val disabledImagePaths: Set<String> = emptySet(),
    val customImageOrder: List<String> = emptyList(),
    val imageWeights: Map<String, Float> = emptyMap(),
    val layerOpacities: Map<String, Float> = emptyMap(),
    val exposureMatchEnabled: Boolean = false,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f,
    val artisticSpreadEnabled: Boolean = false,
    val maskEnabled: Boolean = false,
    val maskRadius: Float = 0.35f,
    val maskFeather: Float = 0.25f,
    val maskIntensity: Float = 0.70f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class OrbitProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val mode: OrbitCaptureMode = OrbitCaptureMode.ORBIT,
    val targetCount: Int = 50,
    val imagePaths: List<String> = emptyList(),
    val referenceImageIndex: Int = 0,
    val abstractionLevel: Float = 50f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val blendMode: OrbitBlendMode = OrbitBlendMode.WEIGHTED_AVERAGE,
    val artStyle: OrbitArtStyle = OrbitArtStyle.IN_THE_ROUND,
    val styleParams: OrbitStyleParameters = OrbitArtStyle.IN_THE_ROUND.defaultParameters,
    val lastRenderedPath: String? = null,
    // V0.4 Guided Capture & Stability fields
    val ghostOverlayEnabled: Boolean = false,
    val ghostOverlayAlpha: Float = 0.45f,
    val ghostReferenceMode: String = "PREVIOUS", // "PREVIOUS" oder "FIRST"
    val isIntervalMode: Boolean = false,
    val intervalSeconds: Int = 3, // 2, 3, 5, 10
    // V0.8 Creative Engine fields
    val compositionStrategy: OrbitCompositionStrategy = OrbitCompositionStrategy.SUBJECT_CENTERED,
    val workspaceMode: OrbitWorkspaceMode = OrbitWorkspaceMode.SIMPLE,
    // V0.9.0 Authentic Multi-Perspective fields
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f,
    val artisticSpreadEnabled: Boolean = false,
    val disabledImagePaths: Set<String> = emptySet(),
    val maskEnabled: Boolean = false,
    val maskRadius: Float = 0.35f,
    val maskFeather: Float = 0.25f,
    val maskIntensity: Float = 0.70f,
    // V1.1 Creative Fusion Expansion fields
    val customImageOrder: List<String> = emptyList(),
    val imageWeights: Map<String, Float> = emptyMap(), // path -> weight (0.0 .. 1.0, default 1.0)
    val layerOpacities: Map<String, Float> = emptyMap(), // path -> opacity (0.0 .. 1.0, default 1.0)
    val exposureMatchEnabled: Boolean = false,
    val variants: List<OrbitVariant> = emptyList(),
    val activeVariantId: String? = null,
    val layerMaskPaths: Map<String, String> = emptyMap() // Preparation for selective layer masks
) {
    val actualCount: Int
        get() = imagePaths.size

    val activeCount: Int
        get() = imagePaths.count { it !in disabledImagePaths }

    val activeImagePaths: List<String>
        get() = imagePaths.filter { it !in disabledImagePaths }

    val orderedImagePaths: List<String>
        get() {
            if (customImageOrder.isEmpty()) return imagePaths
            val validCustom = customImageOrder.filter { it in imagePaths }
            val missing = imagePaths.filter { it !in validCustom }
            return validCustom + missing
        }

    val orderedActiveImagePaths: List<String>
        get() = orderedImagePaths.filter { it !in disabledImagePaths }

    val isCompleted: Boolean
        get() = actualCount >= targetCount

    val progress: Float
        get() = if (targetCount > 0) (actualCount.toFloat() / targetCount).coerceIn(0f, 1f) else 0f
}
