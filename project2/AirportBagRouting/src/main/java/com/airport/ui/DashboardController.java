package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.model.enums.BaggageStatus;
import com.airport.ui.util.AnimationUtil;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Dashboard ekranı controller'ı.
 * TilesFX KPI tile'larını ve aktif uçuş listesini yönetir.
 * Her 5 saniyede bir veriler otomatik güncellenir.
 */
public class DashboardController implements Initializable {

    @FXML private HBox     tilesContainer;
    @FXML private ListView<Flight>  flightListView;
    @FXML private ListView<Baggage> securityPoolList;
    @FXML private ListView<Baggage> waitingQueueList;
    @FXML private Label    flightCountBadge;
    @FXML private Label    securityPoolCount;
    @FXML private Label    waitingCount;

    private final AirportController ctrl = MainApp.CONTROLLER;
    private final DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("HH:mm  dd.MM");

    // TilesFX tile referansları (güncelleme için)
    private Tile totalBaggageTile;
    private Tile securityTile;
    private Tile waitingTile;
    private Tile loadedTile;
    private Tile flightsTile;
    private Tile deliveredTile;

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildTiles();
        setupFlightList();
        setupSecurityList();
        setupWaitingList();
        refreshData();

        // 5 sn'de bir otomatik güncelleme
        Timeline auto = new Timeline(new KeyFrame(Duration.seconds(5),
                e -> Platform.runLater(this::refreshData)));
        auto.setCycleCount(Animation.INDEFINITE);
        auto.play();

        AnimationUtil.cascadeIn(tilesContainer.getChildren(), 80);
    }

    // ==================== TilesFX KARTLARI ─────────────── ====================

    private void buildTiles() {
        double size = 150;

        totalBaggageTile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(size, size)
                .title("Toplam Bagaj")
                .description("Sistemde")
                .value(0)
                .textVisible(true)
                .foregroundBaseColor(javafx.scene.paint.Color.web("#00d4ff"))
                .build();

        securityTile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(size, size)
                .title("Güvenlik Havuzu")
                .description("Karantina")
                .value(0)
                .foregroundBaseColor(javafx.scene.paint.Color.web("#ff3b5c"))
                .build();

        waitingTile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(size, size)
                .title("Bekleyen")
                .description("Kuyrukta")
                .value(0)
                .foregroundBaseColor(javafx.scene.paint.Color.web("#ffb830"))
                .build();

        loadedTile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(size, size)
                .title("Kargoda")
                .description("Uçakta")
                .value(0)
                .foregroundBaseColor(javafx.scene.paint.Color.web("#9d5cff"))
                .build();

        flightsTile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(size, size)
                .title("Aktif Uçuş")
                .description("Planlandı")
                .value(0)
                .foregroundBaseColor(javafx.scene.paint.Color.web("#1a7fff"))
                .build();

        deliveredTile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(size, size)
                .title("Teslim")
                .description("Bagaj")
                .value(0)
                .foregroundBaseColor(javafx.scene.paint.Color.web("#00e5a0"))
                .build();

        tilesContainer.getChildren().addAll(
                totalBaggageTile, securityTile, waitingTile,
                loadedTile, flightsTile, deliveredTile
        );
    }

    // ==================== LİSTE KURULUMU ====================

    private void setupFlightList() {
        flightListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Flight f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) { setGraphic(null); return; }

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding:4 0 4 0;");

                // Renk noktası: status'a göre
                javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5);
                String dotColor = switch (f.getStatus()) {
                    case BOARDING  -> "#00d4ff";
                    case DEPARTED  -> "#ffb830";
                    case ARRIVED   -> "#00e5a0";
                    case CANCELLED -> "#ff3b5c";
                    default        -> "#7a8fa8";
                };
                dot.setStyle("-fx-fill:" + dotColor + ";");

                VBox info = new VBox(2);
                Label flightNo = new Label(f.getFlightNumber() + "  " + f.getRoute());
                flightNo.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#e8eef8;");
                Label detail = new Label(f.getDepartureTime().format(fmt)
                        + "   " + f.getStatus().getDisplayName()
                        + "   " + String.format("%.0f/%.0f kg", f.getCurrentLoadKg(), f.getMaxCapacityKg()));
                detail.setStyle("-fx-font-size:11px; -fx-text-fill:#7a8fa8;");
                info.getChildren().addAll(flightNo, detail);
                HBox.setHgrow(info, Priority.ALWAYS);

                // Doluluk progress
                javafx.scene.control.ProgressBar pb = new javafx.scene.control.ProgressBar(
                        f.getMaxCapacityKg() > 0 ? f.getCurrentLoadKg() / f.getMaxCapacityKg() : 0);
                pb.setPrefWidth(70);
                pb.setPrefHeight(6);
                double ratio = f.getMaxCapacityKg() > 0 ? f.getCurrentLoadKg() / f.getMaxCapacityKg() : 0;
                if (ratio > 0.85) pb.getStyleClass().add("danger");
                else if (ratio > 0.6) pb.getStyleClass().add("warning");

                row.getChildren().addAll(dot, info, pb);
                setGraphic(row);
                setStyle("-fx-background-color:transparent;");

                AnimationUtil.addGlowHover(row, javafx.scene.paint.Color.web("#00d4ff44"));
            }
        });
    }

    private void setupSecurityList() {
        securityPoolList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Baggage b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) { setGraphic(null); return; }
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label id = new Label("⚠  " + b.getBaggageId());
                id.setStyle("-fx-text-fill:#ff3b5c; -fx-font-weight:bold;");
                Label cat = new Label(b.getDangerousCategory() != null
                        ? b.getDangerousCategory().getDescription() : "—");
                cat.setStyle("-fx-text-fill:#7a8fa8; -fx-font-size:11px;");
                HBox.setHgrow(cat, Priority.ALWAYS);
                row.getChildren().addAll(id, cat);
                setGraphic(row);
                setStyle("-fx-background-color:transparent;");
            }
        });
    }

    private void setupWaitingList() {
        waitingQueueList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Baggage b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) { setGraphic(null); return; }
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label id    = new Label(b.getBaggageId());
                id.setStyle("-fx-text-fill:#ffb830;");
                Label wt    = new Label(String.format("%.1f kg", b.getWeightKg()));
                wt.setStyle("-fx-text-fill:#7a8fa8; -fx-font-size:12px;");
                Label cl    = new Label(b.getOwnerClass().getDisplayName());
                cl.setStyle("-fx-font-size:11px; -fx-text-fill:#00d4ff;");
                HBox.setHgrow(wt, Priority.ALWAYS);
                row.getChildren().addAll(id, wt, cl);
                setGraphic(row);
                setStyle("-fx-background-color:transparent;");
            }
        });
    }

    // ==================== VERİ GÜNCELLEME ====================

    @FXML
    private void onRefresh() {
        refreshData();
        tilesContainer.getChildren().forEach(AnimationUtil::bounceIn);
    }

    private void refreshData() {
        Map<BaggageStatus, Long> summary = ctrl.getBaggageStatusSummary();

        long totalBaggage  = summary.values().stream().mapToLong(Long::longValue).sum();
        long securityCount = summary.getOrDefault(BaggageStatus.SECURITY_HOLD, 0L);
        long waitingC      = summary.getOrDefault(BaggageStatus.WAITING_QUEUE, 0L);
        long cargoCount    = summary.getOrDefault(BaggageStatus.CARGO, 0L)
                + summary.getOrDefault(BaggageStatus.LOADED, 0L);
        long deliveredC    = summary.getOrDefault(BaggageStatus.DELIVERED, 0L);
        List<Flight> flights = ctrl.getUpcomingFlights();

        // Tile değerleri güncelle
        totalBaggageTile.setValue(totalBaggage);
        securityTile.setValue(securityCount);
        waitingTile.setValue(waitingC);
        loadedTile.setValue(cargoCount);
        flightsTile.setValue(flights.size());
        deliveredTile.setValue(deliveredC);

        // Listeler
        flightListView.getItems().setAll(flights);
        flightCountBadge.setText(flights.size() + " Uçuş");

        List<Baggage> secPool = ctrl.getSecurityPool();
        securityPoolList.getItems().setAll(secPool);
        securityPoolCount.setText(String.valueOf(secPool.size()));

        List<Baggage> waiting = ctrl.getWaitingBaggage();
        waitingQueueList.getItems().setAll(waiting);
        waitingCount.setText(String.valueOf(waiting.size()));
    }
}