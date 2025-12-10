package com.waveapp.tarotgemini.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveapp.tarotgemini.data.model.DrawnCard
import com.waveapp.tarotgemini.data.model.SpreadType
import com.waveapp.tarotgemini.data.repository.TarotDeckRepository
import com.waveapp.tarotgemini.domain.usecase.InterpretSpreadUseCase
import com.waveapp.tarotgemini.domain.usecase.ShuffleCardsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel que maneja el estado y la lógica de la pantalla de tirada de tarot.
 *
 * Sigue el patrón MVVM:
 * - View (UI): Observa el estado y envía acciones
 * - ViewModel: Procesa acciones y actualiza el estado
 * - Model (Data/Domain): Maneja datos y lógica de negocio
 */
class TarotViewModel(
    private val deckRepository: TarotDeckRepository = TarotDeckRepository(),
    private val shuffleCardsUseCase: ShuffleCardsUseCase = ShuffleCardsUseCase(),
    private val interpretSpreadUseCase: InterpretSpreadUseCase = InterpretSpreadUseCase()
) : ViewModel() {

    // Estado interno mutable
    private val _uiState = MutableStateFlow(TarotUiState())

    // Estado público inmutable para la UI
    val uiState: StateFlow<TarotUiState> = _uiState.asStateFlow()

    /**
     * Actualiza la pregunta del usuario
     */
    fun onQuestionChanged(question: String) {
        _uiState.update { it.copy(question = question) }
    }

    /**
     * Realiza la tirada de cartas
     */
    fun performSpread() {
        val currentState = _uiState.value

        // Validar que hay una pregunta
        if (currentState.question.isBlank()) {
            _uiState.update { it.copy(error = "Por favor escribe una pregunta") }
            return
        }

        // Obtener el mazo y realizar la tirada
        val deck = deckRepository.getFullDeck()
        val drawnCards = shuffleCardsUseCase.drawCards(
            deck = deck,
            spreadType = currentState.spreadType
        )

        // Actualizar el estado con las cartas
        _uiState.update {
            it.copy(
                drawnCards = drawnCards,
                isLoading = false,
                error = null
            )
        }
    }

    /**
     * Solicita la interpretación de la tirada a la IA
     */
    fun requestInterpretation() {
        val currentState = _uiState.value

        // Validar que hay cartas
        if (currentState.drawnCards.isEmpty()) {
            _uiState.update { it.copy(error = "Primero debes realizar una tirada") }
            return
        }

        // Indicar que está cargando
        _uiState.update { it.copy(isLoadingInterpretation = true, error = null) }

        // Solicitar interpretación de forma asíncrona
        viewModelScope.launch {
            val result = interpretSpreadUseCase.execute(
                question = currentState.question,
                drawnCards = currentState.drawnCards
            )

            result.fold(
                onSuccess = { interpretation ->
                    _uiState.update {
                        it.copy(
                            interpretation = interpretation,
                            isLoadingInterpretation = false
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            error = "Error al obtener interpretación: ${exception.message}",
                            isLoadingInterpretation = false
                        )
                    }
                }
            )
        }
    }

    /**
     * Obtiene el significado detallado de una carta específica
     */
    fun showCardMeaning(drawnCard: DrawnCard) {
        _uiState.update { it.copy(isLoadingCardMeaning = true) }

        viewModelScope.launch {
            val result = interpretSpreadUseCase.getCardMeaning(drawnCard)

            result.fold(
                onSuccess = { meaning ->
                    _uiState.update {
                        it.copy(
                            selectedCardMeaning = meaning,
                            selectedCard = drawnCard,
                            isLoadingCardMeaning = false
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            error = "Error al obtener significado: ${exception.message}",
                            isLoadingCardMeaning = false
                        )
                    }
                }
            )
        }
    }

    /**
     * Cierra el diálogo de significado de carta
     */
    fun dismissCardMeaning() {
        _uiState.update {
            it.copy(
                selectedCard = null,
                selectedCardMeaning = null
            )
        }
    }

    /**
     * Limpia el error actual
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Reinicia la tirada para empezar de nuevo
     */
    fun resetSpread() {
        _uiState.update {
            TarotUiState(spreadType = it.spreadType)
        }
    }
}

/**
 * Estado de la UI para la pantalla de tarot.
 * Contiene toda la información que la UI necesita mostrar.
 */
data class TarotUiState(
    val question: String = "",
    val spreadType: SpreadType = SpreadType.ThreeCard,
    val drawnCards: List<DrawnCard> = emptyList(),
    val interpretation: String? = null,
    val selectedCard: DrawnCard? = null,
    val selectedCardMeaning: String? = null,
    val isLoading: Boolean = false,
    val isLoadingInterpretation: Boolean = false,
    val isLoadingCardMeaning: Boolean = false,
    val error: String? = null
) {
    // Computed property: el botón está habilitado solo si hay pregunta
    val isButtonEnabled: Boolean
        get() = question.isNotBlank() && !isLoading
}

