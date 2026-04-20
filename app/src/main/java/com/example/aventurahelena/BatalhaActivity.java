package com.example.aventurahelena;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * BatalhaActivity — Batalha Final com dois modos de combate:
 *
 *   Modo Palavra  (ataque normal): Complete a lacuna da palavra.
 *     Acerto → Bruxo perde 12-26 HP.  Erro → Helena perde 8-19 HP.
 *     2 palavras consecutivas certas → Magia disponível.
 *
 *   Modo Memória  (magia especial): Jogo da Memória com 6 pares e imagens reais.
 *     Todos os pares → Bruxo perde 40-60 HP. Par errado → Helena perde 6-13 HP.
 *
 *   Helena: 100 HP  |  Bruxo das Trevas: 150 HP
 */
public class BatalhaActivity extends Activity {

    /* ─── HP ───────────────────────────────────────────────── */
    private static final int HP_HELENA_MAX = 100;
    private static final int HP_BRUXO_MAX  = 150;

    /* ─── Banco de palavras ─────────────────────────────────── */
    private static final String[][] BANCO = {
        { "GATO",       "Mia e gosta de dormir",               "Animal" },
        { "CACHORRO",   "Melhor amigo do homem",               "Animal" },
        { "BANANA",     "Amarela e dos macacos",               "Fruta"  },
        { "BRASIL",     "Pais do futebol e do carnaval",       "Pais"   },
        { "ESCOLA",     "Lugar onde se aprende",               "Lugar"  },
        { "FUTEBOL",    "Esporte mais popular do Brasil",      "Esporte"},
        { "AMARELO",    "Cor do sol e da banana",              "Cor"    },
        { "MEDICO",     "Cuida da saude das pessoas",          "Prof."  },
        { "ELEFANTE",   "Maior animal terrestre",              "Animal" },
        { "TARTARUGA",  "Animal lento com casco",              "Animal" },
        { "MELANCIA",   "Vermelha por dentro, verde por fora", "Fruta"  },
        { "COMPUTADOR", "Maquina para trabalhar e jogar",      "Tecno." },
    };

    /* ─── Drawables das cartas ──────────────────────────────── */
    private static final int[] CARTAS_FRENTE = {
        R.drawable.card_gato, R.drawable.card_cachorro, R.drawable.card_estrela,
        R.drawable.card_coracao, R.drawable.card_sol, R.drawable.card_lua
    };
    private static final int CARTA_VERSO = R.drawable.card_verso;

    /* ─── Modo de batalha ───────────────────────────────────── */
    private static final int MODO_PALAVRA  = 0;
    private static final int MODO_MEMORIA  = 1;

    /* ─── Estado geral ──────────────────────────────────────── */
    private int hpHelena = HP_HELENA_MAX;
    private int hpBruxo  = HP_BRUXO_MAX;
    private int modoBatalha = MODO_PALAVRA;
    private int acertosConsec = 0;
    private boolean magiaDisp = false;
    private boolean bloqueado = false;
    private boolean batalhaTerminou = false;

    /* ─── Views HP ──────────────────────────────────────────── */
    private TextView     tvHpHelena, tvHpBruxo;
    private ProgressBar  pbHpHelena, pbHpBruxo;
    private TextView     tvFeedback, tvModo;
    private ImageView    ivHelenaBatalha;
    private LinearLayout llBruxoPerfil;

    /* ─── Modo Palavra ──────────────────────────────────────── */
    private LinearLayout llModoPalavra;
    private LinearLayout llLetras;
    private TextView     tvDica;
    private Button[]     botoesOpcao;
    private Button       btnMagia;
    private int          opcaoFocada = 0;
    private boolean      magiaBotaoFocado = false;
    private boolean      aguardandoProxima = false;

    private String   palavraAtual, dicaAtual, catAtual;
    private int[]    blanksAtual;
    private char[]   reveladasAtual;
    private int      blankAtualIdx;
    private char[]   opcoesAtual = new char[4];
    private List<Integer> palavrasUsadas = new ArrayList<Integer>();

    /* ─── Modo Memória ──────────────────────────────────────── */
    private static final int TOTAL_PARES = 6;
    private static final int COLS_MEM    = 6;
    private static final int TOTAL_CARTAS = TOTAL_PARES * 2;

    private LinearLayout glMemoria;
    private List<Integer> valoresCartas;
    private boolean[]    cartasViradas;
    private boolean[]    cartasEncontradas;
    private FrameLayout[] frameCartas;
    private ImageView[]  imgCartas;
    private int primeiraCarta = -1, segundaCarta = -1;
    private int paresEncontrados = 0;
    private boolean bloqMem = false;
    private int focadoMem = 0;

    /* ─── Utilitários ───────────────────────────────────────── */
    private Handler      handler = new Handler();
    private PerfilHelena perfil;
    private SoundManager sound;
    private Random       rng = new Random();

    // ═══════════════════════════════════════════════════════════
    // CICLO DE VIDA
    // ═══════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batalha);

        perfil = new PerfilHelena(this);
        sound  = new SoundManager();

        tvHpHelena   = (TextView)    findViewById(R.id.tv_hp_helena);
        tvHpBruxo    = (TextView)    findViewById(R.id.tv_hp_bruxo);
        pbHpHelena   = (ProgressBar) findViewById(R.id.pb_hp_helena);
        pbHpBruxo    = (ProgressBar) findViewById(R.id.pb_hp_bruxo);
        tvFeedback   = (TextView)    findViewById(R.id.tv_feedback_batalha);
        tvModo       = (TextView)    findViewById(R.id.tv_modo_batalha);
        llModoPalavra = (LinearLayout) findViewById(R.id.ll_modo_palavra);
        llLetras     = (LinearLayout) findViewById(R.id.ll_letras);
        tvDica       = (TextView)   findViewById(R.id.tv_dica_palavra);
        glMemoria       = (LinearLayout) findViewById(R.id.gl_memoria_batalha);
        ivHelenaBatalha = (ImageView)    findViewById(R.id.iv_helena_batalha);
        llBruxoPerfil   = (LinearLayout) findViewById(R.id.ll_bruxo_perfil);

        botoesOpcao = new Button[]{
            (Button) findViewById(R.id.btn_op0),
            (Button) findViewById(R.id.btn_op1),
            (Button) findViewById(R.id.btn_op2),
            (Button) findViewById(R.id.btn_op3)
        };
        btnMagia = (Button) findViewById(R.id.btn_magia);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            botoesOpcao[i].setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { responderLetra(idx); }
            });
            botoesOpcao[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) { opcaoFocada = idx; magiaBotaoFocado = false; }
                }
            });
        }
        btnMagia.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { ativarMagia(); }
        });
        btnMagia.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) { magiaBotaoFocado = true; }
            }
        });

        pbHpHelena.setMax(HP_HELENA_MAX);
        pbHpBruxo.setMax(HP_BRUXO_MAX);
        atualizarHP();
        sound.playBatalhaInicio();

        handler.postDelayed(new Runnable() {
            @Override public void run() { iniciarModoPalavra(); }
        }, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sound.release();
    }

    // ═══════════════════════════════════════════════════════════
    // MODO PALAVRA
    // ═══════════════════════════════════════════════════════════

    private void iniciarModoPalavra() {
        modoBatalha = MODO_PALAVRA;
        glMemoria.setVisibility(View.GONE);
        AnimHelper.fadeIn(llModoPalavra, 200);
        tvModo.setText("Ataque: Complete a Palavra");
        carregarPalavra();
    }

    private void carregarPalavra() {
        aguardandoProxima = false;
        bloqueado = false;
        magiaBotaoFocado = false;

        if (palavrasUsadas.size() >= BANCO.length) palavrasUsadas.clear();

        List<Integer> disponiveis = new ArrayList<Integer>();
        for (int i = 0; i < BANCO.length; i++) {
            if (!palavrasUsadas.contains(i)) disponiveis.add(i);
        }
        int escolhido = disponiveis.get(rng.nextInt(disponiveis.size()));
        palavrasUsadas.add(escolhido);

        palavraAtual = BANCO[escolhido][0];
        dicaAtual    = BANCO[escolhido][1];
        catAtual     = BANCO[escolhido][2];

        int n = palavraAtual.length() <= 4 ? 1 : palavraAtual.length() <= 6 ? 2 : 3;
        List<Integer> indices = new ArrayList<Integer>();
        for (int i = 0; i < palavraAtual.length(); i++) indices.add(i);
        Collections.shuffle(indices);
        blanksAtual = new int[n];
        for (int i = 0; i < n; i++) blanksAtual[i] = indices.get(i);
        java.util.Arrays.sort(blanksAtual);

        reveladasAtual = palavraAtual.toCharArray();
        for (int b : blanksAtual) reveladasAtual[b] = '_';
        blankAtualIdx = 0;

        tvDica.setText("[" + catAtual + "]  " + dicaAtual);
        desenharLetras();
        gerarOpcoes();
        atualizarBotaoMagia();

        opcaoFocada = 0;
        botoesOpcao[0].requestFocus();
        AnimHelper.fadeIn(llLetras, 200);
    }

    private void desenharLetras() {
        llLetras.removeAllViews();
        int boxDp = Math.max(36, Math.min(56, 320 / Math.max(1, palavraAtual.length())));
        int boxPx = dp(boxDp);

        for (int i = 0; i < palavraAtual.length(); i++) {
            boolean isBlank  = contem(blanksAtual, i);
            boolean isAtual  = isBlank && blankAtualIdx < blanksAtual.length
                               && blanksAtual[blankAtualIdx] == i;

            FrameLayout box = new FrameLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(boxPx, boxPx);
            lp.setMargins(dp(3), dp(4), dp(3), dp(4));
            box.setLayoutParams(lp);

            if (isAtual) {
                box.setBackgroundResource(R.drawable.letra_atual_bg);
            } else if (isBlank) {
                box.setBackgroundResource(R.drawable.letra_blank_bg);
            } else {
                box.setBackgroundResource(R.drawable.letra_dada_bg);
            }

            TextView tv = new TextView(this);
            FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            );
            tv.setLayoutParams(tvLp);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(20);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setText(isBlank && reveladasAtual[i] == '_' ? "" : String.valueOf(reveladasAtual[i]));
            box.addView(tv);
            llLetras.addView(box);
        }
    }

    private void gerarOpcoes() {
        if (blankAtualIdx >= blanksAtual.length) return;
        char correta = palavraAtual.charAt(blanksAtual[blankAtualIdx]);

        List<Character> pool = new ArrayList<Character>();
        for (char c = 'A'; c <= 'Z'; c++) {
            if (c != correta && palavraAtual.indexOf(c) == -1) pool.add(c);
        }
        Collections.shuffle(pool);

        List<Character> lista = new ArrayList<Character>();
        lista.add(correta);
        lista.add(pool.get(0));
        lista.add(pool.get(1));
        lista.add(pool.get(2));
        Collections.shuffle(lista);

        for (int i = 0; i < 4; i++) {
            opcoesAtual[i] = lista.get(i);
            botoesOpcao[i].setText(String.valueOf(opcoesAtual[i]));
            botoesOpcao[i].setEnabled(true);
            botoesOpcao[i].setTextColor(0xFFFFFFFF);
            AnimHelper.zoomEntrada(botoesOpcao[i], i * 60);
        }
    }

    private void responderLetra(final int opcaoIdx) {
        if (bloqueado || aguardandoProxima || batalhaTerminou) return;
        if (modoBatalha != MODO_PALAVRA) return;

        char escolhida = opcoesAtual[opcaoIdx];
        char correta   = palavraAtual.charAt(blanksAtual[blankAtualIdx]);

        bloqueado = true;
        for (Button b : botoesOpcao) b.setEnabled(false);

        if (escolhida == correta) {
            sound.playAcerto();
            reveladasAtual[blanksAtual[blankAtualIdx]] = correta;
            botoesOpcao[opcaoIdx].setTextColor(0xFF4CAF50);
            AnimHelper.pulseGold(botoesOpcao[opcaoIdx]);
            blankAtualIdx++;
            desenharLetras();

            if (blankAtualIdx >= blanksAtual.length) {
                final int dano = 12 + rng.nextInt(15);
                hpBruxo = Math.max(0, hpBruxo - dano);
                atualizarHP();
                AnimHelper.flashRed(tvHpBruxo);
                AnimHelper.pulseGold(tvHpBruxo);
                sound.playDano(false);

                acertosConsec++;
                if (acertosConsec >= 2) magiaDisp = true;
                atualizarBotaoMagia();

                mostrarFeedback("\u2694 \"" + palavraAtual + "\" \u2014 Bruxo perdeu " + dano + " HP!", 0xFFFFD700);

                if (hpBruxo <= 0) {
                    handler.postDelayed(new Runnable() {
                        @Override public void run() { finalizarBatalha(true); }
                    }, 800);
                    return;
                }

                aguardandoProxima = true;
                handler.postDelayed(new Runnable() {
                    @Override public void run() { carregarPalavra(); }
                }, 1300);

            } else {
                mostrarFeedback("\u2705 Certo! Continue...", 0xFF69F0AE);
                gerarOpcoes();
                handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        bloqueado = false;
                        for (Button b : botoesOpcao) b.setEnabled(true);
                        opcaoFocada = 0;
                        botoesOpcao[0].requestFocus();
                    }
                }, 350);
            }

        } else {
            sound.playErro();
            sound.playDano(true);
            final int dano = 8 + rng.nextInt(12);
            hpHelena = Math.max(0, hpHelena - dano);
            atualizarHP();
            AnimHelper.flashRed(tvHpHelena);
            AnimHelper.shake(tvFeedback);

            acertosConsec = 0;
            atualizarBotaoMagia();

            mostrarFeedback("\u274C Errado! Bruxo atacou: -" + dano + " HP de Helena", 0xFFFF5252);

            if (hpHelena <= 0) {
                handler.postDelayed(new Runnable() {
                    @Override public void run() { finalizarBatalha(false); }
                }, 800);
                return;
            }

            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    gerarOpcoes();
                    bloqueado = false;
                    for (Button b : botoesOpcao) b.setEnabled(true);
                    opcaoFocada = 0;
                    botoesOpcao[0].requestFocus();
                }
            }, 950);
        }
    }

    private void atualizarBotaoMagia() {
        if (magiaDisp) {
            btnMagia.setText("\u2728 Magia da Memoria (disponivel!)");
            btnMagia.setEnabled(true);
            btnMagia.setTextColor(0xFFFFD700);
        } else {
            int faltam = Math.max(0, 2 - acertosConsec);
            btnMagia.setText("\u2728 Magia da Memoria (acerte " + faltam
                + (faltam == 1 ? " palavra)" : " palavras)"));
            btnMagia.setEnabled(false);
            btnMagia.setTextColor(0xFF7A5A80);
        }
    }

    private void ativarMagia() {
        if (!magiaDisp || batalhaTerminou || modoBatalha != MODO_PALAVRA) return;
        magiaDisp = false;
        acertosConsec = 0;
        sound.playBatalhaInicio();
        tvModo.setText("\u2728 Magia da Memoria!");
        llModoPalavra.setVisibility(View.GONE);
        iniciarModoMemoria();
    }

    // ═══════════════════════════════════════════════════════════
    // MODO MEMÓRIA (magia especial)
    // ═══════════════════════════════════════════════════════════

    private void iniciarModoMemoria() {
        modoBatalha = MODO_MEMORIA;
        paresEncontrados = 0;
        primeiraCarta = -1;
        segundaCarta  = -1;
        bloqMem = false;
        focadoMem = 0;

        valoresCartas = new ArrayList<Integer>();
        for (int i = 0; i < TOTAL_PARES; i++) {
            valoresCartas.add(i);
            valoresCartas.add(i);
        }
        Collections.shuffle(valoresCartas);

        cartasViradas    = new boolean[TOTAL_CARTAS];
        cartasEncontradas = new boolean[TOTAL_CARTAS];
        frameCartas      = new FrameLayout[TOTAL_CARTAS];
        imgCartas        = new ImageView[TOTAL_CARTAS];

        glMemoria.removeAllViews();
        glMemoria.setOrientation(LinearLayout.VERTICAL);

        int linhas = TOTAL_CARTAS / COLS_MEM;
        LinearLayout[] rowLayouts = new LinearLayout[linhas];
        for (int r = 0; r < linhas; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0);
            rp.weight = 1f;
            row.setLayoutParams(rp);
            glMemoria.addView(row);
            rowLayouts[r] = row;
        }

        for (int i = 0; i < TOTAL_CARTAS; i++) {
            final int idx = i;

            FrameLayout frame = new FrameLayout(this);
            frame.setFocusable(true);
            frame.setFocusableInTouchMode(true);
            frame.setBackgroundResource(R.drawable.carta_verso_bg);

            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT);
            fp.weight = 1f;
            fp.setMargins(dp(5), dp(5), dp(5), dp(5));
            frame.setLayoutParams(fp);

            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageResource(CARTA_VERSO);
            frame.addView(iv);

            frame.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    focadoMem = idx;
                    processarCarta(idx);
                }
            });
            frame.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) focadoMem = idx;
                }
            });

            frameCartas[i] = frame;
            imgCartas[i]   = iv;
            rowLayouts[i / COLS_MEM].addView(frame);
            AnimHelper.zoomEntrada(frame, i * 55);
        }

        AnimHelper.fadeIn(glMemoria, 150);
        mostrarFeedback("\u2728 Encontre os 6 pares para causar dano massivo!", 0xFFCE93D8);

        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (frameCartas != null && frameCartas[0] != null)
                    frameCartas[0].requestFocus();
            }
        }, 400);
    }

    private void processarCarta(final int idx) {
        if (bloqMem || batalhaTerminou) return;
        if (cartasEncontradas[idx] || cartasViradas[idx]) return;
        if (primeiraCarta != -1 && segundaCarta != -1) return;

        sound.playCartaVirou();
        cartasViradas[idx] = true;
        final int val = valoresCartas.get(idx);
        final FrameLayout frame = frameCartas[idx];
        final ImageView   iv    = imgCartas[idx];

        AnimHelper.flipCarta(frame, new AnimHelper.OnHalfFlip() {
            @Override public void onHalf() {
                iv.setImageResource(CARTAS_FRENTE[val]);
            }
        });

        if (primeiraCarta == -1) {
            primeiraCarta = idx;
        } else {
            segundaCarta = idx;
            bloqMem = true;
            handler.postDelayed(new Runnable() {
                @Override public void run() { verificarPar(); }
            }, 750);
        }
    }

    private void verificarPar() {
        final int v1   = valoresCartas.get(primeiraCarta);
        final int v2   = valoresCartas.get(segundaCarta);
        final int idx1 = primeiraCarta;
        final int idx2 = segundaCarta;
        primeiraCarta = -1;
        segundaCarta  = -1;

        if (v1 == v2) {
            sound.playAcerto();
            cartasEncontradas[idx1] = true;
            cartasEncontradas[idx2] = true;
            AnimHelper.pulseGold(frameCartas[idx1]);
            AnimHelper.pulseGold(frameCartas[idx2]);

            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    frameCartas[idx1].setAlpha(0.55f);
                    frameCartas[idx2].setAlpha(0.55f);
                }
            }, 280);

            paresEncontrados++;
            bloqMem = false;
            mostrarFeedback("\u2728 Par encontrado! (" + paresEncontrados + "/" + TOTAL_PARES + ")", 0xFFCE93D8);

            if (paresEncontrados >= TOTAL_PARES) {
                final int dano = 40 + rng.nextInt(21);
                hpBruxo = Math.max(0, hpBruxo - dano);
                atualizarHP();
                AnimHelper.flashRed(tvHpBruxo);
                sound.playVitoria();
                mostrarFeedback("\u2728 MAGIA TOTAL! Bruxo perdeu " + dano + " HP!", 0xFFCE93D8);

                if (hpBruxo <= 0) {
                    handler.postDelayed(new Runnable() {
                        @Override public void run() { finalizarBatalha(true); }
                    }, 1200);
                } else {
                    handler.postDelayed(new Runnable() {
                        @Override public void run() { iniciarModoPalavra(); }
                    }, 1600);
                }
            }

        } else {
            sound.playErro();
            final int dano = 6 + rng.nextInt(8);
            hpHelena = Math.max(0, hpHelena - dano);
            atualizarHP();
            AnimHelper.flashRed(tvHpHelena);
            AnimHelper.shake(tvFeedback);
            mostrarFeedback("Par errado! Bruxo contra-atacou: -" + dano + " HP", 0xFFFF8A80);

            cartasViradas[idx1] = false;
            cartasViradas[idx2] = false;

            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    AnimHelper.flipCarta(frameCartas[idx1], new AnimHelper.OnHalfFlip() {
                        @Override public void onHalf() { imgCartas[idx1].setImageResource(CARTA_VERSO); }
                    });
                    AnimHelper.flipCarta(frameCartas[idx2], new AnimHelper.OnHalfFlip() {
                        @Override public void onHalf() { imgCartas[idx2].setImageResource(CARTA_VERSO); }
                    });
                    bloqMem = false;
                    if (hpHelena <= 0) finalizarBatalha(false);
                }
            }, 600);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HP & FIM
    // ═══════════════════════════════════════════════════════════

    private void atualizarHP() {
        int prevH = pbHpHelena.getProgress();
        int prevB = pbHpBruxo.getProgress();

        tvHpHelena.setText("HELENA " + hpHelena + "/" + HP_HELENA_MAX);
        tvHpBruxo.setText("BRUXO " + hpBruxo + "/" + HP_BRUXO_MAX);
        AnimHelper.animarXP(pbHpHelena, prevH, hpHelena);
        AnimHelper.animarXP(pbHpBruxo,  prevB,  hpBruxo);
        tvHpHelena.setTextColor(hpHelena > 30 ? 0xFF4CAF50 : 0xFFEF5350);
        tvHpBruxo.setTextColor(hpBruxo   > 50 ? 0xFFCE93D8 : 0xFFEF5350);

        if (hpHelena < prevH && ivHelenaBatalha != null) {
            AnimHelper.shake(ivHelenaBatalha);
            AnimHelper.flashRed(ivHelenaBatalha);
        }
        if (hpBruxo < prevB && llBruxoPerfil != null) {
            AnimHelper.shake(llBruxoPerfil);
            AnimHelper.flashRed(llBruxoPerfil);
        }
    }

    private void mostrarFeedback(String msg, int cor) {
        tvFeedback.setText(msg);
        tvFeedback.setTextColor(cor);
    }

    private void finalizarBatalha(final boolean helenaGanhou) {
        if (batalhaTerminou) return;
        batalhaTerminou = true;
        bloqueado = true;

        if (helenaGanhou) {
            perfil.setBatalhaStatus("ganhou");
            perfil.addXP(100);
            perfil.addStatInteligencia(5);
            perfil.addStatFoco(5);
            perfil.addStatResponsabilidade(5);
            sound.playVitoria();
            handler.postDelayed(new Runnable() {
                @Override public void run() { sound.playNivelUp(); }
            }, 800);
            AnimHelper.celebracao(tvFeedback);
            if (ivHelenaBatalha != null) AnimHelper.celebracao(ivHelenaBatalha);
            if (llBruxoPerfil  != null) AnimHelper.flashRed(llBruxoPerfil);
            mostrarFeedback("VITORIA! Bruxo derrotado!", 0xFFFFD700);
        } else {
            perfil.setBatalhaStatus("perdeu");
            sound.playDerrota();
            if (ivHelenaBatalha != null) AnimHelper.shake(ivHelenaBatalha);
            mostrarFeedback("Derrota... tente amanha!", 0xFFEF5350);
        }

        final String titulo = helenaGanhou ? "VITORIA!" : "Que pena...";
        final String mensagem = helenaGanhou
            ? "Parabens, Helena! Voce derrotou o Bruxo!\n\n"
              + "Helena: " + hpHelena + "/" + HP_HELENA_MAX + " HP\n\n"
              + "+100 XP\n+5 em todas as stats!\n\n"
              + "Voce e uma verdadeira heroina!"
            : "O Bruxo foi mais forte dessa vez...\n\n"
              + "Mas nao desista! Treine mais e tente amanha!\n"
              + "Bruxo: " + hpBruxo + "/" + HP_BRUXO_MAX + " HP restante";

        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!isFinishing()) {
                    AlertDialog.Builder b = new AlertDialog.Builder(BatalhaActivity.this);
                    b.setTitle(titulo);
                    b.setMessage(mensagem);
                    b.setCancelable(false);
                    b.setNegativeButton("Voltar ao Hub", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) { finish(); }
                    });
                    b.show();
                }
            }
        }, 1800);
    }

    // ═══════════════════════════════════════════════════════════
    // NAVEGAÇÃO D-PAD
    // ═══════════════════════════════════════════════════════════

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (batalhaTerminou) return super.onKeyDown(keyCode, event);
        return modoBatalha == MODO_PALAVRA ? navegarPalavra(keyCode) : navegarMemoria(keyCode);
    }

    private boolean navegarPalavra(int keyCode) {
        if (aguardandoProxima || bloqueado) return true;

        if (magiaBotaoFocado) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    magiaBotaoFocado = false;
                    opcaoFocada = 0;
                    botoesOpcao[opcaoFocada].requestFocus();
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    ativarMagia();
                    return true;
                default:
                    return true;
            }
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (opcaoFocada < 3) { opcaoFocada++; botoesOpcao[opcaoFocada].requestFocus(); }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (opcaoFocada > 0) { opcaoFocada--; botoesOpcao[opcaoFocada].requestFocus(); }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (magiaDisp) { magiaBotaoFocado = true; btnMagia.requestFocus(); }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                responderLetra(opcaoFocada);
                return true;
            default:
                return false;
        }
    }

    private boolean navegarMemoria(int keyCode) {
        if (bloqMem) return true;
        int novo = focadoMem;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (focadoMem % COLS_MEM < COLS_MEM - 1) novo = focadoMem + 1;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (focadoMem % COLS_MEM > 0) novo = focadoMem - 1;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (focadoMem + COLS_MEM < TOTAL_CARTAS) novo = focadoMem + COLS_MEM;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (focadoMem - COLS_MEM >= 0) novo = focadoMem - COLS_MEM;
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                processarCarta(focadoMem);
                return true;
            default:
                return false;
        }
        if (novo != focadoMem && frameCartas != null && frameCartas[novo] != null) {
            focadoMem = novo;
            frameCartas[focadoMem].requestFocus();
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITÁRIOS
    // ═══════════════════════════════════════════════════════════

    private boolean contem(int[] arr, int val) {
        for (int x : arr) if (x == val) return true;
        return false;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
