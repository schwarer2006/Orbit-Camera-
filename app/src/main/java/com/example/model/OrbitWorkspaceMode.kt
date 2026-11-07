package com.example.model

enum class OrbitWorkspaceMode(
    val title: String,
    val description: String
) {
    SIMPLE("Einfach", "Wesentliche Kernregler für schnelle Ergebnisse"),
    CREATIVE("Kreativ", "Erweiterte Farb- und Kontursteuerung"),
    EXPERIMENTAL("Experimentell", "Volle Kontrolle über alle Bildparameter")
}
