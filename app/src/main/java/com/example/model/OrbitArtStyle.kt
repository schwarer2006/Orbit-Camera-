package com.example.model

data class OrbitStyleParameters(
    val abstraction: Float = 50f,        // 0..100
    val subjectStability: Float = 60f,   // 0..100
    val blendStrength: Float = 70f,      // 0..100
    val edgeEmphasis: Float = 40f,       // 0..100
    val colorDensity: Float = 50f,       // 0..100
    val softness: Float = 30f,           // 0..100
    val motionStrength: Float = 30f      // 0..100
)

enum class OrbitArtStyle(
    val title: String,
    val subtitle: String,
    val description: String,
    val defaultParameters: OrbitStyleParameters
) {
    IMPRESSIONIST(
        title = "Impressionist",
        subtitle = "Malerisch & Weich",
        description = "Malerische Mehrfachbelichtung mit sanfter Konturvervielfachung und Farbverdichtung.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 45f,
            subjectStability = 70f,
            blendStrength = 65f,
            edgeEmphasis = 30f,
            colorDensity = 60f,
            softness = 55f,
            motionStrength = 20f
        )
    ),
    IN_THE_ROUND(
        title = "In the Round",
        subtitle = "Motivzentrierte Fusion",
        description = "Motivzentrierte Synthese aller Blickwinkel; das Hauptmotiv bleibt stabil und erkennbar.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 50f,
            subjectStability = 85f,
            blendStrength = 75f,
            edgeEmphasis = 50f,
            colorDensity = 45f,
            softness = 20f,
            motionStrength = 35f
        )
    ),
    ARCHITECTURAL(
        title = "Architektur",
        subtitle = "Grafische Linien & Struktur",
        description = "Grafische Verdichtung von Gebäuden mit betonten Kanten und geometrischer Strukturüberlagerung.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 35f,
            subjectStability = 75f,
            blendStrength = 80f,
            edgeEmphasis = 90f,
            colorDensity = 70f,
            softness = 5f,
            motionStrength = 15f
        )
    ),
    ETHEREAL(
        title = "Atmosphärisch",
        subtitle = "Träumerische Lichtschleier",
        description = "Sanfte Transparenzen, weiche Tonwertübergänge und träumerisch aufgelöste Mikrokontraste.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 65f,
            subjectStability = 50f,
            blendStrength = 85f,
            edgeEmphasis = 15f,
            colorDensity = 30f,
            softness = 85f,
            motionStrength = 40f
        )
    ),
    MOTION_FLOW(
        title = "Bewegungsfluss",
        subtitle = "Kinetische Dynamik",
        description = "Rhythmische Konturwiederholung und gerichtete Dynamik entlang der Aufnahmereihenfolge.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 70f,
            subjectStability = 35f,
            blendStrength = 80f,
            edgeEmphasis = 45f,
            colorDensity = 50f,
            softness = 30f,
            motionStrength = 90f
        )
    ),
    URBAN_LAYERS(
        title = "Urbane Schichten",
        subtitle = "Vielschichtige Straßenszenen",
        description = "Dichte Überlagerung urbaner Perspektiven mit kontrastreichen Details und lebendigem Rhythmus.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 60f,
            subjectStability = 60f,
            blendStrength = 75f,
            edgeEmphasis = 75f,
            colorDensity = 80f,
            softness = 15f,
            motionStrength = 50f
        )
    )
}
