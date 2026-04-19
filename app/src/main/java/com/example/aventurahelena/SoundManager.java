package com.example.aventurahelena;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;

/**
 * SoundManager — gerencia todos os efeitos sonoros do jogo.
 *
 * Usa ToneGenerator (embutido no Android) sem precisar de arquivos de áudio.
 * Não requer nenhuma permissão especial.
 *
 * Sons disponíveis:
 *   playCartaVirou()   — carta virada na memória
 *   playAcerto()       — resposta/par correto
 *   playErro()         — resposta/par errado
 *   playNivelUp()      — subiu de nível
 *   playXPGanho()      — ganhou XP
 *   playBatalhaInicio()— batalha começando
 *   playDano(helena)   — recebeu dano na batalha
 *   playVitoria()      — vitória
 *   playDerrota()      — derrota
 *   release()          — libera recurso (chamar no onDestroy)
 */
public class SoundManager {

    private ToneGenerator tone;
    private final Handler handler = new Handler();

    public SoundManager() {
        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
        } catch (Exception e) {
            tone = null;
        }
    }

    /** Carta virada na memória — beep curto */
    public void playCartaVirou() {
        play(ToneGenerator.TONE_PROP_BEEP, 90);
    }

    /** Par/resposta correto — dois beeps ascendentes */
    public void playAcerto() {
        play(ToneGenerator.TONE_CDMA_PIP, 100);
        postDelayed(new Runnable() {
            @Override public void run() { play(ToneGenerator.TONE_CDMA_ANSWER, 180); }
        }, 160);
    }

    /** Erro — som descendente */
    public void playErro() {
        play(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 350);
    }

    /** Subiu de nível — três beeps ascendentes */
    public void playNivelUp() {
        play(ToneGenerator.TONE_CDMA_PIP, 100);
        postDelayed(new Runnable() {
            @Override public void run() { play(ToneGenerator.TONE_CDMA_PIP, 100); }
        }, 140);
        postDelayed(new Runnable() {
            @Override public void run() { play(ToneGenerator.TONE_CDMA_ANSWER, 250); }
        }, 280);
        postDelayed(new Runnable() {
            @Override public void run() { play(ToneGenerator.TONE_CDMA_ANSWER, 300); }
        }, 560);
    }

    /** XP ganho — beep suave */
    public void playXPGanho() {
        play(ToneGenerator.TONE_CDMA_PIP, 130);
    }

    /** Batalha iniciando — som de alerta */
    public void playBatalhaInicio() {
        play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400);
        postDelayed(new Runnable() {
            @Override public void run() { play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300); }
        }, 500);
    }

    /**
     * Dano recebido na batalha.
     * @param helenaRecebeu true = Helena tomou dano, false = Bruxo tomou dano
     */
    public void playDano(boolean helenaRecebeu) {
        if (helenaRecebeu) {
            play(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 200);
        } else {
            play(ToneGenerator.TONE_CDMA_ANSWER, 150);
        }
    }

    /** Vitória — sequência alegre */
    public void playVitoria() {
        play(ToneGenerator.TONE_CDMA_PIP, 90);
        postDelayed(new Runnable() {
            @Override public void run() { play(ToneGenerator.TONE_CDMA_PIP, 90); }
        }, 120);
        postDelayed(new Runnable() {
            @Override public void run() { play(ToneGenerator.TONE_CDMA_ANSWER, 90); }
        }, 240);
        postDelayed(new Runnable() {
            @Override public void run() { play(ToneGenerator.TONE_CDMA_ANSWER, 90); }
        }, 360);
        postDelayed(new Runnable() {
            @Override public void run() { play(ToneGenerator.TONE_CDMA_ANSWER, 200); }
        }, 500);
    }

    /** Derrota — dois tons descendentes */
    public void playDerrota() {
        play(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 220);
        postDelayed(new Runnable() {
            @Override public void run() { play(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 400); }
        }, 320);
    }

    // ─── helpers internos ───

    private void play(int tipoTone, int duracaoMs) {
        if (tone != null) {
            try {
                tone.startTone(tipoTone, duracaoMs);
            } catch (Exception ignored) {}
        }
    }

    private void postDelayed(Runnable r, long delayMs) {
        handler.postDelayed(r, delayMs);
    }

    /** Chame no onDestroy da Activity para liberar o recurso. */
    public void release() {
        handler.removeCallbacksAndMessages(null);
        if (tone != null) {
            try { tone.release(); } catch (Exception ignored) {}
            tone = null;
        }
    }
}
