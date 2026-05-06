package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.model.enums.BaggageStatus;
import com.airport.ui.util.AnimationUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CapacityController implements Initializable {

    @FXML private ComboBox<String>            flightCombo;
    @FXML private TableView<Baggage>          loadedTable;
    @FXML private TableColumn<Baggage,String> lColId, lColPass, lColWt, lColClass, lColStat;

    // HATA DÜZELTİLDİ: waitingQueueView String değil, Baggage tutmalı.
    @FXML private ListView<String>            flightCapacityList;
    @FXML private ListView<Baggage>           waitingQueueView;

    @FXML private Label  loadedCount, waitingCount;
    @FXML private ProgressBar capacityBar;
    @FXML private Label  capacityPctLabel, remainingLabel;
    @FXML private Label  resultLabel, summaryLabel;

    // HATA DÜZELTİLDİ: lookup yerine doğrudan FXML injection eklendi.
    @FXML private Label  waitingWeightLabel;

    private final AirportController ctrl = MainApp.CONTROLLER;
    private final DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("HH:mm  dd.MM");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> flightNos = ctrl.getUpcomingFlights().stream()
                .map(Flight::getFlightNumber).collect(Collectors.toList());
        flightCombo.getItems().setAll(flightNos);

        flightCombo.setOnAction(e -> refreshCapacityBar(flightCombo.getValue()));

        setupLoadedTable();
        refreshAll();
    }

    private void setupLoadedTable() {
        lColId.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getBaggageId()));
        lColPass.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getPassengerId()));
        lColWt.setCellValueFactory(c    -> new SimpleStringProperty(String.format("%.1f kg", c.getValue().getWeightKg())));
        lColClass.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOwnerClass().getDisplayName()));
        lColStat.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getStatus().getDisplayName()));

        lColClass.setCellFactory(col -> new TableCell<>() {
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

        lColStat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String clr = item.contains("Kargo") || item.contains("Yüklendi")
                        ? "#00e5a0"
                        : item.contains("Bekleme") ? "#ffb830" : "#7a8fa8";
                setStyle("-fx-text-fill:" + clr + ";");
            }
        });
    }

    @FXML private void onLoadWithCapacity() {
        String fn = flightCombo.getValue();
        if (fn == null) { showInfo("Lütfen bir uçuş seçin."); return; }

        Flight flight = ctrl.getFlightByNumber(fn).orElse(null);
        if (flight == null) { showInfo("Uçuş bulunamadı."); return; }

        List<Baggage> toLoad = ctrl.getPriorityLoadingService().processAllForFlight(fn);
        if (toLoad.isEmpty()) {
            toLoad = ctrl.getFlightBaggage(fn).stream()
                    .filter(b -> !b.isHasDangerousGoods())
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (toLoad.isEmpty()) { showInfo("Yüklenecek bagaj bulunamadı."); return; }

        com.airport.service.CapacityService.LoadingResult result = ctrl.loadWithCapacityCheck(toLoad, flight);

        resultLabel.setText(String.format(
                "✓  %s  —  Yüklendi: %d  |  Kuyruğa alındı: %d  |  Yük: %.0f / %.0f kg  (%.1f %%)",
                fn, result.getLoadedCount(), result.getQueuedCount(),
                result.getTotalLoadKg(), result.getMaxCapacityKg(), result.getLoadPercentage()
        ));

        for (Baggage b : result.getLoadedBaggage())
            ctrl.updateBaggageStatus(b.getBaggageId(), BaggageStatus.CARGO);

        refreshAll();
        refreshCapacityBar(fn);
        AnimationUtil.bounceIn(capacityBar);

        if (result.getQueuedCount() > 0) {
            resultLabel.setStyle("-fx-text-fill:#ffb830; -fx-font-size:12px;");
            AnimationUtil.flash(resultLabel);
        } else {
            resultLabel.setStyle("-fx-text-fill:#00e5a0; -fx-font-size:12px;");
        }
    }

    @FXML private void onAssignWaiting() {
        String fn = flightCombo.getValue();
        if (fn == null) { showInfo("Lütfen hedef uçuşu seçin."); return; }

        Flight flight = ctrl.getFlightByNumber(fn).orElse(null);
        if (flight == null) { showInfo("Uçuş bulunamadı."); return; }

        List<Baggage> assigned = ctrl.assignWaitingBaggageToFlight(flight);
        if (assigned.isEmpty()) {
            showInfo("Bekleme kuyruğu boş veya " + fn + " için kapasite yok.");
            return;
        }

        for (Baggage b : assigned)
            ctrl.updateBaggageStatus(b.getBaggageId(), BaggageStatus.CARGO);

        resultLabel.setText("↗  " + assigned.size() + " bekleme bagajı " + fn + " uçuşuna atandı.");
        resultLabel.setStyle("-fx-text-fill:#00d4ff; -fx-font-size:12px;");
        refreshAll();
        refreshCapacityBar(fn);
        AnimationUtil.slideInLeft(resultLabel);
    }

    private void refreshAll() {
        refreshLoadedTable();
        refreshFlightCapacityList();
        refreshWaitingQueue();
        updateSummary();
    }

    private void refreshLoadedTable() {
        List<Baggage> cargoList = ctrl.getTrackingService().getByStatus(BaggageStatus.CARGO);
        cargoList.addAll(ctrl.getTrackingService().getByStatus(BaggageStatus.LOADED));
        loadedTable.getItems().setAll(cargoList);
        loadedCount.setText(String.valueOf(cargoList.size()));
    }

    private void refreshFlightCapacityList() {
        List<Flight> flights = ctrl.getUpcomingFlights();
        List<String> items = flights.stream().map(f -> {
            double pct = f.getMaxCapacityKg() > 0 ? f.getCurrentLoadKg() / f.getMaxCapacityKg() * 100 : 0;
            return String.format("%s  |  %.0f / %.0f kg  (%.0f%%)",
                    f.getFlightNumber(), f.getCurrentLoadKg(), f.getMaxCapacityKg(), pct);
        }).collect(Collectors.toList());
        flightCapacityList.getItems().setAll(items);

        flightCapacityList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                double pct = 0;
                try {
                    String pctStr = s.substring(s.lastIndexOf('(') + 1, s.lastIndexOf('%'));
                    pct = Double.parseDouble(pctStr.trim()) / 100.0;
                } catch (Exception ignored) {}

                Label lbl = new Label(s.split("\\|")[0].trim());
                lbl.setStyle("-fx-text-fill:#e8eef8; -fx-font-weight:bold; -fx-font-size:12px;");
                ProgressBar pb = new ProgressBar(pct);
                pb.setPrefWidth(90); pb.setPrefHeight(7);
                pb.getStyleClass().removeAll("danger","warning");
                if (pct > 0.85) pb.getStyleClass().add("danger");
                else if (pct > 0.6) pb.getStyleClass().add("warning");
                HBox.setHgrow(lbl, Priority.ALWAYS);
                row.getChildren().addAll(lbl, pb);
                setGraphic(row);
                setStyle("-fx-background-color:transparent;");
            }
        });
    }

    private void refreshWaitingQueue() {
        List<Baggage> waiting = ctrl.getWaitingBaggage();
        waitingCount.setText(String.valueOf(waiting.size()));
        double totalWt = waiting.stream().mapToDouble(Baggage::getWeightKg).sum();

        waitingQueueView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Baggage b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) { setGraphic(null); return; }
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label pos = new Label(String.valueOf(getIndex() + 1) + ".");
                pos.setStyle("-fx-text-fill:#3d5068; -fx-font-size:11px; -fx-min-width:20;");
                Label id  = new Label(b.getBaggageId());
                id.setStyle("-fx-text-fill:#ffb830; -fx-font-size:12px;");
                Label wt  = new Label(String.format("%.1f kg", b.getWeightKg()));
                wt.setStyle("-fx-text-fill:#7a8fa8; -fx-font-size:11px;");
                Label cl  = new Label(b.getOwnerClass().getDisplayName());
                cl.setStyle("-fx-font-size:10px; -fx-text-fill:" + classBadgeColor(b.getOwnerClass()) + ";");
                HBox.setHgrow(id, Priority.ALWAYS);
                row.getChildren().addAll(pos, id, wt, cl);
                setGraphic(row);
                setStyle("-fx-background-color:transparent;");
                AnimationUtil.addGlowHover(row, Color.web("#ffb83044"));
            }
        });
        waitingQueueView.getItems().setAll(waiting);

        // HATA DÜZELTİLDİ: NPE fırlatan lookup yerine doğrudan atama yapıldı.
        if (waitingWeightLabel != null) {
            waitingWeightLabel.setText(String.format("Toplam bekleyen: %.1f kg", totalWt));
        }
    }

    private void refreshCapacityBar(String fn) {
        if (fn == null) return;
        Flight f = ctrl.getFlightByNumber(fn).orElse(null);
        if (f == null) return;
        double ratio = f.getMaxCapacityKg() > 0 ? f.getCurrentLoadKg() / f.getMaxCapacityKg() : 0;
        capacityBar.setProgress(ratio);
        capacityBar.getStyleClass().removeAll("danger","warning");
        if (ratio > 0.85)     capacityBar.getStyleClass().add("danger");
        else if (ratio > 0.6) capacityBar.getStyleClass().add("warning");
        capacityPctLabel.setText(String.format("%.0f / %.0f kg  (%.1f %%)", f.getCurrentLoadKg(), f.getMaxCapacityKg(), ratio * 100));
        remainingLabel.setText(String.format("Kalan: %.0f kg", f.getMaxCapacityKg() - f.getCurrentLoadKg()));
    }

    private void updateSummary() {
        long cargoTotal = ctrl.getTrackingService().getByStatus(BaggageStatus.CARGO).size()
                + ctrl.getTrackingService().getByStatus(BaggageStatus.LOADED).size();
        long waitingTotal = ctrl.getWaitingCount();
        summaryLabel.setText(String.format("Kargoda: %d  |  Beklemede: %d", cargoTotal, waitingTotal));
    }

    private String classBadgeColor(com.airport.model.enums.PassengerClass pc) {
        return switch (pc) {
            case VIP      -> "#c580ff";
            case BUSINESS -> "#00d4ff";
            case ECONOMY  -> "#7a8fa8";
        };
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle("Bilgi"); a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/com/airport/css/dark-theme.css").toExternalForm());
        a.showAndWait();
    }
}