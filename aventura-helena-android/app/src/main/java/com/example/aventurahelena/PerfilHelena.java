package com.example.aventurahelena;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * PerfilHelena — Gerencia todos os dados da Helena usando SharedPreferences.
 *
 * Dados salvos:
 * - XP (experiência acumulada, permanente)
 * - Stats: Inteligencia, Foco, Responsabilidade (permanentes, max 99)
 * - Mini-jogos concluídos hoje: memoria, palavras (reseta todo dia)
 * - Tarefas feitas hoje (reseta todo dia)
 * - Status da batalha de hoje (reseta todo dia)
 * - Data do ultimo uso (para detectar novo dia e resetar dados diários)
 */
public class PerfilHelena {

    private static final String PREFS_NAME = "helena_perfil";

    // Chaves do SharedPreferences
    private static final String KEY_XP              = "xp";
    private static final String KEY_STAT_INT        = "stat_inteligencia";
    private static final String KEY_STAT_FOC        = "stat_foco";
    private static final String KEY_STAT_RES        = "stat_responsabilidade";
    private static final String KEY_MINI_MEMORIA    = "mini_memoria";
    private static final String KEY_MINI_PALAVRAS   = "mini_palavras";
    private static final String KEY_TAREFAS_FEITAS  = "tarefas_feitas";
    private static final String KEY_BATALHA_STATUS  = "batalha_status";
    private static final String KEY_DATA_HOJE       = "data_hoje";

    // Títulos por nível
    private static final String[] TITULOS = {
        "Exploradora", "Aprendiz", "Aventureira", "Guerreira",
        "Maga", "Heroina", "Lendaria"
    };

    // XP mínimo para cada nível (índice = nível - 1)
    private static final int[] XP_NIVEIS = {0, 200, 500, 1000, 1700, 2700, 4200};

    // Máximo de cada stat
    private static final int STAT_MAX = 99;

    private final SharedPreferences prefs;

    public PerfilHelena(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        verificarResetDiario();
    }

    // ─────────────────────────────────────────
    // RESET DIÁRIO
    // ─────────────────────────────────────────

    /**
     * Verifica se é um novo dia. Se sim, reseta tarefas, mini-jogos e batalha.
     * XP e stats são permanentes — nunca são resetados.
     */
    public void verificarResetDiario() {
        String hoje = obterDataHoje();
        String dataArmazenada = prefs.getString(KEY_DATA_HOJE, "");

        if (!hoje.equals(dataArmazenada)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_DATA_HOJE, hoje);
            editor.putBoolean(KEY_MINI_MEMORIA, false);
            editor.putBoolean(KEY_MINI_PALAVRAS, false);
            editor.putString(KEY_TAREFAS_FEITAS, "");
            editor.putString(KEY_BATALHA_STATUS, "");
            editor.apply();
        }
    }

    private String obterDataHoje() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // ─────────────────────────────────────────
    // XP e NÍVEL
    // ─────────────────────────────────────────

    public int getXP() {
        return prefs.getInt(KEY_XP, 0);
    }

    public void addXP(int quantidade) {
        int xpAtual = getXP();
        prefs.edit().putInt(KEY_XP, xpAtual + quantidade).apply();
    }

    /**
     * Retorna o nível atual (1 a 7) baseado no XP total.
     */
    public int getNivel() {
        int xp = getXP();
        int nivel = 1;
        for (int i = XP_NIVEIS.length - 1; i >= 0; i--) {
            if (xp >= XP_NIVEIS[i]) {
                nivel = i + 1;
                break;
            }
        }
        return nivel;
    }

    /**
     * Retorna o título do nível atual.
     */
    public String getTitulo() {
        int nivel = getNivel();
        int idx = Math.min(nivel - 1, TITULOS.length - 1);
        return TITULOS[idx];
    }

    /**
     * Calcula a porcentagem de progresso dentro do nível atual (0 a 100).
     * Usado para a barra de XP.
     */
    public int getXPPorcentagem() {
        int xp = getXP();
        int nivel = getNivel();

        if (nivel >= XP_NIVEIS.length) {
            return 100;
        }

        int xpInicio = XP_NIVEIS[nivel - 1];
        int xpFim = XP_NIVEIS[nivel];
        int xpNoNivel = xp - xpInicio;
        int xpNecessario = xpFim - xpInicio;

        if (xpNecessario <= 0) return 100;
        return (int) ((xpNoNivel * 100.0f) / xpNecessario);
    }

    // ─────────────────────────────────────────
    // STATS
    // ─────────────────────────────────────────

    public int getStatInteligencia() {
        return prefs.getInt(KEY_STAT_INT, 0);
    }

    public int getStatFoco() {
        return prefs.getInt(KEY_STAT_FOC, 0);
    }

    public int getStatResponsabilidade() {
        return prefs.getInt(KEY_STAT_RES, 0);
    }

    public void addStatInteligencia(int qtd) {
        int atual = getStatInteligencia();
        prefs.edit().putInt(KEY_STAT_INT, Math.min(STAT_MAX, atual + qtd)).apply();
    }

    public void addStatFoco(int qtd) {
        int atual = getStatFoco();
        prefs.edit().putInt(KEY_STAT_FOC, Math.min(STAT_MAX, atual + qtd)).apply();
    }

    public void addStatResponsabilidade(int qtd) {
        int atual = getStatResponsabilidade();
        prefs.edit().putInt(KEY_STAT_RES, Math.min(STAT_MAX, atual + qtd)).apply();
    }

    // ─────────────────────────────────────────
    // MINI-JOGOS
    // ─────────────────────────────────────────

    public boolean isMemoriaConcluida() {
        return prefs.getBoolean(KEY_MINI_MEMORIA, false);
    }

    public boolean isPalavrasConcluida() {
        return prefs.getBoolean(KEY_MINI_PALAVRAS, false);
    }

    public void setMemoriaConcluida() {
        prefs.edit().putBoolean(KEY_MINI_MEMORIA, true).apply();
    }

    public void setPalavrasConcluida() {
        prefs.edit().putBoolean(KEY_MINI_PALAVRAS, true).apply();
    }

    // ─────────────────────────────────────────
    // TAREFAS
    // ─────────────────────────────────────────

    /**
     * Retorna o conjunto de IDs de tarefas feitas hoje.
     */
    public Set<String> getTarefasFeitas() {
        String raw = prefs.getString(KEY_TAREFAS_FEITAS, "");
        Set<String> set = new HashSet<String>();
        if (raw != null && !raw.isEmpty()) {
            for (String id : raw.split(",")) {
                if (!id.isEmpty()) set.add(id);
            }
        }
        return set;
    }

    public boolean isTarefaFeita(String id) {
        return getTarefasFeitas().contains(id);
    }

    public void marcarTarefa(String id) {
        Set<String> feitas = getTarefasFeitas();
        feitas.add(id);
        StringBuilder sb = new StringBuilder();
        for (String f : feitas) {
            if (sb.length() > 0) sb.append(",");
            sb.append(f);
        }
        prefs.edit().putString(KEY_TAREFAS_FEITAS, sb.toString()).apply();
    }

    public int getQuantidadeTarefasFeitas() {
        return getTarefasFeitas().size();
    }

    public boolean todasTarefasConcluidas(int totalTarefas) {
        return getQuantidadeTarefasFeitas() >= totalTarefas;
    }

    // ─────────────────────────────────────────
    // BATALHA
    // ─────────────────────────────────────────

    /**
     * Retorna "ganhou", "perdeu", ou "" (batalha ainda não feita).
     */
    public String getBatalhaStatus() {
        return prefs.getString(KEY_BATALHA_STATUS, "");
    }

    public boolean batalhaDisponivel() {
        return getBatalhaStatus().isEmpty();
    }

    public void setBatalhaStatus(String status) {
        prefs.edit().putString(KEY_BATALHA_STATUS, status).apply();
    }

    // ─────────────────────────────────────────
    // VERIFICAR DESBLOQUEIO DA BATALHA
    // ─────────────────────────────────────────

    /**
     * Retorna true se todos os requisitos para a batalha estão completos.
     * Requisitos: memoria ✅ + palavras ✅ + todas as 10 tarefas ✅ + batalha ainda não feita
     */
    public boolean batalhaDesbloqueada(int totalTarefas) {
        return isMemoriaConcluida()
            && isPalavrasConcluida()
            && todasTarefasConcluidas(totalTarefas)
            && batalhaDisponivel();
    }

    // ─────────────────────────────────────────
    // RESET (ADMIN)
    // ─────────────────────────────────────────

    public void resetarDiaAtual() {
        prefs.edit()
            .putBoolean(KEY_MINI_MEMORIA,   false)
            .putBoolean(KEY_MINI_PALAVRAS,  false)
            .putString(KEY_TAREFAS_FEITAS,  "")
            .putString(KEY_BATALHA_STATUS,  "")
            .apply();
    }

    public void resetarTudo() {
        prefs.edit()
            .putInt(KEY_XP,                 0)
            .putInt(KEY_STAT_INT,           0)
            .putInt(KEY_STAT_FOC,           0)
            .putInt(KEY_STAT_RES,           0)
            .putBoolean(KEY_MINI_MEMORIA,   false)
            .putBoolean(KEY_MINI_PALAVRAS,  false)
            .putString(KEY_TAREFAS_FEITAS,  "")
            .putString(KEY_BATALHA_STATUS,  "")
            .apply();
    }
}
