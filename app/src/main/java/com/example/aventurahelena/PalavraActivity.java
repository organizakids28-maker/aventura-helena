package com.example.aventurahelena;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PalavraActivity — Jogo "Complete a Palavra" para a Aventura da Helena.
 *
 * 15 palavras no total. Para cada palavra, a criança vê a palavra com uma letra
 * faltando e 4 opções de resposta. Navega com o D-pad e confirma com OK.
 *
 * Ao completar todas: ganha XP e stat de Inteligência.
 */
public class PalavraActivity extends Activity {

    private static final int XP_GANHO       = 50;
    private static final int STAT_INT_GANHO = 4;
    private static final int DELAY_PROXIMO  = 1200;

    // Banco de palavras: {lacuna, dica, op0, op1, op2, op3, indiceCorreto}
    private static final String[][] BANCO = {
        {"G_TO",    "Animal que mia",              "GATO",   "PATO",   "RATO",   "BOLO",   "0"},
        {"C_SA",    "Onde moramos",                "BASA",   "CASA",   "MADA",   "FASA",   "1"},
        {"B_LA",    "Brinquedo redondo",            "ROLA",   "COLA",   "BOLA",   "MOLA",   "2"},
        {"S_L",     "Brilha no ceu",               "SAL",    "SIL",    "SOR",    "SOL",    "3"},
        {"LI_RO",   "Para ler",                    "LIMAO",  "LITRO",  "LINHO",  "LIVRO",  "3"},
        {"AM_GO",   "Pessoa amiga",                "AMIGO",  "AMIDA",  "AMINA",  "AMILO",  "0"},
        {"_EIXE",   "Vive na agua",                "FEICE",  "DEIXE",  "PEIXE",  "MEICE",  "2"},
        {"_OVO",    "Contrario de velho",          "COVO",   "NOVO",   "ROVO",   "POVO",   "1"},
        {"P_O",     "Alimento de farinha",         "PAU",    "PAI",    "PAO",    "PAZ",    "2"},
        {"LE_TE",   "Bebida branca",               "LESTE",  "LENTE",  "LEITE",  "LEVE",   "2"},
        {"BO_O",    "Bolo de aniversario",         "BOSO",   "BOLO",   "BORO",   "BOCO",   "1"},
        {"FL_R",    "Planta bonita",               "FLOU",   "FLOX",   "FLON",   "FLOR",   "3"},
        {"_SCOLA",  "Onde estudamos",              "ESCOBA", "ESCUTA", "ESCOLA", "ESCURA", "2"},
        {"JAN_LA",  "Abertura na parede",          "JANILA", "JANOLA", "JANULA", "JANELA", "3"},
        {"AM_R",    "Sentimento muito bom",        "AMOR",   "ABOR",   "ACOR",   "ADOR",   "0"},
    };

    // Ordem embaralhada das perguntas
    private List<Integer> ordemPerguntas;
    private int perguntaAtual = 0;
    private int acertos = 0;
    private boolean aguardandoProxima = false;

    private TextView tvProgresso;
    private TextView tvDica;
    private TextView tvPalavra;
    private TextView tvFeedback;
    private Button[] btnOpcoes;

    private Handler handler;
    private PerfilHelena perfil;

    // Para navegação D-pad entre as 4 opções
    private int opcaoFocada = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palavra);

        perfil  = new PerfilHelena(this);
        handler = new Handler();

        tvProgresso = (TextView) findViewById(R.id.tv_progresso);
        tvDica      = (TextView) findViewById(R.id.tv_dica);
        tvPalavra   = (TextView) findViewById(R.id.tv_palavra);
        tvFeedback  = (TextView) findViewById(R.id.tv_feedback);

        btnOpcoes = new Button[]{
            (Button) findViewById(R.id.btn_op0),
            (Button) findViewById(R.id.btn_op1),
            (Button) findViewById(R.id.btn_op2),
            (Button) findViewById(R.id.btn_op3)
        };

        // Configura listeners das opções
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            btnOpcoes[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    verificarResposta(idx);
                }
            });
            btnOpcoes[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) opcaoFocada = idx;
                }
            });
        }

        // Embaralha as perguntas
        ordemPerguntas = new ArrayList<Integer>();
        for (int i = 0; i < BANCO.length; i++) ordemPerguntas.add(i);
        Collections.shuffle(ordemPerguntas);

        mostrarPergunta();
    }

    /**
     * Exibe a pergunta atual na tela.
     */
    private void mostrarPergunta() {
        if (perguntaAtual >= BANCO.length) {
            mostrarResultadoFinal();
            return;
        }

        aguardandoProxima = false;
        opcaoFocada = 0;

        int idx = ordemPerguntas.get(perguntaAtual);
        String[] dados = BANCO[idx];

        String lacuna = dados[0];
        String dica   = dados[1];

        // Formata a lacuna para exibição (espaço entre cada letra)
        StringBuilder sb = new StringBuilder();
        for (char c : lacuna.toCharArray()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(c);
        }

        tvProgresso.setText("Pergunta " + (perguntaAtual + 1) + " de " + BANCO.length);
        tvDica.setText(dica);
        tvPalavra.setText(sb.toString());
        tvFeedback.setText("");

        // Preenche as 4 opções
        for (int i = 0; i < 4; i++) {
            btnOpcoes[i].setText(dados[2 + i]);
            btnOpcoes[i].setEnabled(true);
            btnOpcoes[i].setTextColor(0xFFFFFFFF);
            btnOpcoes[i].setBackgroundResource(R.drawable.btn_action);
        }

        // Coloca foco na primeira opção
        btnOpcoes[0].requestFocus();
    }

    /**
     * Verifica a resposta escolhida.
     */
    private void verificarResposta(int opcaoEscolhida) {
        if (aguardandoProxima) return;

        int idx = ordemPerguntas.get(perguntaAtual);
        String[] dados = BANCO[idx];
        int correta = Integer.parseInt(dados[6]);

        // Desabilita todos os botões
        for (Button btn : btnOpcoes) btn.setEnabled(false);

        if (opcaoEscolhida == correta) {
            acertos++;
            tvFeedback.setText("Correto! Muito bem!");
            tvFeedback.setTextColor(0xFF4CAF50);
        } else {
            tvFeedback.setText("Ops! Era: " + dados[2 + correta]);
            tvFeedback.setTextColor(0xFFEF5350);
        }

        aguardandoProxima = true;
        perguntaAtual++;

        // Avança para a próxima pergunta após delay
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mostrarPergunta();
            }
        }, DELAY_PROXIMO);
    }

    /**
     * Mostra o resultado final ao completar todas as 15 perguntas.
     */
    private void mostrarResultadoFinal() {
        // Salva progresso (só uma vez por dia)
        if (!perfil.isPalavrasConcluida()) {
            perfil.addXP(XP_GANHO);
            perfil.addStatInteligencia(STAT_INT_GANHO);
            perfil.setPalavrasConcluida();
        }

        String mensagem =
            "Voce respondeu " + acertos + " de " + BANCO.length + " corretamente!\n\n"
            + "+ " + XP_GANHO + " XP\n"
            + "+ " + STAT_INT_GANHO + " Inteligencia\n\n";

        if (acertos >= 12) {
            mensagem += "Incrivel! Voce eh uma expert em palavras!";
        } else if (acertos >= 8) {
            mensagem += "Muito bem! Continue praticando!";
        } else {
            mensagem += "Bom esforco! Tente novamente amanha!";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Parabens, Helena!");
        builder.setMessage(mensagem);
        builder.setCancelable(false);

        builder.setNegativeButton("Voltar ao Hub", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        builder.show();
    }

    /**
     * Navegação D-pad entre as 4 opções de resposta.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (aguardandoProxima) return true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (opcaoFocada < 3) {
                    opcaoFocada++;
                    btnOpcoes[opcaoFocada].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (opcaoFocada > 0) {
                    opcaoFocada--;
                    btnOpcoes[opcaoFocada].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                verificarResposta(opcaoFocada);
                return true;

            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
