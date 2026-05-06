package com.airport;

import com.airport.controller.AirportController;
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
 * AirportController (backend) burada tek bir instance olarak oluşturulup
 * SceneManager aracılığıyla tüm controller'lara enjekte edilir.
 */
public class MainApp extends Application {

    /** Tüm UI'ın paylaştığı tek backend instance. */
    public static AirportController CONTROLLER;

    @Override
    public void init() {
        // Backend'i JavaFX thread'inden önce başlat
        CONTROLLER = new AirportController();
        SeedData.populate(CONTROLLER);         // Demo verileri yükle
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager.init(primaryStage);

        // Ekran boyutuna göre pencere boyutu
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

        // Uygulama ikonu (resources/images/ altında airport.png koyabilirsin)
        try {
            primaryStage.getIcons().add(
                    new Image(MainApp.class.getResourceAsStream("/com/airport/images/airport.png")));
        } catch (Exception ignored) { /* ikon yoksa sessizce geç */ }

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