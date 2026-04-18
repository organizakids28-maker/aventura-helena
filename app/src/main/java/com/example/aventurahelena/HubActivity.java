package com.example.aventurahelena;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * HubActivity — Tela principal da Aventura da Helena.
 *
 * Mostra o perfil da Helena (XP, nível, stats) e os botões para os mini-jogos,
 * tarefas de casa e batalha final.
 *
 * Navegação pelo controle remoto:
 * - Cima/Baixo: navega entre os botões
 * - OK/Enter: abre o mini-jogo ou ação selecionada
 */
public class HubActivity extends Activity {

    private static final int TOTAL_TAREFAS = 10;

    private PerfilHelena perfil;

    private TextView tvNivel;
    private TextView tvXP;
    private ProgressBar pbXP;
    private TextView tvInt;
    private TextView tvFoc;
    private TextView tvRes;
    private TextView tvChecklist;
    private Button btnBatalha;

    // Botões de ação (para navegação manual no D-pad)
    private Button[] botoesAcao;
    private int indiceFocado = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);

        perfil = new PerfilHelena(this);

        // Referências às views
        tvNivel     = (TextView)  findViewById(R.id.tv_nivel);
        tvXP        = (TextView)  findViewById(R.id.tv_xp);
        pbXP        = (ProgressBar) findViewById(R.id.pb_xp);
        tvInt       = (TextView)  findViewById(R.id.tv_int);
        tvFoc       = (TextView)  findViewById(R.id.tv_foc);
        tvRes       = (TextView)  findViewById(R.id.tv_res);
        tvChecklist = (TextView)  findViewById(R.id.tv_checklist);
        btnBatalha  = (Button)    findViewById(R.id.btn_batalha);

        Button btnMemoria = (Button) findViewById(R.id.btn_memoria);
        Button btnPalavras = (Button) findViewById(R.id.btn_palavras);
        Button btnTarefas  = (Button) findViewById(R.id.btn_tarefas);

        // Array de botões para navegação D-pad (ordem de cima para baixo)
        botoesAcao = new Button[]{btnMemoria, btnPalavras, btnTarefas, btnBatalha};

        // Configura cliques
        btnMemoria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HubActivity.this, MemoriaActivity.class));
            }
        });

        btnPalavras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HubActivity.this, PalavraActivity.class));
            }
        });

        btnTarefas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HubActivity.this, TarefasActivity.class));
            }
        });

        btnBatalha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HubActivity.this, BatalhaActivity.class));
            }
        });

        // Registra qual botão está em foco
        for (int i = 0; i < botoesAcao.length; i++) {
            final int idx = i;
            botoesAcao[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) indiceFocado = idx;
                }
            });
        }

        // Foco inicial no primeiro botão
        btnMemoria.requestFocus();
    }

    /**
     * onResume é chamado sempre que voltamos para esta tela (ex: após terminar um mini-jogo).
     * Atualiza todas as informações do perfil.
     */
    @Override
    protected void onResume() {
        super.onResume();
        perfil.verificarResetDiario();
        atualizarUI();
    }

    /**
     * Atualiza todos os elementos visuais do hub com os dados atuais da Helena.
     */
    private void atualizarUI() {
        int nivel = perfil.getNivel();
        String titulo = perfil.getTitulo();
        int xp = perfil.getXP();
        int xpPct = perfil.getXPPorcentagem();

        // Nível e XP
        tvNivel.setText("Nivel " + nivel + " - " + titulo);
        tvXP.setText(xp + " XP");
        pbXP.setProgress(xpPct);

        // Stats
        tvInt.setText(String.valueOf(perfil.getStatInteligencia()));
        tvFoc.setText(String.valueOf(perfil.getStatFoco()));
        tvRes.setText(String.valueOf(perfil.getStatResponsabilidade()));

        // Checklist de batalha
        boolean memoria   = perfil.isMemoriaConcluida();
        boolean palavras  = perfil.isPalavrasConcluida();
        int tarefasFeitas = perfil.getQuantidadeTarefasFeitas();
        String statusBatalha = perfil.getBatalhaStatus();

        StringBuilder sb = new StringBuilder();

        if (!statusBatalha.isEmpty()) {
            // Batalha já foi feita hoje
            if ("ganhou".equals(statusBatalha)) {
                sb.append("Voce venceu a batalha hoje! Parabens!");
            } else {
                sb.append("Batalha perdida hoje. Tente amanha!");
            }
        } else {
            // Mostra o que falta para desbloquear
            sb.append(memoria  ? "[OK] Memoria\n"  : "[  ] Memoria - complete o jogo!\n");
            sb.append(palavras ? "[OK] Palavras\n" : "[  ] Palavras - complete o jogo!\n");
            sb.append("Tarefas: " + tarefasFeitas + "/" + TOTAL_TAREFAS + "\n");

            if (!memoria || !palavras || tarefasFeitas < TOTAL_TAREFAS) {
                sb.append("\nComplete tudo para desbloquear a batalha!");
            }
        }

        tvChecklist.setText(sb.toString());

        // Mostrar ou esconder botão de batalha
        boolean batalhaDesbloqueada = perfil.batalhaDesbloqueada(TOTAL_TAREFAS);
        if (batalhaDesbloqueada) {
            btnBatalha.setVisibility(View.VISIBLE);
            // Atualiza lista de botões para incluir a batalha
        } else {
            btnBatalha.setVisibility(View.GONE);
        }
    }

    /**
     * Intercepta o D-pad para navegar entre os botões de ação.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Descobre quantos botões estão visíveis
        int totalBotoes = 3; // memoria, palavras, tarefas
        if (btnBatalha.getVisibility() == View.VISIBLE) {
            totalBotoes = 4;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (indiceFocado < totalBotoes - 1) {
                    indiceFocado++;
                    botoesAcao[indiceFocado].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (indiceFocado > 0) {
                    indiceFocado--;
                    botoesAcao[indiceFocado].requestFocus();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                botoesAcao[indiceFocado].performClick();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
