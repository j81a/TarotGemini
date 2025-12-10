package com.waveapp.tarotgemini.data.model

/**
 * Define los tipos de tiradas disponibles.
 * Para Iteración 2, esto será más extensible con configuración JSON.
 */
sealed class SpreadType(
    val id: String,
    val name: String,
    val cardCount: Int,
    val positions: List<CardPosition>
) {
    /**
     * Tirada de 3 cartas - La más común para preguntas generales
     */
    object ThreeCard : SpreadType(
        id = "three_card",
        name = "Tirada de 3 Cartas",
        cardCount = 3,
        positions = listOf(
            CardPosition(0, "Energías actuales", GridPosition(row = 1, col = 0)),
            CardPosition(1, "El problema", GridPosition(row = 1, col = 1)),
            CardPosition(2, "La solución", GridPosition(row = 1, col = 2))
        )
    )
}

/**
 * Define una posición de carta en la tirada
 */
data class CardPosition(
    val index: Int,
    val meaning: String,
    val gridPosition: GridPosition
)

/**
 * Posición en la grilla 3x3
 */
data class GridPosition(
    val row: Int,  // 0-2
    val col: Int   // 0-2
)

