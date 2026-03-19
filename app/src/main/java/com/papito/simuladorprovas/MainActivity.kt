package com.papito.simuladorprovas

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateListOf
import com.papito.simuladorprovas.data.DatabaseHelper
import com.papito.simuladorprovas.model.Question
import com.papito.simuladorprovas.ui.screens.ExamSimulatorApp
import com.papito.simuladorprovas.data.JsonParser

class MainActivity : ComponentActivity() {
    private val questoesCarregadas = mutableStateListOf<Question>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        carregarDadosInternos(db)

        setContent {
            ExamSimulatorApp(
                questoes = questoesCarregadas,
                onFilePickerClick = { filePickerLauncher.launch("*/*") }
            )
        }
    }

    private fun carregarDadosInternos(db: SQLiteDatabase) {
        try {
            val cursor = db.rawQuery(
                "SELECT id, pergunta, opcao_a, opcao_b, opcao_c, opcao_d, correta, texto_referencia, resposta_dada FROM questoes",
                null
            )
            questoesCarregadas.clear()

            while (cursor.moveToNext()) {
                questoesCarregadas.add(
                    Question(
                        // getInt e getString podem retornar erro se a coluna for null no banco
                        id = cursor.getInt(0),

                        // Usamos ?: "" para garantir que, se o banco trouxer null,
                        // o Kotlin receba uma String vazia e não quebre.
                        pergunta = cursor.getString(1),
                        opcaoA = cursor.getString(2),
                        opcaoB = cursor.getString(3),
                        opcaoC = cursor.getString(4),
                        opcaoD = cursor.getString(5),
                        correta = cursor.getString(6),
                        textoReferencia = cursor.getString(7),
                        respostaDada = cursor.getString(8)
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                // 1. Identifica o tipo de arquivo
                val type = contentResolver.getType(it)
                val isJson = type == "application/json" || it.path?.endsWith(".json") == true

                // 2. Instancia o helper para uso
                val dbHelper = DatabaseHelper(this)

                // 3. Executa a importação baseada no tipo
                if (isJson) {
                    // Chama o objeto estático JsonParser que criamos
                    JsonParser.importarQuestoesJSON(this, it)
                } else {
                    // Chama o método da classe DatabaseHelper
                    dbHelper.importarQuestoesDB(it)
                }

                // 4. Recarrega a UI: busca os dados novos que acabaram de ser salvos no banco
                val db = dbHelper.readableDatabase
                carregarDadosInternos(db)

                // Feedback para o usuário
                Toast.makeText(this, "Importação finalizada com sucesso!", Toast.LENGTH_SHORT)
                    .show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Erro ao processar arquivo: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
}