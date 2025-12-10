package com.waveapp.tarotgemini.data.remote

import com.waveapp.tarotgemini.data.model.DrawnCard

/**
 * Cliente para interactuar con Gemini AI y obtener interpretaciones del tarot.
 *
 * Nota: implementación temporal que evita depender directamente de la SDK externa
 * para que el proyecto compile. Si tienes la SDK configurada y quieres usarla,
 * podemos integrar la llamada real más adelante.
 */
class GeminiClient {

    // Obtener la API key preferentemente de BuildConfig (generada en tiempo de compilación)
    // si no existe, usar la variable de entorno GEMINI_API_KEY.
    private val apiKey: String by lazy {
        try {
            val key = com.waveapp.tarotgemini.BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank()) key else (System.getenv("GEMINI_API_KEY") ?: "")
        } catch (_: Throwable) {
            System.getenv("GEMINI_API_KEY") ?: ""
        }
    }

    // Implementación temporal: genera una respuesta simulada basada en el prompt.
    // Reemplazar por la implementación real cuando se integre la SDK de Gemini.
    private fun generateFakeResponse(prompt: String): String {
        // Devuelve un texto corto que incluye un resumen de las cartas para pruebas.
        val maxLen = 300
        val summary = prompt.take(maxLen).replace(Regex("\n+"), " ")
        return "[Simulación Gemini] Interpretación generada a partir del prompt: $summary"
    }

    /**
     * Solicita a Gemini AI una interpretación de la tirada de tarot.
     *
     * @param question La pregunta del usuario
     * @param drawnCards Las cartas que salieron en la tirada
     * @return La interpretación completa (simulada en esta versión)
     */
    fun interpretSpread(
        question: String,
        drawnCards: List<DrawnCard>
    ): Result<String> {
        return try {
            val prompt = buildPrompt(question, drawnCards)
            // Si hay una API key configurada, aquí podríamos invocar la SDK real.
            // Por ahora usamos una respuesta simulada para evitar errores de compilación.
            val responseText = if (apiKey.isNotBlank()) {
                // Placeholder: aún no integrado el cliente real
                generateFakeResponse(prompt)
            } else {
                generateFakeResponse(prompt)
            }
            Result.success(responseText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Construye el prompt para enviar a Gemini con contexto del tarot.
     */
    private fun buildPrompt(question: String, drawnCards: List<DrawnCard>): String {
        return buildString {
            appendLine("Eres un experto tarotista con años de experiencia en lectura de cartas.")
            appendLine("Tu objetivo es proporcionar interpretaciones profundas, místicas y significativas.")
            appendLine()
            appendLine("PREGUNTA DEL CONSULTANTE:")
            appendLine("\"$question\"")
            appendLine()
            appendLine("CARTAS DE LA TIRADA:")
            drawnCards.forEachIndexed { index, drawnCard ->
                appendLine()
                appendLine("Posición ${index + 1}: ${drawnCard.positionMeaning}")
                appendLine("Carta: ${drawnCard.card.name}")
                appendLine("Orientación: ${if (drawnCard.isReversed) "Invertida" else "Normal"}")
                val meaning = if (drawnCard.isReversed) {
                    drawnCard.card.reversedMeaning
                } else {
                    drawnCard.card.uprightMeaning
                }
                appendLine("Significado: $meaning")
            }
            appendLine()
            appendLine("Por favor, proporciona:")
            appendLine("1. Una interpretación general de la tirada completa")
            appendLine("2. Cómo las cartas se relacionan entre sí")
            appendLine("3. Un mensaje final o consejo para el consultante")
            appendLine()
            appendLine("Usa un tono místico pero accesible, comprensivo y esperanzador.")
        }
    }

    /**
     * Obtiene el significado específico de una carta individual.
     * Útil para el botón de información "i" de cada carta.
     */
    fun getCardMeaning(drawnCard: DrawnCard): Result<String> {
        return try {
            val prompt = buildString {
                appendLine("Explica el significado de la carta del tarot:")
                appendLine("Carta: ${drawnCard.card.name}")
                appendLine("Orientación: ${if (drawnCard.isReversed) "Invertida" else "Normal"}")
                appendLine()
                val meaning = if (drawnCard.isReversed) {
                    drawnCard.card.reversedMeaning
                } else {
                    drawnCard.card.uprightMeaning
                }
                appendLine("Significado base: $meaning")
                appendLine()
                appendLine("Proporciona una explicación más detallada y profunda de este significado.")
                appendLine("Incluye consejos prácticos sobre cómo aplicar este mensaje.")
                appendLine("Usa un tono místico pero claro, en 2-3 párrafos.")
            }

            val responseText = generateFakeResponse(prompt)
            Result.success(responseText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
