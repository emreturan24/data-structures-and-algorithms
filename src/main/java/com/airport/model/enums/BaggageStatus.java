package com.airport.model.enums;

/**
 * Bagajın sistemdeki anlık durumu.
 * BaggageTrackingService (HashMap) bu enum'u O(1) ile sorgular.
 */
public enum BaggageStatus {

    CHECK_IN("Check-in"),
    SECURITY_SCREENING("Güvenlik Taraması"),
    SECURITY_HOLD("Güvenlik Havuzu - Tehlikeli Madde"),
    CARGO("Kargo Alanında"),
    LOADED("Uçağa Yüklendi"),
    IN_TRANSIT("Transfer"),
    DELIVERED("Teslim Edildi"),
    WAITING_QUEUE("Bekleme Kuyruğu");

    private final String displayName;

    BaggageStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}