package com.airport.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Ortak dialog yardımcıları.
 * Tüm controller'larda kullanılan mesaj kutularını tek bir yerden yönetir.
 */
public final class DialogUtil {

    private DialogUtil() {}

    // ── Bilgi ──────────────────────────────────────────────────────────────

    /** Başlık + mesajlı bilgi dialog'u. */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(SceneManager.getCssPath());
        alert.showAndWait();
    }

    /** Hızlı versiyon — başlık "Bilgi" olur. */
    public static void showInfo(String message) {
        showInfo("Bilgi", message);
    }

    // ── Onay ───────────────────────────────────────────────────────────────

    /**
     * OK / İptal onay dialog'u gösterir.
     *
     * Kullanım:
     * <pre>
     *   Optional&lt;ButtonType&gt; result = DialogUtil.showConfirm("Başlık", "Mesaj");
     *   if (result.isPresent() && result.get() == ButtonType.OK) { ... }
     * </pre>
     *
     * @return Kullanıcının tıkladığı ButtonType (OK veya CANCEL)
     */
    public static Optional<ButtonType> showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
                ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(SceneManager.getCssPath());
        return alert.showAndWait();
    }

    // ── Hata ───────────────────────────────────────────────────────────────

    /** Hata dialog'u. */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(SceneManager.getCssPath());
        alert.showAndWait();
    }
}
