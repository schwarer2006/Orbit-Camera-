package com.example.model

enum class OrbitCompositionStrategy(
    val title: String,
    val subtitle: String,
    val guideHint: String
) {
    SUBJECT_CENTERED(
        title = "Motivzentriert",
        subtitle = "Zentraler Anker & Umfeld-Auflösung",
        guideHint = "Ideal für Einzelelemente, Skulpturen, Brunnen und freistehende Bäume. Hält das Hauptmotiv stabil und erzeugt perspektivische Konturen im Umfeld."
    ),
    STRUCTURAL_FIELD(
        title = "Flächenorientiert",
        subtitle = "Rhythmisches Struktur- & Texturfeld",
        guideHint = "Ideal für Wälder, Baumkronen, Fassaden und Landschaftsmuster. Behandelt alle Bildbereiche gleichwertig ohne erzwungenes Zentrum."
    )
}
