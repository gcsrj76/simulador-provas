package com.papito.simuladorprovas.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.widget.Toast
import com.papito.simuladorprovas.model.Question
import java.io.File

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, "simulador.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE questoes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pergunta TEXT NOT NULL,
                opcao_a TEXT NOT NULL,
                opcao_b TEXT NOT NULL,
                opcao_c TEXT NOT NULL,
                opcao_d TEXT NOT NULL,
                correta TEXT NOT NULL,
                texto_referencia TEXT,
                resposta_dada TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS questoes")
        onCreate(db)
    }

    fun inserirQuestao(db: SQLiteDatabase, q: Question) {
        val values = ContentValues().apply {
            put("pergunta", q.pergunta)
            put("opcao_a", q.opcaoA)
            put("opcao_b", q.opcaoB)
            put("opcao_c", q.opcaoC)
            put("opcao_d", q.opcaoD)
            put("correta", q.correta)
            put("texto_referencia", q.textoReferencia)
        }
        db.insert("questoes", null, values)
    }

    fun limparQuestoes() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM questoes")
        // Reseta o contador para a próxima questão ser a número 1
        db.execSQL("DELETE FROM sqlite_sequence WHERE name='questoes'")
    }

    // Função para salvar o progresso do usuário
    fun salvarProgresso(idQuestao: Int, resposta: String?) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("resposta_dada", resposta)
        }
        // Atualiza apenas a linha da questão específica
        db.update("questoes", values, "id = ?", arrayOf(idQuestao.toString()))
    }

    fun salvarTodasAsRespostas(respostas: Map<Int, String>) {
        val db = this.writableDatabase
        db.beginTransaction() // Inicia transação para performance
        try {
            respostas.forEach { (id, resposta) ->
                val values = ContentValues().apply {
                    put("resposta_dada", resposta)
                }
                db.update("questoes", values, "id = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // Caso queira resetar o simulado mantendo as perguntas
    fun limparApenasRespostas() {
        val db = this.writableDatabase
        db.execSQL("UPDATE questoes SET resposta_dada = NULL")
    }

    fun importarQuestoesDB(uri: android.net.Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "temp_questoes.db")

            inputStream?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }

            val externalDatabase = SQLiteDatabase.openDatabase(
                tempFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            val cursor = externalDatabase.rawQuery(
                "SELECT id, pergunta, opcao_a, opcao_b, opcao_c, opcao_d, correta, texto_referencia FROM questoes", 
                null
            )

            val dbInterno = this.writableDatabase

            while (cursor.moveToNext()) {
                val qBase = Question(
                    id = 0,
                    pergunta = cursor.getString(1),
                    opcaoA = cursor.getString(2),
                    opcaoB = cursor.getString(3),
                    opcaoC = cursor.getString(4),
                    opcaoD = cursor.getString(5),
                    correta = cursor.getString(6),
                    textoReferencia = cursor.getString(7)
                )
                inserirQuestao(dbInterno, qBase)
            }

            cursor.close()
            externalDatabase.close()
            dbInterno.close()

            Toast.makeText(context, "Banco de dados importado!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro no DB: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}