package com.example.touchflow.utilities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public final class ViewAnimationHelper {

    private ViewAnimationHelper() {}

    public static void animatePressScale(View view, boolean pressed) {
        if (view == null) return;
        float target = pressed ? 0.88f : 1.0f;
        view.animate()
                .scaleX(target)
                .scaleY(target)
                .setDuration(pressed ? 90L : 160L)
                .setInterpolator(pressed ? new DecelerateInterpolator() : new OvershootInterpolator(1.6f))
                .start();
    }

    public static void animateMenuExpand(View container, long durationMs) {
        if (container == null) return;
        container.setAlpha(0f);
        container.setScaleX(0.80f);
        container.setScaleY(0.80f);
        container.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(durationMs)
                .setInterpolator(new OvershootInterpolator(1.15f))
                .setListener(null)
                .start();
    }

    public static void animateMenuCollapse(View container, long durationMs, Runnable onEnd) {
        if (container == null) {
            if (onEnd != null) onEnd.run();
            return;
        }
        container.animate()
                .alpha(0f)
                .scaleX(0.82f)
                .scaleY(0.82f)
                .setDuration(Math.max(100L, durationMs - 40L))
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        container.animate().setListener(null);
                        if (onEnd != null) onEnd.run();
                    }
                })
                .start();
    }

    public static ValueAnimator animateWindowX(int startX, int endX, long durationMs,
                                               ValueAnimator.AnimatorUpdateListener listener,
                                               Runnable onEnd) {
        ValueAnimator animator = ValueAnimator.ofInt(startX, endX);
        animator.setDuration(durationMs);
        animator.setInterpolator(new OvershootInterpolator(0.95f));
        animator.addUpdateListener(listener);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onEnd != null) onEnd.run();
            }
        });
        animator.start();
        return animator;
    }

    public static void animateSpringScale(View view, float targetScale) {
        if (view == null) return;
        SpringAnimation scaleX = new SpringAnimation(view, DynamicAnimation.SCALE_X, targetScale);
        SpringAnimation scaleY = new SpringAnimation(view, DynamicAnimation.SCALE_Y, targetScale);
        scaleX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        scaleX.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
        scaleY.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        scaleY.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
        scaleX.start();
        scaleY.start();
    }
}
