package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.model.enums.BaggageStatus;
import com.airport.model.enums.DangerousGoodsCategory;
import com.airport.model.enums.PassengerClass;
import com.airport.ui.util.AnimationUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Bagaj Yönetimi panel controller'ı.
 * HashMap tabanlı O(1) bagaj takibi, check-in formu, durum güncelleme.
 */
public class BaggageController implements Initializable {

    @FXML private TableView<Baggage>               baggageTable;
    @FXML private TableColumn<Baggage,String>      colId, colPassenger, colFlight,
            colWeight, colClass, colStatus, colDanger;
    @FXML private TextField     searchId;
    @FXML private ComboBox<BaggageStatus>           statusCombo;
    @FXML private Label         totalLabel;

    // Detay
    @FXML private Label  detailId, detailPass, detailFlight, detailWt, detailClass, detailStat, detailDanger;
    @FXML private VBox   timelineBox;

    // Check-in formu
    @FXML private TextField                        ciPassenger, ciWeight;
    @FXML private ComboBox<String>                 ciFlightCombo;
    @FXML private ComboBox<PassengerClass>         ciClassCombo;
    @FXML private CheckBox                         ciDangerous;
    @FXML private ComboBox<DangerousGoodsCategory> ciDangerCombo;

    private final AirportController ctrl = MainApp.CONTROLLER;
    private FilteredList<Baggage> filtered;
    private List<Baggage> allBaggage;

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupComboBoxes();
        setupSelectionDetail();
        loadAll();

        // Tehlikeli madde checkbox → kategori combo
        ciDangerous.selectedProperty().addListener((obs, o, n) ->
                ciDangerCombo.setDisable(!n));
    }

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

        // Sınıf badge
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

        // Tehlike sütunu renk
        colDanger.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                Label lbl = new Label(item);
                lbl.getStyleClass().add(item.startsWith("⚠") ? "badge-danger" : "badge-ok");
                setGraphic(lbl); setText(null);
            }
        });

        // Durum renk
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String clr = item.contains("Havuz") ? "#ff3b5c"
                        : item.contains("Teslim")? "#00e5a0"
                          : item.contains("Kargo") ? "#9d5cff"
                            : item.contains("Güvenlik")? "#ffb830" : "#7a8fa8";
                setStyle("-fx-text-fill:" + clr + ";");
            }
        });
    }

    // ==================== COMBO KUTULARI ====================

    private void setupComboBoxes() {
        statusCombo.getItems().setAll(BaggageStatus.values());
        ciClassCombo.getItems().setAll(PassengerClass.values());
        ciDangerCombo.getItems().setAll(DangerousGoodsCategory.values());

        // Uçuş listesi
        List<String> flightNos = ctrl.getUpcomingFlights().stream()
                .map(Flight::getFlightNumber).collect(Collectors.toList());
        ciFlightCombo.getItems().setAll(flightNos);
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

    @FXML private void onSearch() {
        String id = searchId.getText().trim();
        if (id.isEmpty()) { loadAll(); return; }
        ctrl.getBaggage(id).ifPresentOrElse(
                b -> baggageTable.getItems().setAll(b),
                () -> showInfo("Bagaj bulunamadı: " + id)
        );
        totalLabel.setText("Sonuç: " + baggageTable.getItems().size() + " bagaj");
    }

    @FXML private void onShowAll()           { loadAll(); }
    @FXML private void onFilterAll()         { loadAll(); }
    @FXML private void onFilterCheckin()     { filterByStatus(BaggageStatus.CHECK_IN); }
    @FXML private void onFilterSecurity()    { filterByStatus(BaggageStatus.SECURITY_HOLD); }
    @FXML private void onFilterCargo()       { filterByStatus(BaggageStatus.CARGO); }
    @FXML private void onFilterDelivered()   { filterByStatus(BaggageStatus.DELIVERED); }
    @FXML private void onFilterDanger() {
        List<Baggage> danger = allBaggage.stream()
                .filter(Baggage::isHasDangerousGoods).collect(Collectors.toList());
        baggageTable.getItems().setAll(danger);
        totalLabel.setText("Tehlikeli: " + danger.size() + " bagaj");
    }

    private void filterByStatus(BaggageStatus status) {
        List<Baggage> res = ctrl.getTrackingService().getByStatus(status);
        baggageTable.getItems().setAll(res);
        totalLabel.setText("Filtrelendi: " + res.size() + " bagaj");
    }

    @FXML private void onUpdateStatus() {
        Baggage sel    = baggageTable.getSelectionModel().getSelectedItem();
        BaggageStatus ns = statusCombo.getValue();
        if (sel == null || ns == null) { showInfo("Bagaj ve durum seçin."); return; }
        ctrl.updateBaggageStatus(sel.getBaggageId(), ns);
        loadAll();
        AnimationUtil.rubberBand(baggageTable);
    }

    @FXML private void onCheckIn() {
        String pid    = ciPassenger.getText().trim();
        String flight = ciFlightCombo.getValue();
        String wtStr  = ciWeight.getText().trim();
        PassengerClass pc = ciClassCombo.getValue();

        if (pid.isEmpty() || flight == null || wtStr.isEmpty() || pc == null) {
            AnimationUtil.headShake(ciPassenger);
            showInfo("Tüm alanları doldurun."); return;
        }
        double wt;
        try { wt = Double.parseDouble(wtStr); }
        catch (NumberFormatException ex) { AnimationUtil.headShake(ciWeight);
            showInfo("Geçerli bir ağırlık girin."); return; }

        Baggage b = new Baggage(pid, flight, wt, pc);
        if (ciDangerous.isSelected() && ciDangerCombo.getValue() != null) {
            b.setHasDangerousGoods(true);
            b.setDangerousCategory(ciDangerCombo.getValue());
        }
        ctrl.checkIn(b);
        ctrl.addToPriorityQueue(b);
        loadAll();

        // Form temizle
        ciPassenger.clear(); ciWeight.clear();
        ciFlightCombo.setValue(null); ciClassCombo.setValue(null);
        ciDangerous.setSelected(false); ciDangerCombo.setValue(null);

        AnimationUtil.bounceIn(baggageTable);
        showInfo("✓  Check-in tamamlandı: " + b.getBaggageId());
    }

    // ==================== YARDIMCI ====================

    private void loadAll() {
        allBaggage = ctrl.getTrackingService().getByStatus(BaggageStatus.CHECK_IN);
        // Tüm statüslerden topla
        allBaggage = new java.util.ArrayList<>();
        for (BaggageStatus st : BaggageStatus.values()) {
            allBaggage.addAll(ctrl.getTrackingService().getByStatus(st));
        }
        baggageTable.setItems(FXCollections.observableArrayList(allBaggage));
        totalLabel.setText("Toplam: " + allBaggage.size() + " bagaj");
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle("Bilgi"); a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/airport/css/dark-theme.css").toExternalForm());
        a.showAndWait();
    }
}