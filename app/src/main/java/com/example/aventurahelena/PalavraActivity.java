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

public class PalavraActivity extends Activity {

    private static final int XP_GANHO       = 50;
    private static final int STAT_INT_GANHO = 4;
    private static final int DELAY_PROXIMO  = 1300;

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
    private SoundManager sound;

    private int opcaoFocada = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palavra);

        perfil  = new PerfilHelena(this);
        handler = new Handler();
        sound   = new SoundManager();

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

        ordemPerguntas = new ArrayList<Integer>();
        for (int i = 0; i < BANCO.length; i++) ordemPerguntas.add(i);
        Collections.shuffle(ordemPerguntas);

        // Animação de entrada
        View raiz = findViewById(android.R.id.content);
        AnimHelper.fadeIn(tvPalavra, 350);

        mostrarPergunta();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sound.release();
    }

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
        StringBuilder sb = new StringBuilder();
        for (char c : lacuna.toCharArray()) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(c);
        }

        tvProgresso.setText("Pergunta " + (perguntaAtual + 1) + " de " + BANCO.length);
        tvDica.setText(dados[1]);
        tvFeedback.setText("");
        tvFeedback.setTextColor(0xFFCE93D8);

        // Animação na palavra quando muda
        AnimHelper.fadeIn(tvPalavra, 250);
        tvPalavra.setText(sb.toString());

        for (int i = 0; i < 4; i++) {
            btnOpcoes[i].setText(dados[2 + i]);
            btnOpcoes[i].setEnabled(true);
            btnOpcoes[i].setTextColor(0xFFFFFFFF);
            btnOpcoes[i].setBackgroundResource(R.drawable.btn_action);
            // Botões aparecem escalonados
            AnimHelper.zoomEntrada(btnOpcoes[i], i * 60);
        }

        btnOpcoes[0].requestFocus();
    }

    private void verificarResposta(int opcaoEscolhida) {
        if (aguardandoProxima) return;

        int idx = ordemPerguntas.get(perguntaAtual);
        String[] dados = BANCO[idx];
        int correta = Integer.parseInt(dados[6]);

        for (Button btn : btnOpcoes) btn.setEnabled(false);

        if (opcaoEscolhida == correta) {
            acertos++;
            tvFeedback.setText("\u2705  Correto!  Muito bem!");
            tvFeedback.setTextColor(0xFF4CAF50);

            // Som + animação de acerto
            sound.playAcerto();
            AnimHelper.pulseGold(btnOpcoes[opcaoEscolhida]);
            AnimHelper.pulseGold(tvPalavra);

        } else {
            tvFeedback.setText("\u274C  Ops! Era: " + dados[2 + correta]);
            tvFeedback.setTextColor(0xFFEF5350);

            // Som + shake na palavra + flash no botão errado
            sound.playErro();
            AnimHelper.shake(tvPalavra);
            AnimHelper.flashRed(btnOpcoes[opcaoEscolhida]);
        }

        // Animação no feedback
        AnimHelper.fadeIn(tvFeedback, 200);

        aguardandoProxima = true;
        perguntaAtual++;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mostrarPergunta();
            }
        }, DELAY_PROXIMO);
    }

    private void mostrarResultadoFinal() {
        if (!perfil.isPalavrasConcluida()) {
            perfil.addXP(XP_GANHO);
            perfil.addStatInteligencia(STAT_INT_GANHO);
            perfil.setPalavrasConcluida();
            sound.playVitoria();
            if (perfil.getNivel() > 1) {
                handler.postDelayed(new Runnable() {
                    @Override public void run() { sound.playNivelUp(); }
                }, 700);
            }
        } else {
            sound.playXPGanho();
        }

        String mensagem =
            "Voce respondeu " + acertos + " de " + BANCO.length + " corretamente!\n\n"
            + "+ " + XP_GANHO + " XP\n"
            + "+ " + STAT_INT_GANHO + " Inteligencia\n\n";

        if (acertos >= 12) {
            mensagem += "\uD83C\uDFC6 Incrivel! Voce eh uma expert em palavras!";
        } else if (acertos >= 8) {
            mensagem += "\uD83D\uDC4D Muito bem! Continue praticando!";
        } else {
            mensagem += "\uD83D\uDCAA Bom esforco! Tente novamente amanha!";
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Parabens, Helena!");
        builder.setMessage(mensagem);
        builder.setCancelable(false);

        builder.setNegativeButton("Voltar ao Hub", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) builder.show();
            }
        }, 400);
    }

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
