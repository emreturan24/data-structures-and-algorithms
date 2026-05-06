package com.airport.ui;

import com.airport.ui.util.AnimationUtil;
import com.airport.ui.util.SceneManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Ana pencere controller'ı.
 * Sidebar navigasyonu yönetir; içerik panellerini contentArea'ya enjekte eder.
 */
public class MainWindowController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label     pageTitleLabel;
    @FXML private Label     clockLabel;
    @FXML private Circle    statusDot;
    @FXML private Label     statusLabel;

    // Sidebar butonları
    @FXML private Button navDashboard;
    @FXML private Button navFlights;
    @FXML private Button navBaggage;
    @FXML private Button navSecurity;
    @FXML private Button navRoute;
    @FXML private Button navStack;
    @FXML private Button navCapacity;

    private List<Button> navButtons;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navButtons = Arrays.asList(
                navDashboard, navFlights, navBaggage,
                navSecurity, navRoute, navStack, navCapacity
        );

        // Hover efektleri
        navButtons.forEach(AnimationUtil::addButtonHover);

        // Saat sayacı
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalTime.now().format(timeFmt))
        ));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        // Başlangıç ekranı
        loadPanel("Dashboard.fxml", "Dashboard", navDashboard);
    }

    // ==================== NAVİGASYON ────────────────── ====================

    @FXML private void onNavDashboard() {
        loadPanel("Dashboard.fxml", "Dashboard", navDashboard);
    }
    @FXML private void onNavFlights() {
        loadPanel("FlightPanel.fxml", "✈  Uçuş Takvimi", navFlights);
    }
    @FXML private void onNavBaggage() {
        loadPanel("BaggagePanel.fxml", "🧳  Bagaj Yönetimi", navBaggage);
    }
    @FXML private void onNavSecurity() {
        loadPanel("SecurityPanel.fxml", "🔒  Güvenlik Denetimi", navSecurity);
    }
    @FXML private void onNavRoute() {
        loadPanel("RoutePanel.fxml", "🗺  Rota Haritası", navRoute);
    }
    @FXML private void onNavStack() {
        loadPanel("StackPanel.fxml", "📦  Yükleme Simülasyonu", navStack);
    }
    @FXML private void onNavCapacity() {
        loadPanel("CapacityPanel.fxml", "⚡  Kapasite Yönetimi", navCapacity);
    }

    // ==================== YARDIMCI ====================

    private void loadPanel(String fxml, String title, Button activeBtn) {
        try {
            // Aktif buton stilini güncelle
            navButtons.forEach(b -> {
                b.getStyleClass().remove("active");
            });
            activeBtn.getStyleClass().add("active");

            pageTitleLabel.setText(title);

            // Panel yükle ve fade-in uygula
            Node panel = SceneManager.load(fxml);
            contentArea.getChildren().setAll(panel);
            AnimationUtil.fadeIn(panel);

        } catch (Exception e) {
            System.err.println("[MainWindowController] Panel yüklenemedi: " + fxml);
            e.printStackTrace();
        }
    }
}