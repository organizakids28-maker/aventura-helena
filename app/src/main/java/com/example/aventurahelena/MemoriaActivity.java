package com.example.aventurahelena;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MemoriaActivity — Jogo da Memória com emojis para a Aventura da Helena.
 *
 * Grade 4x6 com 24 cartas (12 pares).
 * Ao completar: ganha XP, sobe stat de Inteligência e Foco, e marca o mini-jogo como concluído.
 *
 * Navegação D-pad:
 * - Setas: movem o foco entre cartas
 * - OK/Enter: vira a carta selecionada
 * - Voltar: sai do jogo (volta ao hub)
 */
public class MemoriaActivity extends Activity {

    private static final int TOTAL_CARTAS = 24;
    private static final int COLUNAS = 6;
    private static final int LINHAS  = 4;
    private static final int TOTAL_PARES = 12;
    private static final int DELAY_FECHAR = 1000;

    // XP e stats ganhos ao completar
    private static final int XP_GANHO = 60;
    private static final int STAT_INT_GANHO = 3;
    private static final int STAT_FOC_GANHO = 3;

    // 12 pares de emojis divertidos para crianças
    private static final String[] EMOJIS = {
        "\u2B50", // ⭐
        "\uD83C\uDF08", // 🌈
        "\uD83D\uDC31", // 🐱
        "\uD83D\uDC36", // 🐶
        "\uD83D\uDC38", // 🐸
        "\uD83E\uDD84", // 🦄
        "\uD83C\uDF79", // 🎵 (actually 🎹)
        "\uD83C\uDF80", // 🎀
        "\uD83C\uDF55", // 🍕
        "\uD83C\uDF6D", // 🍭
        "\uD83C\uDFF5", // 🌺
        "\uD83C\uDFC6"  // 🏆
    };

    private List<Integer> listaValores;
    private Button[] botoes;
    private int indicePrimeira = -1;
    private int indiceSegunda  = -1;
    private int paresEncontrados = 0;
    private boolean bloqueado = false;
    private Handler handler;
    private TextView tvPlacar;
    private int indiceFocado = 0;

    private PerfilHelena perfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memoria);

        perfil  = new PerfilHelena(this);
        handler = new Handler();
        tvPlacar = (TextView) findViewById(R.id.tv_placar);

        TextView tvTitulo = (TextView) findViewById(R.id.tv_titulo_memoria);
        tvTitulo.setText("Jogo da Memoria");

        inicializarCartas();
        criarBotoes();
        atualizarPlacar();

        botoes[indiceFocado].requestFocus();
    }

    /**
     * Cria os 12 pares (cada emoji aparece 2 vezes) e embaralha.
     */
    private void inicializarCartas() {
        listaValores = new ArrayList<Integer>();
        for (int i = 0; i < TOTAL_PARES; i++) {
            listaValores.add(i);
            listaValores.add(i);
        }
        Collections.shuffle(listaValores);
    }

    /**
     * Cria os 24 botões no GridLayout, um por carta.
     */
    private void criarBotoes() {
        GridLayout grid = (GridLayout) findViewById(R.id.grid_cartas);
        botoes = new Button[TOTAL_CARTAS];

        for (int i = 0; i < TOTAL_CARTAS; i++) {
            Button btn = new Button(this);
            btn.setText("?");
            btn.setTextSize(22);
            btn.setTextColor(0xFFFFFFFF);
            btn.setBackgroundResource(R.drawable.btn_selector);
            btn.setFocusable(true);
            btn.setFocusableInTouchMode(true);

            final int indice = i;
            btn.setTag(indice);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width  = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(i % COLUNAS, 1f);
            params.rowSpec    = GridLayout.spec(i / COLUNAS, 1f);
            params.setMargins(6, 6, 6, 6);
            btn.setLayoutParams(params);

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    indiceFocado = indice;
                    processarClique(indice);
                }
            });

            btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) indiceFocado = indice;
                }
            });

            grid.addView(btn);
            botoes[i] = btn;
        }
    }

    /**
     * Navegação pelo controle remoto.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int novoIndice = indiceFocado;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if ((indiceFocado % COLUNAS) < COLUNAS - 1)
                    novoIndice = indiceFocado + 1;
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if ((indiceFocado % COLUNAS) > 0)
                    novoIndice = indiceFocado - 1;
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (indiceFocado + COLUNAS < TOTAL_CARTAS)
                    novoIndice = indiceFocado + COLUNAS;
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (indiceFocado - COLUNAS >= 0)
                    novoIndice = indiceFocado - COLUNAS;
                break;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                processarClique(indiceFocado);
                return true;

            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }

        if (novoIndice != indiceFocado) {
            indiceFocado = novoIndice;
            botoes[indiceFocado].requestFocus();
        }
        return true;
    }

    /**
     * Lógica de virar cartas e verificar par.
     */
    private void processarClique(int indice) {
        if (bloqueado) return;
        if (!botoes[indice].isEnabled()) return; // carta já encontrada
        if (!"?".equals(botoes[indice].getText().toString())) return; // já virada neste turno

        // Vira a carta
        int valorCarta = listaValores.get(indice);
        String emoji = EMOJIS[valorCarta];
        botoes[indice].setText(emoji);

        if (indicePrimeira == -1) {
            indicePrimeira = indice;
        } else {
            indiceSegunda = indice;
            bloqueado = true;
            verificarPar();
        }
    }

    /**
     * Verifica se as duas cartas viradas formam um par.
     */
    private void verificarPar() {
        int val1 = listaValores.get(indicePrimeira);
        int val2 = listaValores.get(indiceSegunda);

        final int idx1 = indicePrimeira;
        final int idx2 = indiceSegunda;

        resetarSelecao();

        if (val1 == val2) {
            // Par encontrado!
            botoes[idx1].setEnabled(false);
            botoes[idx2].setEnabled(false);
            paresEncontrados++;
            atualizarPlacar();

            if (paresEncontrados == TOTAL_PARES) {
                mostrarVitoria();
            }
        } else {
            // Par errado — fecha após delay
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    botoes[idx1].setText("?");
                    botoes[idx2].setText("?");
                    bloqueado = false;
                }
            }, DELAY_FECHAR);
        }
    }

    private void resetarSelecao() {
        indicePrimeira = -1;
        indiceSegunda  = -1;
        bloqueado = false;
    }

    private void atualizarPlacar() {
        tvPlacar.setText("Pares: " + paresEncontrados + " / " + TOTAL_PARES);
    }

    /**
     * Quando o jogador encontra todos os pares, salva o progresso e mostra o diálogo de vitória.
     */
    private void mostrarVitoria() {
        // Salva progresso (só uma vez por dia)
        if (!perfil.isMemoriaConcluida()) {
            perfil.addXP(XP_GANHO);
            perfil.addStatInteligencia(STAT_INT_GANHO);
            perfil.addStatFoco(STAT_FOC_GANHO);
            perfil.setMemoriaConcluida();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Parabens, Helena!");
        builder.setMessage(
            "Voce encontrou todos os " + TOTAL_PARES + " pares!\n\n"
            + "+ " + XP_GANHO + " XP\n"
            + "+ " + STAT_INT_GANHO + " Inteligencia\n"
            + "+ " + STAT_FOC_GANHO + " Foco\n\n"
            + "Otimo trabalho!"
        );
        builder.setCancelable(false);

        builder.setPositiveButton("Jogar de Novo", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                reiniciarJogo();
            }
        });

        builder.setNegativeButton("Voltar ao Hub", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        builder.show();
    }

    /**
     * Reinicia o jogo com novo embaralhamento.
     */
    private void reiniciarJogo() {
        paresEncontrados = 0;
        indicePrimeira   = -1;
        indiceSegunda    = -1;
        bloqueado = false;
        indiceFocado = 0;

        inicializarCartas();

        for (int i = 0; i < TOTAL_CARTAS; i++) {
            botoes[i].setText("?");
            botoes[i].setEnabled(true);
        }

        atualizarPlacar();
        botoes[0].requestFocus();
    }
}
