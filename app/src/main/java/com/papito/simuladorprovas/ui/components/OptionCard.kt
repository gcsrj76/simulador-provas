package com.papito.simuladorprovas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    // Lógica da borda colorida (Verde para acerto, Vermelho para erro)
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
                Text(
                    text = "${letra.uppercase()})",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(30.dp)
                )

                if (isCorrect == false && isSelected) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Erro",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(texto, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            }

            if (isCorrect == false && isSelected) {
                Text(
                    text = "✓ Resposta correta: ${correctOptionText ?: ""}",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}