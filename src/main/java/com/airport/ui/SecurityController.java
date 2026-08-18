package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.model.enums.DangerousGoodsCategory;
import com.airport.ui.util.AnimationUtil;
import com.airport.ui.util.TooltipUtil;
import com.airport.ui.util.DialogUtil;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
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


public class SecurityController implements Initializable, Refreshable {

    // ── Tooltip zaman sabitleri ────────────────────────────────────────────────

    /**
     * Pointer bu kadar saniye hareketsiz kalırsa tooltip görünür.
     * DashboardController.TOOLTIP_HOVER_DELAY_SECONDS ile eşleşmeli.
     */
    private static final double TOOLTIP_HOVER_DELAY_SECONDS = 1.5;

    /** Tooltip ekranda en fazla bu kadar saniye kalır. */
    private static final double TOOLTIP_SHOW_SECONDS = 60.0;

    // ── Tarama Alanı ──────────────────────────────────────────────────────────
    @FXML private ComboBox<String>             flightCombo;
    @FXML private Button                       screenBtn;
    @FXML private Label                        screeningCount;
    @FXML private Label                        screeningResultsLabel;
    @FXML private Label                        statsLabel;
    @FXML private Button                       manualFlagBtn;
    @FXML private TableView<Baggage>           screeningTable;
    @FXML private TableColumn<Baggage, String> sColId;
    @FXML private TableColumn<Baggage, String> sColPass;
    @FXML private TableColumn<Baggage, String> sColWt;
    @FXML private TableColumn<Baggage, String> sColClass;
    @FXML private TableColumn<Baggage, String> sColResult;

    // ── Güvenlik Havuzu ───────────────────────────────────────────────────────
    @FXML private Label                             poolHeaderLabel;
    @FXML private ComboBox<DangerousGoodsCategory> categoryFilter;
    @FXML private ListView<Baggage>                poolList;
    @FXML private Label                            poolCount;
    @FXML private Button                           clearBtn;

    // ── Overlay ───────────────────────────────────────────────────────────────
    @FXML private StackPane securityOverlay;
    @FXML private VBox      securityOverlay_card;
    @FXML private Label     alertBaggageInfo;

    // ── Backend ───────────────────────────────────────────────────────────────
    private final AirportController ctrl = MainApp.CONTROLLER;

    /** Overlay onayı için son tespit edilen tehlikeli bagaj. */
    private Baggage lastDangerousBaggage;

    // ── Tooltip takip alanları (Dashboard ile aynı mekanizma) ─────────────────
    private Timeline activeHoverTimer;
    private Tooltip  activeTooltip;

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> flightNumbers = ctrl.getUpcomingFlights().stream()
                .map(Flight::getFlightNumber)
                .collect(Collectors.toList());
        flightCombo.getItems().setAll(flightNumbers);

        // Kategori filtresi — null = "Tüm Kategoriler"
        categoryFilter.getItems().add(null);
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
        setupTooltips();
        refreshPool();
    }

    // ==================== TOOLTIP KURULUMU ====================

    /**
     * Tüm bileşenlere Dashboard'daki gibi TooltipUtil.install() ile tooltip bağlar.
     * FXML içinde <tooltip> bloğu bulunmaz; tüm kurulum buradadır.
     */
    private void setupTooltips() {
        TooltipUtil.install(flightCombo,
                "Uçuş seçmek taramayı otomatik başlatmaz.\n" +
                        "Seçimden sonra 'Güvenlik Taramasını Başlat' butonuna basmanız gerekir.");

        TooltipUtil.install(screenBtn,
                "Seçili uçuşun tüm bagajlarını tarar.\n\n" +
                        "Tehlikeli olanlar → Güvenlik Havuzu'na alınır (SECURITY_HOLD)\n" +
                        "Temiz olanlar → SECURITY_SCREENING durumuna geçer");

        TooltipUtil.install(screeningResultsLabel,
                "Satıra tıklayıp 'Manuel İşaretle' ile tehlikeli işaretleyebilirsiniz.");

        TooltipUtil.install(manualFlagBtn,
                "Tabloda seçili temiz bagajı tehlikeli olarak işaretler ve " +
                        "Güvenlik Havuzu'na ekler.\n\n" +
                        "Önce tablodan bir satır seçin, sonra bu butona basın.");

        TooltipUtil.install(poolHeaderLabel,
                "Sistem geneli — belirli bir uçuşa bağlı değildir.\n\n" +
                        "Tüm uçuşlardaki tehlikeli bagajlar burada tutulur. " +
                        "Uçuş filtresiyle değişmez.");

        TooltipUtil.install(clearBtn,
                "Seçili bagajı karantinadan çıkarır; durumu SECURITY_SCREENING'e döner.\n\n" +
                        "Önce listeden bir bagaj seçin.");
    }


    // ==================== TABLO KURULUMU ====================

    private void setupScreeningTable() {
        // Sütun değer fabrikaları
        sColId.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getBaggageId()));
        sColPass.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getPassengerId()));
        sColWt.setCellValueFactory(c     -> new SimpleStringProperty(
                String.format("%.1f kg", c.getValue().getWeightKg())));
        sColClass.setCellValueFactory(c  -> new SimpleStringProperty(
                c.getValue().getOwnerClass().getDisplayName()));
        sColResult.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isHasDangerousGoods() ? "⚠ TEHLİKELİ" : "✓ Temiz"));

        // Sonuç sütunu — renkli badge
        sColResult.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.getStyleClass().add(item.startsWith("⚠") ? "badge-danger" : "badge-ok");
                setGraphic(lbl);
                setText(null);
            }
        });

        // Sütun başlıkları — TooltipUtil.install ile programatik tooltip.
        // Label graphic olarak atandığında tooltip yalnızca header satırında
        // aktif olur; veri satırlarına yayılmaz.
        sColId.setGraphic(makeHeaderLabel("ID",
                "Sistemin otomatik atadığı benzersiz bagaj kimliği."));

        sColPass.setGraphic(makeHeaderLabel("YOLCU",
                "Bagajın kayıtlı olduğu yolcunun kimliği."));

        sColWt.setGraphic(makeHeaderLabel("AĞIRLIK",
                "Bagajın kilogram cinsinden ağırlığı."));

        sColClass.setGraphic(makeHeaderLabel("SINIF",
                "Yolcunun seyahat sınıfı.\nFirst Class › Business › Economy"));

        sColResult.setGraphic(makeHeaderLabel("SONUÇ",
                "Güvenlik taramasının sonucu.\n"
                        + "✓ Temiz      → kargo akışına devam\n"
                        + "⚠ TEHLİKELİ → Güvenlik Havuzu'na alındı"));
    }

    // ==================== EYLEMLER ====================

    /** Seçili uçuşun tüm bagajlarını tarar; tehlikeliler otomatik havuza alınır. */
    @FXML
    private void onScreenFlight() {
        String flightNumber = flightCombo.getValue();
        if (flightNumber == null) { DialogUtil.showInfo("Lütfen bir uçuş seçin."); return; }

        List<Baggage> cleared   = ctrl.runSecurityScreening(flightNumber);
        List<Baggage> allFlight = ctrl.getFlightBaggage(flightNumber);

        screeningTable.getItems().setAll(allFlight);
        screeningCount.setText(allFlight.size() + " bagaj");

        long dangerCount = allFlight.stream().filter(Baggage::isHasDangerousGoods).count();
        double dangerRate = allFlight.isEmpty()
                ? 0.0 : (dangerCount / (double) allFlight.size()) * 100;

        statsLabel.setText(String.format(
                "Temiz: %d  |  Tehlikeli: %d  |  İptal Oranı: %.0f%%",
                cleared.size(), dangerCount, dangerRate));

        refreshPool();

        // İlk tehlikeli bagaj için overlay
        allFlight.stream()
                .filter(Baggage::isHasDangerousGoods)
                .findFirst()
                .ifPresent(b -> { lastDangerousBaggage = b; showSecurityAlert(b); });

        AnimationUtil.fadeIn(screeningTable);
    }

    /** Tablodaki seçili bagajı manuel olarak tehlikeli işaretler. */
    @FXML
    private void onManualFlag() {
        Baggage selected = screeningTable.getSelectionModel().getSelectedItem();
        if (selected == null)              { DialogUtil.showInfo("Tarama tablosundan bir bagaj seçin."); return; }
        if (selected.isHasDangerousGoods()) { DialogUtil.showInfo("Bu bagaj zaten tehlikeli olarak işaretli."); return; }

        ChoiceDialog<DangerousGoodsCategory> dlg = new ChoiceDialog<>(
                DangerousGoodsCategory.MISCELLANEOUS, DangerousGoodsCategory.values());
        dlg.setTitle("Tehlike Kategorisi");
        dlg.setHeaderText("Lütfen tehlike kategorisini seçin");
        dlg.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/airport/css/dark-theme.css").toExternalForm());

        dlg.showAndWait().ifPresent(cat -> {
            ctrl.flagAsDangerous(selected.getBaggageId(), cat);
            lastDangerousBaggage = selected;
            onScreenFlight();
            showSecurityAlert(selected);
        });
    }

    /** Havuzdaki seçili bagajı güvenlik tutukluluğundan serbest bırakır. */
    @FXML private void onClearSelected() {
        Baggage selected = poolList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showInfo("Havuzdan bir bagaj seçin.");
            return;
        }
        boolean ok = ctrl.clearFromSecurityHold(selected.getBaggageId());
        if (!ok) {
            DialogUtil.showInfo("Hata", "❌ Bagaj havuzdan çıkarılamadı!");
            return;
        }
        refreshPool();
        AnimationUtil.fadeIn(poolList);
    }

    /** Güvenlik Havuzu'nun metin raporunu dialog ile gösterir. */
    @FXML
    private void onReport() {
        List<Baggage> pool = ctrl.getSecurityPool();
        if (pool.isEmpty()) { DialogUtil.showInfo("Güvenlik havuzu boş."); return; }

        StringBuilder sb = new StringBuilder("=== GÜVENLİK HAVUZU RAPORU ===\n\n");
        for (Baggage b : pool) {
            sb.append(String.format("▸ %s | Yolcu: %s | %.1f kg | %s\n",
                    b.getBaggageId(), b.getPassengerId(), b.getWeightKg(),
                    b.getDangerousCategory() != null
                            ? b.getDangerousCategory().getDescription() : "—"));
        }
        sb.append("\nToplam: ").append(pool.size()).append(" bagaj");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Güvenlik Raporu");
        alert.setHeaderText(null);
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setStyle("-fx-background-color:#0d1117; -fx-text-fill:#e8eef8; -fx-font-family:Consolas;");
        alert.getDialogPane().setContent(ta);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/airport/css/dark-theme.css").toExternalForm());
        alert.showAndWait();
    }

    /** Overlay'i "Yanlış Alarm" olarak kapatır; bagaj havuza eklenmez. */
    @FXML
    private void onDismissAlert() {
        AnimationUtil.fadeOut(securityOverlay, () -> {
            securityOverlay.setVisible(false);
            securityOverlay.setManaged(false);
        });
    }

    /** Overlay'deki bagajı onaylar, havuzu yeniler ve overlay'i kapatır. */
    @FXML
    private void onAddToPool() {
        onDismissAlert();
        refreshPool();
    }

    // ==================== OVERLAY ====================

    private void showSecurityAlert(Baggage b) {
        String categoryText = b.getDangerousCategory() != null
                ? b.getDangerousCategory().getDescription() : "Tehlikeli Madde";

        alertBaggageInfo.setText(
                b.getBaggageId() + "  |  " + categoryText
                        + "  |  " + b.getOwnerClass().getDisplayName());

        securityOverlay.setVisible(true);
        securityOverlay.setManaged(true);
        new animatefx.animation.FadeIn(securityOverlay).play();

        javafx.scene.Node card = securityOverlay.lookup("#security-alert-card");
        if (card != null) {
            new animatefx.animation.Shake(card).play();
            AnimationUtil.glowPulse(card, Color.web("#ff3b5c"), true);
        }
    }

    // ==================== YARDIMCI ====================

    private void filterPool() {
        DangerousGoodsCategory cat = categoryFilter.getValue();
        List<Baggage> pool = (cat == null)
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
                if (empty || b == null) {
                    setGraphic(null);
                    setStyle("");
                    return;
                }
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

                // SEÇİM STİLİ
                if (isSelected()) {
                    setStyle("-fx-background-color: #2a1a3a; -fx-background-radius: 6px;");
                } else {
                    setStyle("-fx-background-color: transparent;");
                }

                AnimationUtil.addGlowHover(row, Color.web("#ff3b5c44"));
            }
        });

        // Seçim değiştiğinde animasyon
        poolList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Seçilen hücrenin index'ini bul ve bounce uygula
                int index = poolList.getItems().indexOf(newVal);
                if (index >= 0) {
                    // ListView'dan o hücreyi al (null olabilir, kontrol et)
                    ListCell<Baggage> cell = (ListCell<Baggage>) poolList.lookup(".list-cell[index=" + index + "]");
                    if (cell != null) {
                        AnimationUtil.bounceIn(cell);
                    }
                }
            }
        });
    }
    /**
     * Sütun başlığı için TooltipUtil.install'li Label oluşturur.
     *
     * <p>Label, TableColumn'un {@code graphic} özelliğine atanır ({@code text}
     * boş bırakılır). Bu sayede tooltip yalnızca header satırında aktif olur —
     * veri satırlarına yayılmaz.
     *
     * @param text        Header'da görüntülenecek metin
     * @param tooltipText Tooltip içeriği
     */
    private Label makeHeaderLabel(String text, String tooltipText) {
        Label lbl = new Label(text);
        TooltipUtil.install(lbl, tooltipText);
        return lbl;
    }
    @Override
    public void onPanelShown() {
        refreshPool(); // güvenlik havuzunu güncelle
    }

}