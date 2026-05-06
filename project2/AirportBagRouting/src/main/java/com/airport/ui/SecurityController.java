package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.model.enums.DangerousGoodsCategory;
import com.airport.ui.util.AnimationUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Güvenlik Denetimi panel controller'ı.
 * Tehlikeli bagaj tespitinde animasyonlu overlay + Shake efekti.
 */
public class SecurityController implements Initializable {

    @FXML private ComboBox<String>           flightCombo;
    @FXML private ComboBox<DangerousGoodsCategory> categoryFilter;
    @FXML private TableView<Baggage>         screeningTable;
    @FXML private TableColumn<Baggage,String> sColId, sColPass, sColWt, sColClass, sColResult;
    @FXML private ListView<Baggage>          poolList;
    @FXML private Label poolCount, screeningCount, statsLabel;

    // Overlay
    @FXML private StackPane securityOverlay;
    @FXML private VBox      securityOverlay_card; // #security-alert-card — fx:id ile erişim
    @FXML private Label     alertBody, alertBaggageInfo;

    private final AirportController ctrl = MainApp.CONTROLLER;
    private Baggage lastDangerousBaggage;

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Uçuş combo
        List<String> nums = ctrl.getUpcomingFlights().stream()
                .map(Flight::getFlightNumber).collect(Collectors.toList());
        flightCombo.getItems().setAll(nums);

        // Kategori filtresi
        categoryFilter.getItems().add(null); // "Tümü"
        categoryFilter.getItems().addAll(DangerousGoodsCategory.values());
        categoryFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(DangerousGoodsCategory c, boolean empty) {
                super.updateItem(c, empty);
                setText(c == null ? "Tüm Kategoriler" : c.getDescription());
            }
        });
        categoryFilter.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(DangerousGoodsCategory c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty ? null : (c == null ? "Tüm Kategoriler" : c.getDescription()));
            }
        });
        categoryFilter.getSelectionModel().selectFirst();
        categoryFilter.setOnAction(e -> filterPool());

        setupScreeningTable();
        refreshPool();
    }

    // ==================== TABLO KURULUMU ====================

    private void setupScreeningTable() {
        sColId.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getBaggageId()));
        sColPass.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getPassengerId()));
        sColWt.setCellValueFactory(c    -> new SimpleStringProperty(
                String.format("%.1f kg", c.getValue().getWeightKg())));
        sColClass.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOwnerClass().getDisplayName()));
        sColResult.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isHasDangerousGoods() ? "⚠ TEHLİKELİ" : "✓ Temiz"));

        // Sonuç sütunu renkli
        sColResult.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.getStyleClass().add(item.startsWith("⚠") ? "badge-danger" : "badge-ok");
                setGraphic(lbl); setText(null);
            }
        });
    }

    // ==================== EYLEMLER ====================

    @FXML private void onScreenFlight() {
        String fn = flightCombo.getValue();
        if (fn == null) { showInfo("Lütfen bir uçuş seçin."); return; }

        List<Baggage> cleared = ctrl.runSecurityScreening(fn);
        List<Baggage> allFlight = ctrl.getFlightBaggage(fn);

        screeningTable.getItems().setAll(allFlight);
        screeningCount.setText(allFlight.size() + " bagaj");

        long dangerCount = allFlight.stream().filter(Baggage::isHasDangerousGoods).count();
        statsLabel.setText(String.format("Temiz: %d  |  Tehlikeli: %d  |  İptal Oranı: %.0f%%",
                cleared.size(), dangerCount,
                allFlight.isEmpty() ? 0.0 : (dangerCount / (double) allFlight.size()) * 100));

        refreshPool();

        // Tehlikeli bagaj varsa overlay aç
        allFlight.stream().filter(Baggage::isHasDangerousGoods).findFirst().ifPresent(b -> {
            lastDangerousBaggage = b;
            showSecurityAlert(b);
        });

        AnimationUtil.fadeIn(screeningTable);
    }

    @FXML private void onManualFlag() {
        Baggage sel = screeningTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Tarama tablosundan bir bagaj seçin."); return; }
        if (sel.isHasDangerousGoods()) { showInfo("Bu bagaj zaten işaretli."); return; }

        // Kategori seçim dialogu
        ChoiceDialog<DangerousGoodsCategory> dlg = new ChoiceDialog<>(
                DangerousGoodsCategory.MISCELLANEOUS, DangerousGoodsCategory.values());
        dlg.setTitle("Tehlike Kategorisi");
        dlg.setHeaderText("Lütfen tehlike kategorisini seçin");
        dlg.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/airport/css/dark-theme.css").toExternalForm());
        dlg.showAndWait().ifPresent(cat -> {
            ctrl.flagAsDangerous(sel.getBaggageId(), cat);
            lastDangerousBaggage = sel;
            onScreenFlight(); // Tabloyu yenile
            showSecurityAlert(sel);
        });
    }

    @FXML private void onClearBaggage() {
        Baggage sel = poolList.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Havuzdan bir bagaj seçin."); return; }
        ctrl.clearFromSecurityHold(sel.getBaggageId());
        refreshPool();
        AnimationUtil.fadeIn(poolList);
    }

    @FXML private void onClearSelected() {
        onClearBaggage();
    }

    @FXML private void onReport() {
        List<Baggage> pool = ctrl.getSecurityPool();
        if (pool.isEmpty()) { showInfo("Güvenlik havuzu boş."); return; }

        StringBuilder sb = new StringBuilder("=== GÜVENLİK HAVUZU RAPORU ===\n\n");
        for (Baggage b : pool) {
            sb.append(String.format("▸ %s | Yolcu: %s | %.1f kg | %s\n",
                    b.getBaggageId(), b.getPassengerId(), b.getWeightKg(),
                    b.getDangerousCategory() != null ? b.getDangerousCategory().getDescription() : "—"));
        }
        sb.append("\nToplam: ").append(pool.size()).append(" bagaj");

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Güvenlik Raporu");
        a.setHeaderText(null);
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setStyle("-fx-background-color:#0d1117; -fx-text-fill:#e8eef8; -fx-font-family:Consolas;");
        a.getDialogPane().setContent(ta);
        a.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/airport/css/dark-theme.css").toExternalForm());
        a.showAndWait();
    }

    @FXML private void onDismissAlert() {
        AnimationUtil.fadeOut(securityOverlay, () -> {
            securityOverlay.setVisible(false);
            securityOverlay.setManaged(false);
        });
    }

    @FXML private void onAddToPool() {
        onDismissAlert();
        refreshPool();
    }

    // ==================== OVERLAY ====================

    private void showSecurityAlert(Baggage b) {
        alertBaggageInfo.setText(b.getBaggageId() + "  |  "
                + (b.getDangerousCategory() != null
                ? b.getDangerousCategory().getDescription() : "Tehlikeli Madde")
                + "  |  " + b.getOwnerClass().getDisplayName());

        securityOverlay.setVisible(true);
        securityOverlay.setManaged(true);

        // Overlay fade-in
        new animatefx.animation.FadeIn(securityOverlay).play();

        // Uyarı kartına Shake + glowing border
        // Kartı lookup ile bul (FXML'de #security-alert-card ID'li)
        javafx.scene.Node card = securityOverlay.lookup("#security-alert-card");
        if (card != null) {
            new animatefx.animation.Shake(card).play();
            AnimationUtil.glowPulse(card, Color.web("#ff3b5c"), true);
        }
    }

    private void filterPool() {
        DangerousGoodsCategory cat = categoryFilter.getValue();
        List<Baggage> pool = cat == null
                ? ctrl.getSecurityPool()
                : ctrl.getSecurityPoolByCategory(cat);
        poolList.getItems().setAll(pool);
        poolCount.setText(String.valueOf(pool.size()));
    }

    private void refreshPool() {
        filterPool();
        poolList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Baggage b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) { setGraphic(null); return; }
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label id  = new Label("⚠  " + b.getBaggageId());
                id.setStyle("-fx-text-fill:#ff3b5c; -fx-font-weight:bold;");
                Label cat = new Label(b.getDangerousCategory() != null
                        ? b.getDangerousCategory().getDescription() : "—");
                cat.setStyle("-fx-text-fill:#7a8fa8; -fx-font-size:11px;");
                Label wt  = new Label(String.format("%.1f kg", b.getWeightKg()));
                wt.setStyle("-fx-text-fill:#ffb830;");
                row.getChildren().addAll(id, cat, wt);
                setGraphic(row);
                setStyle("-fx-background-color:transparent;");
                AnimationUtil.addGlowHover(row, Color.web("#ff3b5c44"));
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