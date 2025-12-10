package com.waveapp.tarotgemini.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waveapp.tarotgemini.ui.components.CardBack
import com.waveapp.tarotgemini.ui.components.TarotCardView
import com.waveapp.tarotgemini.ui.viewmodel.TarotViewModel

/**
 * Pantalla principal de la aplicación de tarot.
 * Muestra el flujo completo: pregunta → tirada → interpretación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarotScreen(
    modifier: Modifier = Modifier,
    viewModel: TarotViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Diálogo para mostrar el significado de una carta
    if (uiState.selectedCard != null && uiState.selectedCardMeaning != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCardMeaning() },
            title = {
                Text(
                    text = uiState.selectedCard!!.card.name,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (uiState.selectedCard!!.isReversed) "Invertida" else "Normal",
                        color = if (uiState.selectedCard!!.isReversed)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (uiState.isLoadingCardMeaning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                        )
                    } else {
                        Text(text = uiState.selectedCardMeaning ?: "")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissCardMeaning() }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo de error
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🔮 Tarot Gemini",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título de la sección
            Text(
                text = "Consulta el Tarot",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Campo de pregunta
            OutlinedTextField(
                value = uiState.question,
                onValueChange = { viewModel.onQuestionChanged(it) },
                label = { Text("Escribe tu pregunta") },
                placeholder = { Text("¿Qué quieres saber?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                minLines = 2,
                maxLines = 4
            )

            // Mazo de cartas (dorso) antes de la tirada
            if (uiState.drawnCards.isEmpty()) {
                Text(
                    text = "El mazo está listo",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy((-40).dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    repeat(5) {
                        CardBack()
                    }
                }
            }

            // Botón de tirada
            Button(
                onClick = { viewModel.performSpread() },
                enabled = uiState.isButtonEnabled && uiState.drawnCards.isEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = if (uiState.drawnCards.isEmpty())
                        "Realizar Tirada"
                    else
                        "Nueva Tirada",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Cartas sacadas
            if (uiState.drawnCards.isNotEmpty()) {
                // Reemplazado Divider por HorizontalDivider
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    text = "Tu tirada",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Mostrar las 3 cartas horizontalmente
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    uiState.drawnCards.forEach { drawnCard ->
                        TarotCardView(
                            drawnCard = drawnCard,
                            onInfoClick = { viewModel.showCardMeaning(drawnCard) }
                        )
                    }
                }

                // Botón de interpretación
                Button(
                    onClick = { viewModel.requestInterpretation() },
                    enabled = !uiState.isLoadingInterpretation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    if (uiState.isLoadingInterpretation) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    Text(
                        text = if (uiState.isLoadingInterpretation)
                            "Interpretando..."
                        else
                            "✨ Interpretar Tirada",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                // Interpretación
                if (uiState.interpretation != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🌟 Interpretación",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = uiState.interpretation!!,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Justify,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Botón para nueva consulta
                    OutlinedButton(
                        onClick = { viewModel.resetSpread() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Nueva Consulta")
                    }
                }
            }
        }
    }
}
