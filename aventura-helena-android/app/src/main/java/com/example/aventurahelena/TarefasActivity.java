package com.example.aventurahelena;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TarefasActivity extends Activity {

    private static final String[] TAREFAS_IDS = {
        "t01", "t02", "t03", "t04", "t05",
        "t06", "t07", "t08", "t09", "t10"
    };

    private static final String[] TAREFAS_TEXTOS = {
        "Arrumar a cama",
        "Guardar os brinquedos",
        "Escovar os dentes (manha)",
        "Escovar os dentes (noite)",
        "Tomar banho",
        "Colocar o prato na pia",
        "Ajudar a arrumar a mesa",
        "Estudar ou ler um livro",
        "Colocar roupa suja no cesto",
        "Dizer boa noite para a familia"
    };

    private static final int XP_POR_TAREFA   = 15;
    private static final int STAT_POR_TAREFA = 1;

    private PerfilHelena perfil;
    private SoundManager sound;
    private Handler handler;

    private Button[] btnTarefas;
    private Button btnVoltar;
    private Button[] todosBotoes;
    private int indiceFocado = 0;

    private TextView tvProgresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tarefas);

        perfil     = new PerfilHelena(this);
        sound      = new SoundManager();
        handler    = new Handler();

        tvProgresso = (TextView) findViewById(R.id.tv_progresso_tarefas);
        btnVoltar   = (Button)   findViewById(R.id.btn_voltar);

        criarBotoesTarefas();
        atualizarProgresso();

        todosBotoes = new Button[TAREFAS_IDS.length + 1];
        for (int i = 0; i < TAREFAS_IDS.length; i++) todosBotoes[i] = btnTarefas[i];
        todosBotoes[TAREFAS_IDS.length] = btnVoltar;

        for (int i = 0; i < todosBotoes.length; i++) {
            final int idx = i;
            todosBotoes[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) indiceFocado = idx;
                }
            });
        }

        btnVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        indiceFocado = 0;
        for (int i = 0; i < TAREFAS_IDS.length; i++) {
            if (!perfil.isTarefaFeita(TAREFAS_IDS[i])) {
                indiceFocado = i;
                break;
            }
        }
        todosBotoes[indiceFocado].requestFocus();

        // Animação de entrada: lista aparece com fade
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll_tarefas);
        AnimHelper.fadeIn(ll, 350);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sound.release();
    }

    private void criarBotoesTarefas() {
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll_tarefas);
        btnTarefas = new Button[TAREFAS_IDS.length];

        for (int i = 0; i < TAREFAS_IDS.length; i++) {
            Button btn = new Button(this);
            final String id     = TAREFAS_IDS[i];
            final String texto  = TAREFAS_TEXTOS[i];
            final int indice    = i;

            boolean jaConcluida = perfil.isTarefaFeita(id);

            btn.setText(jaConcluida ? ("[OK] " + texto) : ("[  ] " + texto));
            btn.setTextSize(18);
            btn.setTextColor(0xFFFFFFFF);
            btn.setFocusable(true);
            btn.setFocusableInTouchMode(true);
            btn.setBackgroundResource(R.drawable.btn_action);
            btn.setEnabled(!jaConcluida);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 10);
            btn.setLayoutParams(params);

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!perfil.isTarefaFeita(id)) {
                        perfil.marcarTarefa(id);
                        perfil.addXP(XP_POR_TAREFA);
                        perfil.addStatResponsabilidade(STAT_POR_TAREFA);

                        btn.setText("[OK] " + texto);
                        btn.setEnabled(false);

                        // Som + animação de conclusão
                        sound.playAcerto();
                        AnimHelper.pulseGold(btn);

                        atualizarProgresso();

                        // Verifica se completou todas as tarefas
                        if (perfil.getQuantidadeTarefasFeitas() >= TAREFAS_IDS.length) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sound.playVitoria();
                                    handler.postDelayed(new Runnable() {
                                        @Override public void run() { sound.playNivelUp(); }
                                    }, 700);
                                }
                            }, 300);
                            AnimHelper.celebracao(tvProgresso);
                        } else {
                            // Só XP ganho para tarefas individuais
                            handler.postDelayed(new Runnable() {
                                @Override public void run() { sound.playXPGanho(); }
                            }, 400);
                        }
                    }
                }
            });

            ll.addView(btn);
            btnTarefas[i] = btn;
        }
    }

    private void atualizarProgresso() {
        int feitas = perfil.getQuantidadeTarefasFeitas();
        tvProgresso.setText(feitas + " de " + TAREFAS_IDS.length + " tarefas concluidas");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (indiceFocado < todosBotoes.length - 1) {
                    indiceFocado++;
                    todosBotoes[indiceFocado].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (indiceFocado > 0) {
                    indiceFocado--;
                    todosBotoes[indiceFocado].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                todosBotoes[indiceFocado].performClick();
                return true;

            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
