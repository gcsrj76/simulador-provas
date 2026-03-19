package com.papito.simuladorprovas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papito.simuladorprovas.model.Question

@Composable
fun ResultScreen(
    questoes: SnapshotStateList<Question>,
    selectedAnswers: Map<Int, String>,
    onVoltarMenu: () -> Unit,           // Apenas volta para a tela inicial
    onReiniciarSimulado: () -> Unit     // Limpa respostas e volta para o início do quiz
) {
    val totalProva = questoes.size
    val correctAnswers = selectedAnswers.count { (questionId, answer) ->
        questoes.find { it.id == questionId }?.correta == answer
    }

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
                        color = if (percentage >= 70) Color(0xFF00FFBC) else Color(0xFFF44336),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Botão de Ação Principal (Voltar ao Menu)
                Button(
                    onClick = onVoltarMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp), // Altura padrão Material Design
                    shape = RoundedCornerShape(12.dp), // Bordas levemente arredondadas, não pílula
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3) // Azul mais moderno
                    )
                ) {
                    Text(
                        "Voltar ao Menu Inicial",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botão de Ação de Limpeza (Reiniciar)
                // Usamos OutlinedButton para indicar que é uma ação secundária/destrutiva
                OutlinedButton(
                    onClick = onReiniciarSimulado,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF009688)), // Vermelho mais elegante
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF009688)
                    )
                ) {
                    Text("Reiniciar a Avaliação", fontSize = 16.sp)
                }
            }
        }
    }
}