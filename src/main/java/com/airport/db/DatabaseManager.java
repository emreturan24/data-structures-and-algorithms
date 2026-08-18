package com.airport.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Veritabanı bağlantı yöneticisi.
 * Tüm DAO sınıfları buradan Connection alır.
 *
 * Kullanım: MainApp.init() içinde bir kez DatabaseManager.init() çağrılır.
 * Sonrasında her DAO try-with-resources ile getConnection() kullanır.
 */
public class DatabaseManager {

    private static String url;
    private static String user;
    private static String password;
    private static boolean initialized = false;

    // ==================== BAŞLATMA ====================

    public static void init() {
        try (InputStream in = DatabaseManager.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (in == null) {
                throw new RuntimeException(
                        "db.properties bulunamadı! src/main/resources/ altında olduğundan emin ol.");
            }

            Properties props = new Properties();
            props.load(in);

            url      = props.getProperty("db.url");
            user     = props.getProperty("db.user");
            password = props.getProperty("db.password");
            initialized = true;

            // Bağlantıyı test et
            try (Connection test = getConnection()) {
                System.out.println("✓ MySQL bağlantısı başarılı → " + url);
            }

        } catch (Exception e) {
            throw new RuntimeException("DatabaseManager başlatılamadı: " + e.getMessage(), e);
        }
    }

    // ==================== BAĞLANTI ====================

    /**
     * Her DAO çağrısı için yeni bir Connection döner.
     * try-with-resources ile kullanılmalı → otomatik kapanır.
     */
    public static Connection getConnection() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("DatabaseManager.init() henüz çağrılmadı!");
        }
        return DriverManager.getConnection(url, user, password);
    }
}