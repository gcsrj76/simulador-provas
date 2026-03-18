package com.papito.simuladorprovas.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.papito.simuladorprovas.model.Question
import org.json.JSONArray

object JsonParser {

    fun importarQuestoesJSON(context: Context, uri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() } ?: ""
            
            val jsonArray = JSONArray(jsonString)
            val dbHelper = DatabaseHelper(context)
            val dbInterno = dbHelper.writableDatabase

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val q = Question(
                    id = 0,
                    pergunta = obj.getString("pergunta"),
                    opcaoA = obj.getString("opcao_a"),
                    opcaoB = obj.getString("opcao_b"),
                    opcaoC = obj.getString("opcao_c"),
                    opcaoD = obj.getString("opcao_d"),
                    correta = obj.getString("correta"),
                    textoReferencia = obj.optString("texto_referencia", null)
                )
                dbHelper.inserirQuestao(dbInterno, q)
            }

            dbInterno.close()
            Toast.makeText(context, "JSON importado com sucesso!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao ler JSON: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}