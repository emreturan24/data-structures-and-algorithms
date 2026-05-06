package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.model.enums.PassengerClass;
import com.airport.ui.util.AnimationUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Yükleme Simülasyonu (Stack) panel controller'ı.
 *
 * <p>Kullandığı veri yapıları:
 * <ul>
 *   <li>{@link com.airport.datastructures.BaggageStack} — LIFO Stack (WeightLoadingService)</li>
 *   <li>PriorityQueue   — VIP öncelik sırası (PriorityLoadingService)</li>
 * </ul>
 *
 * <p>AnimateFX SlideInUp ile bagajlar sırayla üst üste eklenir.
 * Pop işleminde SlideOutDown animasyonu oynanır.
 */
public class StackController implements Initializable {

    @FXML private ComboBox<String> flightCombo;
    @FXML private VBox   stackVisualBox;
    @FXML private ListView<String> priorityQueueView, unloadLogView;
    @FXML private Label  stackSizeLabel, logLabel;
    @FXML private Label  infoFlight, infoSize, infoWeight, infoTop, infoBottom;

    private final AirportController ctrl = MainApp.CONTROLLER;
    private final List<String> unloadLog = new ArrayList<>();

    // Görsel stack: üstte = en son eklenen = en hafif
    // Her öğe bir HBox kartı
    private final List<HBox> visualItems = new ArrayList<>();

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> flightNos = ctrl.getUpcomingFlights().stream()
                .map(Flight::getFlightNumber).collect(Collectors.toList());
        flightCombo.getItems().setAll(flightNos);
        flightCombo.setOnAction(e -> refreshPriorityQueue(flightCombo.getValue()));

        // Stack zemini
        stackVisualBox.setAlignment(Pos.BOTTOM_CENTER);
        stackVisualBox.setStyle("-fx-background-color:linear-gradient(to bottom,#0a1220,#101a2a);"
                + "-fx-background-radius:10; -fx-border-color:#1e3050; -fx-border-radius:10;"
                + "-fx-border-width:1; -fx-padding:14;");
    }

    // ==================== YÜKLEME ====================

    @FXML private void onLoadFlight() {
        String fn = flightCombo.getValue();
        if (fn == null) { showInfo("Lütfen bir uçuş seçin."); return; }

        // Önce güvenlik taramasından geçmiş bagajları al
        List<Baggage> bags = ctrl.getFlightBaggage(fn).stream()
                .filter(b -> !b.isHasDangerousGoods())
                .collect(Collectors.toList());

        if (bags.isEmpty()) { showInfo("Bu uçuşa ait yüklenecek bagaj yok."); return; }

        // Stack'i temizle
        stackVisualBox.getChildren().clear();
        visualItems.clear();
        unloadLog.clear();
        unloadLogView.getItems().clear();

        // Ağır → hafif sırala (WeightLoadingService davranışını yansıt)
        bags.sort((a, b) -> Double.compare(b.getWeightKg(), a.getWeightKg()));
        ctrl.loadFlightStack(fn, bags);

        // Sırayla görsel kart ekle (ağır önce = altta)
        for (int i = 0; i < bags.size(); i++) {
            final Baggage b = bags.get(i);
            final int idx   = i;
            javafx.animation.PauseTransition delay =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(idx * 180L));
            delay.setOnFinished(e -> addVisualCard(b, false));
            delay.play();
        }

        logLabel.setText("✓  " + fn + " için " + bags.size()
                + " bagaj sıralandı. (Ağır altta, hafif üstte)");
        refreshStackInfo(fn);
        refreshPriorityQueue(fn);
    }

    // ==================== İNDİRME ====================

    @FXML private void onUnloadStep() {
        String fn = flightCombo.getValue();
        if (fn == null) { showInfo("Lütfen bir uçuş seçin."); return; }

        Baggage b = ctrl.unloadNextBaggage(fn);
        if (b == null) {
            showInfo("Stack boş! Tüm bagajlar indirildi.");
            logLabel.setText("Stack boş.");
            return;
        }

        // Üstteki (son eklenen) görsel kartı kaldır
        if (!visualItems.isEmpty()) {
            HBox topCard = visualItems.get(visualItems.size() - 1);
            AnimationUtil.slideOutDown(topCard, () -> {
                stackVisualBox.getChildren().remove(topCard);
                visualItems.remove(topCard);
            });
        }

        String logEntry = String.format("⬇  %s  |  %.1f kg  |  %s",
                b.getBaggageId(), b.getWeightKg(), b.getOwnerClass().getDisplayName());
        unloadLog.add(0, logEntry);
        unloadLogView.getItems().setAll(unloadLog);

        logLabel.setText("Pop: " + b.getBaggageId()
                + "  (%.1f kg)".formatted(b.getWeightKg()));
        refreshStackInfo(fn);
    }

    @FXML private void onUnloadAll() {
        String fn = flightCombo.getValue();
        if (fn == null) { showInfo("Lütfen bir uçuş seçin."); return; }

        List<Baggage> unloaded = ctrl.unloadFlight(fn);
        if (unloaded.isEmpty()) { showInfo("Stack zaten boş."); return; }

        // Kartları cascade çıkar (hafiften başlayarak)
        for (int i = visualItems.size() - 1; i >= 0; i--) {
            final HBox card = visualItems.get(i);
            final long delayMs = (long)(visualItems.size() - 1 - i) * 120;
            javafx.animation.PauseTransition d =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(delayMs));
            d.setOnFinished(e -> AnimationUtil.slideOutDown(card,
                    () -> stackVisualBox.getChildren().remove(card)));
            d.play();
        }
        visualItems.clear();

        for (Baggage b : unloaded) {
            unloadLog.add(0, String.format("⬇  %s  |  %.1f kg  |  %s",
                    b.getBaggageId(), b.getWeightKg(), b.getOwnerClass().getDisplayName()));
        }
        unloadLogView.getItems().setAll(unloadLog);
        logLabel.setText("✓  " + unloaded.size() + " bagaj indirildi. (LIFO sırası)");
        stackSizeLabel.setText("Stack: 0 öğe");
        infoSize.setText("Öğe sayısı: 0");
        infoWeight.setText("Toplam ağırlık: 0 kg");
        infoTop.setText("Üstteki: —");
        infoBottom.setText("Alttaki: —");
    }

    @FXML private void onClearLog() {
        unloadLog.clear();
        unloadLogView.getItems().clear();
    }

    // ==================== GÖRSEL KART ====================

    /**
     * Stack'e yeni kart ekler. SlideInUp animasyonu oynar.
     * @param fromBottom true = üste değil, alta ekle (şu an kullanılmıyor — ağırlar ilk push)
     */
    private void addVisualCard(Baggage b, boolean fromBottom) {
        HBox card = buildCard(b);
        // Stack'te ağır altta → görsel olarak altta = VBox'un altına ekle
        // VBox BOTTOM_CENTER → son eklenen üstte görünür, ama biz sıralı yüklüyoruz
        // Bu yüzden ilk eklenen (ağır) en alta, son eklenen (hafif) en üste
        stackVisualBox.getChildren().add(0, card); // 0 = üste ekle (hafif üstte görünür)
        visualItems.add(card);
        AnimationUtil.slideInUp(card);
        stackSizeLabel.setText("Stack: " + visualItems.size() + " öğe");
    }

    private HBox buildCard(Baggage b) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new javafx.geometry.Insets(10, 16, 10, 16));
        card.setMaxWidth(Double.MAX_VALUE);

        // Sol renk bandı
        String bandColor = switch (b.getOwnerClass()) {
            case VIP      -> "#9d5cff";
            case BUSINESS -> "#00d4ff";
            case ECONOMY  -> "#1e3050";
        };
        Rectangle band = new Rectangle(4, 36);
        band.setFill(Color.web(bandColor));
        band.setArcWidth(3); band.setArcHeight(3);

        // Ağırlık göstergesi
        double maxWt = 50.0;
        double ratio = Math.min(b.getWeightKg() / maxWt, 1.0);
        int shade    = (int)(ratio * 200);
        String cardBg = b.isHasDangerousGoods()
                ? "rgba(255,59,92,0.08)"
                : String.format("rgba(%d,%d,%d,0.08)", shade / 2, shade / 3, shade);

        card.setStyle("-fx-background-color:" + cardBg + ";"
                + "-fx-background-radius:8; -fx-border-color:" + bandColor + "40;"
                + "-fx-border-radius:8; -fx-border-width:1;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),6,0,0,2);");

        // İçerik
        VBox info = new VBox(3);
        Label id  = new Label(b.getBaggageId());
        id.setStyle("-fx-font-weight:bold; -fx-font-size:12px; -fx-text-fill:#e8eef8;");
        Label detail = new Label(String.format("%.1f kg  ·  %s%s",
                b.getWeightKg(), b.getOwnerClass().getDisplayName(),
                b.isHasDangerousGoods() ? "  ⚠" : ""));
        detail.setStyle("-fx-font-size:10px; -fx-text-fill:#7a8fa8;");
        info.getChildren().addAll(id, detail);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Ağırlık progress bar
        javafx.scene.control.ProgressBar pb =
                new javafx.scene.control.ProgressBar(ratio);
        pb.setPrefWidth(60); pb.setPrefHeight(5);
        pb.setStyle("-fx-background-color:#0a1220; -fx-background-radius:3;");

        card.getChildren().addAll(band, info, pb);
        AnimationUtil.addGlowHover(card, Color.web(bandColor + "44"));
        return card;
    }

    // ==================== YARDIMCI ====================

    private void refreshStackInfo(String fn) {
        List<Baggage> preview = ctrl.previewStackOrder(fn);
        int sz = preview.size();
        stackSizeLabel.setText("Stack: " + sz + " öğe");
        infoFlight.setText("Uçuş: " + fn);
        infoSize.setText("Öğe sayısı: " + sz);
        double totalWt = preview.stream().mapToDouble(Baggage::getWeightKg).sum();
        infoWeight.setText(String.format("Toplam ağırlık: %.1f kg", totalWt));
        if (!preview.isEmpty()) {
            Baggage top = preview.get(0);    // üstteki (hafif)
            Baggage bot = preview.get(sz-1); // alttaki (ağır)
            infoTop.setText(String.format("Üstteki (hafif): %s  %.1f kg",
                    top.getBaggageId(), top.getWeightKg()));
            infoBottom.setText(String.format("Alttaki (ağır): %s  %.1f kg",
                    bot.getBaggageId(), bot.getWeightKg()));
        } else {
            infoTop.setText("Üstteki: —");
            infoBottom.setText("Alttaki: —");
        }
    }

    private void refreshPriorityQueue(String fn) {
        if (fn == null) return;
        List<Baggage> pq = ctrl.getPriorityLoadingService().peekQueue(fn);
        List<String> items = new ArrayList<>();
        for (int i = 0; i < pq.size(); i++) {
            Baggage b = pq.get(i);
            String prefix = i == 0 ? "▶  " : "    ";
            items.add(String.format("%s[%d] %s  |  %.1f kg  |  %s",
                    prefix, i + 1, b.getBaggageId(),
                    b.getWeightKg(), b.getOwnerClass().getDisplayName()));
        }
        priorityQueueView.getItems().setAll(items);
        priorityQueueView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                if (s.contains("VIP")) setStyle("-fx-text-fill:#c580ff;");
                else if (s.contains("Business")) setStyle("-fx-text-fill:#00d4ff;");
                else setStyle("-fx-text-fill:#7a8fa8;");
            }
        });
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle("Bilgi"); a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/airport/css/dark-theme.css").toExternalForm());
        a.showAndWait();
    }
}