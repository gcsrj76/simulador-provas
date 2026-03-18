package com.papito.simuladorprovas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papito.simuladorprovas.data.DatabaseHelper
import com.papito.simuladorprovas.model.Question
import java.util.HashMap
import java.util.HashSet

@Composable
fun ExamSimulatorApp(
    questoes: SnapshotStateList<Question>,
    onFilePickerClick: () -> Unit
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswers by remember { mutableStateOf<Map<Int, String>>(HashMap()) }
    var answeredQuestions by remember { mutableStateOf<Set<Int>>(HashSet()) }
    var showResult by remember { mutableStateOf(false) }
    var showQuestions by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    when {
        !showQuestions && !showResult -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Simulador de Provas",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { onFilePickerClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFBC))
                    ) {
                        Text("Carregar Questões", color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { 
                            if (questoes.isEmpty()) {
                                questoes.addAll(
                                    listOf(
                                        Question(1, "O que é Kotlin?", "Uma linguagem", "Banco de dados", "SO", "Browser", "a"),
                                        Question(2, "O que é Android?", "Framework", "SO Móvel", "Banco", "Linguagem", "b")
                                    )
                                )
                            }
                            showQuestions = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Iniciar Simulado", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val dbHelper = DatabaseHelper(context)
                            dbHelper.limparQuestoes()

                            // Limpa a lista da memória para a tela atualizar na hora!
                            questoes.clear()

                            android.widget.Toast.makeText(context, "Banco limpo com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF64235))
                    ) {
                        Text("Limpar Questões", color = Color.White)
                    }
                }
            }
        }
        showQuestions && !showResult -> {
            QuestionScreen(
                questoes = questoes,
                currentIndex = currentQuestionIndex,
                selectedAnswers = selectedAnswers,
                answeredQuestions = answeredQuestions,
                onAnswerSelected = { questionId, answer ->
                    if (!answeredQuestions.contains(questionId)) {
                        selectedAnswers = selectedAnswers + (questionId to answer)
                        answeredQuestions = answeredQuestions + questionId
                    }
                },
                onNext = {
                    if (currentQuestionIndex < questoes.size - 1) {
                        currentQuestionIndex++
                    } else {
                        showResult = true
                        showQuestions = false
                    }
                },
                onPrevious = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                onQuestionSelect = { index -> currentQuestionIndex = index }
            )
        }
        showResult -> {
            ResultScreen(
                questoes = questoes,
                selectedAnswers = selectedAnswers,
                answeredQuestions = answeredQuestions,
                onRestart = {
                    selectedAnswers = HashMap()
                    answeredQuestions = HashSet()
                    showResult = false
                    showQuestions = false
                    currentQuestionIndex = 0
                }
            )
        }
    }
}