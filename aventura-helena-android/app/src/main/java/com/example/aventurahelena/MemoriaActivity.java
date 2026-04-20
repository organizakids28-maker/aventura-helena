package com.example.aventurahelena;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoriaActivity extends Activity {

    /* ─── Modos de dificuldade ─────────────────────────────── */
    private static final int MODO_4X4 = 0;
    private static final int MODO_6X6 = 1;
    private static final int MODO_8X8 = 2;

    /* ─── Banco de palavras (32 pares) ─────────────────────── */
    private static final String[] PALAVRAS = {
        "SOL",   "LUA",   "GATO",  "CAO",
        "SAPO",  "REI",   "PEIXE", "FLOR",
        "BOLO",  "BOLA",  "RATO",  "COPA",
        "FOGO",  "AGUA",  "ARCO",  "ILHA",
        "NAVIO", "AVIAO", "TREM",  "CARRO",
        "NINHO", "OVO",   "ASA",   "DENTE",
        "OLHO",  "MAO",   "LUZ",   "SOM",
        "VOZ",   "RIO",   "MAR",   "CEU"
    };

    /* ─── Constantes fixas ─────────────────────────────────── */
    private static final int DELAY_FECHAR    = 1100;
    private static final int XP_GANHO        = 60;
    private static final int STAT_INT_GANHO  = 3;
    private static final int STAT_FOC_GANHO  = 3;

    /* ─── Configuração da dificuldade atual ────────────────── */
    private int modoAtual   = MODO_6X6;
    private int colunas     = 6;
    private int linhas      = 6;
    private int totalCartas = 36;
    private int totalPares  = 18;
    private int tamTextoQ   = 18; // "?" fechada
    private int tamTextoPal = 12; // palavra aberta
    private int margemCard  = 5;  // dp

    /* ─── Estado do jogo ───────────────────────────────────── */
    private List<Integer> listaValores;
    private Button[]      botoes;
    private int  indicePrimeira  = -1;
    private int  indiceSegunda   = -1;
    private int  paresEncontrados = 0;
    private boolean bloqueado    = false;
    private int  indiceFocado    = 0;

    /* ─── Views ────────────────────────────────────────────── */
    private LinearLayout llSelecao, llJogo, gridCartas;
    private TextView     tvPlacar, tvTitulo;
    private Button       btn4x4, btn6x6, btn8x8;
    private int          selecaoFocada = 0; // 0=4x4, 1=6x6, 2=8x8

    /* ─── Utilitários ──────────────────────────────────────── */
    private Handler      handler;
    private PerfilHelena perfil;
    private SoundManager sound;
    private boolean      naSelecao = true;

    // ═══════════════════════════════════════════════════════════
    // CICLO DE VIDA
    // ═══════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memoria);

        perfil  = new PerfilHelena(this);
        handler = new Handler();
        sound   = new SoundManager();

        llSelecao  = (LinearLayout) findViewById(R.id.ll_selecao);
        llJogo     = (LinearLayout) findViewById(R.id.ll_jogo);
        gridCartas = (LinearLayout) findViewById(R.id.grid_cartas);
        tvPlacar   = (TextView) findViewById(R.id.tv_placar);
        tvTitulo   = (TextView) findViewById(R.id.tv_titulo_memoria);
        btn4x4     = (Button) findViewById(R.id.btn_4x4);
        btn6x6     = (Button) findViewById(R.id.btn_6x6);
        btn8x8     = (Button) findViewById(R.id.btn_8x8);

        btn4x4.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { iniciarJogo(MODO_4X4); }
        });
        btn6x6.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { iniciarJogo(MODO_6X6); }
        });
        btn8x8.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { iniciarJogo(MODO_8X8); }
        });

        btn4x4.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) { if (f) selecaoFocada = 0; }
        });
        btn6x6.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) { if (f) selecaoFocada = 1; }
        });
        btn8x8.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean f) { if (f) selecaoFocada = 2; }
        });

        mostrarSelecao();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sound.release();
    }

    // ═══════════════════════════════════════════════════════════
    // TELA DE SELEÇÃO
    // ═══════════════════════════════════════════════════════════

    private void mostrarSelecao() {
        naSelecao = true;
        llSelecao.setVisibility(View.VISIBLE);
        llJogo.setVisibility(View.GONE);
        selecaoFocada = 0;
        btn4x4.requestFocus();
        AnimHelper.fadeIn(llSelecao, 300);
    }

    private Button getBotaoSelecao(int idx) {
        if (idx == 0) return btn4x4;
        if (idx == 1) return btn6x6;
        return btn8x8;
    }

    // ═══════════════════════════════════════════════════════════
    // INICIAR JOGO COM DIFICULDADE
    // ═══════════════════════════════════════════════════════════

    private void iniciarJogo(int modo) {
        naSelecao = false;
        modoAtual = modo;

        switch (modo) {
            case MODO_4X4:
                colunas = 4; linhas = 4;
                totalPares = 8;  totalCartas = 16;
                tamTextoQ = 22;  tamTextoPal = 16;
                margemCard = 8;
                tvTitulo.setText("Memoria - Facil (4x4)");
                break;
            case MODO_8X8:
                colunas = 6; linhas = 6;
                totalPares = 18; totalCartas = 36;
                tamTextoQ = 15;  tamTextoPal = 10;
                margemCard = 5;
                tvTitulo.setText("Memoria - Dificil (6x6)");
                break;
            default:
                colunas = 6; linhas = 4;
                totalPares = 12; totalCartas = 24;
                tamTextoQ = 17;  tamTextoPal = 12;
                margemCard = 6;
                tvTitulo.setText("Memoria - Medio (4x6)");
                break;
        }

        llSelecao.setVisibility(View.GONE);
        llJogo.setVisibility(View.VISIBLE);

        paresEncontrados = 0;
        indicePrimeira   = -1;
        indiceSegunda    = -1;
        bloqueado        = false;
        indiceFocado     = 0;

        inicializarCartas();
        criarBotoes();
        atualizarPlacar();

        botoes[0].requestFocus();
        AnimHelper.fadeIn(gridCartas, 400);
    }

    // ═══════════════════════════════════════════════════════════
    // LÓGICA DO JOGO
    // ═══════════════════════════════════════════════════════════

    private void inicializarCartas() {
        listaValores = new ArrayList<Integer>();
        for (int i = 0; i < totalPares; i++) {
            listaValores.add(i);
            listaValores.add(i);
        }
        Collections.shuffle(listaValores);
    }

    private void criarBotoes() {
        gridCartas.removeAllViews();
        botoes = new Button[totalCartas];
        int margPx = dp(margemCard);

        for (int row = 0; row < linhas; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0);
            rowParams.weight = 1f;
            rowLayout.setLayoutParams(rowParams);

            for (int col = 0; col < colunas; col++) {
                final int indice = row * colunas + col;

                Button btn = new Button(this);
                btn.setText("?");
                btn.setTextSize(tamTextoQ);
                btn.setTextColor(0xFFFFFFFF);
                btn.setBackgroundResource(R.drawable.btn_selector);
                btn.setFocusable(true);
                btn.setFocusableInTouchMode(true);

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT);
                btnParams.weight = 1f;
                btnParams.setMargins(margPx, margPx, margPx, margPx);
                btn.setLayoutParams(btnParams);

                btn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        indiceFocado = indice;
                        processarClique(indice);
                    }
                });
                btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) indiceFocado = indice;
                    }
                });

                rowLayout.addView(btn);
                botoes[indice] = btn;
            }

            gridCartas.addView(rowLayout);
        }
    }

    private void processarClique(int indice) {
        if (bloqueado) return;
        if (!botoes[indice].isEnabled()) return;
        if (!"?".equals(botoes[indice].getText().toString())) return;

        final int valorCarta  = listaValores.get(indice);
        final String palavra  = PALAVRAS[valorCarta];
        final Button carta    = botoes[indice];
        final int tamPal      = tamTextoPal;

        sound.playCartaVirou();
        AnimHelper.flipCarta(carta, new AnimHelper.OnHalfFlip() {
            @Override public void onHalf() {
                carta.setText(palavra);
                carta.setTextSize(tamPal);
            }
        });

        if (indicePrimeira == -1) {
            indicePrimeira = indice;
        } else {
            indiceSegunda = indice;
            bloqueado = true;
            handler.postDelayed(new Runnable() {
                @Override public void run() { verificarPar(); }
            }, 380);
        }
    }

    private void verificarPar() {
        int val1 = listaValores.get(indicePrimeira);
        int val2 = listaValores.get(indiceSegunda);

        final int idx1 = indicePrimeira;
        final int idx2 = indiceSegunda;
        resetarSelecao();

        if (val1 == val2) {
            sound.playAcerto();
            AnimHelper.pulseGold(botoes[idx1]);
            AnimHelper.pulseGold(botoes[idx2]);

            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    botoes[idx1].setEnabled(false);
                    botoes[idx2].setEnabled(false);
                    paresEncontrados++;
                    atualizarPlacar();
                    if (paresEncontrados == totalPares) mostrarVitoria();
                }
            }, 200);

        } else {
            sound.playErro();
            AnimHelper.shake(botoes[idx1]);
            AnimHelper.shake(botoes[idx2]);

            final int tamQ = tamTextoQ;
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    AnimHelper.flipCarta(botoes[idx1], new AnimHelper.OnHalfFlip() {
                        @Override public void onHalf() {
                            botoes[idx1].setText("?");
                            botoes[idx1].setTextSize(tamQ);
                        }
                    });
                    AnimHelper.flipCarta(botoes[idx2], new AnimHelper.OnHalfFlip() {
                        @Override public void onHalf() {
                            botoes[idx2].setText("?");
                            botoes[idx2].setTextSize(tamQ);
                        }
                    });
                    bloqueado = false;
                }
            }, DELAY_FECHAR);
        }
    }

    private void resetarSelecao() {
        indicePrimeira = -1;
        indiceSegunda  = -1;
        bloqueado      = false;
    }

    private void atualizarPlacar() {
        tvPlacar.setText("Pares: " + paresEncontrados + " / " + totalPares);
    }

    private void mostrarVitoria() {
        if (!perfil.isMemoriaConcluida()) {
            perfil.addXP(XP_GANHO);
            perfil.addStatInteligencia(STAT_INT_GANHO);
            perfil.addStatFoco(STAT_FOC_GANHO);
            perfil.setMemoriaConcluida();
            sound.playVitoria();
            if (perfil.getNivel() > 1) sound.playNivelUp();
        } else {
            sound.playVitoria();
        }

        AnimHelper.celebracao(tvPlacar);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Parabens, Helena!");
        builder.setMessage(
            "Voce encontrou todos os " + totalPares + " pares!\n\n"
            + "+ " + XP_GANHO + " XP\n"
            + "+ " + STAT_INT_GANHO + " Inteligencia\n"
            + "+ " + STAT_FOC_GANHO + " Foco\n\n"
            + "Otimo trabalho!"
        );
        builder.setCancelable(false);

        builder.setPositiveButton("Jogar de Novo", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                mostrarSelecao();
            }
        });
        builder.setNegativeButton("Voltar ao Hub", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!isFinishing()) builder.show();
            }
        }, 600);
    }

    // ═══════════════════════════════════════════════════════════
    // NAVEGAÇÃO D-PAD
    // ═══════════════════════════════════════════════════════════

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (naSelecao) {
            return navegarSelecao(keyCode, event);
        }
        return navegarJogo(keyCode, event);
    }

    private boolean navegarSelecao(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (selecaoFocada < 2) {
                    selecaoFocada++;
                    getBotaoSelecao(selecaoFocada).requestFocus();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (selecaoFocada > 0) {
                    selecaoFocada--;
                    getBotaoSelecao(selecaoFocada).requestFocus();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                iniciarJogo(selecaoFocada);
                return true;
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private boolean navegarJogo(int keyCode, KeyEvent event) {
        int novoIndice = indiceFocado;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if ((indiceFocado % colunas) < colunas - 1)
                    novoIndice = indiceFocado + 1;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if ((indiceFocado % colunas) > 0)
                    novoIndice = indiceFocado - 1;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (indiceFocado + colunas < totalCartas)
                    novoIndice = indiceFocado + colunas;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (indiceFocado - colunas >= 0)
                    novoIndice = indiceFocado - colunas;
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                processarClique(indiceFocado);
                return true;
            case KeyEvent.KEYCODE_BACK:
                mostrarSelecao();
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

    // ═══════════════════════════════════════════════════════════
    // UTILITÁRIOS
    // ═══════════════════════════════════════════════════════════

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
