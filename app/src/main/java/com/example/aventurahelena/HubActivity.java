package com.example.aventurahelena;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
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
 * - Baixo: vai para o card da Batalha (quando disponível)
 * - Cima: volta para os cards de jogos
 * - OK/Enter: entra no jogo selecionado
 */
public class HubActivity extends Activity {

    private static final int TOTAL_TAREFAS = 10;

    private PerfilHelena perfil;

    // Views do perfil
    private TextView tvNivel;
    private TextView tvXP;
    private ProgressBar pbXP;
    private TextView tvInt;
    private TextView tvFoc;
    private TextView tvRes;

    // Views dos cards
    private TextView tvSubTarefas;
    private TextView tvChecklist;
    private TextView tvChecklistTitulo;
    private LinearLayout cardBatalha;

    // Cards navegáveis: 0=Memória, 1=Palavras, 2=Tarefas, 3=Batalha
    private View[] cards;
    private int indiceFocado = 0;
    private boolean linhaInferior = false; // true = foco na batalha

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);

        perfil = new PerfilHelena(this);

        // Corte circular no avatar (API 21+)
        ImageView ivAvatar = (ImageView) findViewById(R.id.iv_avatar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ivAvatar.setClipToOutline(true);
        }

        // Referências às views do perfil
        tvNivel   = (TextView)    findViewById(R.id.tv_nivel);
        tvXP      = (TextView)    findViewById(R.id.tv_xp);
        pbXP      = (ProgressBar) findViewById(R.id.pb_xp);
        tvInt     = (TextView)    findViewById(R.id.tv_int);
        tvFoc     = (TextView)    findViewById(R.id.tv_foc);
        tvRes     = (TextView)    findViewById(R.id.tv_res);

        // Referências dos cards e textos dinâmicos
        tvSubTarefas       = (TextView)      findViewById(R.id.tv_sub_tarefas);
        tvChecklist        = (TextView)      findViewById(R.id.tv_checklist);
        tvChecklistTitulo  = (TextView)      findViewById(R.id.tv_checklist_titulo);
        cardBatalha        = (LinearLayout)  findViewById(R.id.card_batalha);

        LinearLayout cardMemoria  = (LinearLayout) findViewById(R.id.card_memoria);
        LinearLayout cardPalavras = (LinearLayout) findViewById(R.id.card_palavras);
        LinearLayout cardTarefas  = (LinearLayout) findViewById(R.id.card_tarefas);

        // Array de cards (linha superior)
        cards = new View[]{cardMemoria, cardPalavras, cardTarefas};

        // ─── Configura cliques dos cards ───
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

        // Rastreia qual card está em foco
        for (int i = 0; i < cards.length; i++) {
            final int idx = i;
            cards[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        indiceFocado = idx;
                        linhaInferior = false;
                    }
                }
            });
        }

        cardBatalha.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) linhaInferior = true;
            }
        });

        // Foco inicial no card da Memória
        cardMemoria.requestFocus();
    }

    /**
     * Atualiza toda a UI sempre que voltamos ao hub (após terminar um mini-jogo).
     */
    @Override
    protected void onResume() {
        super.onResume();
        perfil.verificarResetDiario();
        atualizarUI();
    }

    /**
     * Atualiza todas as informações do perfil e estado dos cards.
     */
    private void atualizarUI() {
        int nivel     = perfil.getNivel();
        String titulo = perfil.getTitulo();
        int xp        = perfil.getXP();
        int xpPct     = perfil.getXPPorcentagem();

        // Perfil
        tvNivel.setText("Nivel " + nivel + " \u2022 " + titulo);
        tvXP.setText(xp + " XP \u2192 Nivel " + (nivel + 1));
        pbXP.setProgress(xpPct);

        // Stats
        tvInt.setText(String.valueOf(perfil.getStatInteligencia()));
        tvFoc.setText(String.valueOf(perfil.getStatFoco()));
        tvRes.setText(String.valueOf(perfil.getStatResponsabilidade()));

        // Subtítulo do card de tarefas (mostra quantas feitas)
        int tarefasFeitas = perfil.getQuantidadeTarefasFeitas();
        if (tarefasFeitas >= TOTAL_TAREFAS) {
            tvSubTarefas.setText("Tudo feito! \u2705");
        } else {
            tvSubTarefas.setText(tarefasFeitas + "/" + TOTAL_TAREFAS + " feitas");
        }

        // Estado da batalha
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

        // Mostra ou esconde o card da batalha
        boolean desbloqueada = perfil.batalhaDesbloqueada(TOTAL_TAREFAS);
        cardBatalha.setVisibility(desbloqueada ? View.VISIBLE : View.GONE);
    }

    /**
     * Navegação D-pad pelos cards.
     *
     * Layout:
     * [Memória] [Palavras] [Tarefas]   ← linha superior (left/right)
     *       [⚔️ BATALHAR!]             ← linha inferior (down/up)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean batalhaVisivel = cardBatalha.getVisibility() == View.VISIBLE;

        switch (keyCode) {

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!linhaInferior && indiceFocado < 2) {
                    indiceFocado++;
                    cards[indiceFocado].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!linhaInferior && indiceFocado > 0) {
                    indiceFocado--;
                    cards[indiceFocado].requestFocus();
                } else if (linhaInferior) {
                    // De baixo, vai pro card do meio
                    linhaInferior = false;
                    indiceFocado = 1;
                    cards[indiceFocado].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (!linhaInferior && batalhaVisivel) {
                    linhaInferior = true;
                    cardBatalha.requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (linhaInferior) {
                    linhaInferior = false;
                    cards[indiceFocado].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (linhaInferior) {
                    cardBatalha.performClick();
                } else {
                    cards[indiceFocado].performClick();
                }
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
