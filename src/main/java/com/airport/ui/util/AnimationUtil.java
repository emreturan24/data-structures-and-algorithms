package com.airport.ui.util;

import animatefx.animation.*;
import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Tekrar kullanılabilir animasyon yardımcıları.
 * Tüm controller'lar bu sınıfı kullanır; AnimateFX doğrudan çağrılmaz.
 */
public final class AnimationUtil {

    private AnimationUtil() {}

    // ── GİRİŞ ANİMASYONLARI ──────────────────────────────────────────────

    public static void fadeIn(Node node) {
        new FadeInUp(node).setSpeed(1.4).play();
    }

    public static void zoomIn(Node node) {
        new ZoomIn(node).setSpeed(1.2).play();
    }

    public static void bounceIn(Node node) {
        new BounceIn(node).setSpeed(1.2).play();
    }

    public static void slideInLeft(Node node) {
        new SlideInLeft(node).setSpeed(1.3).play();
    }

    public static void slideInRight(Node node) {
        new SlideInRight(node).setSpeed(1.3).play();
    }

    public static void slideInUp(Node node) {
        new SlideInUp(node).setSpeed(1.4).play();
    }

    public static void slideInDown(Node node) {
        new SlideInDown(node).setSpeed(1.3).play();
    }

    // ── ÇIKIŞ ANİMASYONLARI ──────────────────────────────────────────────

    public static void slideOutDown(Node node, Runnable onFinish) {
        SlideOutDown anim = new SlideOutDown(node);
        anim.setSpeed(1.4);
        if (onFinish != null) anim.getTimeline().setOnFinished(e -> onFinish.run());
        anim.play();
    }

    public static void fadeOut(Node node, Runnable onFinish) {
        FadeOut anim = new FadeOut(node);
        anim.setSpeed(1.5);
        if (onFinish != null) anim.getTimeline().setOnFinished(e -> onFinish.run());
        anim.play();
    }

    // ── UYARI ANİMASYONLARI ──────────────────────────────────────────────

    /** Güvenlik uyarısı için Shake efekti. */
    public static void shake(Node node) {
        new Shake(node).setSpeed(0.9).play();
    }

    /** Dikkat çekmek için Pulse efekti (sürekli döngü). */
    public static Timeline pulseLoop(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(600), node);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.08);  st.setToY(1.08);
        st.setAutoReverse(true);
        st.setCycleCount(Animation.INDEFINITE);
        st.play();

        Timeline tl = new Timeline();
        tl.setOnFinished(e -> st.stop());
        return tl;
    }

    public static void flash(Node node) {
        new Flash(node).setSpeed(0.8).play();
    }

    public static void rubberBand(Node node) {
        new RubberBand(node).setSpeed(1.0).play();
    }

    // ── HOVER EFEKTLERİ ──────────────────────────────────────────────────

    /** Kart / satır üstüne gelince parlayan glow efekti. */
    public static void addGlowHover(Node node, Color glowColor) {
        DropShadow glow = new DropShadow(18, glowColor);
        glow.setSpread(0.15);

        ScaleTransition scaleIn  = new ScaleTransition(Duration.millis(150), node);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), node);
        scaleIn.setToX(1.025); scaleIn.setToY(1.025);
        scaleOut.setToX(1.0);  scaleOut.setToY(1.0);

        node.setOnMouseEntered(e -> { node.setEffect(glow); scaleIn.play(); });
        node.setOnMouseExited(e  -> { node.setEffect(null); scaleOut.play(); });
    }

    /** Buton hover için hafif büyüme. */
    public static void addButtonHover(Node button) {
        ScaleTransition in  = new ScaleTransition(Duration.millis(120), button);
        ScaleTransition out = new ScaleTransition(Duration.millis(120), button);
        in.setToX(1.05);  in.setToY(1.05);
        out.setToX(1.0);  out.setToY(1.0);
        button.setOnMouseEntered(e -> in.play());
        button.setOnMouseExited(e  -> out.play());
    }

    // ── GECİKMELİ ANİMASYON ──────────────────────────────────────────────

    /** Liste öğelerini sırayla (cascade) animasyonla gösterir. */
    public static void cascadeIn(java.util.List<? extends Node> nodes, int delayMs) {
        for (int i = 0; i < nodes.size(); i++) {
            final Node n = nodes.get(i);
            n.setOpacity(0);
            PauseTransition delay = new PauseTransition(Duration.millis((long) i * delayMs));
            delay.setOnFinished(e -> new FadeInUp(n).setSpeed(1.5).play());
            delay.play();
        }
    }

    // ── TITREYEREK ÇARPMA (invalid input) ────────────────────────────────

    public static void headShake(Node node) {
        // HeadShake yerine kütüphanede var olan Shake veya Wobble animasyonunu kullanıyoruz
        new Shake(node).play();
    }

    // ── GLOWING BORDER PULSE ─────────────────────────────────────────────

    public static void glowPulse(Node node, Color color, boolean start) {
        if (start) {
            Glow glow = new Glow(0.0);
            node.setEffect(glow);
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,      new KeyValue(glow.levelProperty(), 0.0)),
                    new KeyFrame(Duration.millis(700), new KeyValue(glow.levelProperty(), 0.85)),
                    new KeyFrame(Duration.millis(1400),new KeyValue(glow.levelProperty(), 0.0))
            );
            tl.setCycleCount(Animation.INDEFINITE);
            tl.play();
            node.setUserData(tl);
        } else {
            if (node.getUserData() instanceof Timeline tl) { tl.stop(); }
            node.setEffect(null);
        }
    }
}