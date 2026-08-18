package com.airport.ui;

import com.airport.MainApp;
import com.airport.ui.util.TooltipUtil;
import com.airport.ui.util.DialogUtil;
import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.model.enums.BaggageStatus;
import com.airport.model.enums.DangerousGoodsCategory;
import com.airport.model.enums.PassengerClass;
import com.airport.ui.util.AnimationUtil;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Bagaj Yönetimi panel controller'ı.
 * HashMap tabanlı O(1) bagaj takibi, check-in formu, durum güncelleme.
 *
 * DEĞİŞİKLİKLER (v4 — TooltipUtil.install migrasyonu):
 *
 * 1. GERÇEKZAMANLı FİLTRE (FilteredList):
 *    masterList (ObservableList) + filteredList (FilteredList) mimarisine geçildi.
 *    searchId ve filterCombo listener'ları updateFilter() çağırır.
 *
 * 2. TABLO BAŞLIK TOOLTİP:
 *    Tooltip sadece sütun başlık satırına (.column-header-background) bağlıdır.
 *    Platform.runLater kullanılarak scene graph tamamen render olduktan sonra
 *    lookup yapılır — liste satırlarında tooltip ÇIKMAZ.
 *
 * 3. KART TOOLTİP:
 *    TooltipUtil.install() — Dashboard ile aynı mekanizma:
 *    1,5 sn hareketsizlik → aç; hareket et → kapat; kart dışına çık → iptal.
 *
 * 4. ★ YENİ — BİREYSEL KONTROL TOOLTİP MİGRASYONU:
 *    BaggagePanel.fxml'deki 13 <tooltip> bloğu kaldırıldı.
 *    Tooltip'ler setupCardTooltips() içinde TooltipUtil.install() ile kurulur.
 *    Yeni @FXML alanları: clearFilterBtn, applyStatusBtn, checkInBtn.
 */
public class BaggageController implements Initializable,Refreshable {

    // ── Tablo ──────────────────────────────────────────────────────────────
    @FXML private TableView<Baggage>           baggageTable;
    @FXML private TableColumn<Baggage, String> colId, colPassenger, colFlight,
            colWeight, colClass, colStatus, colDanger;

    // ── Arama / Filtre çubuğu ───────────────────────────────────────────────
    @FXML private TextField       searchId;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Label            totalLabel;

    /**
     * ★ YENİ — fx:id="clearFilterBtn" eklendi (BaggagePanel.fxml).
     * FXML <tooltip> kaldırıldı; TooltipUtil.install ile kurulur.
     */
    @FXML private Button clearFilterBtn;

    // ── Seçili bagaj detayı ─────────────────────────────────────────────────
    @FXML private Label  detailId, detailPass, detailFlight,
            detailWt, detailClass, detailStat, detailDanger;
    @FXML private VBox   timelineBox;

    // ── Check-in formu ──────────────────────────────────────────────────────
    @FXML private TextField                        ciPassenger, ciWeight;
    @FXML private ComboBox<String>                 ciFlightCombo;
    @FXML private ComboBox<PassengerClass>         ciClassCombo;
    @FXML private CheckBox                         ciDangerous;
    @FXML private ComboBox<DangerousGoodsCategory> ciDangerCombo;
    @FXML private ComboBox<BaggageStatus>          statusCombo;

    /**
     * ★ YENİ — fx:id="applyStatusBtn" eklendi (BaggagePanel.fxml).
     * FXML <tooltip> kaldırıldı; TooltipUtil.install ile kurulur.
     */
    @FXML private Button applyStatusBtn;

    /**
     * ★ YENİ — fx:id="checkInBtn" eklendi (BaggagePanel.fxml).
     * FXML <tooltip> kaldırıldı; TooltipUtil.install ile kurulur.
     */
    @FXML private Button checkInBtn;

    // ── Kart tooltip referansları (FXML'de fx:id tanımlı) ──────────────────
    @FXML private VBox detailCard;
    @FXML private VBox checkinCard;

    // ── Filtre sabitleri ────────────────────────────────────────────────────
    private static final String F_ALL       = "Tüm Bagajlar";
    private static final String F_CHECKIN   = "Check-in";
    private static final String F_SEC_SCR   = "Güvenlik Taraması";
    private static final String F_CARGO     = "Kargo";
    private static final String F_LOADED    = "Yüklendi";
    private static final String F_WAITING   = "Bekleme Kuyruğu";
    private static final String F_HOLD      = "Güvenlik Havuzu";
    private static final String F_DELIVERED = "Teslim Edildi";
    private static final String F_DANGER    = "⚠  Tehlikeli Maddeler";

    // ── Veri katmanı ────────────────────────────────────────────────────────
    private final ObservableList<Baggage> masterList    = FXCollections.observableArrayList();
    private FilteredList<Baggage>         filteredList;

    // ── Kart tooltip paylaşımlı durum ────────────────────────────────────────
    private Timeline activeHoverTimer;
    private Tooltip  activeTooltip;

    private final AirportController ctrl = MainApp.CONTROLLER;

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filteredList = new FilteredList<>(masterList, b -> true);
        baggageTable.setItems(filteredList);

        setupColumns();
        setupComboBoxes();
        setupSelectionDetail();
        setupRealtimeFilter();
        setupTableHeaderTooltip();
        setupCardTooltips();   // ← kart + bireysel kontrol tooltip'leri
        loadAll();

        ciDangerous.selectedProperty().addListener((obs, o, n) ->
                ciDangerCombo.setDisable(!n));
    }

    // ==================== GERÇEKZAMANLı FİLTRE KURULUMU ====================

    private void setupRealtimeFilter() {
        searchId.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
    }

    private void updateFilter() {
        String text     = searchId.getText() == null ? "" : searchId.getText().trim().toLowerCase();
        String selected = filterCombo.getValue();

        filteredList.setPredicate(b -> {
            boolean matchesText = text.isEmpty()
                    || b.getBaggageId().toLowerCase().contains(text)
                    || b.getPassengerId().toLowerCase().contains(text)
                    || b.getFlightNumber().toLowerCase().contains(text);

            boolean matchesFilter = true;
            if (selected != null && !selected.equals(F_ALL)) {
                matchesFilter = switch (selected) {
                    case F_CHECKIN   -> b.getStatus() == BaggageStatus.CHECK_IN;
                    case F_SEC_SCR   -> b.getStatus() == BaggageStatus.SECURITY_SCREENING;
                    case F_CARGO     -> b.getStatus() == BaggageStatus.CARGO;
                    case F_LOADED    -> b.getStatus() == BaggageStatus.LOADED;
                    case F_WAITING   -> b.getStatus() == BaggageStatus.WAITING_QUEUE;
                    case F_HOLD      -> b.getStatus() == BaggageStatus.SECURITY_HOLD;
                    case F_DELIVERED -> b.getStatus() == BaggageStatus.DELIVERED;
                    case F_DANGER    -> b.isHasDangerousGoods();
                    default          -> true;
                };
            }

            return matchesText && matchesFilter;
        });

        long shown = filteredList.size();
        long total = masterList.size();
        if (text.isEmpty() && (selected == null || selected.equals(F_ALL))) {
            totalLabel.setText("Toplam: " + total + " bagaj");
        } else {
            totalLabel.setText("Gösterilen: " + shown + " / " + total);
        }
    }

    @FXML
    private void onClearFilter() {
        searchId.clear();
        filterCombo.setValue(null);
    }

    // ==================== TABLO BAŞLIK TOOLTİP ====================

    private void setupTableHeaderTooltip() {
        Platform.runLater(() -> {
            Node headerBg = baggageTable.lookup(".column-header-background");
            if (headerBg == null) return;

            Tooltip headerTip = new Tooltip(
                    "Sütun başlığına tıklayarak tabloya sıralama uygulayabilirsiniz.\n\n" +
                            "ID          → Benzersiz bagaj kimliği\n" +
                            "YOLCU    → Bagaj sahibinin ID'si\n" +
                            "UÇUŞ      → Atandığı uçuş numarası\n" +
                            "AĞIRLIK  → kg cinsinden ağırlık\n" +
                            "SINIF      → VIP / Business / Economy\n" +
                            "DURUM   → Güncel izleme statüsü\n" +
                            "GÜVENLİK → Tehlikeli madde durumu"
            );
            headerTip.setWrapText(true);
            headerTip.setMaxWidth(260);
            headerTip.setShowDelay(Duration.millis(400));
            headerTip.setShowDuration(Duration.seconds(60));
            Tooltip.install(headerBg, headerTip);
        });
    }

    // ==================== TOOLTİP KURULUMU ====================

    /**
     * Kart seviyesi VE bireysel kontrol tooltip'lerini kurar.
     *
     * <p>Kart seviyesi (detailCard, checkinCard): tüm karta bağlı,
     * Dashboard stili gecikmeli & harekete duyarlı tooltip.
     *
     * <p>Bireysel kontroller: BaggagePanel.fxml'den kaldırılan 13 {@code <tooltip>}
     * bloğunun karşılığı — her bileşen için tek satır TooltipUtil.install() yeterlidir.
     */
    private void setupCardTooltips() {

        // ── Kart seviyesi ────────────────────────────────────────────────────

        TooltipUtil.install(detailCard,
                "SEÇİLİ BAGAJ DETAYI\n\n" +
                        "Tablodan tıklanan bagajın tam bilgilerini ve\n" +
                        "izleme zaman çizelgesini gösterir.\n\n" +
                        "İzleme Noktaları:\n" +
                        "  • Mavi  → bagajın şu anki konumu\n" +
                        "  • Yeşil → tamamlanan aşama\n" +
                        "  • Koyu  → henüz ulaşılmamış aşama\n\n" +
                        "Alt kısımdaki listeden yeni durum seçip\n" +
                        "'Uygula' ile statüyü güncelleyebilirsiniz."
        );

        TooltipUtil.install(checkinCard,
                "YENİ CHECK-İN FORMU\n\n" +
                        "Yeni bir bagajı sisteme kaydetmek için\n" +
                        "tüm alanları doldurun:\n\n" +
                        "  • Yolcu ID  → P012 gibi sisteme kayıtlı kimlik\n" +
                        "  • Uçuş      → Yüklenecek uçuşu seçin\n" +
                        "  • Ağırlık   → kg, ondalık için nokta kullanın\n" +
                        "  • Sınıf     → VIP/Business önce yüklenir\n" +
                        "  • Tehlikeli → Güvenlik öncelikli tarama\n\n" +
                        "Kayıt sonrası bagaj otomatik olarak\n" +
                        "öncelik kuyruğuna eklenir."
        );

        // ── Filtre çubuğu kontrolleri ────────────────────────────────────────

        TooltipUtil.install(searchId,
                "Bagaj ID, Yolcu ID veya Uçuş Numarasına göre gerçek zamanlı arama yapar.\n" +
                        "Yazdıkça tablo otomatik filtrelenir; Enter'a basmak gerekmez.\n" +
                        "Örnek: B001 · P012 · TK505"
        );

        TooltipUtil.install(filterCombo,
                "Bagajları statü veya özelliğe göre filtreler.\n\n" +
                        "Seçenekler:\n" +
                        "  • Check-in              → Yeni kayıtlı bagajlar\n" +
                        "  • Güvenlik Taraması  → Taranan bagajlar\n" +
                        "  • Kargo / Yüklendi   → Uçakta veya kargo alanında\n" +
                        "  • Bekleme Kuyruğu  → Kapasite nedeniyle bekleyen\n" +
                        "  • Güvenlik Havuzu   → Tehlike tespiti yapılanlar\n" +
                        "  • Teslim Edildi         → Yolcuya ulaşmış\n" +
                        "  • ⚠ Tehlikeli            → Tehlikeli madde bayrağı taşıyanlar\n\n" +
                        "Metin aramayla birlikte çalışır (AND mantığı)."
        );

        TooltipUtil.install(clearFilterBtn,
                "Hem metin arama kutusunu hem de statü filtresini temizler.\n" +
                        "Tüm bagajlar yeniden listelenir."
        );

        TooltipUtil.install(totalLabel,
                "Aktif filtre ve arama sonucunda tabloda görünen bagaj sayısını gösterir."
        );

        // ── Detay kartı içindeki bireysel kontroller ─────────────────────────

        TooltipUtil.install(statusCombo,
                "Bagajın atanacağı yeni statüyü seçin.\n" +
                        "Ardından 'Uygula' butonuna basın.\n" +
                        "(Önce tablodan bir bagaj seçmeyi unutmayın.)"
        );

        TooltipUtil.install(applyStatusBtn,
                "Tabloda seçili bagajın durumunu, soldaki listeden seçilen yeni statüye günceller.\n" +
                        "Güncelleme kalıcıdır ve izleme geçmişine yansır."
        );

        // ── Check-in kartı içindeki bireysel kontroller ──────────────────────

        TooltipUtil.install(ciPassenger,
                "Yolcunun sistemdeki kimliğini girin.\n" +
                        "Örnek: P012 — Büyük harf + rakamdan oluşur."
        );

        TooltipUtil.install(ciFlightCombo,
                "Bagajın yükleneceği uçuşu seçin.\n" +
                        "Yalnızca kapasitesi dolu olmayan aktif uçuşlar listelenir."
        );

        TooltipUtil.install(ciWeight,
                "Bagajın kilogram cinsinden ağırlığını girin.\n" +
                        "Örnek: 23.5 — Ondalık için nokta kullanın.\n" +
                        "Uçuş kapasite hesabına dahil edilir."
        );

        TooltipUtil.install(ciClassCombo,
                "Yolcunun uçuş sınıfı.\n" +
                        "VIP (First Class) ve Business bagajlar yükleme sırasında Economy'den önce alınır."
        );

        TooltipUtil.install(ciDangerous,
                "İşaretlenirse bagaj tehlikeli madde olarak kaydedilir.\n" +
                        "Güvenlik taraması sırasında otomatik olarak Güvenlik Havuzuna alınabilir.\n" +
                        "Alt menüden kategoriyi mutlaka belirtin."
        );

        TooltipUtil.install(ciDangerCombo,
                "Tehlikeli madde kategorisini seçin (IATA sınıflandırması).\n" +
                        "'Tehlikeli Madde İçeriyor' işaretlendiğinde aktif olur.\n" +
                        "Yanlış kategori güvenlik ihlali olarak kayıt edilebilir."
        );

        TooltipUtil.install(checkInBtn,
                "Tüm alanlar doldurulduktan sonra yeni bagajı sisteme kaydeder.\n" +
                        "Bagaj otomatik olarak öncelik kuyruğuna (VIP önce) eklenir ve izleme başlatılır."
        );
    }

    /**
     * Herhangi bir Region'a gecikmeli & harekete duyarlı tooltip kurar.
     * Dashboard'daki TooltipUtil.install() ile birebir aynı mekanizma.
     *
     * ⚙ GECIKME AYARI → TOOLTIP_HOVER_DELAY_SECONDS sabitini değiştir.
     */


    // ==================== SÜTUNLAR ====================

    private void setupColumns() {
        colId.setCellValueFactory(c       -> new SimpleStringProperty(c.getValue().getBaggageId()));
        colPassenger.setCellValueFactory(c-> new SimpleStringProperty(c.getValue().getPassengerId()));
        colFlight.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getFlightNumber()));
        colWeight.setCellValueFactory(c   -> new SimpleStringProperty(
                String.format("%.1f", c.getValue().getWeightKg())));
        colClass.setCellValueFactory(c    -> new SimpleStringProperty(
                c.getValue().getOwnerClass().getDisplayName()));
        colStatus.setCellValueFactory(c   -> new SimpleStringProperty(
                c.getValue().getStatus().getDisplayName()));
        colDanger.setCellValueFactory(c   -> new SimpleStringProperty(
                c.getValue().isHasDangerousGoods() ? "⚠ TEHLİKELİ" : "✓ Temiz"));

        colClass.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.getStyleClass().add(switch (item) {
                    case "VIP"      -> "badge-vip";
                    case "Business" -> "badge-business";
                    default         -> "badge-economy";
                });
                setGraphic(lbl); setText(null);
            }
        });

        colDanger.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label lbl = new Label(item);
                lbl.getStyleClass().add(item.startsWith("⚠") ? "badge-danger" : "badge-ok");
                setGraphic(lbl); setText(null);
            }
        });

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); setStyle(""); return; }
                setText(item);
                setGraphic(null);
                String clr = item.contains("Havuz")    ? "#ff3b5c"
                        : item.contains("Teslim")   ? "#00e5a0"
                          : item.contains("Kargo")    ? "#9d5cff"
                            : item.contains("Güvenlik") ? "#ffb830" : "#7a8fa8";
                setStyle("-fx-text-fill:" + clr + ";");
            }
        });
    }

    // ==================== COMBO KUTULARI ====================

    private void setupComboBoxes() {
        statusCombo.getItems().setAll(BaggageStatus.values());
        ciClassCombo.getItems().setAll(PassengerClass.values());
        ciDangerCombo.getItems().setAll(DangerousGoodsCategory.values());

        List<String> flightNos = ctrl.getUpcomingFlights().stream()
                .map(Flight::getFlightNumber).collect(Collectors.toList());
        ciFlightCombo.getItems().setAll(flightNos);

        filterCombo.getItems().addAll(
                F_ALL, F_CHECKIN, F_SEC_SCR, F_CARGO, F_LOADED,
                F_WAITING, F_HOLD, F_DELIVERED, F_DANGER
        );
    }

    // ==================== SEÇİM DETAYI ====================

    private void setupSelectionDetail() {
        baggageTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, b) -> {
                    if (b == null) return;
                    detailId.setText("ID: " + b.getBaggageId());
                    detailPass.setText("Yolcu: " + b.getPassengerId());
                    detailFlight.setText("Uçuş: " + b.getFlightNumber());
                    detailWt.setText(String.format("Ağırlık: %.1f kg", b.getWeightKg()));
                    detailClass.setText("Sınıf: " + b.getOwnerClass().getDisplayName());
                    detailStat.setText("Durum: " + b.getStatus().getDisplayName());
                    detailDanger.setText(b.isHasDangerousGoods()
                            ? "⚠  " + (b.getDangerousCategory() != null
                                       ? b.getDangerousCategory().getDescription() : "Tehlikeli") : "");
                    buildTimeline(b);
                    AnimationUtil.slideInRight(detailId);
                });
    }

    private void buildTimeline(Baggage b) {
        timelineBox.getChildren().clear();
        BaggageStatus[] allSteps = {
                BaggageStatus.CHECK_IN,
                BaggageStatus.SECURITY_SCREENING,
                BaggageStatus.CARGO,
                BaggageStatus.LOADED,
                BaggageStatus.DELIVERED
        };
        boolean passed = true;
        for (BaggageStatus step : allSteps) {
            if (step == b.getStatus()) passed = false;
            boolean current = step == b.getStatus();
            boolean done    = passed || current;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Circle dot = new Circle(5);
            dot.setStyle("-fx-fill:" + (current ? "#00d4ff" : done ? "#00e5a0" : "#1e3050") + ";");
            Label lbl = new Label(step.getDisplayName());
            lbl.setStyle("-fx-font-size:11px; -fx-text-fill:"
                    + (current ? "#00d4ff" : done ? "#7a8fa8" : "#1e3050") + ";"
                    + (current ? " -fx-font-weight:bold;" : ""));
            row.getChildren().addAll(dot, lbl);
            row.setPadding(new Insets(0, 0, 0, 4));
            timelineBox.getChildren().add(row);
        }
    }

    // ==================== EYLEMLER ====================
    @FXML
    private void onUpdateStatus() {
        Baggage sel      = baggageTable.getSelectionModel().getSelectedItem();
        BaggageStatus ns = statusCombo.getValue();
        if (sel == null || ns == null) {
            DialogUtil.showInfo("Bagaj ve durum seçin.");
            return;
        }
        boolean ok = ctrl.updateBaggageStatus(sel.getBaggageId(), ns);
        if (!ok) {
            DialogUtil.showInfo("Hata", "❌ Durum güncellenemedi!\nVeritabanı bağlantısını kontrol edin.");
            return;
        }
        loadAll();
        AnimationUtil.rubberBand(baggageTable);
    }

    @FXML
    private void onCheckIn() {
        String pid    = ciPassenger.getText().trim();
        String flight = ciFlightCombo.getValue();
        String wtStr  = ciWeight.getText().trim();
        PassengerClass pc = ciClassCombo.getValue();

        if (pid.isEmpty() || flight == null || wtStr.isEmpty() || pc == null) {
            AnimationUtil.headShake(ciPassenger);
            DialogUtil.showInfo("Tüm alanları doldurun.");
            return;
        }
        double wt;
        try { wt = Double.parseDouble(wtStr); }
        catch (NumberFormatException ex) {
            AnimationUtil.headShake(ciWeight);
            DialogUtil.showInfo("Geçerli bir ağırlık girin.");
            return;
        }

        Baggage b = new Baggage(pid, flight, wt, pc);
        if (ciDangerous.isSelected() && ciDangerCombo.getValue() != null) {
            b.setHasDangerousGoods(true);
            b.setDangerousCategory(ciDangerCombo.getValue());
        }

        Optional<String> result = ctrl.checkIn(b);
        if (result.isEmpty()) {
            DialogUtil.showInfo("Hata", "❌ Bagaj veritabanına kaydedilemedi!\nLütfen sistem yöneticinize başvurun.");
            return;
        }
        ctrl.addToPriorityQueue(b);
        loadAll();

        ciPassenger.clear(); ciWeight.clear();
        ciFlightCombo.setValue(null); ciClassCombo.setValue(null);
        ciDangerous.setSelected(false); ciDangerCombo.setValue(null);

        AnimationUtil.bounceIn(baggageTable);
        DialogUtil.showInfo("✓  Check-in tamamlandı: " + b.getBaggageId());
    }

    // ==================== YARDIMCI ====================

    private void loadAll() {
        baggageTable.getSelectionModel().clearSelection();

        List<Baggage> all = new java.util.ArrayList<>();
        for (BaggageStatus st : BaggageStatus.values()) {
            all.addAll(ctrl.getTrackingService().getByStatus(st));
        }
        masterList.setAll(all);
        updateFilter();
    }
    @Override
    public void onPanelShown() {
        loadAll();   // tabloyu yeniden yükle
    }

}