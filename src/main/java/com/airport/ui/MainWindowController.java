package com.airport.ui;

import com.airport.ui.util.AnimationUtil;
import com.airport.ui.util.SceneManager;
import com.airport.ui.Refreshable;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import com.airport.ui.util.TooltipUtil;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Ana pencere controller'ı — sidebar navigasyonu yönetir.
 *
 * TOOLTIP DÜZELTME:
 *   Önceki versiyonda Tooltip.install() + 300ms showDelay kullanılıyordu.
 *   Bu yöntem popup ekrana gelince sahte MOUSE_EXITED tetikleyerek
 *   kapat-aç titremesine neden oluyordu.
 *
 *   Yeni versiyonda tüm nav butonları TooltipUtil.install() ile
 *   Dashboard'dakiyle aynı mekanizmayı kullanır:
 *     1.5 sn hareketsiz → aç | hareket → anında kapat
 *
 * ⚙ Gecikme ayarı: TOOLTIP_HOVER_DELAY_SECONDS sabiti
 */
public class MainWindowController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label     pageTitleLabel;
    @FXML private Label     clockLabel;
    @FXML private Circle    statusDot;
    @FXML private Label     statusLabel;

    @FXML private Button navDashboard;
    @FXML private Button navFlights;
    @FXML private Button navBaggage;
    @FXML private Button navSecurity;
    @FXML private Button navRoute;
    @FXML private Button navStack;


    private List<Button> navButtons;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Tooltip paylaşımlı durum (Dashboard ile aynı mekanizma) ─────────────

    private Timeline activeHoverTimer;
    private Tooltip  activeTooltip;

    /**
     * ⚙ GECIKME AYARI — pointer bu kadar saniye hareketsiz kalırsa tooltip açılır.
     * Dashboard ve diğer panellerle eşit tutulması önerilir.
     */
    private static final double TOOLTIP_HOVER_DELAY_SECONDS = 1.5; // ← saniye

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navButtons = Arrays.asList(
                navDashboard, navFlights, navBaggage,
                navSecurity, navRoute, navStack
        );

        navButtons.forEach(AnimationUtil::addButtonHover);

        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalTime.now().format(timeFmt))
        ));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        setupTooltips();
        loadPanel("Dashboard.fxml", "Dashboard", navDashboard);
    }

    // ==================== TOOLTIP KURULUMU ====================

    /**
     * Sidebar nav butonlarına TooltipUtil.install() ile tooltip bağlar.
     * Tüm butonlar 1.5 sn hareketsizlikte açılan, harekette kapanan
     * tooltip kullanır — Dashboard ile aynı davranış.
     */
    private void setupTooltips() {
        TooltipUtil.install(navDashboard,
                "Dashboard\nGenel sistem özetini ve anlık verileri gösterir.");
        TooltipUtil.install(navFlights,
                "Uçuş Takvimi\nUçuşları planlayın, iptal edin veya durumlarını güncelleyin.");
        TooltipUtil.install(navBaggage,
                "Bagaj Yönetimi\nYeni check-in yapın ve bagaj durumlarını takip edin.");
        TooltipUtil.install(navSecurity,
                "Güvenlik Denetimi\nTehlikeli madde tespitlerini ve güvenlik havuzunu yönetin.");
        TooltipUtil.install(navRoute,
                "Rota Haritası\nHavaalanı ağını ve en kısa uçuş rotalarını (BFS/DFS) görüntüleyin.");
        TooltipUtil.install(navStack,
                "Yükleme Simülasyonu\nUçakların LIFO mantığına göre bagaj yükleme adımlarını izleyin.");
    }

    // ==================== NAVİGASYON ====================

    @FXML private void onNavDashboard() { loadPanel("Dashboard.fxml",     "Dashboard",             navDashboard); }
    @FXML private void onNavFlights()   { loadPanel("FlightPanel.fxml",   "✈  Uçuş Takvimi",      navFlights);   }
    @FXML private void onNavBaggage()   { loadPanel("BaggagePanel.fxml",  "🧳  Bagaj Yönetimi",   navBaggage);   }
    @FXML private void onNavSecurity()  { loadPanel("SecurityPanel.fxml", "🔒  Güvenlik Denetimi", navSecurity);  }
    @FXML private void onNavRoute()     { loadPanel("RoutePanel.fxml",    "🗺  Rota Haritası",     navRoute);     }
    @FXML private void onNavStack()     { loadPanel("StackPanel.fxml",    "📦  Yükleme Simülasyonu", navStack);  }


    // ==================== YARDIMCI ====================

    private void loadPanel(String fxml, String title, Button activeBtn) {
        try {
            navButtons.forEach(b -> b.getStyleClass().remove("active"));
            activeBtn.getStyleClass().add("active");
            pageTitleLabel.setText(title);

            Node panel = SceneManager.load(fxml);
            contentArea.getChildren().setAll(panel);
            AnimationUtil.fadeIn(panel);

            // Panel gösterildi → controller'ı tazele
            Object controller = SceneManager.getController(fxml);
            if (controller instanceof Refreshable) {
                ((Refreshable) controller).onPanelShown();
            }

        } catch (Exception e) {
            System.err.println("[MainWindowController] Panel yüklenemedi: " + fxml);
            e.printStackTrace();
        }
    }
}