package com.example.model

data class OrbitStyleParameters(
    // 5 Sichtbare Hauptregler
    val abstraction: Float = 50f,          // 0..100: Abstraktion
    val subjectStability: Float = 70f,     // 0..100: Motivstabilität
    val colorIntensity: Float = 75f,       // 0..100: Farbintensität / Leuchtkraft
    val edgeEmphasis: Float = 60f,         // 0..100: Konturprägung
    val backgroundDissolve: Float = 55f,   // 0..100: Hintergrundauflösung

    // Erweiterte kreative Parameter
    val echoStrength: Float = 65f,         // 0..100: Stärke der Mehrfachkonturen / Echos
    val echoCount: Float = 7f,             // 3..12: Anzahl sichtbarer Perspektiv-Staffelungen
    val perspectiveSpread: Float = 50f,    // 0..100: Räumlicher Versatz der Perspektiven
    val colorDensity: Float = 60f,         // 0..100: Farbdichte & Tonwertfülle
    val warmth: Float = 45f,               // 0..100: Warme Farbstimmung (+Gold / -Kälte)
    val lightGlow: Float = 30f,            // 0..100: Glanz & Leuchtschleier
    val shadowDepth: Float = 50f,          // 0..100: Schattentiefe & Mikrokontrast
    val softness: Float = 25f,             // 0..100: Malerische Weichheit
    val motionStrength: Float = 35f,       // 0..100: Bewegungsdynamik
    val detailDensity: Float = 60f,        // 0..100: Detail- & Texturdichte
    val atmosphere: Float = 45f,           // 0..100: Atmosphärische Schleierwirkung
    val blendStrength: Float = 70f         // 0..100: Technische Überlagerungsstärke
)

enum class OrbitArtStyle(
    val title: String,
    val subtitle: String,
    val description: String,
    val defaultParameters: OrbitStyleParameters
) {
    IMPRESSIONIST(
        title = "Impressionist",
        subtitle = "Malerisch & Farbdicht",
        description = "Malerische Mehrfachbelichtung mit poetisch weicher Konturvervielfachung, warmer Farbdichte und aufgelöstem Hintergrund.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 45f,
            subjectStability = 70f,
            colorIntensity = 80f,
            edgeEmphasis = 45f,
            backgroundDissolve = 75f,
            echoStrength = 55f,
            echoCount = 6f,
            perspectiveSpread = 35f,
            colorDensity = 70f,
            warmth = 60f,
            lightGlow = 45f,
            shadowDepth = 35f,
            softness = 55f,
            motionStrength = 25f,
            detailDensity = 45f,
            atmosphere = 65f,
            blendStrength = 70f
        )
    ),
    IN_THE_ROUND(
        title = "In the Round",
        subtitle = "Pepa Ventosa / ORBIT-Look",
        description = "Stärkster ORBIT-Charakter: Hauptmotiv als klares Zentrum, stark sichtbare Mehrfachkonturen, lebendige Farben und diffuser Hintergrund.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 50f,
            subjectStability = 85f,
            colorIntensity = 90f,
            edgeEmphasis = 85f,
            backgroundDissolve = 80f,
            echoStrength = 90f,
            echoCount = 8f,
            perspectiveSpread = 65f,
            colorDensity = 70f,
            warmth = 52f,
            lightGlow = 35f,
            shadowDepth = 65f,
            softness = 15f,
            motionStrength = 45f,
            detailDensity = 80f,
            atmosphere = 40f,
            blendStrength = 80f
        )
    ),
    ARCHITECTURAL(
        title = "Architektur",
        subtitle = "Grafische Kanten & Struktur",
        description = "Starke geometrische Verdichtung von Baukörpern mit prägnant gestaffelten Linien und hohem Strukturkontrast.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 40f,
            subjectStability = 80f,
            colorIntensity = 65f,
            edgeEmphasis = 95f,
            backgroundDissolve = 35f,
            echoStrength = 75f,
            echoCount = 5f,
            perspectiveSpread = 50f,
            colorDensity = 80f,
            warmth = 30f,
            lightGlow = 10f,
            shadowDepth = 85f,
            softness = 0f,
            motionStrength = 20f,
            detailDensity = 95f,
            atmosphere = 20f,
            blendStrength = 80f
        )
    ),
    ETHEREAL(
        title = "Atmosphärisch",
        subtitle = "Träumerische Lichtschleier",
        description = "Schwebende Mehrfachbelichtung mit sanften Transparenzen, zartem Highlight-Glow und poetischer Farbharmonie.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 70f,
            subjectStability = 45f,
            colorIntensity = 70f,
            edgeEmphasis = 25f,
            backgroundDissolve = 85f,
            echoStrength = 40f,
            echoCount = 9f,
            perspectiveSpread = 55f,
            colorDensity = 40f,
            warmth = 40f,
            lightGlow = 80f,
            shadowDepth = 25f,
            softness = 80f,
            motionStrength = 35f,
            detailDensity = 30f,
            atmosphere = 85f,
            blendStrength = 85f
        )
    ),
    MOTION_FLOW(
        title = "Bewegungsfluss",
        subtitle = "Kinetische Dynamik",
        description = "Rhythmische Bewegungswiederholung und gerichtete Echoformen entlang der chronologischen Aufnahmeserie.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 65f,
            subjectStability = 40f,
            colorIntensity = 80f,
            edgeEmphasis = 70f,
            backgroundDissolve = 55f,
            echoStrength = 85f,
            echoCount = 10f,
            perspectiveSpread = 80f,
            colorDensity = 60f,
            warmth = 45f,
            lightGlow = 40f,
            shadowDepth = 55f,
            softness = 25f,
            motionStrength = 95f,
            detailDensity = 65f,
            atmosphere = 50f,
            blendStrength = 80f
        )
    ),
    URBAN_LAYERS(
        title = "Urbane Schichten",
        subtitle = "Vielschichtige Straßenszenen",
        description = "Dichte Stadtüberlagerung mit rhythmischen Mehrfachkonturen, tiefen Schatten und ausdrucksstarker Straßenfarbigkeit.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 55f,
            subjectStability = 65f,
            colorIntensity = 85f,
            edgeEmphasis = 85f,
            backgroundDissolve = 45f,
            echoStrength = 80f,
            echoCount = 7f,
            perspectiveSpread = 60f,
            colorDensity = 85f,
            warmth = 45f,
            lightGlow = 25f,
            shadowDepth = 80f,
            softness = 10f,
            motionStrength = 40f,
            detailDensity = 85f,
            atmosphere = 50f,
            blendStrength = 75f
        )
    )
}
