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

public class MemoriaActivity extends Activity {

    private static final int TOTAL_CARTAS = 24;
    private static final int COLUNAS = 6;
    private static final int LINHAS  = 4;
    private static final int TOTAL_PARES = 12;
    private static final int DELAY_FECHAR = 1100;

    private static final int XP_GANHO       = 60;
    private static final int STAT_INT_GANHO = 3;
    private static final int STAT_FOC_GANHO = 3;

    private static final String[] EMOJIS = {
        "\u2B50", "\uD83C\uDF08", "\uD83D\uDC31", "\uD83D\uDC36",
        "\uD83D\uDC38", "\uD83E\uDD84", "\uD83C\uDF79", "\uD83C\uDF80",
        "\uD83C\uDF55", "\uD83C\uDF6D", "\uD83C\uDFF5", "\uD83C\uDFC6"
    };

    private static final int COR_CARTA_FECHADA = 0xFF4A148C;
    private static final int COR_CARTA_ABERTA  = 0xFF1565C0;
    private static final int COR_PAR_CORRETO   = 0xFF2E7D32;
    private static final int COR_PAR_ERRADO    = 0xFFB71C1C;

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
    private SoundManager sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memoria);

        perfil  = new PerfilHelena(this);
        handler = new Handler();
        sound   = new SoundManager();

        tvPlacar = (TextView) findViewById(R.id.tv_placar);

        TextView tvTitulo = (TextView) findViewById(R.id.tv_titulo_memoria);
        tvTitulo.setText("Jogo da Memoria");

        inicializarCartas();
        criarBotoes();
        atualizarPlacar();

        botoes[indiceFocado].requestFocus();

        // Animação de entrada: cartas aparecem com fade
        GridLayout grid = (GridLayout) findViewById(R.id.grid_cartas);
        AnimHelper.fadeIn(grid, 400);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sound.release();
    }

    private void inicializarCartas() {
        listaValores = new ArrayList<Integer>();
        for (int i = 0; i < TOTAL_PARES; i++) {
            listaValores.add(i);
            listaValores.add(i);
        }
        Collections.shuffle(listaValores);
    }

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
     * Vira a carta com animação de flip 3D + som.
     */
    private void processarClique(int indice) {
        if (bloqueado) return;
        if (!botoes[indice].isEnabled()) return;
        if (!"?".equals(botoes[indice].getText().toString())) return;

        final int valorCarta = listaValores.get(indice);
        final String emoji   = EMOJIS[valorCarta];
        final Button carta   = botoes[indice];

        // Som + animação de flip
        sound.playCartaVirou();
        AnimHelper.flipCarta(carta, new AnimHelper.OnHalfFlip() {
            @Override
            public void onHalf() {
                carta.setText(emoji);
                carta.setTextSize(26);
            }
        });

        if (indicePrimeira == -1) {
            indicePrimeira = indice;
        } else {
            indiceSegunda = indice;
            bloqueado = true;

            // Pequeno delay para deixar o flip terminar antes de verificar
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    verificarPar();
                }
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
            // PAR CORRETO — pulso dourado nas duas cartas
            sound.playAcerto();
            AnimHelper.pulseGold(botoes[idx1]);
            AnimHelper.pulseGold(botoes[idx2]);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    botoes[idx1].setEnabled(false);
                    botoes[idx2].setEnabled(false);
                    paresEncontrados++;
                    atualizarPlacar();

                    if (paresEncontrados == TOTAL_PARES) {
                        mostrarVitoria();
                    }
                }
            }, 200);

        } else {
            // PAR ERRADO — shake nas cartas + som de erro
            sound.playErro();
            AnimHelper.shake(botoes[idx1]);
            AnimHelper.shake(botoes[idx2]);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Flip de volta para "?"
                    AnimHelper.flipCarta(botoes[idx1], new AnimHelper.OnHalfFlip() {
                        @Override
                        public void onHalf() {
                            botoes[idx1].setText("?");
                            botoes[idx1].setTextSize(22);
                        }
                    });
                    AnimHelper.flipCarta(botoes[idx2], new AnimHelper.OnHalfFlip() {
                        @Override
                        public void onHalf() {
                            botoes[idx2].setText("?");
                            botoes[idx2].setTextSize(22);
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
        bloqueado = false;
    }

    private void atualizarPlacar() {
        tvPlacar.setText("Pares: " + paresEncontrados + " / " + TOTAL_PARES);
    }

    private void mostrarVitoria() {
        if (!perfil.isMemoriaConcluida()) {
            int xpAntes = perfil.getXPPorcentagem();
            perfil.addXP(XP_GANHO);
            perfil.addStatInteligencia(STAT_INT_GANHO);
            perfil.addStatFoco(STAT_FOC_GANHO);
            perfil.setMemoriaConcluida();

            // Som de vitória + XP ganho
            sound.playVitoria();
            if (perfil.getNivel() > 1) sound.playNivelUp();
        } else {
            sound.playVitoria();
        }

        // Celebração visual no placar
        AnimHelper.celebracao(tvPlacar);

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

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) builder.show();
            }
        }, 600);
    }

    private void reiniciarJogo() {
        paresEncontrados = 0;
        indicePrimeira   = -1;
        indiceSegunda    = -1;
        bloqueado = false;
        indiceFocado = 0;

        inicializarCartas();

        for (int i = 0; i < TOTAL_CARTAS; i++) {
            botoes[i].setText("?");
            botoes[i].setTextSize(22);
            botoes[i].setEnabled(true);
        }

        atualizarPlacar();
        botoes[0].requestFocus();
    }
}
