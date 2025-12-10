package com.waveapp.tarotgemini.domain.usecase

import com.waveapp.tarotgemini.data.model.DrawnCard
import com.waveapp.tarotgemini.data.model.SpreadType
import com.waveapp.tarotgemini.data.model.TarotCard
import kotlin.random.Random

/**
 * Caso de uso que se encarga de barajar y sacar cartas del mazo según el tipo de tirada.
 */
class ShuffleCardsUseCase {

    /**
     * Baraja el mazo y devuelve la lista de cartas sacadas con su posición y si están invertidas.
     *
     * @param deck Mazo completo de cartas
     * @param spreadType Tipo de tirada que define cuántas cartas y sus posiciones
     * @return Lista de cartas sacadas (DrawnCard). Si el mazo es menor que el número requerido, retorna lista vacía.
     */
    fun drawCards(deck: List<TarotCard>, spreadType: SpreadType): List<DrawnCard> {
        val count = spreadType.cardCount
        if (deck.size < count) return emptyList()

        val shuffled = deck.shuffled(Random.Default)
        val picked = shuffled.take(count)

        return picked.mapIndexed { index, card ->
            val positionInfo = spreadType.positions.getOrNull(index)
            DrawnCard(
                card = card,
                isReversed = Random.nextBoolean(),
                position = positionInfo?.index ?: index,
                positionMeaning = positionInfo?.meaning ?: ""
            )
        }
    }
}

