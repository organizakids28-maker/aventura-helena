package com.example.aventurahelena;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * TarefasActivity — Lista de tarefas de casa da Helena.
 *
 * Mostra 10 tarefas como botões. Cada tarefa pode ser marcada como concluída
 * uma vez por dia. Ao concluir, ganha XP e Responsabilidade.
 *
 * Navegação D-pad: cima/baixo entre as tarefas, OK para marcar como feita.
 */
public class TarefasActivity extends Activity {

    // Definição das 10 tarefas
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

    private static final int XP_POR_TAREFA  = 15;
    private static final int STAT_POR_TAREFA = 1;

    private PerfilHelena perfil;
    private Button[] btnTarefas;
    private Button btnVoltar;

    // Todos os botões em sequência para navegação
    private Button[] todosBotoes;
    private int indiceFocado = 0;

    private TextView tvProgresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tarefas);

        perfil     = new PerfilHelena(this);
        tvProgresso = (TextView) findViewById(R.id.tv_progresso_tarefas);
        btnVoltar  = (Button) findViewById(R.id.btn_voltar);

        criarBotoesTarefas();
        atualizarProgresso();

        // Array com todos os botões para navegação (tarefas + voltar)
        todosBotoes = new Button[TAREFAS_IDS.length + 1];
        for (int i = 0; i < TAREFAS_IDS.length; i++) todosBotoes[i] = btnTarefas[i];
        todosBotoes[TAREFAS_IDS.length] = btnVoltar;

        // Configura foco
        for (int i = 0; i < todosBotoes.length; i++) {
            final int idx = i;
            todosBotoes[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) indiceFocado = idx;
                }
            });
        }

        // Botão voltar
        btnVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Foco no primeiro botão não concluído, ou no primeiro
        indiceFocado = 0;
        for (int i = 0; i < TAREFAS_IDS.length; i++) {
            if (!perfil.isTarefaFeita(TAREFAS_IDS[i])) {
                indiceFocado = i;
                break;
            }
        }
        todosBotoes[indiceFocado].requestFocus();
    }

    /**
     * Cria os botões das tarefas dinamicamente no LinearLayout.
     */
    private void criarBotoesTarefas() {
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll_tarefas);
        btnTarefas = new Button[TAREFAS_IDS.length];

        for (int i = 0; i < TAREFAS_IDS.length; i++) {
            Button btn = new Button(this);
            final String id   = TAREFAS_IDS[i];
            final String texto = TAREFAS_TEXTOS[i];
            final int indice = i;

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
                        atualizarProgresso();
                    }
                }
            });

            ll.addView(btn);
            btnTarefas[i] = btn;
        }
    }

    /**
     * Atualiza o contador de progresso no topo.
     */
    private void atualizarProgresso() {
        int feitas = perfil.getQuantidadeTarefasFeitas();
        tvProgresso.setText(feitas + " de " + TAREFAS_IDS.length + " tarefas concluidas");
    }

    /**
     * Navegação D-pad entre as tarefas.
     */
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
