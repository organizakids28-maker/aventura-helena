package com.example.aventurahelena;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AdminActivity extends Activity {

    private static final String PREFS_ADMIN = "helena_admin";
    private static final String KEY_PIN     = "admin_pin";
    private static final String PIN_PADRAO  = "1234";

    private final StringBuilder pinDigitado = new StringBuilder();
    private String pinCorreto;

    private LinearLayout llPinEntry;
    private LinearLayout llPainel;

    private TextView tvPinD1, tvPinD2, tvPinD3, tvPinD4;
    private TextView tvErroPin;
    private TextView tvAdminStats;

    private PerfilHelena perfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        perfil = new PerfilHelena(this);
        SharedPreferences adminPrefs = getSharedPreferences(PREFS_ADMIN, MODE_PRIVATE);
        pinCorreto = adminPrefs.getString(KEY_PIN, PIN_PADRAO);

        llPinEntry = (LinearLayout) findViewById(R.id.ll_pin_entry);
        llPainel   = (LinearLayout) findViewById(R.id.ll_painel_admin);

        tvPinD1      = (TextView) findViewById(R.id.tv_pin_d1);
        tvPinD2      = (TextView) findViewById(R.id.tv_pin_d2);
        tvPinD3      = (TextView) findViewById(R.id.tv_pin_d3);
        tvPinD4      = (TextView) findViewById(R.id.tv_pin_d4);
        tvErroPin    = (TextView) findViewById(R.id.tv_erro_pin);
        tvAdminStats = (TextView) findViewById(R.id.tv_admin_stats);

        configurarNumpad();
        configurarPainel();

        ((Button) findViewById(R.id.btn_n1)).requestFocus();
    }

    private void configurarNumpad() {
        int[] ids = {
            R.id.btn_n0, R.id.btn_n1, R.id.btn_n2, R.id.btn_n3,
            R.id.btn_n4, R.id.btn_n5, R.id.btn_n6,
            R.id.btn_n7, R.id.btn_n8, R.id.btn_n9
        };
        for (int i = 0; i < ids.length; i++) {
            final String digito = String.valueOf(i);
            ((Button) findViewById(ids[i])).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { adicionarDigito(digito); }
            });
        }
        ((Button) findViewById(R.id.btn_del)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { removerDigito(); }
        });
        ((Button) findViewById(R.id.btn_entrar)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { verificarPin(); }
        });
    }

    private void configurarPainel() {
        ((Button) findViewById(R.id.btn_resetar_dia)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { resetarDia(); }
        });
        ((Button) findViewById(R.id.btn_limpar_batalha)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { limparBatalha(); }
        });
        ((Button) findViewById(R.id.btn_resetar_tudo)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { resetarTudo(); }
        });
        ((Button) findViewById(R.id.btn_voltar_admin)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
    }

    private void adicionarDigito(String d) {
        if (pinDigitado.length() < 4) {
            pinDigitado.append(d);
            atualizarDisplayPin();
        }
    }

    private void removerDigito() {
        if (pinDigitado.length() > 0) {
            pinDigitado.deleteCharAt(pinDigitado.length() - 1);
            atualizarDisplayPin();
        }
    }

    private void atualizarDisplayPin() {
        TextView[] tv = {tvPinD1, tvPinD2, tvPinD3, tvPinD4};
        for (int i = 0; i < 4; i++) {
            tv[i].setText(i < pinDigitado.length() ? "*" : "-");
        }
        tvErroPin.setVisibility(View.GONE);
    }

    private void verificarPin() {
        if (pinDigitado.toString().equals(pinCorreto)) {
            llPinEntry.setVisibility(View.GONE);
            llPainel.setVisibility(View.VISIBLE);
            atualizarStatsAdmin();
            ((Button) findViewById(R.id.btn_resetar_dia)).requestFocus();
        } else {
            pinDigitado.setLength(0);
            atualizarDisplayPin();
            tvErroPin.setVisibility(View.VISIBLE);
        }
    }

    private void atualizarStatsAdmin() {
        StringBuilder sb = new StringBuilder();
        sb.append("XP Total: ").append(perfil.getXP());
        sb.append("  |  Nivel ").append(perfil.getNivel());
        sb.append(" — ").append(perfil.getTitulo()).append("\n\n");
        sb.append("Inteligencia: ").append(perfil.getStatInteligencia()).append("\n");
        sb.append("Foco: ").append(perfil.getStatFoco()).append("\n");
        sb.append("Responsabilidade: ").append(perfil.getStatResponsabilidade()).append("\n\n");
        sb.append("Memoria hoje: ").append(perfil.isMemoriaConcluida() ? "Concluida" : "Nao jogada").append("\n");
        sb.append("Palavras hoje: ").append(perfil.isPalavrasConcluida() ? "Concluida" : "Nao jogada").append("\n");
        sb.append("Tarefas hoje: ").append(perfil.getQuantidadeTarefasFeitas()).append("\n\n");
        String bat = perfil.getBatalhaStatus();
        sb.append("Batalha hoje: ").append(bat.isEmpty() ? "Disponivel" : bat);
        tvAdminStats.setText(sb.toString());
    }

    private void resetarDia() {
        perfil.resetarDiaAtual();
        atualizarStatsAdmin();
    }

    private void limparBatalha() {
        perfil.setBatalhaStatus("");
        atualizarStatsAdmin();
    }

    private void resetarTudo() {
        perfil.resetarTudo();
        atualizarStatsAdmin();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && llPainel.getVisibility() == View.VISIBLE) {
            llPainel.setVisibility(View.GONE);
            llPinEntry.setVisibility(View.VISIBLE);
            pinDigitado.setLength(0);
            atualizarDisplayPin();
            ((Button) findViewById(R.id.btn_n1)).requestFocus();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
