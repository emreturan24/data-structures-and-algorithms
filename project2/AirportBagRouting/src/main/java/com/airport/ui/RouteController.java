package com.airport.ui;

import com.airport.MainApp;
import com.airport.controller.AirportController;
import com.airport.ui.util.AnimationUtil;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.VPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
 * <p>AirportGraph'ı ekrana çizer:
 * <ul>
 *   <li>Her havaalanı → glowing daire + isim etiketi</li>
 *   <li>Her rota → ok çizgisi</li>
 *   <li>BFS rotası → PathTransition ile animasyonlu kırmızı nokta</li>
 *   <li>DFS tüm rotalar → sırayla listeye eklenir</li>
 * </ul>
 */
public class RouteController implements Initializable {

    @FXML private Pane     graphPane;
    @FXML private ComboBox<String> fromCombo, toCombo;
    @FXML private Label    bfsResult, animStatusLabel, transferLabel, reachableLabel;
    @FXML private ListView<String> allRoutesList, neighborList;

    private final AirportController ctrl = MainApp.CONTROLLER;

    // Node pozisyonları: kod → [x, y]
    private final Map<String, double[]> nodePositions = new LinkedHashMap<>();
    // Node circle referansları (glow için)
    private final Map<String, Circle> nodeCircles   = new HashMap<>();
    private final Map<String, Label>  nodeLabels    = new HashMap<>();

    // Animasyon topu
    private Circle animBall;

    // ==================== INITIALIZE ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Combo'ları doldur
        List<String> codes = new ArrayList<>(ctrl.getRoutingService().getAllAirports());
        Collections.sort(codes);
        fromCombo.getItems().setAll(codes);
        toCombo.getItems().setAll(codes);

        // Seçim değişince komşuları güncelle
        fromCombo.setOnAction(e -> updateNeighbors(fromCombo.getValue()));

        // Pane boyutlandıktan sonra çiz
        graphPane.widthProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() > 10) drawGraph();
        });
        graphPane.heightProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() > 10) drawGraph();
        });
    }

    // ==================== GRAF ÇİZİMİ ====================

    private void drawGraph() {
        graphPane.getChildren().clear();
        nodeCircles.clear();
        nodeLabels.clear();
        nodePositions.clear();

        double w = graphPane.getWidth();
        double h = graphPane.getHeight();

        // Türkiye haritasına yakın koordinatlar (nisbi)
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
                // Bilinmeyen havaalanı: çember üzerine yerleştir
                double angle = (2 * Math.PI / allCodes.size()) * idx;
                nodePositions.put(code, new double[]{
                        w / 2 + (w * 0.38) * Math.cos(angle),
                        h / 2 + (h * 0.38) * Math.sin(angle)
                });
            }
            idx++;
        }

        // Kenarları çiz (önce, node'ların altında kalsın)
        drawAllEdges();

        // Node'ları çiz
        for (String code : nodePositions.keySet()) {
            drawNode(code);
        }

        // Animasyon topunu ekle (başlangıçta gizli)
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
                drawEdge(from, to, bidirectional, Color.web("#1e3050"), 1.5, false);
            }
        }
    }

    private void drawEdge(String from, String to, boolean bidirectional,
                          Color color, double width, boolean highlighted) {
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

        // Ok başlığı
        if (!bidirectional) addArrow(p1[0], p1[1], p2[0], p2[1], color);
    }

    private void addArrow(double x1, double y1, double x2, double y2, Color color) {
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
        graphPane.getChildren().add(arrow);
    }

    private void drawNode(String code) {
        double[] pos = nodePositions.get(code);
        if (pos == null) return;

        // Dış glow halkası
        Circle glow = new Circle(pos[0], pos[1], 22,
                Color.web("#00d4ff", 0.08));
        glow.setStroke(Color.web("#00d4ff", 0.25));
        glow.setStrokeWidth(1.5);

        // Ana daire
        Circle node = new Circle(pos[0], pos[1], 16,
                Color.web("#131a26"));
        node.setStroke(Color.web("#00d4ff"));
        node.setStrokeWidth(2);
        node.setEffect(new javafx.scene.effect.DropShadow(12, Color.web("#00d4ff55")));

        // Kod etiketi (içeride)
        Text codeText = new Text(code);
        codeText.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
        codeText.setFill(Color.web("#00d4ff"));
        codeText.setTextAlignment(TextAlignment.CENTER);
        codeText.setTextOrigin(VPos.CENTER);
        codeText.setX(pos[0] - codeText.getLayoutBounds().getWidth() / 2);
        codeText.setY(pos[1]);

        // İsim etiketi (altında)
        String name = ctrl.getRoutingService().getAirportName(code);
        Label nameLabel = new Label(name.length() > 14 ? name.substring(0, 12) + "…" : name);
        nameLabel.setFont(Font.font("Segoe UI", 9));
        nameLabel.setTextFill(Color.web("#7a8fa8"));
        nameLabel.setLayoutX(pos[0] - 40);
        nameLabel.setLayoutY(pos[1] + 20);
        nameLabel.setMinWidth(80);
        nameLabel.setAlignment(javafx.geometry.Pos.CENTER);

        // Hover efekti
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

        // Graf üstünde rotayı vurgula + animasyon oynat
        highlightPath(path);
        animateBallAlongPath(path);
    }

    private void highlightPath(List<String> path) {
        // Önce tüm node'ları normale döndür
        nodeCircles.forEach((code, c) -> {
            c.setStroke(Color.web("#00d4ff"));
            c.setStrokeWidth(2);
            c.setFill(Color.web("#131a26"));
            c.setEffect(new javafx.scene.effect.DropShadow(12, Color.web("#00d4ff55")));
        });

        // Rota node'larını vurgula
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

        // Rota kenarlarını vurgula
        for (int i = 0; i < path.size() - 1; i++) {
            drawEdge(path.get(i), path.get(i + 1), false, Color.web("#00d4ff"), 2.5, true);
        }
    }

    private void animateBallAlongPath(List<String> path) {
        if (path.size() < 2) return;
        animBall.setVisible(true);

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
        });
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
}