package com.papito.simuladorprovas

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

data class Question(
    val id: Int,
    val pergunta: String,
    val opcaoA: String,
    val opcaoB: String,
    val opcaoC: String,
    val opcaoD: String,
    val correta: String,
    val textoReferencia: String? = null
)

class MainActivity : ComponentActivity() {
    private val questoesCarregadas = mutableStateListOf<Question>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ExamSimulatorApp(
                questoes = questoesCarregadas,
                onFilePickerClick = { filePickerLauncher.launch("*/*") }
            )
        }
    }
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                // Ler questões do arquivo selecionado
                val inputStream = contentResolver.openInputStream(it)
                val tempFile = File(cacheDir, "temp_questoes.db")
                
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Abrir o banco de dados externo
                val externalDatabase = SQLiteDatabase.openDatabase(
                    tempFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                
                // Ler as questões do arquivo externo
                val cursor = externalDatabase.rawQuery("SELECT id, pergunta, opcao_a, opcao_b, opcao_c, opcao_d, correta, texto_referencia FROM questoes", null)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0)
                    val pergunta = cursor.getString(1)
                    val opcaoA = cursor.getString(2)
                    val opcaoB = cursor.getString(3)
                    val opcaoC = cursor.getString(4)
                    val opcaoD = cursor.getString(5)
                    val correta = cursor.getString(6)
                    val textoReferencia = cursor.getString(7)

                    questoesCarregadas.add(
                        Question(
                            id = id,
                            pergunta = pergunta,
                            opcaoA = opcaoA,
                            opcaoB = opcaoB,
                            opcaoC = opcaoC,
                            opcaoD = opcaoD,
                            correta = correta,
                            textoReferencia = textoReferencia
                        )
                    )
                }
                
                cursor.close()
                externalDatabase.close()
                
                Toast.makeText(this, "Carregadas ${questoesCarregadas.size} questões com sucesso!", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Erro ao carregar questões: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun ExamSimulatorApp(questoes: SnapshotStateList<Question>,onFilePickerClick: () -> Unit) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswers by remember { mutableStateOf<Map<Int, String>>(HashMap()) }
    var answeredQuestions by remember { mutableStateOf<Set<Int>>(HashSet()) }
    var showResult by remember { mutableStateOf(false) }
    var showQuestions by remember { mutableStateOf(false) }

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
                        onClick = { 
                            // Abrir o seletor de arquivos para carregar questões
                            onFilePickerClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Carregar Questões", color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            // Iniciar o simulado com as questões carregadas ou usar dados de exemplo
                            if (questoes.isEmpty()) {
                                // Adicionar questões de exemplo
                                questoes.addAll(
                                    listOf(
                                        Question(
                                            1, "O que é Kotlin?", 
                                            "Uma linguagem de programação", "Um banco de dados", "Um sistema operacional", "Um navegador",
                                            "a", "Kotlin é uma linguagem de programação moderna"
                                        ),
                                        Question(
                                            2, "O que é Android?",
                                            "Um framework web", "Um sistema operacional móvel", "Um banco de dados", "Uma linguagem de programação",
                                            "b", "Android é um sistema operacional para dispositivos móveis"
                                        )
                                    )
                                )
                            }
                            showQuestions = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("Iniciar Simulado", color = Color.White)
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
                        // Finalizar questionário
                        showResult = true
                        showQuestions = false
                    }
                },
                onPrevious = {
                    if (currentQuestionIndex > 0) {
                        currentQuestionIndex--
                    }
                },
                onQuestionSelect = { index ->
                    currentQuestionIndex = index
                }
            )
        }
        showResult -> {
            ResultScreen(
                questoes = questoes,
                selectedAnswers = selectedAnswers,
                answeredQuestions = answeredQuestions,
                onRestart = {
                    questoes.clear()
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

@Composable
fun QuestionScreen(
    questoes: SnapshotStateList<Question>, // Corrigido para o tipo que aceita mudanças
    currentIndex: Int,
    selectedAnswers: Map<Int, String>,
    answeredQuestions: Set<Int>,
    onAnswerSelected: (Int, String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onQuestionSelect: (Int) -> Unit
) {
    val currentQuestion = questoes[currentIndex]
    val isAnswered = answeredQuestions.contains(currentQuestion.id)

    // Lógica para os contadores no topo (círculos verde e vermelho)
    val correctCount = selectedAnswers.count { (id, answer) ->
        questoes.find { it.id == id }?.correta == answer
    }
    val errorCount = answeredQuestions.size - correctCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
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

            // Slider de progresso (barra branca central)
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

            // Círculos de Acertos e Erros
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

        // --- MOLDURA BRANCA COM PERGUNTA E OPÇÕES ---
        Column(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color.White, RoundedCornerShape(12.dp)) // A moldura da imagem 2
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

        // --- BOTÕES DE NAVEGAÇÃO (ESTILO CINZA ARREDONDADO) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.width(120.dp)
            ) {
                Text("Voltar", color = Color.White)
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.width(120.dp)
            ) {
                Text(
                    text = if (currentIndex == questoes.size - 1) "Finalizar" else "Próxima",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun OptionCard(
    letra: String,
    texto: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    isClickable: Boolean,
    onClick: () -> Unit,
    correctOptionText: String? = null
) {
    // Define a borda baseada no acerto/erro
    val borderStroke = when {
        isCorrect == true && isSelected -> BorderStroke(2.dp, Color(0xFF4CAF50))
        isCorrect == false && isSelected -> BorderStroke(2.dp, Color(0xFFF44336))
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable) { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(8.dp),
        border = borderStroke
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${letra.uppercase()})", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(30.dp))

                // Se errou, mostra o X vermelho ao lado da letra
                if (isCorrect == false && isSelected) {
                    Icon(Icons.Default.Close, "Erro", tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(texto, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            }

            if (isCorrect == false && isSelected) {
                Text(
                    "✓ Resposta correta: ${correctOptionText ?: ""}",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
@Composable
fun ResultScreen(
    questoes: SnapshotStateList<Question>,
    selectedAnswers: Map<Int, String>,
    answeredQuestions: Set<Int>,
    onRestart: () -> Unit
) {
    // 1. Cálculo baseado no total de questões da prova (questoes.size)
    val totalProva = questoes.size
    val correctAnswers = selectedAnswers.count { (questionId, answer) ->
        questoes.find { it.id == questionId }?.correta == answer
    }

    // Porcentagem real sobre o total do simulado
    val percentage = if (totalProva > 0) (correctAnswers * 100) / totalProva else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Resultado Final",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Exibe Acertos / Total da Prova
                    Text(
                        text = "$correctAnswers/$totalProva",
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "questões corretas",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "$percentage% de aproveitamento",
                        color = if (percentage >= 70) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Botão agora fixo e visível (estilo Pill como os outros)
            Button(
                onClick = onRestart,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    "Voltar ao Início",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
