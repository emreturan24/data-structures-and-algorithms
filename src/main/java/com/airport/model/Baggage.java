package com.airport.model;

import com.airport.model.enums.BaggageStatus;
import com.airport.model.enums.DangerousGoodsCategory;
import com.airport.model.enums.PassengerClass;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Bagaj modeli — sistemin merkezi varlığı.
 *
 * <p>Comparable implementasyonu PriorityLoadingService'deki PriorityQueue için:
 * <ul>
 *   <li>Önce passengerClass önceliği (VIP=1 < BUSINESS=2 < ECONOMY=3)</li>
 *   <li>Aynı sınıfta: ağır bagaj önce işlenir (descending weight)</li>
 * </ul>
 *
 * <p>Benzersiz baggageId, BaggageTrackingService HashMap'inde O(1) anahtar olarak kullanılır.
 */
public class Baggage implements Comparable<Baggage> {

    private final String baggageId;        // Benzersiz ID — UUID'den üretilir
    private final String passengerId;      // Sahibi yolcu
    private final String flightNumber;     // Bağlı uçuş
    private double weightKg;              // Ağırlık (kg)
    private PassengerClass ownerClass;    // Sahibin sınıfı (öncelik için)
    private boolean hasDangerousGoods;    // Tehlikeli madde bayrağı
    private DangerousGoodsCategory dangerousCategory; // null ise tehlikesiz
    private BaggageStatus status;         // Anlık durum
    private LocalDateTime checkInTime;    // Check-in zamanı
    private String description;           // Opsiyonel açıklama

    // ==================== CONSTRUCTORS ====================

    /**
     * Otomatik ID üreten standart constructor.
     */
    public Baggage(String passengerId, String flightNumber,
                   double weightKg, PassengerClass ownerClass) {
        this.baggageId = generateId();
        this.passengerId = passengerId;
        this.flightNumber = flightNumber;
        this.weightKg = weightKg;
        this.ownerClass = ownerClass;
        this.hasDangerousGoods = false;
        this.dangerousCategory = null;
        this.status = BaggageStatus.CHECK_IN;
        this.checkInTime = LocalDateTime.now();
    }

    /**
     * Test / seed data için manuel ID verilebilen constructor.
     */
    public Baggage(String baggageId, String passengerId, String flightNumber,
                   double weightKg, PassengerClass ownerClass) {
        this.baggageId = baggageId;
        this.passengerId = passengerId;
        this.flightNumber = flightNumber;
        this.weightKg = weightKg;
        this.ownerClass = ownerClass;
        this.hasDangerousGoods = false;
        this.dangerousCategory = null;
        this.status = BaggageStatus.CHECK_IN;
        this.checkInTime = LocalDateTime.now();
    }

    // ==================== ID GENERATION ====================

    private static String generateId() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "BAG-" + uuid;
    }

    // ==================== COMPARABLE (PriorityQueue için) ====================

    /**
     * Min-heap'te düşük değer = yüksek öncelik.
     * 1) passengerClass.priority karşılaştır (VIP=1 önce gelir)
     * 2) Beraberlikte: ağır bagaj önce (negatif fark → ağır küçük sayılır)
     */
    @Override
    public int compareTo(Baggage other) {
        int classCmp = Integer.compare(
                this.ownerClass.getPriority(),
                other.ownerClass.getPriority()
        );
        if (classCmp != 0) return classCmp;
        // Aynı sınıf: ağır önce → descending weight
        return Double.compare(other.weightKg, this.weightKg);
    }

    // ==================== GETTERS ====================

    public String getBaggageId()                  { return baggageId; }
    public String getPassengerId()                { return passengerId; }
    public String getFlightNumber()               { return flightNumber; }
    public double getWeightKg()                   { return weightKg; }
    public PassengerClass getOwnerClass()         { return ownerClass; }
    public boolean isHasDangerousGoods()          { return hasDangerousGoods; }
    public DangerousGoodsCategory getDangerousCategory() { return dangerousCategory; }
    public BaggageStatus getStatus()              { return status; }
    public LocalDateTime getCheckInTime()         { return checkInTime; }
    public String getDescription()                { return description; }

    // ==================== SETTERS ====================

    public void setWeightKg(double weightKg)               { this.weightKg = weightKg; }
    public void setOwnerClass(PassengerClass ownerClass)   { this.ownerClass = ownerClass; }
    public void setHasDangerousGoods(boolean flag)         { this.hasDangerousGoods = flag; }
    public void setDangerousCategory(DangerousGoodsCategory c) { this.dangerousCategory = c; }
    public void setStatus(BaggageStatus status)            { this.status = status; }
    public void setDescription(String description)         { this.description = description; }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Baggage)) return false;
        Baggage b = (Baggage) o;
        return Objects.equals(baggageId, b.baggageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baggageId);
    }

    @Override
    public String toString() {
        return String.format(
                "Baggage{id='%s', passenger='%s', flight='%s', weight=%.1fkg, class=%s, status=%s, dangerous=%b}",
                baggageId, passengerId, flightNumber, weightKg, ownerClass, status, hasDangerousGoods
        );
    }
}