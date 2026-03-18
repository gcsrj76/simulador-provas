package com.papito.simuladorprovas

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

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
    private var questoesCarregadas = mutableListOf<Question>()
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                // Copiar o arquivo para um local acessível e ler o banco de dados
                val inputStream = contentResolver.openInputStream(it)
                val tempFile = File(cacheDir, "temp_questoes.db")
                
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Abrir o banco de dados SQLite
                val database = SQLiteDatabase.openDatabase(
                    tempFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                
                // Ler as questões da tabela
                val cursor = database.rawQuery("SELECT id, pergunta, opcao_a, opcao_b, opcao_c, opcao_d, correta, texto_referencia FROM questoes", null)
                val questoes = mutableListOf<Question>()
                
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0)
                    val pergunta = cursor.getString(1)
                    val opcaoA = cursor.getString(2)
                    val opcaoB = cursor.getString(3)
                    val opcaoC = cursor.getString(4)
                    val opcaoD = cursor.getString(5)
                    val correta = cursor.getString(6)
                    val textoReferencia = cursor.getString(7)
                    
                    questoes.add(
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
                database.close()
                
                this@MainActivity.questoesCarregadas.clear()
                this@MainActivity.questoesCarregadas.addAll(questoes)
                
                Toast.makeText(this, "Carregadas ${questoes.size} questões com sucesso!", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Erro ao carregar questões: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExamSimulatorApp(
                onFilePickerClick = { filePickerLauncher.launch("*/*") },
                questoesCarregadas = questoesCarregadas
            )
        }
    }
}

@Composable
fun ExamSimulatorApp(onFilePickerClick: () -> Unit, questoesCarregadas: List<Question>) {
    var questoes by remember { mutableStateOf<List<Question>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var answeredQuestions by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showResult by remember { mutableStateOf(false) }
    var showQuestions by remember { mutableStateOf(false) }

    when {
        questoes.isEmpty() -> {
            // Tela inicial
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
                            // Abrir o seletor de arquivos
                            onFilePickerClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray
                        )
                    ) {
                        Text("Carregar Questões", color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            // Usar as questões carregadas do arquivo ou dados de exemplo
                            questoes = if (questoesCarregadas.isNotEmpty()) {
                                questoesCarregadas
                            } else {
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
                            }
                            showQuestions = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Simulado", color = Color.White)
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
                    questoes = emptyList()
                    selectedAnswers = emptyMap()
                    answeredQuestions = emptySet()
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
    questoes: List<Question>,
    currentIndex: Int,
    selectedAnswers: Map<Int, String>,
    answeredQuestions: Set<Int>,
    onAnswerSelected: (Int, String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onQuestionSelect: (Int) -> Unit
) {
    val currentQuestion = questoes[currentIndex]
    val progress = "${currentIndex + 1}/${questoes.size}"
    
    // Calcular acertos e erros
    val acertos = answeredQuestions.count { questionId ->
        val correctAnswer = questoes.find { it.id == questionId }?.correta
        selectedAnswers[questionId] == correctAnswer
    }
    val erros = answeredQuestions.size - acertos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header com navegação e contadores
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Componente de navegação
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Questão $progress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentIndex > 0) onQuestionSelect(currentIndex - 1) },
                        enabled = currentIndex > 0
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Anterior",
                            tint = Color.White
                        )
                    }
                    
                    Slider(
                        value = currentIndex.toFloat(),
                        onValueChange = { onQuestionSelect(it.toInt()) },
                        valueRange = 0f..(questoes.size - 1).toFloat(),
                        steps = questoes.size - 1,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.Gray,
                            thumbColor = Color.White
                        )
                    )
                    
                    IconButton(
                        onClick = { if (currentIndex < questoes.size - 1) onQuestionSelect(currentIndex + 1) },
                        enabled = currentIndex < questoes.size - 1
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "Próxima",
                            tint = Color.White
                        )
                    }
                }
            }
            
            // Contadores de acertos e erros
            Row {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "$acertos",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "$erros",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Área do texto auxiliar (se existir)
        if (!currentQuestion.textoReferencia.isNullOrEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        currentQuestion.textoReferencia,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Cartão da questão
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(20.dp)
            ) {
                Text(
                    currentQuestion.pergunta,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                val isAnswered = answeredQuestions.contains(currentQuestion.id)
                val selectedAnswer = selectedAnswers[currentQuestion.id]
                val isCorrect = if (isAnswered && selectedAnswer != null) {
                    selectedAnswer == currentQuestion.correta
                } else null
                
                Column {
                    OptionCard(
                        letra = "a",
                        texto = currentQuestion.opcaoA,
                        isSelected = selectedAnswer == "a",
                        isCorrect = if (selectedAnswer == "a") isCorrect else null,
                        isClickable = !answeredQuestions.contains(currentQuestion.id),
                        onClick = { onAnswerSelected(currentQuestion.id, "a") },
                        correctOptionText = if (selectedAnswer == "a" && isCorrect == false) 
                            getCorrectAnswerText(currentQuestion) else null
                    )
                    
                    OptionCard(
                        letra = "b",
                        texto = currentQuestion.opcaoB,
                        isSelected = selectedAnswer == "b",
                        isCorrect = if (selectedAnswer == "b") isCorrect else null,
                        isClickable = !answeredQuestions.contains(currentQuestion.id),
                        onClick = { onAnswerSelected(currentQuestion.id, "b") },
                        correctOptionText = if (selectedAnswer == "b" && isCorrect == false) 
                            getCorrectAnswerText(currentQuestion) else null
                    )
                    
                    OptionCard(
                        letra = "c",
                        texto = currentQuestion.opcaoC,
                        isSelected = selectedAnswer == "c",
                        isCorrect = if (selectedAnswer == "c") isCorrect else null,
                        isClickable = !answeredQuestions.contains(currentQuestion.id),
                        onClick = { onAnswerSelected(currentQuestion.id, "c") },
                        correctOptionText = if (selectedAnswer == "c" && isCorrect == false) 
                            getCorrectAnswerText(currentQuestion) else null
                    )
                    
                    OptionCard(
                        letra = "d",
                        texto = currentQuestion.opcaoD,
                        isSelected = selectedAnswer == "d",
                        isCorrect = if (selectedAnswer == "d") isCorrect else null,
                        isClickable = !answeredQuestions.contains(currentQuestion.id),
                        onClick = { onAnswerSelected(currentQuestion.id, "d") },
                        correctOptionText = if (selectedAnswer == "d" && isCorrect == false) 
                            getCorrectAnswerText(currentQuestion) else null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botões de navegação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentIndex > 0) Color.DarkGray else Color.Gray,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text("Voltar", color = Color.White)
            }

            // Debug: Sempre mostrar o estado atual
            
            if (currentIndex == questoes.size - 1) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Finalizar", color = Color.White)
                }
            } else {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray
                    )
                ) {
                    Text("Próxima", color = Color.White)
                }
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
    val backgroundColor = if (isSelected) Color.DarkGray else Color(0xFF1E1E1E)
    val showBorder = isSelected || isCorrect != null

    val borderColor = when (isCorrect) {
        true -> Color(0xFF4CAF50)
        false -> Color(0xFFF44336)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = isClickable) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (showBorder) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${letra.uppercase()})",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(30.dp)
                )
                
                if (isCorrect != null) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Text(
                    texto,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (isCorrect != null) 8.dp else 0.dp),
                    lineHeight = 18.sp
                )
            }
            
            if (isCorrect == false && correctOptionText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "✓ Resposta correta: $correctOptionText",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

fun getCorrectAnswerText(question: Question): String {
    return when (question.correta) {
        "a" -> "a) ${question.opcaoA}"
        "b" -> "b) ${question.opcaoB}"
        "c" -> "c) ${question.opcaoC}"
        "d" -> "d) ${question.opcaoD}"
        else -> ""
    }
}

@Composable
fun ResultScreen(
    questoes: List<Question>,
    selectedAnswers: Map<Int, String>,
    answeredQuestions: Set<Int>,
    onRestart: () -> Unit
) {
    val acertos = answeredQuestions.count { questionId ->
        val correctAnswer = questoes.find { it.id == questionId }?.correta
        selectedAnswers[questionId] == correctAnswer
    }
    val erros = answeredQuestions.size - acertos
    val nota = if (questoes.isNotEmpty()) {
        ((acertos.toDouble() / questoes.size) * 10.0)
    } else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Resultado Final",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Acertos: $acertos de ${questoes.size}\nNota: %.1f".format(nota),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$acertos",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            "Acertos",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$erros",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                        Text(
                            "Erros",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray
                    )
                ) {
                    Text("Voltar ao Início", color = Color.White)
                }
            }
        }
    }
}
