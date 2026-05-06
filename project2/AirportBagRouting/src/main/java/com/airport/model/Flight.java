package com.airport.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Uçuş modeli.
 *
 * <p>Comparable implementasyonu FlightScheduleService'deki PriorityQueue (min-heap) için:
 * En erken kalkış en önce gelir (kronolojik sıra).
 *
 * <p>FlightStatus iç enum'u ile durum yönetimi yapılır.
 * Kapasite takibi CapacityService tarafından güncellenir.
 */
public class Flight implements Comparable<Flight> {

    // ==================== İÇ ENUM ====================

    public enum FlightStatus {
        SCHEDULED("Planlandı"),
        BOARDING("Biniş"),
        DEPARTED("Kalktı"),
        ARRIVED("İndi"),
        CANCELLED("İptal");

        private final String displayName;
        FlightStatus(String d) { this.displayName = d; }
        public String getDisplayName() { return displayName; }

        @Override
        public String toString() { return displayName; }
    }

    // ==================== ALANLAR ====================

    private final String flightNumber;       // Uçuş numarası (TK101 gibi)
    private final String departureAirport;   // IATA kodu (IST, ESB, ADB...)
    private final String arrivalAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private double maxCapacityKg;            // Toplam kargo kapasitesi
    private double currentLoadKg;            // Anlık yüklü ağırlık
    private FlightStatus status;
    private String gate;                     // Kapı no (opsiyonel)

    // ==================== CONSTRUCTOR ====================

    public Flight(String flightNumber, String departureAirport, String arrivalAirport,
                  LocalDateTime departureTime, LocalDateTime arrivalTime,
                  double maxCapacityKg) {
        this.flightNumber = flightNumber;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.maxCapacityKg = maxCapacityKg;
        this.currentLoadKg = 0.0;
        this.status = FlightStatus.SCHEDULED;
    }

    // ==================== COMPARABLE (PriorityQueue için) ====================

    /**
     * Min-heap: erken kalkan uçuş küçük sayılır.
     */
    @Override
    public int compareTo(Flight other) {
        return this.departureTime.compareTo(other.departureTime);
    }

    // ==================== YARDIMCI METODLAR ====================

    public double getRemainingCapacityKg() {
        return maxCapacityKg - currentLoadKg;
    }

    public double getLoadPercentage() {
        if (maxCapacityKg == 0) return 0;
        return (currentLoadKg / maxCapacityKg) * 100.0;
    }

    public boolean isFull() {
        return currentLoadKg >= maxCapacityKg;
    }

    public boolean canLoad(double weightKg) {
        return (currentLoadKg + weightKg) <= maxCapacityKg;
    }

    public String getRoute() {
        return departureAirport + " → " + arrivalAirport;
    }

    // ==================== GETTERS ====================

    public String getFlightNumber()       { return flightNumber; }
    public String getDepartureAirport()   { return departureAirport; }
    public String getArrivalAirport()     { return arrivalAirport; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public LocalDateTime getArrivalTime()   { return arrivalTime; }
    public double getMaxCapacityKg()      { return maxCapacityKg; }
    public double getCurrentLoadKg()      { return currentLoadKg; }
    public FlightStatus getStatus()       { return status; }
    public String getGate()               { return gate; }

    // ==================== SETTERS ====================

    public void setDepartureTime(LocalDateTime dt)  { this.departureTime = dt; }
    public void setArrivalTime(LocalDateTime at)    { this.arrivalTime = at; }
    public void setMaxCapacityKg(double cap)        { this.maxCapacityKg = cap; }
    public void setCurrentLoadKg(double load)       { this.currentLoadKg = load; }
    public void setStatus(FlightStatus status)      { this.status = status; }
    public void setGate(String gate)                { this.gate = gate; }

    // ==================== OBJECT METHODS ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flight)) return false;
        Flight f = (Flight) o;
        return Objects.equals(flightNumber, f.flightNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightNumber);
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return String.format(
                "Flight{no='%s', route=%s, dep=%s, cap=%.0f/%.0fkg, status=%s}",
                flightNumber, getRoute(),
                departureTime.format(fmt),
                currentLoadKg, maxCapacityKg, status
        );
    }
}