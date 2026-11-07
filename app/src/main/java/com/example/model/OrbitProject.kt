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
        description = "Harmonische Mehrfachbelichtung mit dynamischem Referenzfokus"
    ),
    LIGHTEN_ADDITIVE(
        title = "Lichtspuren",
        description = "Betont Glanzlichter und bewegte Lichtkanten"
    ),
    MULTIPLY_DENSITY(
        title = "Dichte & Tiefe",
        description = "Verdichtet Texturen und grafische Konturen"
    ),
    DRIFT_FLOW(
        title = "Fließende Dynamik",
        description = "Verstärkt die Bewegungsrichtung der Bildserie"
    )
}

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
    val workspaceMode: OrbitWorkspaceMode = OrbitWorkspaceMode.SIMPLE
) {
    val actualCount: Int
        get() = imagePaths.size

    val isCompleted: Boolean
        get() = actualCount >= targetCount

    val progress: Float
        get() = if (targetCount > 0) (actualCount.toFloat() / targetCount).coerceIn(0f, 1f) else 0f
}
