package com.waveapp.tarotgemini.data.model

/**
 * Representa una carta sacada en una tirada.
 *
 * @param card La carta del tarot
 * @param isReversed Si salió invertida (true) o normal (false)
 * @param position Posición en la tirada (0, 1, 2 para tirada de 3 cartas)
 * @param positionMeaning Qué representa esta posición (ej: "Energías actuales")
 */
data class DrawnCard(
    val card: TarotCard,
    val isReversed: Boolean,
    val position: Int,
    val positionMeaning: String
)

