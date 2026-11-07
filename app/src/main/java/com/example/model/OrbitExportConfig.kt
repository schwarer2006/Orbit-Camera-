package com.example.model

import kotlin.math.roundToInt

enum class OrbitExportSize(
    val title: String,
    val longEdge: Int,
    val description: String
) {
    STANDARD_1080("Standard", 1080, "1080 px • Schneller Alltags-Export"),
    HIGH_2048("Hoch", 2048, "2048 px • Empfohlen (Standardauswahl)"),
    VERY_HIGH_3072("Sehr hoch", 3072, "3072 px • Hohe Detailtiefe"),
    MAX_4096("Maximum", 4096, "Bis 4096 px • Maximale Studio-Auflösung");

    companion object {
        fun isSizeSupported(size: OrbitExportSize): Boolean {
            val maxMemoryMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
            return when (size) {
                MAX_4096 -> maxMemoryMb >= 512
                VERY_HIGH_3072 -> maxMemoryMb >= 256
                else -> true
            }
        }
    }
}

enum class OrbitExportRatio(
    val title: String,
    val ratioLabel: String,
    val description: String
) {
    SQUARE_1_1("1:1", "1:1", "Quadrat (1:1) • Zentriert"),
    PORTRAIT_4_5("4:5", "4:5", "Portrait (4:5) • Feed & Social"),
    LANDSCAPE_16_9("16:9", "16:9", "Kino (16:9) • Displays & Breitbild");

    fun calculateDimensions(longEdge: Int): Pair<Int, Int> {
        return when (this) {
            SQUARE_1_1 -> Pair(longEdge, longEdge)
            PORTRAIT_4_5 -> Pair((longEdge * 4f / 5f).roundToInt(), longEdge)
            LANDSCAPE_16_9 -> Pair(longEdge, (longEdge * 9f / 16f).roundToInt())
        }
    }
}
