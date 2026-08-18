package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.ui.util.AnimationUtil;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import com.airport.ui.util.TooltipUtil;
import com.airport.ui.util.DialogUtil;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Uçuş Takvimi panel controller'ı.
 * Min-Heap PriorityQueue'daki uçuşları tabloya yansıtır.
 * Seçilen uçuşun detaylarını ve bagajlarını yan panelde gösterir.
 *
 * TOOLTIP EKLEMESİ (v2 — TooltipUtil.install migrasyonu):
 *   FlightPanel.fxml'deki 14 <tooltip> bloğu kaldırıldı.
 *   Kart seviyesi tooltip'ler (flightDetailCard, flightBaggageCard):
 *     setupCardTooltips() → TooltipUtil.install
 *   Bireysel kontrol tooltip'leri:
 *     setupControlTooltips() → TooltipUtil.install
 *   ★ Yeni @FXML alanları: refreshFlightBtn, boardingBtn, departBtn, cancelFlightBtn
 */
public class FlightController implements Initializable,Refreshable {

    // ── Mevcut FXML alanları ─────────────────────────────────────────────────

    @FXML private TableView<Flight>              flightTable;
    @FXML private TableColumn<Flight,String>     colNo, colRoute, colDep, colArr, colLoad, colStatus, colGate;
    @FXML private TextField                      searchField;
    @FXML private ListView<Baggage>              flightBaggageList;
    @FXML private Label  detailFlightNo, detailRoute, detailStatus, detailCapacity, detailDep;
    @FXML private ProgressBar capacityBar;
    @FXML private Label  capacityPct;

    // ── Kart referansları — TooltipUtil.install için ────────────────────────
    @FXML private VBox flightDetailCard;
    @FXML private VBox flightBaggageCard;

    // ── ★ YENİ — bireysel kontrol tooltip referansları ───────────────────────

    /**
     * fx:id="refreshFlightBtn" eklendi (FlightPanel.fxml).
     * FXML {@code <tooltip>} kaldırıldı; TooltipUtil.install ile kurulur.
     */
    @FXML private Button refreshFlightBtn;

    /**
     * fx:id="boardingBtn" eklendi (FlightPanel.fxml).
     * FXML {@code <tooltip>} kaldırıldı; TooltipUtil.install ile kurulur.
     */
    @FXML private Button boardingBtn;

    /**
     * fx:id="departBtn" eklendi (FlightPanel.fxml).
     * FXML {@code <tooltip>} kaldırıldı; TooltipUtil.install ile kurulur.
     */
    @FXML private Button departBtn;

    /**
     * fx:id="cancelFlightBtn" eklendi (FlightPanel.fxml).
     * FXML {@code <tooltip>} kaldırıldı; TooltipUtil.install ile kurulur.
     */
    @FXML private Button cancelFlightBtn;

    // ── Tooltip paylaşımlı durum ─────────────────────────────────────────────

    /** Aynı anda yalnızca bir tooltip açık kalabilmesi için paylaşımlı takip. */
    private Timeline activeHoverTimer;
    private Tooltip  activeTooltip;

    private static final double TOOLTIP_HOVER_DELAY_SECONDS = 1.5;

    // ── Diğer alanlar ────────────────────────────────────────────────────────

    private final AirportController ctrl = MainApp.CONTROLLER;
    private final DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private FilteredList<Flight> filtered;
    private Flight selectedFlight = null;

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupSearch();
        setupSelectionDetail();
        setupCardTooltips();    // ← kart seviyesi tooltip'ler
        setupControlTooltips(); // ← ★ YENİ — bireysel kontrol tooltip'leri
        loadData();
    }

    // ==================== KART TOOLTİP KURULUMU ====================

    /**
     * flightDetailCard ve flightBaggageCard için kart seviyesi tooltip'leri kurar.
     * Dashboard stili — tüm kart alanına bağlı, gecikmeli & harekete duyarlı.
     */
    private void setupCardTooltips() {
        TooltipUtil.install(flightDetailCard,
                "UÇUŞ DETAYI\n\n" +
                        "Tablodan tıkladığınız uçuşun tam bilgilerini gösterir:\n" +
                        "uçuş no, rota, durum, kalkış saati ve anlık kapasite doluluk oranı.\n\n" +
                        "Kapasite çubuğu:\n" +
                        "  Mavi  → normal yük\n" +
                        "  Sarı  → %60 üzeri\n" +
                        "  Kırmızı → %85 üzeri (kritik)\n\n" +
                        "Butonlar seçili uçuş üzerinde çalışır:\n" +
                        "  Boarding → durumu 'Biniş' yapar\n" +
                        "  Depart   → PriorityQueue'dan çıkarır\n" +
                        "  İptal    → durumu 'İptal' yapar"
        );

        TooltipUtil.install(flightBaggageCard,
                "KAYITLI BAGAJLAR\n\n" +
                        "Tablodan seçilen uçuşa kayıtlı tüm bagajları listeler.\n\n" +
                        "Her satırda: Bagaj ID · Ağırlık (kg) · Yolcu Sınıfı\n\n" +
                        "Kırmızı renk → tehlikeli madde işaretli bagaj.\n" +
                        "Detaylı inceleme için Bagaj Yönetimi ekranına gidin."
        );
    }

    // ==================== BİREYSEL KONTROL TOOLTİP KURULUMU ====================

    /**
     * ★ YENİ — FlightPanel.fxml'den kaldırılan 14 {@code <tooltip>} bloğunun
     * programatik karşılığı. Tüm bireysel kontrollere TooltipUtil.install() bağlar.
     *
     * <p>Yeni bir kontrol eklendiğinde buraya tek satır eklemek yeterlidir.
     */
    private void setupControlTooltips() {

        // ── Arama çubuğu ─────────────────────────────────────────────────────

        TooltipUtil.install(searchField,
                "Uçuş numarası (TK101) veya rota (IST → ADB) ile anlık filtreleme yapın.\n" +
                        "Yazdıkça tablo otomatik güncellenir; silince tüm uçuşlar yeniden görünür."
        );

        TooltipUtil.install(refreshFlightBtn,
                "Uçuş listesini PriorityQueue'dan yeniden yükler.\n" +
                        "Yeni eklenen uçuşlar veya durum değişiklikleri tabloya yansır."
        );

        // ── Uçuş tablosu ─────────────────────────────────────────────────────

        TooltipUtil.install(flightTable,
                "Sistemdeki tüm uçuşlar, Min-Heap PriorityQueue'dan en erken kalkış sırasına göre listelenir.\n\n" +
                        "Bir satıra tıkladığınızda sağ panelde uçuş detayları ve ilgili bagajlar görünür."
        );

        // ── Detay kartı — bireysel kontroller ────────────────────────────────

        TooltipUtil.install(detailFlightNo,
                "Seçili uçuşun IATA kodu (örn. TK101)."
        );

        TooltipUtil.install(detailRoute,
                "Kalkış → Varış havaalanı (IATA kodları)."
        );

        TooltipUtil.install(detailDep,
                "Planlanan kalkış tarihi ve saati."
        );

        TooltipUtil.install(detailStatus,
                "Uçuşun anlık durumu:\n" +
                        "Planlandı → Biniş → Kalktı → İndi\n" +
                        "veya İptal"
        );

        TooltipUtil.install(detailCapacity,
                "Mevcut yük / Maksimum kapasite (kg cinsinden)."
        );

        TooltipUtil.install(capacityPct,
                "Uçuşun mevcut bagaj yükünün toplam kapasiteye oranı.\n" +
                        "%85 üzeri → kırmızı (kritik)\n" +
                        "%60–85 → sarı (uyarı)"
        );

        TooltipUtil.install(capacityBar,
                "Kapasite doluluk çubuğu.\n" +
                        "Mavi: normal — Sarı: %60+ — Kırmızı: %85+"
        );

        TooltipUtil.install(boardingBtn,
                "Seçili uçuşun durumunu 'Biniş' olarak günceller.\n" +
                        "Önce tablodan bir uçuş seçilmiş olmalıdır."
        );

        TooltipUtil.install(departBtn,
                "Min-Heap PriorityQueue'nun tepesindeki uçuşu (en erken kalkan) listeden çıkarır\n" +
                        "ve durumunu 'Kalktı' yapar.\n\n" +
                        "Bu işlem geri alınamaz."
        );

        TooltipUtil.install(cancelFlightBtn,
                "Seçili uçuşu iptal eder ve durumunu 'İptal' yapar.\n" +
                        "Bu uçuşa kayıtlı bagajlar etkilenmez.\n\n" +
                        "Önce tablodan bir uçuş seçilmiş olmalıdır."
        );

        // ── Bagaj listesi ─────────────────────────────────────────────────────

        TooltipUtil.install(flightBaggageList,
                "Seçili uçuşa kayıtlı tüm bagajlar.\n\n" +
                        "Her satırda: Bagaj ID, ağırlık (kg) ve yolcu sınıfı gösterilir.\n" +
                        "Kırmızı renk → tehlikeli madde içeren bagaj."
        );
    }

    // ==================== SÜTUN KURULUMU ====================

    private void setupColumns() {
        colNo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFlightNumber()));
        colRoute.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRoute()));
        colDep.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartureTime().format(fmt)));
        colArr.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getArrivalTime().format(fmt)));
        colLoad.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.0f / %.0f kg", c.getValue().getCurrentLoadKg(), c.getValue().getMaxCapacityKg())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getDisplayName()));
        colGate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getGate() != null ? c.getValue().getGate() : "—"));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "Biniş"     -> "#00d4ff";
                    case "Kalktı"    -> "#ffb830";
                    case "İndi"      -> "#00e5a0";
                    case "İptal"     -> "#ff3b5c";
                    default          -> "#7a8fa8";
                };
                setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold;");
            }
        });

        colLoad.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar pb = new ProgressBar(0);
            {
                pb.setMaxWidth(Double.MAX_VALUE);
                pb.setPrefHeight(8);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Flight f = (Flight) getTableRow().getItem();
                double ratio = f.getMaxCapacityKg() > 0 ? f.getCurrentLoadKg() / f.getMaxCapacityKg() : 0;
                pb.setProgress(ratio);
                pb.getStyleClass().removeAll("danger","warning");
                if (ratio > 0.85) pb.getStyleClass().add("danger");
                else if (ratio > 0.6) pb.getStyleClass().add("warning");

                Label lbl = new Label(String.format("%.0f%%", ratio * 100));
                lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#7a8fa8;");
                HBox box = new HBox(6, pb, lbl);
                box.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(pb, Priority.ALWAYS);
                setGraphic(box);
                setText(null);
            }
        });
    }

    // ==================== ARAMA ====================

    private void setupSearch() {
        searchField.textProperty().addListener((obs, o, n) -> {
            if (filtered != null)
                filtered.setPredicate(f -> n == null || n.isEmpty()
                        || f.getFlightNumber().toLowerCase().contains(n.toLowerCase())
                        || f.getRoute().toLowerCase().contains(n.toLowerCase()));
        });
    }

    // ==================== SEÇİM DETAYI ====================

    private void setupSelectionDetail() {
        flightTable.getSelectionModel().selectedItemProperty().addListener((obs, old, f) -> {
            selectedFlight = f;
            if (f == null) return;
            detailFlightNo.setText(f.getFlightNumber());
            detailRoute.setText("Rota: " + f.getRoute());
            detailStatus.setText("Durum: " + f.getStatus().getDisplayName());
            detailCapacity.setText(String.format("Kapasite: %.0f / %.0f kg", f.getCurrentLoadKg(), f.getMaxCapacityKg()));
            detailDep.setText("Kalkış: " + f.getDepartureTime().format(fmt));

            double ratio = f.getMaxCapacityKg() > 0 ? f.getCurrentLoadKg() / f.getMaxCapacityKg() : 0;
            capacityBar.setProgress(ratio);
            capacityPct.setText(String.format("%.1f %%", ratio * 100));
            capacityBar.getStyleClass().removeAll("danger","warning");
            if (ratio > 0.85) capacityBar.getStyleClass().add("danger");
            else if (ratio > 0.6) capacityBar.getStyleClass().add("warning");

            List<Baggage> bags = ctrl.getFlightBaggage(f.getFlightNumber());
            flightBaggageList.getItems().setAll(bags);
            flightBaggageList.setCellFactory(lv -> buildBaggageCell());

            AnimationUtil.slideInRight(detailFlightNo);
        });
    }

    // ==================== EYLEMLER ====================

    @FXML private void onRefresh() {
        loadData();
        AnimationUtil.fadeIn(flightTable);
    }

    @FXML private void onDepartNext() {
        boolean ok = ctrl.departNextFlight();  // artık boolean dönüyor
        if (!ok) {
            DialogUtil.showInfo("Hata", "❌ Kalkış işlemi başarısız oldu!");
            return;
        }
        loadData();
        DialogUtil.showInfo("Uçak kaldırıldı.");
    }

    @FXML private void onStartBoarding() {
        if (selectedFlight == null) {
            DialogUtil.showInfo("Lütfen bir uçuş seçin.");
            return;
        }
        String flightNo = selectedFlight.getFlightNumber();
        boolean ok = ctrl.startBoarding(flightNo);
        if (!ok) {
            DialogUtil.showInfo("Hata", "❌ Biniş başlatılamadı!");
            return;
        }
        selectedFlight = null;
        loadData();
    }

    @FXML private void onCancelFlight() {
        if (selectedFlight == null) {
            DialogUtil.showInfo("Lütfen bir uçuş seçin.");
            return;
        }
        String flightNo = selectedFlight.getFlightNumber();
        boolean ok = ctrl.cancelFlight(flightNo);
        if (!ok) {
            DialogUtil.showInfo("Hata", "❌ Uçuş iptal edilemedi!");
            return;
        }
        selectedFlight = null;
        loadData();
    }

    // ==================== YARDIMCI ====================

    private void loadData() {
        List<Flight> flights = ctrl.getUpcomingFlights();
        ObservableList<Flight> items = FXCollections.observableArrayList(flights);
        filtered = new FilteredList<>(items);

        String searchText = searchField.getText();
        if (searchText != null && !searchText.isEmpty()) {
            filtered.setPredicate(f ->
                    f.getFlightNumber().toLowerCase().contains(searchText.toLowerCase()) ||
                            f.getRoute().toLowerCase().contains(searchText.toLowerCase()));
        }

        flightTable.setItems(filtered);
        flightTable.refresh();
    }

    private ListCell<Baggage> buildBaggageCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Baggage b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) { setGraphic(null); return; }
                Label id = new Label(b.getBaggageId());
                id.setStyle("-fx-text-fill:#e8eef8; -fx-font-size:12px;");
                Label wt = new Label(String.format("%.1f kg", b.getWeightKg()));
                wt.setStyle("-fx-text-fill:#7a8fa8; -fx-font-size:11px;");
                Label cl = new Label(b.getOwnerClass().getDisplayName());
                cl.setStyle("-fx-font-size:11px; -fx-text-fill:" +
                        (b.isHasDangerousGoods() ? "#ff3b5c" : "#00d4ff") + ";");
                HBox row = new HBox(10, id, wt, cl);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(wt, Priority.ALWAYS);
                setGraphic(row);
                setStyle("-fx-background-color:transparent;");
            }
        };
    }
    @Override
    public void onPanelShown() {
        loadData();  // uçuş listesini tazele
    }

}