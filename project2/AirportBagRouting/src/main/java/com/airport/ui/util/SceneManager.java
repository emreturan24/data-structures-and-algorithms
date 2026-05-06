package com.airport.ui.util;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Merkezi sahne / navigasyon yöneticisi.
 * Tüm ekranlar bu sınıf üzerinden açılır; FXML'ler cache'lenir.
 */
public class SceneManager {

    private static Stage        primaryStage;
    private static Scene        mainScene;
    private static final Map<String, Parent> cache = new HashMap<>();

    private static final String BASE = "/com/airport/fxml/";
    private static final String CSS  = SceneManager.class
            .getResource("/com/airport/css/dark-theme.css").toExternalForm();

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    /** Ana pencereyi (MainWindow) aç. */
    public static void showMainWindow() throws IOException {
        Parent root = load("MainWindow.fxml");
        mainScene   = new Scene(root);
        mainScene.getStylesheets().add(CSS);
        primaryStage.setScene(mainScene);
    }

    /** FXML yükle ve cache'e al. */
    public static Parent load(String fxmlFile) throws IOException {
        if (cache.containsKey(fxmlFile)) return cache.get(fxmlFile);
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource(BASE + fxmlFile));
        Parent root = loader.load();
        cache.put(fxmlFile, root);
        return root;
    }

    /** FXML yükle ve controller'a eriş (cache'lenmez — her seferinde yeni). */
    public static <T> T loadFresh(String fxmlFile, FXMLLoader[] loaderOut) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource(BASE + fxmlFile));
        loader.load();
        if (loaderOut != null) loaderOut[0] = loader;
        return loader.getController();
    }

    /** Bir parent'ın içine başka bir FXML'i fade-in ile yerleştir. */
    public static void switchContent(javafx.scene.layout.Pane container,
                                     String fxmlFile) throws IOException {
        Parent node = load(fxmlFile);
        container.getChildren().setAll(node);

        FadeTransition ft = new FadeTransition(Duration.millis(320), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    public static Stage  getStage()      { return primaryStage; }
    public static Scene  getMainScene()  { return mainScene; }
    public static String getCssPath()    { return CSS; }
}