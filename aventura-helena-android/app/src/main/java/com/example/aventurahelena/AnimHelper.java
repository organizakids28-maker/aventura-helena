package com.example.aventurahelena;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;

/**
 * AnimHelper — utilitários de animação para a Aventura da Helena.
 *
 * Usa apenas APIs nativas do Android (sem bibliotecas externas).
 *
 * Métodos disponíveis:
 *   shake(view)               — balança a view (resposta errada)
 *   pulseGold(view)           — pulso dourado (acerto)
 *   flipCarta(view, callback) — fade out → troca conteúdo → fade in (memória)
 *   fadeIn(view, ms)          — fade de 0 a 1
 *   animarXP(pb, de, para)    — barra de XP animando suavemente
 *   flashRed(view)            — flash vermelho (dano / erro)
 *   zoomEntrada(view, delay)  — zoom + fade de entrada
 *   celebracao(view)          — escala + rotação pequena (vitória)
 */
public class AnimHelper {

    /** Interface para callback quando a carta fica "transparente" no meio do flip */
    public interface OnHalfFlip {
        void onHalf();
    }

    // ─── Shake (erro) ───────────────────────────────────────────────────────

    public static void shake(View view) {
        TranslateAnimation anim = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, -0.05f,
            Animation.RELATIVE_TO_SELF,  0.05f,
            Animation.RELATIVE_TO_SELF,  0f,
            Animation.RELATIVE_TO_SELF,  0f
        );
        anim.setDuration(60);
        anim.setRepeatCount(5);
        anim.setRepeatMode(Animation.REVERSE);
        view.startAnimation(anim);
    }

    // ─── Pulso dourado (acerto) ──────────────────────────────────────────────

    public static void pulseGold(final View view) {
        ScaleAnimation anim = new ScaleAnimation(
            1f, 1.18f, 1f, 1.18f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        anim.setDuration(130);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);
        view.startAnimation(anim);
    }

    // ─── "Flip" de carta via fade (mais compatível com TV boxes) ─────────────

    /**
     * Simula o flip de carta com fade out → troca conteúdo → fade in.
     * Mais compatível do que rotationY em boxes Android TV mais antigos.
     */
    public static void flipCarta(final View carta, final OnHalfFlip callback) {
        // Fase 1: fade out (carta "desaparece")
        final AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(130);
        fadeOut.setFillAfter(true);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}

            @Override
            public void onAnimationEnd(Animation a) {
                // Troca o conteúdo no ponto mais transparente
                if (callback != null) callback.onHalf();

                // Fase 2: fade in (carta "aparece" com novo conteúdo)
                carta.setAlpha(0f);
                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(130);
                fadeIn.setFillAfter(false);
                fadeIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation a) {}
                    @Override public void onAnimationRepeat(Animation a) {}
                    @Override public void onAnimationEnd(Animation a) {
                        carta.setAlpha(1f);
                    }
                });
                carta.startAnimation(fadeIn);
            }
        });

        carta.startAnimation(fadeOut);
    }

    // ─── Flash vermelho (dano / erro) ────────────────────────────────────────

    public static void flashRed(View view) {
        AlphaAnimation anim = new AlphaAnimation(1f, 0.2f);
        anim.setDuration(80);
        anim.setRepeatCount(3);
        anim.setRepeatMode(Animation.REVERSE);
        view.startAnimation(anim);
    }

    // ─── Fade in ─────────────────────────────────────────────────────────────

    public static void fadeIn(View view, int duracaoMs) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(duracaoMs);
        anim.setFillAfter(true);
        view.startAnimation(anim);
        view.setAlpha(1f);
    }

    // ─── Barra de XP animando ────────────────────────────────────────────────

    public static void animarXP(final ProgressBar pb, int valorAtual, int valorNovo) {
        ValueAnimator anim = ValueAnimator.ofInt(valorAtual, valorNovo);
        anim.setDuration(700);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                pb.setProgress((Integer) animation.getAnimatedValue());
            }
        });
        anim.start();
    }

    // ─── Zoom de entrada de card/tela ────────────────────────────────────────

    public static void zoomEntrada(View view, int delayMs) {
        ScaleAnimation anim = new ScaleAnimation(
            0.7f, 1f, 0.7f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        AnimationSet set = new AnimationSet(true);
        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        set.addAnimation(anim);
        set.addAnimation(alpha);
        set.setDuration(300);
        set.setStartOffset(delayMs);
        view.startAnimation(set);
    }

    // ─── Celebração (vitória) ────────────────────────────────────────────────

    public static void celebracao(View view) {
        AnimatorSet set = new AnimatorSet();

        ObjectAnimator scaleX  = ObjectAnimator.ofFloat(view, "scaleX",  1f, 1.3f, 1f);
        ObjectAnimator scaleY  = ObjectAnimator.ofFloat(view, "scaleY",  1f, 1.3f, 1f);
        ObjectAnimator rotacao = ObjectAnimator.ofFloat(view, "rotation", 0f, -8f, 8f, -4f, 0f);

        scaleX.setDuration(500);
        scaleY.setDuration(500);
        rotacao.setDuration(500);

        set.playTogether(scaleX, scaleY, rotacao);
        set.start();
    }
}
