package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.datastructures.AirportGraph;
import com.airport.ui.util.AnimationUtil;
import javafx.animation.*;
import javafx.fxml.FXML;
import com.airport.ui.util.TooltipUtil;
import javafx.fxml.Initializable;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

/**
 * Rota Haritası panel controller'ı.
 *
 * Düzeltmeler:
 *  1. highlightOverlay listesi — highlight kenarları takip edilir,
 *     her yeni BFS'te önce temizlenir → çizgiler birikmez.
 *  2. currentAnimation referansı — yeni arama başlamadan önce
 *     önceki animasyon durdurulur.
 *  3. onFindShortestRoute → updateNeighbors(to) çağrısı eklendi.
 *  4. drawNode → Tooltip.install() ile her havaalanı düğümüne ipucu eklendi.
 *     (Circle, Region değil Shape olduğundan TooltipUtil.install kullanılamaz.)
 *  5. FXML kontrolleri → Dashboard ile aynı TooltipUtil.install() mekanizması.
 */
public class RouteController implements Initializable, Refreshable  {

    // ── FXML alanları — mevcut ────────────────────────────────────────────────
    @FXML private Pane               graphPane;
    @FXML private ComboBox<String>   fromCombo, toCombo;
    @FXML private Label              bfsResult, animStatusLabel, transferLabel, reachableLabel;
    @FXML private ListView<String>   allRoutesList, neighborList;

    // ── FXML alanları — tooltip kurulumu için ────────────────────────────────
    @FXML private RadioButton bfsRadio;
    @FXML private RadioButton dijkstraRadio;
    @FXML private ToggleGroup algorithmGroup;
    @FXML private Button resetBtn;
    @FXML private Label  bfsHeaderLabel;
    @FXML private Label  fromLabel;
    @FXML private Label  toLabel;
    @FXML private Button findRouteBtn;
    @FXML private Label  dfsHeaderLabel;
    @FXML private Button findAllBtn;
    @FXML private Label  transferHeaderLabel;
    @FXML private Label  connectionHeaderLabel;

    private final AirportController ctrl = MainApp.CONTROLLER;

    private final Map<String, double[]> nodePositions = new LinkedHashMap<>();
    private final Map<String, Circle>   nodeCircles   = new HashMap<>();
    private final Map<String, Label>    nodeLabels    = new HashMap<>();

    /** Animasyon topu */
    private Circle animBall;

    /**
     * BFS highlight sırasında graphPane'e eklenen kenar/ok Node'ları.
     * Bir sonraki BFS çağrısında önce bunlar temizlenir → üst üste
     * birikim ve isim kapama sorunu ortadan kalkar.
     */
    private final List<javafx.scene.Node> highlightOverlay = new ArrayList<>();

    /**
     * Çalışmakta olan SequentialTransition referansı.
     * Yeni rota bulunmadan önce durdurulur.
     */
    private SequentialTransition currentAnimation;

    // ── Tooltip takip alanları (Dashboard ile aynı mekanizma) ─────────────────
    private Timeline activeHoverTimer;
    private Tooltip  activeTooltip;

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> codes = new ArrayList<>(ctrl.getRoutingService().getAllAirports());
        Collections.sort(codes);
        fromCombo.getItems().setAll(codes);
        toCombo.getItems().setAll(codes);
        algorithmGroup = new ToggleGroup();
        bfsRadio.setToggleGroup(algorithmGroup);
        dijkstraRadio.setToggleGroup(algorithmGroup);
        bfsRadio.setSelected(true);

        // Kaynak ComboBox değişince komşuları güncelle
        fromCombo.setOnAction(e -> updateNeighbors(fromCombo.getValue()));

        // Pane boyutlanınca grafı çiz
        graphPane.widthProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() > 10) drawGraph();
        });
        graphPane.heightProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() > 10) drawGraph();
        });

        setupTooltips();
    }

    // ==================== TOOLTIP KURULUMU ====================

    /**
     * Tüm FXML kontrollerine Dashboard'daki gibi TooltipUtil.install() ile
     * tooltip bağlar. FXML içinde {@code <tooltip>} bloğu bulunmaz.
     *
     * <p>Not: Graf canvas düğümleri (Circle/Shape) için tooltip kurulumu
     * drawNode() içinde Tooltip.install() ile ayrıca yapılır.
     */
    private void setupTooltips() {
        TooltipUtil.install(resetBtn,
                "Graf üzerindeki rota vurgusunu, animasyonu ve seçili düğümleri temizler.\n" +
                        "Havaalanı düğümleri ve kenarlar kaybolmaz — sadece highlight sıfırlanır.");

        TooltipUtil.install(bfsHeaderLabel,
                "BFS — Breadth-First Search (Genişlik Öncelikli Arama)\n\n" +
                        "Kaynak havaalanından hedefe en az durakla ulaşan rotayı bulur. " +
                        "Mesafe veya süre değil, aktarma sayısı minimuma indirilir.");

        TooltipUtil.install(fromLabel,
                "Rotanın başlayacağı havaalanı.\n" +
                        "Seçim yapıldığında 'Bağlantı' bölümü otomatik güncellenir.");

        TooltipUtil.install(fromCombo,
                "Başlangıç havaalanı kodu.\n" +
                        "Seçim yapılınca sağdaki 'Bağlantı' listesi bu havaalanının " +
                        "komşularıyla güncellenir.");

        TooltipUtil.install(toLabel,
                "Rotanın biteceği havaalanı.\n" +
                        "BFS bu noktaya ulaşmayı hedefler.");

        TooltipUtil.install(toCombo,
                "Bitiş havaalanı kodu.\n" +
                        "Kaynak ile aynı seçilirse işlem başlamaz.");

        TooltipUtil.install(findRouteBtn,
                "BFS algoritması çalışır:\n" +
                        "1. En kısa rota hesaplanır\n" +
                        "2. Rota kenarları mavi ile vurgulanır\n" +
                        "3. Kırmızı top rotayı animasyonlu geçer\n" +
                        "4. Bağlantı listesi hedef havaalanına güncellenir");

        TooltipUtil.install(bfsResult,
                "BFS tarafından bulunan en kısa rotanın havaalanı dizisi.\n" +
                        "Format: KAYNAK → AKTARMA → HEDEF\n\n" +
                        "Rota yoksa 'bulunamadı' mesajı gösterilir.");

        TooltipUtil.install(animStatusLabel,
                "BFS animasyonunun anlık durumunu gösterir.\n" +
                        "Top hedefe ulaştığında 'Hedef Ulaşıldı!' yazar.");

        TooltipUtil.install(dfsHeaderLabel,
                "DFS — Depth-First Search (Derinlik Öncelikli Arama)\n\n" +
                        "Kaynak'tan Hedef'e ulaşan tüm olası rotaları bulur. " +
                        "Kısa veya uzun fark etmeksizin döngüsüz her yol listelenir.");

        TooltipUtil.install(findAllBtn,
                "DFS ile Kaynak → Hedef arasındaki tüm döngüsüz yollar hesaplanır.\n\n" +
                        "Kaynak ve Hedef ComboBox'larından seçim yapılmış olmalıdır.\n" +
                        "Sonuçlar numaralı liste olarak aşağıda gösterilir.");

        TooltipUtil.install(allRoutesList,
                "DFS ile bulunan tüm rotaların listesi.\n\n" +
                        "Her satır bir alternatif yolu temsil eder.\n" +
                        "Format: 1.  A → B → C → D\n\n" +
                        "Liste 'Tüm Rotaları Tara' butonuna basılınca dolar.");

        TooltipUtil.install(transferHeaderLabel,
                "BFS sonucundaki aktarma bilgilerini özetler.\n" +
                        "Aktarma = rota üzerindeki ara durak sayısı (kaynak ve hedef dahil değil).");

        TooltipUtil.install(transferLabel,
                "BFS rotasındaki aktarma (ara durak) sayısı.\n\n" +
                        "• 0 aktarma → Direkt uçuş, tek segment\n" +
                        "• 1 aktarma → 1 ara havaalanı, 2 segment\n" +
                        "• N aktarma → N ara durak, N+1 segment");

        TooltipUtil.install(reachableLabel,
                "Hedef havaalanına ulaşılıp ulaşılamadığını gösterir.\n\n" +
                        "✓ Ulaşılabilir → BFS en az bir rota buldu\n" +
                        "❌ Ulaşılamaz  → İki havaalanı arasında bağlantı yok");

        TooltipUtil.install(connectionHeaderLabel,
                "Seçili havaalanına doğrudan bağlı komşu hatları listeler.\n\n" +
                        "Liste şu üç durumda güncellenir:\n" +
                        "• Kaynak ComboBox'tan seçim yapılınca\n" +
                        "• Haritada bir düğümün üzerine gelinince\n" +
                        "• BFS çalıştırılınca (hedef havaalanı gösterilir)");

        TooltipUtil.install(neighborList,
                "Aktif havaalanının doğrudan uçuş bağlantıları.\n\n" +
                        "Format: KOD  —  Havaalanı Adı\n" +
                        "Örn: IST  —  İstanbul Havalimanı\n\n" +
                        "Bu listedeki her havaalanına tek aktarmasız uçulabilir.");
    }

    // ==================== GRAF ÇİZİMİ ====================

    private void drawGraph() {
        graphPane.getChildren().clear();
        nodeCircles.clear();
        nodeLabels.clear();
        nodePositions.clear();
        highlightOverlay.clear(); // overlay listesini de sıfırla

        double w = graphPane.getWidth();
        double h = graphPane.getHeight();

        Map<String, double[]> relPos = new LinkedHashMap<>();
        relPos.put("IST", new double[]{0.22, 0.28});
        relPos.put("SAW", new double[]{0.26, 0.32});
        relPos.put("ESB", new double[]{0.44, 0.30});
        relPos.put("ADB", new double[]{0.18, 0.52});
        relPos.put("AYT", new double[]{0.36, 0.70});
        relPos.put("TZX", new double[]{0.65, 0.22});
        relPos.put("VAN", new double[]{0.78, 0.38});
        relPos.put("GZP", new double[]{0.44, 0.72});

        Set<String> allCodes = ctrl.getRoutingService().getAllAirports();
        int idx = 0;
        for (String code : allCodes) {
            if (relPos.containsKey(code)) {
                double[] rel = relPos.get(code);
                nodePositions.put(code, new double[]{rel[0] * w, rel[1] * h});
            } else {
                double angle = (2 * Math.PI / allCodes.size()) * idx;
                nodePositions.put(code, new double[]{
                        w / 2 + (w * 0.38) * Math.cos(angle),
                        h / 2 + (h * 0.38) * Math.sin(angle)
                });
            }
            idx++;
        }

        drawAllEdges();

        for (String code : nodePositions.keySet()) {
            drawNode(code);
        }

        // Animasyon topu — başlangıçta gizli
        animBall = new Circle(10, Color.web("#ff3b5c"));
        animBall.setEffect(new javafx.scene.effect.DropShadow(16, Color.web("#ff3b5c")));
        animBall.setVisible(false);
        graphPane.getChildren().add(animBall);
    }

    private void drawAllEdges() {
        Set<String> drawn = new HashSet<>();
        for (String from : nodePositions.keySet()) {
            for (String to : ctrl.getRoutingService().getConnectedAirports(from)) {
                String key = from.compareTo(to) < 0 ? from + to : to + from;
                boolean bidirectional = ctrl.getRoutingService().hasDirectFlight(from, to)
                        && ctrl.getRoutingService().hasDirectFlight(to, from);
                if (bidirectional && drawn.contains(key)) continue;
                drawn.add(key);
                drawEdge(from, to, bidirectional, Color.web("#1e3050"), 1.5, false, false);
            }
        }
    }

    /**
     * @param tracked true ise çizilen node highlightOverlay'e eklenir
     *                → bir sonraki BFS'te temizlenebilir
     */
    private void drawEdge(String from, String to, boolean bidirectional,
                          Color color, double width, boolean highlighted, boolean tracked) {
        double[] p1 = nodePositions.get(from);
        double[] p2 = nodePositions.get(to);
        if (p1 == null || p2 == null) return;

        Line line = new Line(p1[0], p1[1], p2[0], p2[1]);
        line.setStroke(color);
        line.setStrokeWidth(width);
        if (highlighted) {
            line.setEffect(new javafx.scene.effect.DropShadow(8, color));
        }
        graphPane.getChildren().add(line);
        if (tracked) highlightOverlay.add(line);

        if (!bidirectional) {
            Polygon arrow = buildArrow(p1[0], p1[1], p2[0], p2[1], color);
            graphPane.getChildren().add(arrow);
            if (tracked) highlightOverlay.add(arrow);
        }
    }

    private Polygon buildArrow(double x1, double y1, double x2, double y2, Color color) {
        double angle  = Math.atan2(y2 - y1, x2 - x1);
        double dist   = Math.hypot(x2 - x1, y2 - y1);
        double ax     = x1 + (dist - 22) * Math.cos(angle);
        double ay     = y1 + (dist - 22) * Math.sin(angle);
        double spread = 0.45;
        double len    = 10;

        Polygon arrow = new Polygon(
                ax, ay,
                ax - len * Math.cos(angle - spread), ay - len * Math.sin(angle - spread),
                ax - len * Math.cos(angle + spread), ay - len * Math.sin(angle + spread)
        );
        arrow.setFill(color);
        return arrow;
    }

    private void drawNode(String code) {
        double[] pos = nodePositions.get(code);
        if (pos == null) return;

        String fullName   = ctrl.getRoutingService().getAirportName(code);
        Set<String> nbrs  = ctrl.getRoutingService().getConnectedAirports(code);

        // Dış glow halkası
        Circle glow = new Circle(pos[0], pos[1], 22, Color.web("#00d4ff", 0.08));
        glow.setStroke(Color.web("#00d4ff", 0.25));
        glow.setStrokeWidth(1.5);

        // Ana daire
        Circle node = new Circle(pos[0], pos[1], 16, Color.web("#131a26"));
        node.setStroke(Color.web("#00d4ff"));
        node.setStrokeWidth(2);
        node.setEffect(new javafx.scene.effect.DropShadow(12, Color.web("#00d4ff55")));

        // Düğüm tooltip'i — Circle, Region değil Shape olduğundan
        // Tooltip.install() kullanılır (TooltipUtil.install uygulanamaz).
        // Hover alanı büyük olsun diye glow'a da ekliyoruz.
        String tipText = "✈  " + fullName
                + "\nHavaalanı kodu: " + code
                + "\nDoğrudan bağlantı: " + nbrs.size() + " hat"
                + "\nBağlı: " + String.join(", ", nbrs);
        Tooltip nodeTip = new Tooltip(tipText);
        nodeTip.setShowDuration(Duration.seconds(60));
        nodeTip.setShowDelay(Duration.millis(300));
        nodeTip.setHideDelay(Duration.millis(400));
        nodeTip.setWrapText(true);
        nodeTip.setMaxWidth(240);
        Tooltip.install(node, nodeTip);
        Tooltip.install(glow, nodeTip);

        // Kod etiketi (içeride)
        Text codeText = new Text(code);
        codeText.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
        codeText.setFill(Color.web("#00d4ff"));
        codeText.setTextAlignment(TextAlignment.CENTER);
        codeText.setTextOrigin(VPos.CENTER);
        codeText.setX(pos[0] - codeText.getLayoutBounds().getWidth() / 2);
        codeText.setY(pos[1]);

        // İsim etiketi (altında)
        Label nameLabel = new Label(fullName.length() > 14 ? fullName.substring(0, 12) + "…" : fullName);
        nameLabel.setFont(Font.font("Segoe UI", 9));
        nameLabel.setTextFill(Color.web("#7a8fa8"));
        nameLabel.setLayoutX(pos[0] - 40);
        nameLabel.setLayoutY(pos[1] + 20);
        nameLabel.setMinWidth(80);
        nameLabel.setAlignment(javafx.geometry.Pos.CENTER);

        // Hover
        node.setOnMouseEntered(e -> {
            node.setFill(Color.web("#1a3060"));
            node.setStroke(Color.web("#22dfff"));
            nameLabel.setTextFill(Color.web("#e8eef8"));
            updateNeighbors(code);
        });
        node.setOnMouseExited(e -> {
            node.setFill(Color.web("#131a26"));
            node.setStroke(Color.web("#00d4ff"));
            nameLabel.setTextFill(Color.web("#7a8fa8"));
        });

        graphPane.getChildren().addAll(glow, node, codeText, nameLabel);
        nodeCircles.put(code, node);
        nodeLabels.put(code, nameLabel);
    }

    // ==================== BFS ANİMASYON ====================

    @FXML private void onFindShortestRoute() {
        String from = fromCombo.getValue();
        String to   = toCombo.getValue();

        if (from == null || to == null || from.equals(to)) {
            bfsResult.setText("Lütfen iki farklı havaalanı seçin.");
            return;
        }

        if (algorithmGroup.getSelectedToggle() == dijkstraRadio) {
            // Dijkstra
            AirportGraph.PathResult result = ctrl.getRoutingService().findShortestRouteByDistance(from, to);
            if (!result.hasPath()) {
                bfsResult.setText("Mesafeli rota bulunamadı (muhtemelen mesafe bilgisi eksik).");
                return;
            }
            String pathStr = String.join(" → ", result.getPath());
            bfsResult.setText("✓ " + pathStr + "   [" + String.format("%.0f km", result.getTotalDistance()) + "]");
            transferLabel.setText("Toplam mesafe: " + String.format("%.0f km", result.getTotalDistance()));
            reachableLabel.setText("✓  Ulaşılabilir");
            reachableLabel.setStyle("-fx-text-fill:#00e5a0; -fx-font-size:12px;");
            highlightPath(result.getPath());
            animateBallAlongPath(result.getPath());
            updateNeighbors(to);
        } else {
            // BFS
            List<String> path = ctrl.findShortestRoute(from, to);
            int transfers = ctrl.getTransferCount(from, to);

            if (path.isEmpty()) {
                bfsResult.setText("Bu iki nokta arasında rota bulunamadı.");
                transferLabel.setText("Rota yok");
                reachableLabel.setText("❌  Ulaşılamaz");
                reachableLabel.setStyle("-fx-text-fill:#ff3b5c; -fx-font-size:12px;");
                return;
            }

            String pathStr = String.join(" → ", path);
            bfsResult.setText("✓  " + pathStr);
            transferLabel.setText("Aktarma: " + transfers
                    + (transfers == 0 ? " (Direkt Uçuş)" : " durak"));
            reachableLabel.setText("✓  Ulaşılabilir");
            reachableLabel.setStyle("-fx-text-fill:#00e5a0; -fx-font-size:12px;");

            highlightPath(path);
            animateBallAlongPath(path);
            updateNeighbors(to);
        }
    }

    /**
     * Önceki BFS highlight'ını (kenarlar + oklar) graphPane'den tamamen siler,
     * ardından yeni rotayı vurgular.
     */
    private void highlightPath(List<String> path) {
        graphPane.getChildren().removeAll(highlightOverlay);
        highlightOverlay.clear();

        nodeCircles.forEach((code, c) -> {
            c.setStroke(Color.web("#00d4ff"));
            c.setStrokeWidth(2);
            c.setFill(Color.web("#131a26"));
            c.setEffect(new javafx.scene.effect.DropShadow(12, Color.web("#00d4ff55")));
        });

        for (int i = 0; i < path.size(); i++) {
            Circle c = nodeCircles.get(path.get(i));
            if (c == null) continue;
            if (i == 0 || i == path.size() - 1) {
                c.setFill(Color.web("#00d4ff"));
                c.setStroke(Color.web("#22dfff"));
                c.setStrokeWidth(3);
                c.setEffect(new javafx.scene.effect.DropShadow(20, Color.web("#00d4ff")));
            } else {
                c.setFill(Color.web("#1a3060"));
                c.setStroke(Color.web("#9d5cff"));
                c.setStrokeWidth(2.5);
                c.setEffect(new javafx.scene.effect.DropShadow(14, Color.web("#9d5cff")));
            }
        }

        for (int i = 0; i < path.size() - 1; i++) {
            drawEdge(path.get(i), path.get(i + 1), false,
                    Color.web("#00d4ff"), 2.5, true, true);
        }

        graphPane.getChildren().remove(animBall);
        graphPane.getChildren().add(animBall);
    }

    /**
     * Önceki animasyonu durdurur, yeni rotada topu başlatır.
     */
    private void animateBallAlongPath(List<String> path) {
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }

        if (path.size() < 2) return;

        animBall.setVisible(true);
        animBall.setTranslateX(0);
        animBall.setTranslateY(0);

        double[] startPos = nodePositions.get(path.get(0));
        animBall.setCenterX(startPos[0]);
        animBall.setCenterY(startPos[1]);

        SequentialTransition seq = new SequentialTransition();
        animStatusLabel.setText("Animasyon: " + String.join(" → ", path));

        for (int i = 0; i < path.size() - 1; i++) {
            double[] p1 = nodePositions.get(path.get(i));
            double[] p2 = nodePositions.get(path.get(i + 1));
            if (p1 == null || p2 == null) continue;

            final String fromCode = path.get(i);
            final String toCode   = path.get(i + 1);

            TranslateTransition tt = new TranslateTransition(Duration.millis(700), animBall);
            tt.setFromX(p1[0] - startPos[0]);
            tt.setFromY(p1[1] - startPos[1]);
            tt.setToX(p2[0] - startPos[0]);
            tt.setToY(p2[1] - startPos[1]);
            tt.setInterpolator(Interpolator.EASE_BOTH);

            final int fi = i;
            tt.setOnFinished(e -> {
                animStatusLabel.setText("✓  " + fromCode + " → " + toCode
                        + (fi == path.size() - 2 ? "  [Hedef Ulaşıldı!]" : ""));
                Circle dest = nodeCircles.get(toCode);
                if (dest != null) AnimationUtil.bounceIn(dest);
            });

            seq.getChildren().add(tt);
            seq.getChildren().add(new PauseTransition(Duration.millis(120)));
        }

        seq.setOnFinished(e -> {
            PauseTransition pause = new PauseTransition(Duration.millis(1500));
            pause.setOnFinished(ev -> animBall.setVisible(false));
            pause.play();
            currentAnimation = null;
        });

        currentAnimation = seq;
        seq.play();
    }

    // ==================== DFS ====================

    @FXML private void onFindAllRoutes() {
        String from = fromCombo.getValue();
        String to   = toCombo.getValue();
        if (from == null || to == null) {
            allRoutesList.getItems().setAll("Lütfen kaynak ve hedef seçin.");
            return;
        }

        List<List<String>> allPaths = ctrl.findAllRoutes(from, to);
        allRoutesList.getItems().clear();

        if (allPaths.isEmpty()) {
            allRoutesList.getItems().add("Rota bulunamadı.");
            return;
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < allPaths.size(); i++) {
            items.add((i + 1) + ".  " + String.join(" → ", allPaths.get(i)));
        }
        allRoutesList.getItems().setAll(items);
        AnimationUtil.cascadeIn(allRoutesList.getChildrenUnmodifiable().isEmpty()
                ? java.util.Collections.emptyList()
                : java.util.List.of(allRoutesList), 0);
    }

    // ==================== SIFIRLA ====================

    @FXML private void onReset() {
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }
        drawGraph();
        bfsResult.setText("");
        transferLabel.setText("Aktarma sayısı: —");
        reachableLabel.setText("");
        allRoutesList.getItems().clear();
        neighborList.getItems().clear();
        animStatusLabel.setText("");
        fromCombo.setValue(null);
        toCombo.setValue(null);
    }

    // ==================== KOMŞULAR ====================

    private void updateNeighbors(String code) {
        if (code == null) return;
        Set<String> neighbors = ctrl.getRoutingService().getConnectedAirports(code);
        List<String> items = new ArrayList<>();
        for (String n : neighbors) {
            items.add(n + "  —  " + ctrl.getRoutingService().getAirportName(n));
        }
        neighborList.getItems().setAll(items);
    }
    @Override
    public void onPanelShown() {
        // Harita paneli açıldığında komşu listesini güncelleyelim (grafik zaten yeniden çizilmez ama bilgi tazelenir)
        String from = fromCombo.getValue();
        if (from != null) updateNeighbors(from);
    }
}