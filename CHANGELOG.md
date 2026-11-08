# Changelog – ORBIT Camera

All notable changes to this project will be documented in this file.

## [0.9.0] - 2026-11-07

### Added
- **Echte 2D-Bildregistrierung**: Gradienten-basierte Kantenkorrelation (Sobel-Edge Feature Matching) mit statistischer Qualitätsbewertung (Confidence Score) und Erkennung unzureichender Übereinstimmungen.
- **Frei positionierbarer Motivanker**: Interaktive Festlegung des Motivzentrums per Fingertipp im Vorschaubild (`anchorX`, `anchorY`) für die motivzentrierte Komposition.
- **Natural Perspective vs. Artistic Spread**: Klare Trennung zwischen realer Mehrperspektiven-Abweichung der Kamera und optionaler algorithmischer Spreizung.
- **Funktionsfähige Mischverfahren (Blend Modes)**: Vollständige mathematische Implementierung von `WEIGHTED_AVERAGE`, `LIGHTEN`, `MULTIPLY`, `SCREEN` und `DRIFT_FLOW` in Vorschau und Export.
- **Intelligente Bildauswahl**: Gezieltes Aktivieren und Deaktivieren einzelner Aufnahmen aus der Bildserie sowie lokale Qualitätsindikatoren (Unschärfe-, Belichtungs- und Duplikaterkennung).
- **Selektive Motivgewichtung**: Weichkantige Bereichsmaske um den Motivanker zur gezielten Verankerung des Hauptmotivs.
- **Zentrale Versionsverwaltung**: Dynamisches Auslesen von `BuildConfig.VERSION_NAME` ("0.9.0") und `BuildConfig.VERSION_CODE` ("9") in Studio-Header und "Über ORBIT"-Dialog.
- **Über-ORBIT-Dialog**: Informationsansicht mit Versions-, Entwicklungsstatus- und kuratorischen Hintergrundangaben zum Ventosa-Ansatz.

### Changed
- `OrbitFusionEngine`: Numerisch stabile Pixel-Mischmodi bei 30–100 Bildern gegen Über- oder Unterbelichtung.
- `StudioScreen`: Integrierter Motivanker-Fokuszeiger, Bildauswahl-Umschalter, Blend-Mode-Selektor und dynamische Versionsanzeige.
- `OrbitProjectRepository`: Volle Abwärtskompatibilität und Persistierung aller V0.9.0-Parameter.
