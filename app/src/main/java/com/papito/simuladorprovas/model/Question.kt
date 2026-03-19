package com.papito.simuladorprovas.model

data class Question(
    val id: Int,
    val pergunta: String,
    val opcaoA: String,
    val opcaoB: String,
    val opcaoC: String,
    val opcaoD: String,
    val correta: String,
    val textoReferencia: String? = null,
    var respostaDada: String? = null // Adicione este campo
)