package com.example.aventurahelena;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * HubActivity — Tela principal da Aventura da Helena.
 *
 * Mostra o perfil da Helena (XP, nível, stats) e os cards coloridos
 * para cada mini-jogo (azul=Memória, roxo=Palavras, laranja=Tarefas).
 *
 * Navegação D-pad:
 * - Esquerda/Direita: navega entre os 3 cards de jogos (linha de cima)
 * - Baixo: vai para o card da Batalha (quando disponível), depois versão/admin
 * - Cima: volta para os cards de jogos
 * - OK/Enter: entra no jogo selecionado
 *
 * Acesso admin: navegue até o número de versão (canto inferior dir.) e pressione OK
 */
public class HubActivity extends Activity {

    private static final int TOTAL_TAREFAS = 10;

    private PerfilHelena perfil;
    private int xpPctAnterior = 0;

    // Views do perfil
    private TextView tvNivel;
    private TextView tvXP;
    private ProgressBar pbXP;
    private TextView tvInt;
    private TextView tvFoc;
    private TextView tvRes;
    private TextView tvVersao;

    // Views dos cards
    private TextView tvSubTarefas;
    private TextView tvChecklist;
    private TextView tvChecklistTitulo;
    private LinearLayout cardBatalha;

    // Cards navegáveis: 0=Memória, 1=Palavras, 2=Tarefas, 3=Batalha
    private View[] cards;
    private int indiceFocado = 0;

    // Controle de linha de foco: 0=cards superiores, 1=batalha, 2=versao/admin
    private int linhaAtual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);

        perfil = new PerfilHelena(this);

        // Referências às views do perfil
        tvNivel   = (TextView)    findViewById(R.id.tv_nivel);
        tvXP      = (TextView)    findViewById(R.id.tv_xp);
        pbXP      = (ProgressBar) findViewById(R.id.pb_xp);
        tvInt     = (TextView)    findViewById(R.id.tv_int);
        tvFoc     = (TextView)    findViewById(R.id.tv_foc);
        tvRes     = (TextView)    findViewById(R.id.tv_res);
        tvVersao  = (TextView)    findViewById(R.id.tv_versao);

        // Referências dos cards e textos dinâmicos
        tvSubTarefas       = (TextView)      findViewById(R.id.tv_sub_tarefas);
        tvChecklist        = (TextView)      findViewById(R.id.tv_checklist);
        tvChecklistTitulo  = (TextView)      findViewById(R.id.tv_checklist_titulo);
        cardBatalha        = (LinearLayout)  findViewById(R.id.card_batalha);

        LinearLayout cardMemoria  = (LinearLayout) findViewById(R.id.card_memoria);
        LinearLayout cardPalavras = (LinearLayout) findViewById(R.id.card_palavras);
        LinearLayout cardTarefas  = (LinearLayout) findViewById(R.id.card_tarefas);

        cards = new View[]{cardMemoria, cardPalavras, cardTarefas};

        // ─── Cliques dos cards ───
        cardMemoria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HubActivity.this, MemoriaActivity.class));
            }
        });

        cardPalavras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HubActivity.this, PalavraActivity.class));
            }
        });

        cardTarefas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HubActivity.this, TarefasActivity.class));
            }
        });

        cardBatalha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HubActivity.this, BatalhaActivity.class));
            }
        });

        // Versão/admin — click abre a tela de administrador
        tvVersao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HubActivity.this, AdminActivity.class));
            }
        });

        // Rastreia qual card está em foco
        for (int i = 0; i < cards.length; i++) {
            final int idx = i;
            cards[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        indiceFocado = idx;
                        linhaAtual = 0;
                    }
                }
            });
        }

        cardBatalha.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) linhaAtual = 1;
            }
        });

        tvVersao.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) linhaAtual = 2;
            }
        });

        // Animação de entrada dos cards
        AnimHelper.zoomEntrada(cardMemoria,  0);
        AnimHelper.zoomEntrada(cardPalavras, 80);
        AnimHelper.zoomEntrada(cardTarefas,  160);

        cardMemoria.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        perfil.verificarResetDiario();
        atualizarUI();
    }

    private void atualizarUI() {
        int nivel     = perfil.getNivel();
        String titulo = perfil.getTitulo();
        int xp        = perfil.getXP();
        int xpPct     = perfil.getXPPorcentagem();

        tvNivel.setText("Nivel " + nivel + " \u2022 " + titulo);
        tvXP.setText(xp + " XP \u2192 Nivel " + (nivel + 1));

        AnimHelper.animarXP(pbXP, xpPctAnterior, xpPct);
        xpPctAnterior = xpPct;

        tvInt.setText(String.valueOf(perfil.getStatInteligencia()));
        tvFoc.setText(String.valueOf(perfil.getStatFoco()));
        tvRes.setText(String.valueOf(perfil.getStatResponsabilidade()));

        int tarefasFeitas = perfil.getQuantidadeTarefasFeitas();
        if (tarefasFeitas >= TOTAL_TAREFAS) {
            tvSubTarefas.setText("Tudo feito! \u2705");
        } else {
            tvSubTarefas.setText(tarefasFeitas + "/" + TOTAL_TAREFAS + " feitas");
        }

        String statusBatalha = perfil.getBatalhaStatus();
        boolean memoria      = perfil.isMemoriaConcluida();
        boolean palavras     = perfil.isPalavrasConcluida();

        StringBuilder checklist = new StringBuilder();

        if (!statusBatalha.isEmpty()) {
            tvChecklistTitulo.setText("Batalha de hoje:");
            if ("ganhou".equals(statusBatalha)) {
                checklist.append("\uD83C\uDFC6 Voce venceu o Bruxo hoje! Parabens!");
            } else {
                checklist.append("\uD83D\uDE22 Derrota... Tente amanha com mais forca!");
            }
        } else {
            tvChecklistTitulo.setText("Complete tudo para desbloquear a Batalha:");
            checklist.append(memoria  ? "\u2705 Memoria\n"  : "\u2B1C Memoria — jogue para desbloquear\n");
            checklist.append(palavras ? "\u2705 Palavras\n" : "\u2B1C Palavras — jogue para desbloquear\n");
            checklist.append("Tarefas: " + tarefasFeitas + "/" + TOTAL_TAREFAS);
        }

        tvChecklist.setText(checklist.toString().trim());

        boolean desbloqueada = perfil.batalhaDesbloqueada(TOTAL_TAREFAS);
        cardBatalha.setVisibility(desbloqueada ? View.VISIBLE : View.GONE);
    }

    /**
     * Navegação D-pad pelos cards.
     *
     * Linhas:
     *   0: [Memória] [Palavras] [Tarefas]   ← esquerda/direita
     *   1: [⚔️ BATALHAR!]                   ← disponível quando desbloqueada
     *   2: [versão — acesso admin]           ← DOWN a partir da linha 1 (ou 0 se sem batalha)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean batalhaVisivel = cardBatalha.getVisibility() == View.VISIBLE;

        switch (keyCode) {

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (linhaAtual == 0 && indiceFocado < 2) {
                    indiceFocado++;
                    cards[indiceFocado].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (linhaAtual == 0 && indiceFocado > 0) {
                    indiceFocado--;
                    cards[indiceFocado].requestFocus();
                } else if (linhaAtual == 1) {
                    linhaAtual = 0;
                    indiceFocado = 1;
                    cards[indiceFocado].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (linhaAtual == 0 && batalhaVisivel) {
                    linhaAtual = 1;
                    cardBatalha.requestFocus();
                } else if (linhaAtual == 0 && !batalhaVisivel) {
                    linhaAtual = 2;
                    tvVersao.requestFocus();
                } else if (linhaAtual == 1) {
                    linhaAtual = 2;
                    tvVersao.requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (linhaAtual == 2) {
                    if (batalhaVisivel) {
                        linhaAtual = 1;
                        cardBatalha.requestFocus();
                    } else {
                        linhaAtual = 0;
                        cards[indiceFocado].requestFocus();
                    }
                } else if (linhaAtual == 1) {
                    linhaAtual = 0;
                    cards[indiceFocado].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (linhaAtual == 2) {
                    tvVersao.performClick();
                } else if (linhaAtual == 1) {
                    cardBatalha.performClick();
                } else {
                    cards[indiceFocado].performClick();
                }
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
