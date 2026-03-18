import java.awt.*
import java.awt.event.ActionEvent
import java.net.URI
import javax.swing.*
import javax.swing.border.EmptyBorder
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.math.round

// ====== Data models (equivalentes aos dicionários Python) ======

data class Question(
    val id: Int,
    val pergunta: String,
    val opcaoA: String,
    val opcaoB: String,
    val opcaoC: String,
    val opcaoD: String,
    val correta: String,
    val linkConteudo: String?,
    val textoReferencia: String?,
    val materia: String?
)

data class QuestionOptions(
    val a: String,
    val b: String,
    val c: String,
    val d: String
)

data class QuestionResult(
    val pergunta: String,
    val correta: String,
    val respUsuario: String,
    val linkConteudo: String?,
    val opcoes: QuestionOptions,
    val correto: Boolean
)

// ====== Acesso a banco de dados (equivalente a database.py, mas apenas leitura usada pelo simulado) ======

private const val DEFAULT_DB_PATH = "questoes.db"

@Throws(SQLException::class)
fun getConnection(dbPath: String): Connection {
    // Garante que o driver JDBC do SQLite esteja disponível
    Class.forName("org.sqlite.JDBC")
    val url = "jdbc:sqlite:$dbPath"
    val conn = DriverManager.getConnection(url)
    criarTabelaQuestoesSeNecessario(conn)
    return conn
}

@Throws(SQLException::class)
private fun criarTabelaQuestoesSeNecessario(conn: Connection) {
    var stmt: java.sql.Statement? = null
    try {
        stmt = conn.createStatement()
        stmt.execute(
            """
            CREATE TABLE IF NOT EXISTS questoes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pergunta TEXT NOT NULL,
                opcao_a TEXT NOT NULL,
                opcao_b TEXT NOT NULL,
                opcao_c TEXT NOT NULL,
                opcao_d TEXT NOT NULL,
                correta TEXT NOT NULL,
                link_conteudo TEXT,
                texto_referencia TEXT,
                materia TEXT
            )
            """.trimIndent()
        )
    } finally {
        try {
            stmt?.close()
        } catch (_: Exception) {
        }
    }
}

@Throws(SQLException::class)
fun contarQuestoes(dbPath: String): Int {
    var conn: Connection? = null
    var stmt: java.sql.Statement? = null
    var rs: ResultSet? = null
    return try {
        conn = getConnection(dbPath)
        stmt = conn.createStatement()
        rs = stmt.executeQuery("SELECT COUNT(*) FROM questoes")
        if (rs.next()) rs.getInt(1) else 0
    } finally {
        try {
            rs?.close()
        } catch (_: Exception) {
        }
        try {
            stmt?.close()
        } catch (_: Exception) {
        }
        try {
            conn?.close()
        } catch (_: Exception) {
        }
    }
}

@Throws(SQLException::class)
fun listarQuestoes(dbPath: String): List<Question> {
    val questoes = mutableListOf<Question>()
    var conn: Connection? = null
    var stmt: java.sql.Statement? = null
    var rs: ResultSet? = null
    try {
        conn = getConnection(dbPath)
        stmt = conn.createStatement()
        rs = stmt.executeQuery(
            """
                SELECT
                    id, pergunta, opcao_a, opcao_b, opcao_c, opcao_d,
                    correta, link_conteudo, texto_referencia, materia
                FROM questoes
                ORDER BY id
                """.trimIndent()
        )
        while (rs.next()) {
            questoes.add(
                Question(
                    id = rs.getInt("id"),
                    pergunta = rs.getString("pergunta") ?: "",
                    opcaoA = rs.getString("opcao_a") ?: "",
                    opcaoB = rs.getString("opcao_b") ?: "",
                    opcaoC = rs.getString("opcao_c") ?: "",
                    opcaoD = rs.getString("opcao_d") ?: "",
                    correta = (rs.getString("correta") ?: "").trim(),
                    linkConteudo = rs.getString("link_conteudo"),
                    textoReferencia = rs.getString("texto_referencia"),
                    materia = rs.getString("materia")
                )
            )
        }
    } finally {
        try {
            rs?.close()
        } catch (_: Exception) {
        }
        try {
            stmt?.close()
        } catch (_: Exception) {
        }
        try {
            conn?.close()
        } catch (_: Exception) {
        }
    }
    return questoes
}

// ====== Lógica de negócio do simulado (equivalente a simulado_page.py) ======

fun calcularNota(acertos: Int, total: Int): Double {
    if (total == 0) return 0.0
    val nota = (acertos.toDouble() / total.toDouble()) * 10.0
    // arredonda para 2 casas decimais, como no Python (round(..., 2))
    return round(nota * 100.0) / 100.0
}

fun avaliarRespostas(
    questoes: List<Question>,
    respostas: Map<Int, String>
): Pair<Double, List<QuestionResult>> {
    var acertos = 0
    val resultados = mutableListOf<QuestionResult>()

    for (q in questoes) {
        val correta = q.correta.trim().toLowerCase()
        val respUsuario = (respostas[q.id] ?: "").trim().toLowerCase()
        val correto = respUsuario == correta
        if (correto) {
            acertos += 1
        }

        val opcoes = QuestionOptions(
            a = q.opcaoA,
            b = q.opcaoB,
            c = q.opcaoC,
            d = q.opcaoD
        )

        resultados.add(
            QuestionResult(
                pergunta = q.pergunta,
                correta = correta,
                respUsuario = respUsuario,
                linkConteudo = q.linkConteudo,
                opcoes = opcoes,
                correto = correto
            )
        )
    }

    val total = questoes.size
    val nota = calcularNota(acertos, total)
    return nota to resultados
}

// ====== UI Swing (Home + Simulado) ======

class ExamSimulatorApp : JFrame("Simulador de Provas") {

    private val cards = CardLayout()
    private val mainPanel = JPanel(cards)

    private val dbPathField = JTextField(DEFAULT_DB_PATH, 25)
    private val totalQuestoesLabel = JLabel("Total de questões: -")

    private val simuladoScroll = JScrollPane()
    private val resultadoTextArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    // Armazena as respostas selecionadas: questionId -> alternativa ("a"/"b"/"c"/"d")
    private val respostasSelecionadas: MutableMap<Int, String> = mutableMapOf()

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(900, 600)
        layout = BorderLayout()

        mainPanel.add(criarHomePanel(), "HOME")
        mainPanel.add(criarSimuladoPanel(), "SIMULADO")

        contentPane.add(mainPanel, BorderLayout.CENTER)

        // Começa na tela Home e tenta já ler o total de questões
        SwingUtilities.invokeLater {
            atualizarTotalQuestoes()
            mostrarHome()
        }
    }

    private fun mostrarHome() {
        cards.show(mainPanel, "HOME")
    }

    private fun mostrarSimulado() {
        cards.show(mainPanel, "SIMULADO")
    }

    private fun criarHomePanel(): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.border = EmptyBorder(20, 20, 20, 20)

        val titulo = JLabel("Simulador de Provas", SwingConstants.CENTER).apply {
            font = font.deriveFont(Font.BOLD, 26f)
        }

        val centerPanel = JPanel()
        centerPanel.layout = BoxLayout(centerPanel, BoxLayout.Y_AXIS)
        centerPanel.border = EmptyBorder(20, 20, 20, 20)

        val dbLabel = JLabel("Arquivo do banco de dados (SQLite):")
        val dbPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(dbLabel)
            add(dbPathField)
        }

        val atualizarDbButton = JButton("Atualizar contagem").apply {
            addActionListener {
                atualizarTotalQuestoes()
            }
        }

        val iniciarButton = JButton("Iniciar simulado").apply {
            font = font.deriveFont(Font.BOLD, 18f)
            addActionListener {
                iniciarSimulado()
            }
        }

        totalQuestoesLabel.font = totalQuestoesLabel.font.deriveFont(Font.PLAIN, 16f)

        centerPanel.add(Box.createVerticalStrut(10))
        centerPanel.add(dbPanel)
        centerPanel.add(atualizarDbButton)
        centerPanel.add(Box.createVerticalStrut(20))
        centerPanel.add(totalQuestoesLabel)
        centerPanel.add(Box.createVerticalStrut(30))
        centerPanel.add(iniciarButton)

        panel.add(titulo, BorderLayout.NORTH)
        panel.add(centerPanel, BorderLayout.CENTER)

        return panel
    }

    private fun criarSimuladoPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(10, 10, 10, 10)

        val topo = JPanel(BorderLayout())
        val titulo = JLabel("Simulado", SwingConstants.CENTER).apply {
            font = font.deriveFont(Font.BOLD, 22f)
        }
        val voltarButton = JButton("Voltar").apply {
            addActionListener {
                mostrarHome()
            }
        }
        topo.add(voltarButton, BorderLayout.WEST)
        topo.add(titulo, BorderLayout.CENTER)

        simuladoScroll.border = EmptyBorder(10, 10, 10, 10)

        val finalizarButton = JButton("Finalizar simulado").apply {
            font = font.deriveFont(Font.BOLD, 16f)
            addActionListener { finalizarSimulado() }
        }

        val resultadoPanel = JPanel(BorderLayout()).apply {
            border = EmptyBorder(10, 0, 0, 0)
            add(JLabel("Resultado e relatório:"), BorderLayout.NORTH)
            add(JScrollPane(resultadoTextArea), BorderLayout.CENTER)
        }

        val bottomPanel = JPanel()
        bottomPanel.layout = BoxLayout(bottomPanel, BoxLayout.Y_AXIS)
        bottomPanel.add(finalizarButton)
        bottomPanel.add(Box.createVerticalStrut(10))
        bottomPanel.add(resultadoPanel)

        panel.add(topo, BorderLayout.NORTH)
        panel.add(simuladoScroll, BorderLayout.CENTER)
        panel.add(bottomPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun atualizarTotalQuestoes() {
        val dbPath = dbPathField.text.trim().ifEmpty { DEFAULT_DB_PATH }
        try {
            val total = contarQuestoes(dbPath)
            totalQuestoesLabel.text = "Total de questões: $total"
        } catch (ex: Exception) {
            totalQuestoesLabel.text = "Total de questões: erro ao acessar banco"
            JOptionPane.showMessageDialog(
                this,
                "Erro ao contar questões:\n${ex.message}",
                "Erro",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun iniciarSimulado() {
        val dbPath = dbPathField.text.trim().ifEmpty { DEFAULT_DB_PATH }
        val questoes: List<Question> = try {
            listarQuestoes(dbPath)
        } catch (ex: Exception) {
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar questões:\n${ex.message}",
                "Erro",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        if (questoes.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Nenhuma questão cadastrada. Importe questões antes de realizar o simulado.",
                "Aviso",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }

        respostasSelecionadas.clear()

        val listaQuestoesPanel = JPanel()
        listaQuestoesPanel.layout = BoxLayout(listaQuestoesPanel, BoxLayout.Y_AXIS)
        listaQuestoesPanel.border = EmptyBorder(10, 10, 10, 10)

        questoes.forEachIndexed { index, q ->
            val questaoPanel = JPanel()
            questaoPanel.layout = BoxLayout(questaoPanel, BoxLayout.Y_AXIS)
            questaoPanel.border = EmptyBorder(10, 10, 10, 10)

            val titulo = JLabel("Questão ${index + 1}")
            titulo.font = titulo.font.deriveFont(Font.BOLD, 16f)
            questaoPanel.add(titulo)

            val textoRef = (q.textoReferencia ?: "").trim()
            if (textoRef.isNotEmpty()) {
                val refArea = JTextArea(textoRef).apply {
                    isEditable = false
                    lineWrap = true
                    wrapStyleWord = true
                    background = Color(245, 245, 245)
                }
                refArea.border = EmptyBorder(5, 5, 5, 5)
                questaoPanel.add(refArea)
            }

            val perguntaLabel = JLabel("<html>${q.pergunta.replace("\n", "<br/>")}</html>")
            questaoPanel.add(perguntaLabel)

            val link = (q.linkConteudo ?: "").trim()
            if (link.isNotEmpty()) {
                val abrirLinkButton = JButton("Abrir link").apply {
                    addActionListener {
                        abrirNoNavegador(link)
                    }
                }
                questaoPanel.add(abrirLinkButton)
            }

            val opcoes = QuestionOptions(
                a = q.opcaoA,
                b = q.opcaoB,
                c = q.opcaoC,
                d = q.opcaoD
            )

            val grupo = ButtonGroup()
            val painelOpcoes = JPanel()
            painelOpcoes.layout = BoxLayout(painelOpcoes, BoxLayout.Y_AXIS)

            fun criarRadio(letra: String, texto: String): JRadioButton {
                val radio = JRadioButton("$letra) $texto")
                radio.actionCommand = letra.toLowerCase()
                radio.addActionListener { e: ActionEvent ->
                    val escolha = e.actionCommand ?: ""
                    respostasSelecionadas[q.id] = escolha
                }
                grupo.add(radio)
                return radio
            }

            painelOpcoes.add(criarRadio("A", opcoes.a))
            painelOpcoes.add(criarRadio("B", opcoes.b))
            painelOpcoes.add(criarRadio("C", opcoes.c))
            painelOpcoes.add(criarRadio("D", opcoes.d))

            questaoPanel.add(painelOpcoes)
            questaoPanel.add(JSeparator())

            listaQuestoesPanel.add(questaoPanel)
            listaQuestoesPanel.add(Box.createVerticalStrut(10))
        }

        simuladoScroll.setViewportView(listaQuestoesPanel)
        resultadoTextArea.text = ""
        mostrarSimulado()
    }

    private fun finalizarSimulado() {
        val dbPath = dbPathField.text.trim().ifEmpty { DEFAULT_DB_PATH }

        val questoes: List<Question> = try {
            listarQuestoes(dbPath)
        } catch (ex: Exception) {
            JOptionPane.showMessageDialog(
                this,
                "Erro ao recarregar questões para correção:\n${ex.message}",
                "Erro",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        if (questoes.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Nenhuma questão cadastrada.",
                "Aviso",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }

        val (nota, resultados) = avaliarRespostas(questoes, respostasSelecionadas)

        val acertos = resultados.count { it.correto }
        val total = resultados.size

        val builder = StringBuilder()
        builder.append("Resultado do Simulado").append("\n")
        builder.append("Você acertou $acertos de $total questões.").append("\n")
        builder.append("Sua nota final foi: $nota / 10.").append("\n")
        builder.append("\n")
        builder.append("Relatório por questão").append("\n")
        builder.append("---------------------").append("\n")

        resultados.forEachIndexed { index, r ->
            val status = if (r.correto) "✅ Acertou" else "❌ Errou"
            builder.append("Questão ${index + 1}: $status").append("\n")
            builder.append(r.pergunta).append("\n")
            builder.append("Opções:").append("\n")
            builder.append(" - A) ${r.opcoes.a}").append("\n")
            builder.append(" - B) ${r.opcoes.b}").append("\n")
            builder.append(" - C) ${r.opcoes.c}").append("\n")
            builder.append(" - D) ${r.opcoes.d}").append("\n")
            if (r.respUsuario.isNotEmpty()) {
                builder.append("Sua resposta: ${r.respUsuario.toUpperCase()}").append("\n")
            } else {
                builder.append("Você não respondeu esta questão.").append("\n")
            }
            builder.append("Resposta correta: ${r.correta.toUpperCase()}").append("\n")
            val link = (r.linkConteudo ?: "").trim()
            if (link.isNotEmpty()) {
                builder.append("Conteúdo para estudo: $link").append("\n")
            }
            builder.append("\n")
        }

        resultadoTextArea.text = builder.toString()
        resultadoTextArea.caretPosition = 0
    }

    private fun abrirNoNavegador(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI(url))
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Ação de abrir navegador não suportada neste sistema.",
                        "Aviso",
                        JOptionPane.WARNING_MESSAGE
                    )
                }
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Desktop não suportado para abrir URLs.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        } catch (ex: Exception) {
            JOptionPane.showMessageDialog(
                this,
                "Não foi possível abrir o link:\n${ex.message}",
                "Erro",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}

// ====== Ponto de entrada ======

fun main() {
    // Garante que o AWT não rode em modo headless
    System.setProperty("java.awt.headless", "false")

    SwingUtilities.invokeLater {
        try {
            // Tenta aplicar um look and feel mais moderno, se disponível
            for (info in UIManager.getInstalledLookAndFeels()) {
                if (info.name.toLowerCase().contains("nimbus")) {
                    UIManager.setLookAndFeel(info.className)
                    break
                }
            }
        } catch (_: Exception) {
            // mantém o padrão se não conseguir aplicar
        }

        val app = ExamSimulatorApp()
        app.setLocationRelativeTo(null)
        app.isVisible = true
    }
}

