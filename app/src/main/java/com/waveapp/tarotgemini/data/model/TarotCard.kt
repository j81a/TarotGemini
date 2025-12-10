package com.waveapp.tarotgemini.data.model

/**
 * Representa una carta del tarot.
 *
 * @param id Identificador único de la carta
 * @param name Nombre de la carta (ej: "El Loco", "As de Copas")
 * @param arcanaType Tipo de arcano (Mayor o Menor)
 * @param suit Para arcanos menores: Copas, Espadas, Bastos, Oros. Null para mayores
 * @param imageResName Nombre del recurso de imagen (sin extensión)
 * @param uprightMeaning Significado cuando sale normal
 * @param reversedMeaning Significado cuando sale invertida
 */
data class TarotCard(
    val id: Int,
    val name: String,
    val arcanaType: ArcanaType,
    val suit: Suit? = null,
    val imageResName: String,
    val uprightMeaning: String,
    val reversedMeaning: String
)

/**
 * Palos de los arcanos menores
 */
enum class Suit {
    CUPS,      // Copas - Elemento Agua - Emociones
    SWORDS,    // Espadas - Elemento Aire - Intelecto
    WANDS,     // Bastos - Elemento Fuego - Acción
    PENTACLES  // Oros - Elemento Tierra - Material
}

