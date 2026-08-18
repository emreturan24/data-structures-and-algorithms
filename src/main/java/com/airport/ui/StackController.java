package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.ui.util.TooltipUtil;
import com.airport.ui.util.DialogUtil;
import com.airport.ui.util.AnimationUtil;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;


public class StackController implements Initializable, Refreshable {

    // ── FXML alanları ────────────────────────────────────────────────────────

    @FXML private ComboBox<String>     flightCombo;
    @FXML private VBox                 stackVisualBox;
    @FXML private Label                stackSizeLabel, logLabel;
    @FXML private Label                infoFlight, infoSize, infoWeight, infoTop, infoBottom;

    @FXML private Button loadFlightBtn;
    @FXML private Button emptyAircraftBtn;   // ★ YENİ — "Uçağı Boşalt" butonu
    @FXML private Label  stackStatsLabel;
    @FXML private Label  lifoCtrlLabel;
    @FXML private Button unloadStepBtn;
    @FXML private Button unloadAllBtn;

    // ── Tooltip paylaşımlı durum ─────────────────────────────────────────────

    private Timeline activeHoverTimer;
    private Tooltip  activeTooltip;
    private static final double TOOLTIP_HOVER_DELAY_SECONDS = 1.5;

    // ── İş mantığı alanları ──────────────────────────────────────────────────

    private final AirportController ctrl      = MainApp.CONTROLLER;

    // Görsel stack listesi
    private final List<HBox> visualItems = new ArrayList<>();

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> flightNos = ctrl.getUpcomingFlights().stream()
                .map(Flight::getFlightNumber).collect(Collectors.toList());
        flightCombo.getItems().setAll(flightNos);
        flightCombo.setOnAction(e -> refreshStackInfo(flightCombo.getValue()));

        stackVisualBox.setAlignment(Pos.BOTTOM_CENTER);
        stackVisualBox.setStyle(
                "-fx-background-color:linear-gradient(to bottom,#0a1220,#101a2a);"
                        + "-fx-background-radius:10; -fx-border-color:#1e3050; -fx-border-radius:10;"
                        + "-fx-border-width:1; -fx-padding:14;");

        setupTooltips();
    }

    // ==================== TOOLTIP KURULUMU ====================

    private void setupTooltips() {

        TooltipUtil.install(flightCombo,
                "Yükleme simülasyonu yapılacak uçuşu seçin.\n\n" +
                        "Seçilen uçuşun bagajları Öncelik Kuyruğu (Priority Queue)\n" +
                        "ile sınıflandırılarak Stack'e yüklenir."
        );

        TooltipUtil.install(loadFlightBtn,
                "YÜKLEME — Priority Queue → Stack\n\n" +
                        "Bagajlar PriorityQueue ile sınıflandırılır:\n" +
                        "  VIP (öncelik 1)  → ilk push edilir → stack'in ALTINA\n" +
                        "  Business (2)     → ikinci push\n" +
                        "  Economy (3)      → son push  → stack'in ÜSTÜNE\n\n" +
                        "LIFO ile boşaltırken:\n" +
                        "  Economy önce çıkar, VIP en son çıkar."
        );

        TooltipUtil.install(emptyAircraftBtn,
                "UÇAĞI BOŞALT — Tek Seferde\n\n" +
                        "Stack'teki tüm bagajları LIFO sırasıyla çıkarır.\n" +
                        "Economy → Business → VIP sırası ile iner.\n\n" +
                        "Yer hizmetlerinin tam boşaltma operasyonunu simüle eder."
        );

        TooltipUtil.install(stackSizeLabel,
                "Şu an Stack içindeki toplam bagaj sayısı.\n\n" +
                        "Yükleme veya Pop işlemlerinde anlık olarak güncellenir."
        );

        TooltipUtil.install(stackStatsLabel,
                "STACK İSTATİSTİĞİ\n\n" +
                        "Üstteki = Economy (son push, ilk çıkar — LIFO)\n" +
                        "Alttaki = VIP (ilk push, en son çıkar — LIFO)"
        );

        TooltipUtil.install(infoTop,
                "Stack'in tepesindeki bagaj.\n\n" +
                        "LIFO prensibiyle boşaltmada ilk çıkacak olan.\n" +
                        "(Economy — son yüklendi, ilk iner)"
        );

        TooltipUtil.install(infoBottom,
                "Stack'in tabanındaki bagaj.\n\n" +
                        "İlk push edilen; boşaltmada en son çıkar.\n" +
                        "(VIP — ilk yüklendi, en son iner)"
        );

        TooltipUtil.install(lifoCtrlLabel,
                "BOŞALTMA — LIFO (Last In, First Out)\n\n" +
                        "En son yüklenen (Economy) ilk indirilir.\n" +
                        "Gerçek uçak boşaltma mantığını simüle eder."
        );

        TooltipUtil.install(unloadStepBtn,
                "POP — Tek Bagaj İndir\n\n" +
                        "Stack'in tepesinden tek bir bagaj indirir.\n" +
                        "Adım adım izlemek için kullanın."
        );

        TooltipUtil.install(unloadAllBtn,
                "TAM BOŞALTMA — İniş Simülasyonu\n\n" +
                        "Tüm bagajları sırayla Pop ederek Stack'i tamamen boşaltır."
        );
    }

    // ==================== YÜKLEME (PRİORİTY QUEUE → STACK) ====================

    /**
     * Bagajları PriorityQueue ile sınıf önceliğine göre sıralar,
     * ardından bu sırayla Stack'e push eder:
     *
     *   Yükleme sırası (push):  VIP → Business → Economy
     *   Stack durumu:           [alt: VIP] [Business] [üst: Economy]
     *   LIFO ile çıkış:         Economy → Business → VIP
     *
     * PriorityQueue burada yükleme PLANLAMA aracıdır;
     * Stack ise uçaktaki fiziksel yerleşimi (LIFO) temsil eder.
     */
    @FXML private void onLoadFlight() {
        String fn = flightCombo.getValue();
        if (fn == null) { DialogUtil.showInfo("Lütfen bir uçuş seçin."); return; }

        List<Baggage> allBags = ctrl.getFlightBaggage(fn).stream()
                .filter(b -> !b.isHasDangerousGoods())
                .collect(Collectors.toList());

        if (allBags.isEmpty()) { DialogUtil.showInfo("Bu uçuşa ait yüklenecek bagaj yok."); return; }

        // Stack ve görseli temizle
        stackVisualBox.getChildren().clear();
        visualItems.clear();

        // ── 1. PRİORİTY QUEUE: VIP(1) → Business(2) → Economy(3) sırası ─────
        // Baggage.compareTo() min-heap ile çalışır: priority 1 = VIP en önce
        PriorityQueue<Baggage> pq = new PriorityQueue<>(allBags);

        List<Baggage> loadOrder = new ArrayList<>();
        while (!pq.isEmpty()) {
            loadOrder.add(pq.poll()); // VIP önce, Economy son
        }

        // ── 2. STACK'E PUSH: bu sırayla push et ──────────────────────────────
        // VIP ilk push → stack'in altına gider
        // Economy son push → stack'in üstüne gelir
        // LIFO pop: Economy önce çıkar (üstten), VIP en son çıkar (alttan)
        ctrl.loadFlightStack(fn, loadOrder);

        // ── 3. GÖRSEL ANİMASYON ───────────────────────────────────────────────
        // addVisualCard() her kartı görsel olarak VBox'ın en üstüne ekler (add(0,...)).
        // VIP (ilk eklenen) zamanla alta iner; Economy (son eklenen) en üstte kalır.
        // Bu, stack'in gerçek durumunu yansıtır: Economy üstte, VIP altta.
        for (int i = 0; i < loadOrder.size(); i++) {
            final Baggage b   = loadOrder.get(i);
            final int     idx = i;
            javafx.animation.PauseTransition delay =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(idx * 180L));
            delay.setOnFinished(e -> addVisualCard(b));
            delay.play();
        }

        long vipCount = loadOrder.stream().filter(b -> b.getOwnerClass().getPriority() == 1).count();
        long bizCount = loadOrder.stream().filter(b -> b.getOwnerClass().getPriority() == 2).count();
        long ecoCount = loadOrder.stream().filter(b -> b.getOwnerClass().getPriority() == 3).count();
        logLabel.setText(String.format(
                "✓  %s — %d bagaj yüklendi  [VIP:%d · Biz:%d · Eco:%d]  " +
                        "  PQ sırası: VIP→Biz→Eco  |  Stack: VIP alta, Eco üste  |  LIFO çıkış: Eco önce",
                fn, loadOrder.size(), vipCount, bizCount, ecoCount));

        refreshStackInfo(fn);
    }

    @FXML private void onUnloadStep() {
        String fn = flightCombo.getValue();
        if (fn == null) { DialogUtil.showInfo("Lütfen bir uçuş seçin."); return; }

        Baggage b = ctrl.unloadNextBaggage(fn);
        if (b == null) {
            DialogUtil.showInfo("Stack boş! Tüm bagajlar indirildi.");
            logLabel.setText("Stack boş.");
            return;
        }

        // Üstteki (son push edilen = Economy) görsel kartı kaldır
        if (!visualItems.isEmpty()) {
            HBox topCard = visualItems.get(visualItems.size() - 1);
            AnimationUtil.slideOutDown(topCard, () -> {
                stackVisualBox.getChildren().remove(topCard);
                visualItems.remove(topCard);
            });
        }

        logLabel.setText(String.format("Pop: %s  (%.1f kg  ·  %s)",
                b.getBaggageId(), b.getWeightKg(), b.getOwnerClass().getDisplayName()));
        refreshStackInfo(fn);
    }

    // ==================== TÜMÜNÜ İNDİR ====================

    @FXML private void onUnloadAll() {
        String fn = flightCombo.getValue();
        if (fn == null) { DialogUtil.showInfo("Lütfen bir uçuş seçin."); return; }

        List<Baggage> unloaded = ctrl.unloadFlight(fn);
        if (unloaded.isEmpty()) { DialogUtil.showInfo("Stack zaten boş."); return; }

        removeAllVisualCards();

        logLabel.setText("✓  " + unloaded.size() + " bagaj indirildi. (LIFO: Economy→Business→VIP)");
        resetInfoLabels();
    }

    // ==================== ★ UÇAĞI BOŞALT ====================

    /**
     * Uçaktaki tüm bagajları stack'ten LIFO sırasıyla tek seferde çıkarır.
     * LIFO ile Economy üstten önce iner, VIP en son iner.
     */
    @FXML private void onEmptyAircraft() {
        String fn = flightCombo.getValue();
        if (fn == null) { DialogUtil.showInfo("Lütfen bir uçuş seçin."); return; }

        List<Baggage> unloaded = ctrl.unloadFlight(fn);
        if (unloaded.isEmpty()) { DialogUtil.showInfo("Uçak zaten boş — stack'te bagaj yok."); return; }

        removeAllVisualCards();

        logLabel.setText("✈  Uçak boşaltıldı — " + unloaded.size()
                + " bagaj indi. (LIFO: Economy→Business→VIP)");
        resetInfoLabels();
    }

    // ==================== GÖRSEL KART ====================

    /**
     * Stack'e yeni kart ekler.
     * stackVisualBox.getChildren().add(0, card) → görsel olarak en üste eklenir.
     * VIP (ilk çağrılan) zamanla alta iner; Economy (son çağrılan) en üstte kalır.
     */
    private void addVisualCard(Baggage b) {
        HBox card = buildCard(b);
        stackVisualBox.getChildren().add(0, card); // üste ekle — sonraki kartlar daha üste gelir
        visualItems.add(card);
        AnimationUtil.slideInUp(card);
        stackSizeLabel.setText("Stack: " + visualItems.size() + " öğe");
    }

    private HBox buildCard(Baggage b) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new javafx.geometry.Insets(10, 16, 10, 16));
        card.setMaxWidth(Double.MAX_VALUE);

        String bandColor = switch (b.getOwnerClass()) {
            case VIP      -> "#9d5cff";
            case BUSINESS -> "#00d4ff";
            case ECONOMY  -> "#1e3050";
        };
        Rectangle band = new Rectangle(4, 36);
        band.setFill(Color.web(bandColor));
        band.setArcWidth(3); band.setArcHeight(3);

        double maxWt  = 50.0;
        double ratio  = Math.min(b.getWeightKg() / maxWt, 1.0);
        int    shade  = (int)(ratio * 200);
        String cardBg = String.format("rgba(%d,%d,%d,0.08)", shade / 2, shade / 3, shade);

        card.setStyle("-fx-background-color:" + cardBg + ";"
                + "-fx-background-radius:8; -fx-border-color:" + bandColor + "40;"
                + "-fx-border-radius:8; -fx-border-width:1;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),6,0,0,2);");

        VBox info    = new VBox(3);
        Label id     = new Label(b.getBaggageId());
        id.setStyle("-fx-font-weight:bold; -fx-font-size:12px; -fx-text-fill:#e8eef8;");
        Label detail = new Label(String.format("%.1f kg  ·  %s",
                b.getWeightKg(), b.getOwnerClass().getDisplayName()));
        detail.setStyle("-fx-font-size:10px; -fx-text-fill:#7a8fa8;");
        info.getChildren().addAll(id, detail);
        HBox.setHgrow(info, Priority.ALWAYS);

        javafx.scene.control.ProgressBar pb =
                new javafx.scene.control.ProgressBar(ratio);
        pb.setPrefWidth(60); pb.setPrefHeight(5);
        pb.setStyle("-fx-background-color:#0a1220; -fx-background-radius:3;");

        card.getChildren().addAll(band, info, pb);
        AnimationUtil.addGlowHover(card, Color.web(bandColor + "44"));
        return card;
    }

    // ==================== YARDIMCI METODlar ====================

    /** Tüm görsel kartları kademeli animasyonla kaldırır. */
    private void removeAllVisualCards() {
        for (int i = visualItems.size() - 1; i >= 0; i--) {
            final HBox card    = visualItems.get(i);
            final long delayMs = (long)(visualItems.size() - 1 - i) * 120;
            javafx.animation.PauseTransition d =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(delayMs));
            d.setOnFinished(e -> AnimationUtil.slideOutDown(card,
                    () -> stackVisualBox.getChildren().remove(card)));
            d.play();
        }
        visualItems.clear();
    }

    private void resetInfoLabels() {
        stackSizeLabel.setText("Stack: 0 öğe");
        infoSize.setText("Öğe sayısı: 0");
        infoWeight.setText("Toplam ağırlık: 0 kg");
        infoTop.setText("Üstteki: —");
        infoBottom.setText("Alttaki: —");
    }

    private void refreshStackInfo(String fn) {
        List<Baggage> preview = ctrl.previewStackOrder(fn);
        int sz = preview.size();
        stackSizeLabel.setText("Stack: " + sz + " öğe");
        infoFlight.setText("Uçuş: " + fn);
        infoSize.setText("Öğe sayısı: " + sz);
        double totalWt = preview.stream().mapToDouble(Baggage::getWeightKg).sum();
        infoWeight.setText(String.format("Toplam ağırlık: %.1f kg", totalWt));
        if (!preview.isEmpty()) {
            // peekAll() üstten alta döner → get(0) = üstteki = son push edilen (Economy)
            Baggage top = preview.get(0);
            Baggage bot = preview.get(sz - 1);
            infoTop.setText(String.format("Üstteki (ilk çıkar): %s  %.1f kg  [%s]",
                    top.getBaggageId(), top.getWeightKg(), top.getOwnerClass().getDisplayName()));
            infoBottom.setText(String.format("Alttaki (son çıkar): %s  %.1f kg  [%s]",
                    bot.getBaggageId(), bot.getWeightKg(), bot.getOwnerClass().getDisplayName()));
        } else {
            infoTop.setText("Üstteki: —");
            infoBottom.setText("Alttaki: —");
        }
    }

    @Override
    public void onPanelShown() {
        String fn = flightCombo.getValue();
        if (fn != null) refreshStackInfo(fn);
    }
}