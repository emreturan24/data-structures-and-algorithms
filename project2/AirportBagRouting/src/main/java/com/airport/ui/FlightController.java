package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.ui.util.AnimationUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Uçuş Takvimi panel controller'ı.
 * Min-Heap PriorityQueue'daki uçuşları tabloya yansıtır.
 * Seçilen uçuşun detaylarını ve bagajlarını yan panelde gösterir.
 */
public class FlightController implements Initializable {

    @FXML private TableView<Flight>              flightTable;
    @FXML private TableColumn<Flight,String>     colNo, colRoute, colDep, colArr, colLoad, colStatus, colGate;
    @FXML private TextField                      searchField;
    @FXML private ListView<Baggage>              flightBaggageList;
    @FXML private Label  detailFlightNo, detailRoute, detailStatus, detailCapacity, detailDep;
    @FXML private ProgressBar capacityBar;
    @FXML private Label  capacityPct;

    private final AirportController ctrl = MainApp.CONTROLLER;
    private final DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private FilteredList<Flight> filtered;

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupSearch();
        setupSelectionDetail();
        loadData();
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

        // Durum sütunu renk
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

        // Yük sütunu progress bar
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
        Flight f = ctrl.departNextFlight();
        if (f != null) {
            loadData();
            showInfo("Kalktı: " + f.getFlightNumber() + " — " + f.getRoute());
        }
    }

    @FXML private void onStartBoarding() {
        Flight sel = flightTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Lütfen bir uçuş seçin."); return; }
        ctrl.startBoarding(sel.getFlightNumber());
        loadData();
    }

    @FXML private void onCancelFlight() {
        Flight sel = flightTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Lütfen bir uçuş seçin."); return; }
        ctrl.cancelFlight(sel.getFlightNumber());
        loadData();
    }

    // ==================== YARDIMCI ====================

    private void loadData() {
        List<Flight> flights = ctrl.getUpcomingFlights();
        filtered = new FilteredList<>(FXCollections.observableArrayList(flights));
        flightTable.setItems(filtered);
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

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle("Bilgi");
        a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/airport/css/dark-theme.css").toExternalForm());
        a.showAndWait();
    }
}