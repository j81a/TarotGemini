package com.waveapp.tarotgemini.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waveapp.tarotgemini.data.model.DrawnCard

/**
 * Componente que muestra una carta del tarot con su información.
 *
 * @param drawnCard La carta a mostrar
 * @param onInfoClick Callback cuando se presiona el botón de información
 * @param modifier Modificador opcional
 */
@Composable
fun TarotCardView(
    drawnCard: DrawnCard,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(110.dp)
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (drawnCard.isReversed) {
                        Color(0xFFE3F2FD) // Azul claro para invertidas
                    } else {
                        Color(0xFFFFF3E0) // Amarillo claro para normales
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Botón de info en la esquina superior derecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información de la carta",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Contenido de la carta (rotada si está invertida)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .rotate(if (drawnCard.isReversed) 180f else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    // Por ahora mostramos el nombre, después usaremos imágenes
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = drawnCard.card.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(4.dp)
                        )

                        // Indicador visual del tipo de arcano
                        Text(
                            text = if (drawnCard.card.arcanaType == com.waveapp.tarotgemini.data.model.ArcanaType.MAJOR)
                                "Arcano Mayor" else "Arcano Menor",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Posición y significado
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = drawnCard.positionMeaning,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    if (drawnCard.isReversed) {
                        Text(
                            text = "⭮ Invertida",
                            fontSize = 9.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente para mostrar el "dorso" de la carta (antes de la tirada)
 */
@Composable
fun CardBack(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(110.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A148C),
                        Color(0xFF7B1FA2)
                    )
                )
            )
            .border(
                width = 2.dp,
                color = Color(0xFFFFD700),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🔮",
            fontSize = 48.sp
        )
    }
}

