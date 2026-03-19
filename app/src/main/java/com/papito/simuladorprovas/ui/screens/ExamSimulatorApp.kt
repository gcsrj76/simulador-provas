package com.papito.simuladorprovas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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

@Composable
fun ExamSimulatorApp(
    questoes: SnapshotStateList<Question>,
    onFilePickerClick: () -> Unit
) {
    //var currentQuestionIndex by remember { mutableStateOf(0) }
    var currentQuestionIndex by remember(questoes) {
        // Procura o índice da primeira questão que NÃO tem resposta salva
        val primeiroPendente = questoes.indexOfFirst { it.respostaDada == null }

        // Se encontrar (index != -1), começa nela.
        // Se todas estiverem respondidas, começa na última (ou na 0, como preferir).
        mutableIntStateOf(if (primeiroPendente != -1) primeiroPendente else 0)
    }

    // Removida a dependência excessiva do remember para evitar travamento de estado
    val respostasIniciais = questoes.filter { it.respostaDada != null }
        .associate { it.id to it.respostaDada!! }

    var selectedAnswers by remember(questoes.size) { mutableStateOf(respostasIniciais) }
    var answeredQuestions by remember(questoes.size) { mutableStateOf(respostasIniciais.keys.toSet()) }

    var showResult by remember { mutableStateOf(false) }
    var showQuestions by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    when {
        // --- TELA INICIAL ---
        !showQuestions && !showResult -> {
            Scaffold(
                containerColor = Color.Black, // Garante o fundo preto em toda a tela
                bottomBar = {
                    // O botão de Reset agora fica fixo aqui embaixo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 16.dp) // Espaço extra para não colar na barra do sistema
                    ) {
                        OutlinedButton(
                            onClick = {
                                val dbHelper = DatabaseHelper(context)
                                dbHelper.limparQuestoes()
                                questoes.clear()
                                selectedAnswers = emptyMap()
                                answeredQuestions = emptySet()

                                android.widget.Toast.makeText(
                                    context,
                                    "Base de questões apagada com sucesso!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF009688).copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(
                                0xFF009688
                            )
                            )

                        ) {
                            Text("Resetar Banco de Questões")
                        }
                    }
                }
            ) { paddingValues ->
                // O conteúdo centralizado vai aqui dentro
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues), // Respeita o espaço do botão lá embaixo
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Simulador de Provas",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Button(
                            onClick = {
                                if (questoes.isNotEmpty()) {
                                    val primeiroPendente = questoes.indexOfFirst { it.respostaDada == null }
                                    currentQuestionIndex = if (primeiroPendente != -1) primeiroPendente else 0
                                    showQuestions = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("Iniciar Simulado", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { onFilePickerClick() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("Carregar Questões", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } // Fim do bloco Menu Inicial

        // --- TELA DE QUESTÕES ---
        showQuestions && !showResult -> {
            QuestionScreen(
                questoes = questoes,
                currentIndex = currentQuestionIndex,
                selectedAnswers = selectedAnswers,
                answeredQuestions = answeredQuestions,
                onAnswerSelected = { questionId, answer ->
                    selectedAnswers = selectedAnswers + (questionId to answer)
                    answeredQuestions = answeredQuestions + questionId
                },
                onNext = {
                    if (currentQuestionIndex < questoes.size - 1) currentQuestionIndex++
                },
                onPrevious = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                onFinalizar = {
                    val dbHelper = DatabaseHelper(context)
                    dbHelper.salvarTodasAsRespostas(selectedAnswers)
                    showResult = true
                    showQuestions = false
                },
                onQuestionSelect = { index -> currentQuestionIndex = index }
            )
        } // Fim do bloco QuestionScreen

        // --- TELA DE RESULTADO ---
        showResult -> {
            ResultScreen(
                questoes = questoes,
                selectedAnswers = selectedAnswers,
                onVoltarMenu = {
                    showResult = false
                    showQuestions = false
                    currentQuestionIndex = 0
                },
                onReiniciarSimulado = {
                    val dbHelper = DatabaseHelper(context)
                    dbHelper.limparApenasRespostas()
                    selectedAnswers = emptyMap()
                    answeredQuestions = emptySet()
                    showResult = false
                    showQuestions = false
                    currentQuestionIndex = 0
                }
            )
        } // Fim do bloco ResultScreen
    } // Fim do When
} // Fim da Composable