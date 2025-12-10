package com.waveapp.tarotgemini.domain.usecase

import com.waveapp.tarotgemini.data.model.DrawnCard
import com.waveapp.tarotgemini.data.remote.GeminiClient

/**
 * Caso de uso para interpretar una tirada de cartas usando IA.
 *
 * Este caso de uso se encarga de:
 * - Enviar la pregunta y las cartas a Gemini AI
 * - Obtener y procesar la interpretación
 * - Manejar errores de comunicación con la API
 */
class InterpretSpreadUseCase(
    private val geminiClient: GeminiClient = GeminiClient()
) {

    /**
     * Solicita la interpretación de una tirada completa.
     *
     * @param question La pregunta del consultante
     * @param drawnCards Las cartas que salieron en la tirada
     * @return Result con la interpretación o el error
     */
    suspend fun execute(
        question: String,
        drawnCards: List<DrawnCard>
    ): Result<String> {
        return geminiClient.interpretSpread(question, drawnCards)
    }

    /**
     * Obtiene el significado detallado de una carta individual.
     * Para mostrar cuando el usuario toca el ícono "i" de una carta.
     *
     * @param drawnCard La carta de la que se quiere el significado
     * @return Result con el significado detallado o el error
     */
    suspend fun getCardMeaning(drawnCard: DrawnCard): Result<String> {
        return geminiClient.getCardMeaning(drawnCard)
    }
}

