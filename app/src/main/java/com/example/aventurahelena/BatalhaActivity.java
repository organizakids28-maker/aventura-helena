package com.example.aventurahelena;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BatalhaActivity — Batalha Final em duas fases.
 *
 * FASE 1: Jogo da Memória com 3 pares (6 cartas em grade 3x2).
 *   - Acertar um par  → Bruxo perde 1 HP
 *   - Errar um par    → Helena perde 1 HP
 *
 * FASE 2: Complete a Palavra com 3 perguntas.
 *   - Acertar         → Bruxo perde 1 HP
 *   - Errar           → Helena perde 1 HP
 *
 * Se HP de algum lado chegar a 0 antes do fim → termina imediatamente.
 * Caso contrário, quem tiver mais HP ao final vence.
 *
 * HP inicial de ambos: 3
 */
public class BatalhaActivity extends Activity {

    private static final int HP_MAX = 3;

    // ── EMOJIS para a fase de memória (apenas 3 pares) ──
    private static final String[] EMOJIS_BATALHA = {
        "\u2B50",         // ⭐
        "\uD83C\uDF08",   // 🌈
        "\uD83D\uDC31"    // 🐱
    };

    // ── Banco de palavras para a fase 2 (sorteia 3) ──
    private static final String[][] BANCO_PALAVRAS = {
        {"G_TO",   "Animal que mia",     "GATO",  "PATO",  "RATO",  "BOLO",  "0"},
        {"C_SA",   "Onde moramos",       "BASA",  "CASA",  "MADA",  "FASA",  "1"},
        {"B_LA",   "Brinquedo redondo",  "ROLA",  "COLA",  "BOLA",  "MOLA",  "2"},
        {"S_L",    "Brilha no ceu",      "SAL",   "SIL",   "SOR",   "SOL",   "3"},
        {"AM_GO",  "Pessoa amiga",       "AMIGO", "AMIDA", "AMINA", "AMILO", "0"},
        {"P_O",    "Alimento de farinha","PAU",   "PAI",   "PAO",   "PAZ",   "2"},
        {"LE_TE",  "Bebida branca",      "LESTE", "LEITE", "LENTO", "LEVE",  "1"},
        {"_EIXE",  "Vive na agua",       "FEICE", "DEIXE", "PEIXE", "MEICE", "2"},
        {"BO_O",   "Sobremesa",          "BOSO",  "BOLO",  "BORO",  "BOCO",  "1"},
        {"FL_R",   "Planta bonita",      "FLOU",  "FLOX",  "FLON",  "FLOR",  "3"},
    };

    // ── Estado da batalha ──
    private int hpHelena   = HP_MAX;
    private int hpInimigo  = HP_MAX;
    private int fase = 1; // 1 = memória, 2 = palavras

    // ── Views ──
    private TextView tvHpHelena;
    private TextView tvHpInimigo;
    private TextView tvFase;
    private TextView tvResultado;
    private GridLayout gridBatalha;
    private LinearLayout llPalavrasBatalha;
    private TextView tvPerguntaBatalha;
    private TextView tvPalavraBatalha;
    private Button[] btnBat;

    // ── Fase 1: Memória ──
    private static final int TOTAL_CARTAS_BAT  = 6;
    private static final int COLUNAS_BAT       = 3;
    private static final int TOTAL_PARES_BAT   = 3;
    private static final int DELAY_FECHAR      = 900;

    private List<Integer> listaValoresBat;
    private Button[] botoesBat;
    private int primeiraBat = -1;
    private int segundaBat  = -1;
    private int paresEncontradosBat = 0;
    private int errosBat = 0;
    private boolean bloqueadoBat = false;
    private int indiceFocadoBat = 0;

    // ── Fase 2: Palavras ──
    private List<Integer> indicesPalavras; // índices sortidos do BANCO_PALAVRAS
    private int perguntaAtualBat = 0;
    private boolean aguardandoProximaBat = false;
    private int opcaoFocadaBat = 0;

    private Handler handler;
    private PerfilHelena perfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batalha);

        perfil  = new PerfilHelena(this);
        handler = new Handler();

        // Views do cabeçalho
        tvHpHelena  = (TextView) findViewById(R.id.tv_hp_helena);
        tvHpInimigo = (TextView) findViewById(R.id.tv_hp_inimigo);
        tvFase      = (TextView) findViewById(R.id.tv_fase);
        tvResultado = (TextView) findViewById(R.id.tv_resultado_batalha);

        // Views das fases
        gridBatalha      = (GridLayout)    findViewById(R.id.grid_batalha);
        llPalavrasBatalha = (LinearLayout) findViewById(R.id.ll_palavras_batalha);
        tvPerguntaBatalha = (TextView)     findViewById(R.id.tv_pergunta_batalha);
        tvPalavraBatalha  = (TextView)     findViewById(R.id.tv_palavra_batalha);

        btnBat = new Button[]{
            (Button) findViewById(R.id.btn_bat0),
            (Button) findViewById(R.id.btn_bat1),
            (Button) findViewById(R.id.btn_bat2),
            (Button) findViewById(R.id.btn_bat3)
        };

        // Configura os botões de resposta da fase 2
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            btnBat[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    verificarRespostaPalavra(idx);
                }
            });
            btnBat[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) opcaoFocadaBat = idx;
                }
            });
        }

        // Sorteia 3 perguntas para a fase 2
        indicesPalavras = new ArrayList<Integer>();
        for (int i = 0; i < BANCO_PALAVRAS.length; i++) indicesPalavras.add(i);
        Collections.shuffle(indicesPalavras);
        // Mantém apenas os 3 primeiros
        while (indicesPalavras.size() > 3) {
            indicesPalavras.remove(indicesPalavras.size() - 1);
        }

        atualizarHP();
        iniciarFase1();
    }

    // ════════════════════════════════════════════
    // FASE 1 — MEMÓRIA
    // ════════════════════════════════════════════

    private void iniciarFase1() {
        fase = 1;
        tvFase.setText("FASE 1\nMemoria");
        tvResultado.setText("");

        gridBatalha.setVisibility(View.VISIBLE);
        llPalavrasBatalha.setVisibility(View.GONE);

        // Cria e embaralha as cartas (3 pares)
        listaValoresBat = new ArrayList<Integer>();
        for (int i = 0; i < TOTAL_PARES_BAT; i++) {
            listaValoresBat.add(i);
            listaValoresBat.add(i);
        }
        Collections.shuffle(listaValoresBat);

        // Limpa o grid e cria os botões
        gridBatalha.removeAllViews();
        botoesBat = new Button[TOTAL_CARTAS_BAT];

        for (int i = 0; i < TOTAL_CARTAS_BAT; i++) {
            Button btn = new Button(this);
            btn.setText("?");
            btn.setTextSize(26);
            btn.setTextColor(0xFFFFFFFF);
            btn.setBackgroundResource(R.drawable.btn_selector);
            btn.setFocusable(true);
            btn.setFocusableInTouchMode(true);

            final int indice = i;
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    indiceFocadoBat = indice;
                    processarCliqueMemoria(indice);
                }
            });
            btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) indiceFocadoBat = indice;
                }
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width  = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(i % COLUNAS_BAT, 1f);
            params.rowSpec    = GridLayout.spec(i / COLUNAS_BAT, 1f);
            params.setMargins(10, 10, 10, 10);
            btn.setLayoutParams(params);

            gridBatalha.addView(btn);
            botoesBat[i] = btn;
        }

        indiceFocadoBat = 0;
        botoesBat[0].requestFocus();
    }

    private void processarCliqueMemoria(int indice) {
        if (bloqueadoBat) return;
        if (!botoesBat[indice].isEnabled()) return;
        if (!"?".equals(botoesBat[indice].getText().toString())) return;

        int valor = listaValoresBat.get(indice);
        botoesBat[indice].setText(EMOJIS_BATALHA[valor]);

        if (primeiraBat == -1) {
            primeiraBat = indice;
        } else {
            segundaBat = indice;
            bloqueadoBat = true;
            verificarParBatalha();
        }
    }

    private void verificarParBatalha() {
        int val1 = listaValoresBat.get(primeiraBat);
        int val2 = listaValoresBat.get(segundaBat);

        final int idx1 = primeiraBat;
        final int idx2 = segundaBat;
        primeiraBat = -1;
        segundaBat  = -1;

        if (val1 == val2) {
            // Par certo → Bruxo perde HP
            botoesBat[idx1].setEnabled(false);
            botoesBat[idx2].setEnabled(false);
            paresEncontradosBat++;
            bloqueadoBat = false;

            causarDano(false); // false = dano no inimigo

            if (hpInimigo <= 0) {
                // Inimigo derrotado na fase 1 → vitória imediata
                finalizarBatalha(true);
                return;
            }

            if (paresEncontradosBat == TOTAL_PARES_BAT) {
                // Todos os pares encontrados → avança para fase 2
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        iniciarFase2();
                    }
                }, 800);
            }

        } else {
            // Par errado → Helena perde HP
            errosBat++;
            causarDano(true); // true = dano na Helena

            if (hpHelena <= 0) {
                // Helena derrotada na fase 1 → derrota imediata
                finalizarBatalha(false);
                return;
            }

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    botoesBat[idx1].setText("?");
                    botoesBat[idx2].setText("?");
                    bloqueadoBat = false;
                }
            }, DELAY_FECHAR);
        }
    }

    // ════════════════════════════════════════════
    // FASE 2 — PALAVRAS
    // ════════════════════════════════════════════

    private void iniciarFase2() {
        fase = 2;
        perguntaAtualBat = 0;
        tvFase.setText("FASE 2\nPalavras");

        gridBatalha.setVisibility(View.GONE);
        llPalavrasBatalha.setVisibility(View.VISIBLE);

        mostrarPerguntaBatalha();
    }

    private void mostrarPerguntaBatalha() {
        if (perguntaAtualBat >= indicesPalavras.size()) {
            // Fase 2 concluída — define resultado por HP
            finalizarBatalha(hpHelena > hpInimigo);
            return;
        }

        aguardandoProximaBat = false;
        opcaoFocadaBat = 0;

        int idx = indicesPalavras.get(perguntaAtualBat);
        String[] dados = BANCO_PALAVRAS[idx];

        String lacuna = dados[0];
        StringBuilder sb = new StringBuilder();
        for (char c : lacuna.toCharArray()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(c);
        }

        tvPerguntaBatalha.setText("Pergunta " + (perguntaAtualBat + 1) + " de 3");
        tvPalavraBatalha.setText(sb.toString());
        tvResultado.setText(dados[1]); // Dica

        for (int i = 0; i < 4; i++) {
            btnBat[i].setText(dados[2 + i]);
            btnBat[i].setEnabled(true);
            btnBat[i].setTextColor(0xFFFFFFFF);
        }

        btnBat[0].requestFocus();
    }

    private void verificarRespostaPalavra(int opcaoEscolhida) {
        if (aguardandoProximaBat) return;

        int idx = indicesPalavras.get(perguntaAtualBat);
        String[] dados = BANCO_PALAVRAS[idx];
        int correta = Integer.parseInt(dados[6]);

        for (Button btn : btnBat) btn.setEnabled(false);
        aguardandoProximaBat = true;

        if (opcaoEscolhida == correta) {
            tvResultado.setText("Correto! Bruxo leva 1 de dano!");
            tvResultado.setTextColor(0xFF4CAF50);
            causarDano(false); // dano no inimigo

            if (hpInimigo <= 0) {
                finalizarBatalha(true);
                return;
            }
        } else {
            tvResultado.setText("Errado! Helena leva 1 de dano! Era: " + dados[2 + correta]);
            tvResultado.setTextColor(0xFFEF5350);
            causarDano(true); // dano na Helena

            if (hpHelena <= 0) {
                finalizarBatalha(false);
                return;
            }
        }

        perguntaAtualBat++;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mostrarPerguntaBatalha();
            }
        }, 1400);
    }

    // ════════════════════════════════════════════
    // HP e RESULTADO
    // ════════════════════════════════════════════

    /**
     * Causa 1 de dano em Helena (helenaTomaDano=true) ou no Bruxo (false).
     */
    private void causarDano(boolean helenaTomaDano) {
        if (helenaTomaDano) {
            hpHelena = Math.max(0, hpHelena - 1);
        } else {
            hpInimigo = Math.max(0, hpInimigo - 1);
        }
        atualizarHP();
    }

    private void atualizarHP() {
        tvHpHelena.setText("HP: " + hpHelena + "/" + HP_MAX);
        tvHpInimigo.setText("HP: " + hpInimigo + "/" + HP_MAX);

        tvHpHelena.setTextColor(hpHelena <= 1 ? 0xFFEF5350 : 0xFF4CAF50);
        tvHpInimigo.setTextColor(hpInimigo <= 1 ? 0xFFEF5350 : 0xFF4CAF50);
    }

    /**
     * Finaliza a batalha, salva o resultado e mostra o diálogo.
     */
    private void finalizarBatalha(final boolean helenaGanhou) {
        // Bloqueia interações
        bloqueadoBat = true;
        aguardandoProximaBat = true;

        // Salva o resultado
        if (helenaGanhou) {
            perfil.setBatalhaStatus("ganhou");
            perfil.addXP(100);
            perfil.addStatInteligencia(5);
            perfil.addStatFoco(5);
            perfil.addStatResponsabilidade(5);
        } else {
            perfil.setBatalhaStatus("perdeu");
        }

        String titulo = helenaGanhou ? "VITORIA!" : "Que pena...";
        String mensagem;

        if (helenaGanhou) {
            mensagem = "Parabens, Helena! Voce derrotou o Bruxo!\n\n"
                + "Helena: " + hpHelena + "/" + HP_MAX + " HP restante\n\n"
                + "+ 100 XP\n"
                + "+ 5 em todas as stats!\n\n"
                + "Voce e uma verdadeira heroina!";
        } else {
            mensagem = "O Bruxo foi mais forte dessa vez...\n\n"
                + "Mas nao desista! Treine mais e tente amanha!\n"
                + "Bruxo: " + hpInimigo + "/" + HP_MAX + " HP restante";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titulo);
        builder.setMessage(mensagem);
        builder.setCancelable(false);

        builder.setNegativeButton("Voltar ao Hub", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        // Pequeno delay para mostrar o resultado na tela antes do diálogo
        final String tituloFinal = titulo;
        final String mensagemFinal = mensagem;
        tvResultado.setText(helenaGanhou ? "VITORIA! Bruxo derrotado!" : "Derrota... tente amanha!");
        tvResultado.setTextColor(helenaGanhou ? 0xFFFFD700 : 0xFFEF5350);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    AlertDialog.Builder b = new AlertDialog.Builder(BatalhaActivity.this);
                    b.setTitle(tituloFinal);
                    b.setMessage(mensagemFinal);
                    b.setCancelable(false);
                    b.setNegativeButton("Voltar ao Hub", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    b.show();
                }
            }
        }, 1500);
    }

    // ════════════════════════════════════════════
    // NAVEGAÇÃO D-PAD
    // ════════════════════════════════════════════

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (fase == 1) {
            int novoIndice = indiceFocadoBat;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if ((indiceFocadoBat % COLUNAS_BAT) < COLUNAS_BAT - 1)
                        novoIndice = indiceFocadoBat + 1;
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if ((indiceFocadoBat % COLUNAS_BAT) > 0)
                        novoIndice = indiceFocadoBat - 1;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (indiceFocadoBat + COLUNAS_BAT < TOTAL_CARTAS_BAT)
                        novoIndice = indiceFocadoBat + COLUNAS_BAT;
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (indiceFocadoBat - COLUNAS_BAT >= 0)
                        novoIndice = indiceFocadoBat - COLUNAS_BAT;
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    processarCliqueMemoria(indiceFocadoBat);
                    return true;
                default:
                    return super.onKeyDown(keyCode, event);
            }
            if (novoIndice != indiceFocadoBat && botoesBat != null) {
                indiceFocadoBat = novoIndice;
                botoesBat[indiceFocadoBat].requestFocus();
            }
            return true;

        } else {
            if (aguardandoProximaBat) return true;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (opcaoFocadaBat < 3) {
                        opcaoFocadaBat++;
                        btnBat[opcaoFocadaBat].requestFocus();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (opcaoFocadaBat > 0) {
                        opcaoFocadaBat--;
                        btnBat[opcaoFocadaBat].requestFocus();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    verificarRespostaPalavra(opcaoFocadaBat);
                    return true;
                default:
                    return super.onKeyDown(keyCode, event);
            }
        }
    }
}
