package com.papito.simuladorprovas.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papito.simuladorprovas.model.Question
import com.papito.simuladorprovas.ui.components.OptionCard
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs // Para a função abs() de distância

@Composable
fun QuestionScreen(
    questoes: SnapshotStateList<Question>,
    currentIndex: Int,
    selectedAnswers: Map<Int, String>,
    answeredQuestions: Set<Int>,
    onAnswerSelected: (Int, String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinalizar: () -> Unit,
    onQuestionSelect: (Int) -> Unit
) {
    val currentQuestion = questoes[currentIndex]
    val isAnswered = answeredQuestions.contains(currentQuestion.id)

    val correctCount = selectedAnswers.count { (id, answer) ->
        questoes.find { it.id == id }?.correta == answer
    }
    val errorCount = answeredQuestions.size - correctCount

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .pointerInput(Unit) {
                // Usamos o Initial para "espiar" o toque antes dos botões
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    var dragged = false

                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val dragChange = event.changes.firstOrNull()

                        if (dragChange != null && dragChange.pressed) {
                            // Acumulamos o movimento
                            offsetX += dragChange.positionChange().x
                            offsetY += dragChange.positionChange().y

                            // Se moveu mais de 10 pixels, consideramos um arrasto e não um clique
                            if (abs(offsetX) > 10 || abs(offsetY) > 10) {
                                dragged = true
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // Ao soltar o dedo (onDragEnd manual)
                    if (dragged) {
                        if (offsetX < -160) onNext()
                        else if (offsetX > 160) onPrevious()
                        else if (offsetY < -250) onFinalizar()
                    }

                    // Reseta para o próximo toque
                    offsetX = 0f
                    offsetY = 0f
                }
            }
    )
    {
        // --- HEADER COM SLIDER E CONTADORES ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Questão ${currentIndex + 1}/${questoes.size}",
                color = Color.White,
                fontSize = 14.sp
            )

            Slider(
                value = currentIndex.toFloat(),
                onValueChange = { onQuestionSelect(it.toInt()) },
                valueRange = 0f..(if (questoes.size > 1) (questoes.size - 1).toFloat() else 1f),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White
                )
            )

            Row {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$correctCount", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFF44336)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$errorCount", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- MOLDURA DA PERGUNTA ---
        Column(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "${currentIndex + 1}) ${currentQuestion.pergunta}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            val options = listOf(
                "a" to currentQuestion.opcaoA,
                "b" to currentQuestion.opcaoB,
                "c" to currentQuestion.opcaoC,
                "d" to currentQuestion.opcaoD
            )

            options.forEach { (letra, texto) ->
                OptionCard(
                    letra = letra,
                    texto = texto,
                    isSelected = selectedAnswers[currentQuestion.id] == letra,
                    isCorrect = if (isAnswered) letra == currentQuestion.correta else null,
                    isClickable = !isAnswered,
                    onClick = { onAnswerSelected(currentQuestion.id, letra) },
                    correctOptionText = if (isAnswered && letra != currentQuestion.correta) {
                        currentQuestion.correta
                    } else null
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- BOTÕES DE NAVEGAÇÃO ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                modifier = Modifier
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Voltar", color = Color.White)
            }

            OutlinedButton(
                onClick = {onFinalizar()},
                modifier = Modifier
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF009688))
            ) {
                Text("Pausa",color = Color.White)
            }

            Button(
                onClick = {
                    if (currentIndex == questoes.size - 1) {
                        // Se for a última questão, chama a função de finalizar (que grava no banco)
                        onFinalizar()
                    } else {
                        // Se não for a última, apenas navega para a próxima (sem gravar)
                        onNext()
                    }
                },
                modifier = Modifier
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(
                    text = if (currentIndex == questoes.size - 1) "Finalizar" else "Próxima",
                    color = Color.White
                )
            }
        }
    }
}