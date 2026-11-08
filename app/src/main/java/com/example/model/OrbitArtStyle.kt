package com.example.model

data class OrbitStyleParameters(
    // Hauptparameter
    val abstraction: Float = 50f,          // 0..100: Abstraktion
    val subjectStability: Float = 70f,     // 0..100: Motivstabilität
    val colorIntensity: Float = 75f,       // 0..100: Gesamte Farbintensität
    val vibrance: Float = 65f,             // 0..100: Selektive Farbleuchtkraft (Vibrance)
    val contrast: Float = 55f,             // 0..100: Tonwertkontrast / S-Kurve
    val warmth: Float = 0f,                // -100..+100: Farbtemperatur (-Kühl / +Warm)
    val edgeEmphasis: Float = 60f,         // 0..100: Konturschärfung
    val backgroundDissolve: Float = 55f,   // 0..100: Hintergrundauflösung / Weichheit

    // Erweiterte kreative Parameter
    val echoStrength: Float = 65f,         // 0..100: Stärke der Mehrfachkonturen / Echos
    val echoCount: Float = 7f,             // 3..12: Anzahl sichtbarer Perspektiv-Staffelungen
    val perspectiveSpread: Float = 50f,    // 0..100: Räumlicher Versatz der Perspektiven
    val colorDensity: Float = 65f,         // 0..100: Farbdichte & Tonwertfülle
    val lightGlow: Float = 30f,            // 0..100: Glanz & Leuchtschleier
    val shadowDepth: Float = 50f,          // 0..100: Schattentiefe & Mikrokontrast
    val softness: Float = 25f,             // 0..100: Malerische Weichheit
    val motionStrength: Float = 35f,       // 0..100: Bewegungsdynamik
    val detailDensity: Float = 60f,        // 0..100: Detail- & Texturdichte
    val atmosphere: Float = 45f,           // 0..100: Atmosphärische Schleierwirkung
    val blendStrength: Float = 70f,        // 0..100: Technische Überlagerungsstärke

    // V0.9.0 Authentic Multi-Perspective Parameter
    val artisticSpreadEnabled: Boolean = false, // false = Natural Perspective, true = künstlicher Spread
    val maskEnabled: Boolean = false,           // Selektive Motivgewichtung (weichkantige Bereichsmaske)
    val maskRadius: Float = 0.35f,              // 0.1..0.8: Relativer Radius der Motivmaske
    val maskFeather: Float = 0.25f,             // 0.05..0.6: Weichheit des Maskenübergangs
    val maskIntensity: Float = 0.70f            // 0..1: Gewichtungsstärke der Bereichsmaske
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
        description = "Malerische Mehrfachbelichtung mit organischer Konturvervielfachung, hoher Farbharmonie, leuchtender Vibrance und weich verschmolzenen Flächen.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 45f,
            subjectStability = 65f,
            colorIntensity = 85f,
            vibrance = 85f,
            contrast = 50f,
            warmth = 25f,
            colorDensity = 80f,
            edgeEmphasis = 40f,
            backgroundDissolve = 75f,
            echoStrength = 55f,
            echoCount = 6f,
            perspectiveSpread = 40f,
            lightGlow = 40f,
            shadowDepth = 40f,
            softness = 55f,
            motionStrength = 20f,
            detailDensity = 50f,
            atmosphere = 65f,
            blendStrength = 75f
        )
    ),
    IN_THE_ROUND(
        title = "In the Round",
        subtitle = "Pepa Ventosa / ORBIT-Look",
        description = "Stärkster ORBIT-Charakter: Ausgeprägte Mehrfachkonturen, rhythmische Echo-Staffelung, hohe Motivstabilität und leuchtend ausdrucksstarke Farben.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 50f,
            subjectStability = 85f,
            colorIntensity = 90f,
            vibrance = 85f,
            contrast = 65f,
            warmth = 15f,
            colorDensity = 80f,
            edgeEmphasis = 85f,
            backgroundDissolve = 75f,
            echoStrength = 90f,
            echoCount = 8f,
            perspectiveSpread = 65f,
            lightGlow = 35f,
            shadowDepth = 65f,
            softness = 15f,
            motionStrength = 40f,
            detailDensity = 80f,
            atmosphere = 40f,
            blendStrength = 80f
        )
    ),
    ARCHITECTURAL(
        title = "Architektur",
        subtitle = "Grafische Kanten & Struktur",
        description = "Prägnante geometrische Verdichtung mit scharfen Linien, kühler Tonalität, starkem Tonwertkontrast und kontrollierter Strukturüberlagerung.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 35f,
            subjectStability = 80f,
            colorIntensity = 70f,
            vibrance = 60f,
            contrast = 80f,
            warmth = -15f,
            colorDensity = 85f,
            edgeEmphasis = 95f,
            backgroundDissolve = 30f,
            echoStrength = 75f,
            echoCount = 5f,
            perspectiveSpread = 45f,
            lightGlow = 10f,
            shadowDepth = 85f,
            softness = 0f,
            motionStrength = 15f,
            detailDensity = 95f,
            atmosphere = 20f,
            blendStrength = 80f
        )
    ),
    ETHEREAL(
        title = "Atmosphärisch",
        subtitle = "Träumerische Lichtschleier",
        description = "Sanfte, schwebende Bildwirkung mit harmonischen Farbverläufen, dezentem Highlight-Glow und transluzenter Mehrfachbelichtung.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 70f,
            subjectStability = 45f,
            colorIntensity = 65f,
            vibrance = 70f,
            contrast = 35f,
            warmth = 10f,
            colorDensity = 45f,
            edgeEmphasis = 25f,
            backgroundDissolve = 85f,
            echoStrength = 35f,
            echoCount = 9f,
            perspectiveSpread = 55f,
            lightGlow = 85f,
            shadowDepth = 25f,
            softness = 80f,
            motionStrength = 30f,
            detailDensity = 30f,
            atmosphere = 90f,
            blendStrength = 85f
        )
    ),
    MOTION_FLOW(
        title = "Bewegungsfluss",
        subtitle = "Kinetische Dynamik",
        description = "Gerichtete Bewegungsüberlagerung entlang der Aufnahmereihenfolge mit dynamischem Rhythmus und fließenden Konturspuren.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 60f,
            subjectStability = 45f,
            colorIntensity = 80f,
            vibrance = 75f,
            contrast = 60f,
            warmth = 5f,
            colorDensity = 65f,
            edgeEmphasis = 70f,
            backgroundDissolve = 55f,
            echoStrength = 85f,
            echoCount = 10f,
            perspectiveSpread = 85f,
            lightGlow = 35f,
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
        description = "Dichte urbane Ebenen mit hohem Kontrast, markanten Schatten und expressiver, differenzierter Farbigkeit.",
        defaultParameters = OrbitStyleParameters(
            abstraction = 55f,
            subjectStability = 65f,
            colorIntensity = 85f,
            vibrance = 80f,
            contrast = 75f,
            warmth = 10f,
            colorDensity = 85f,
            edgeEmphasis = 85f,
            backgroundDissolve = 45f,
            echoStrength = 80f,
            echoCount = 7f,
            perspectiveSpread = 60f,
            lightGlow = 25f,
            shadowDepth = 80f,
            softness = 10f,
            motionStrength = 40f,
            detailDensity = 85f,
            atmosphere = 50f,
            blendStrength = 75f
        )
    );

    val defaultStrategy: OrbitCompositionStrategy
        get() = when (this) {
            ARCHITECTURAL, URBAN_LAYERS -> OrbitCompositionStrategy.STRUCTURAL_FIELD
            else -> OrbitCompositionStrategy.SUBJECT_CENTERED
        }
}
