package com.airport;

import com.airport.controller.AirportController;
import com.airport.db.DatabaseManager;
import com.airport.ui.util.SceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Uygulama giriş noktası.
 *
 * MySQL entegrasyonu sonrası değişiklikler:
 *   - DatabaseManager.init() eklendi → db.properties okunur, bağlantı test edilir.
 *   - SeedData.populate() kaldırıldı → veriler artık MySQL'den geliyor.
 *   - AirportController oluşturulunca servisler DB'den veri yükler.
 */
public class MainApp extends Application {

    /** Tüm UI'ın paylaştığı tek backend instance. */
    public static AirportController CONTROLLER;

    @Override
    public void init() {
        // 1. MySQL bağlantısını başlat (db.properties okunur)
        DatabaseManager.init();

        // 2. Backend'i başlat — servisler constructor'larında DB'den veri yükler
        CONTROLLER = new AirportController();

        // NOT: SeedData.populate() artık çağrılmıyor.
        // Veriler schema.sql ile DB'ye yüklenmiş olmalı.
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager.init(primaryStage);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double w = Math.min(1600, screenBounds.getWidth()  * 0.92);
        double h = Math.min(960,  screenBounds.getHeight() * 0.92);

        primaryStage.setWidth(w);
        primaryStage.setHeight(h);
        primaryStage.setMinWidth(1280);
        primaryStage.setMinHeight(780);
        primaryStage.centerOnScreen();
        primaryStage.setTitle("✈  AirportRouter — Havaalanı Bagaj Yönetim Sistemi");
        primaryStage.initStyle(StageStyle.DECORATED);

        try {
            primaryStage.getIcons().add(
                    new Image(MainApp.class.getResourceAsStream("/com/airport/images/airport.png")));
        } catch (Exception ignored) { }

        SceneManager.showMainWindow();
        primaryStage.show();
    }

    @Override
    public void stop() {
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}