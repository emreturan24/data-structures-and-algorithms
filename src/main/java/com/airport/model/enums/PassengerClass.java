package com.airport.model.enums;

/**
 * Yolcu sınıfı ve işlem önceliği.
 * priority değeri küçüldükçe öncelik artar (PriorityQueue min-heap için).
 * VIP=1 en yüksek öncelik, Economy=3 en düşük.
 */
public enum PassengerClass {

    VIP(1, "VIP"),
    BUSINESS(2, "Business"),
    ECONOMY(3, "Economy");

    private final int priority;
    private final String displayName;

    PassengerClass(int priority, String displayName) {
        this.priority = priority;
        this.displayName = displayName;
    }

    public int getPriority() {
        return priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}