package com.airport.ui.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Ortak Tooltip yardımcıları.
 * Tüm controller'larda aynı gecikme tabanlı & harekete duyarlı tooltip
 * mekanizmasını kullanmak için bu sınıfa taşındı.
 */
public final class TooltipUtil {

    /** Pointer bu kadar saniye hareketsiz kalırsa tooltip görünür */
    private static final double HOVER_DELAY_SECONDS = 1.5;

    /** Aynı anda yalnızca bir tooltip açık kalması için paylaşımlı takip */
    private static Timeline activeHoverTimer;
    private static Tooltip  activeTooltip;

    private TooltipUtil() {}

    /**
     * Herhangi bir Region'a gecikmeli & harekete duyarlı tooltip kurar.
     *
     * <p>Davranış:
     * <ul>
     *   <li>Mouse aynı konumda 1.5 sn hareketsiz kalırsa tooltip açılır</li>
     *   <li>Mouse hareket eder etmez tooltip anında kapanır (titreme yok)</li>
     *   <li>Aynı anda yalnızca bir tooltip açık kalır; diğerine geçilince kapanır</li>
     * </ul>
     *
     * @param node  tooltip bağlanacak bileşen
     * @param text  tooltip içeriği (satır sonu için \n kullanılır)
     */
    public static void install(Region node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(280);
        tooltip.setShowDuration(Duration.seconds(60));

        final double[] lastScreen = {0, 0};

        Timeline hoverTimer = new Timeline(
                new KeyFrame(Duration.seconds(HOVER_DELAY_SECONDS), e ->
                        tooltip.show(node.getScene().getWindow(),
                                lastScreen[0] + 14,
                                lastScreen[1] + 14)
                )
        );
        hoverTimer.setCycleCount(1);

        // Node scene'den ayrılınca (panel navigasyonu) timer ve tooltip'i kapat
        node.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                hoverTimer.stop();
                tooltip.hide();
            }
        });

        node.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (activeTooltip != null && activeTooltip != tooltip) {
                activeHoverTimer.stop();
                activeTooltip.hide();
            }
            activeHoverTimer = hoverTimer;
            activeTooltip    = tooltip;
            lastScreen[0]    = event.getScreenX();
            lastScreen[1]    = event.getScreenY();
            if (tooltip.isShowing()) tooltip.hide();
            hoverTimer.playFromStart();
        });

        node.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
            Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
            if (screenBounds == null
                    || !screenBounds.contains(event.getScreenX(), event.getScreenY())) {
                hoverTimer.stop();
                tooltip.hide();
                if (activeTooltip == tooltip) {
                    activeTooltip    = null;
                    activeHoverTimer = null;
                }
            }
        });
    }
}