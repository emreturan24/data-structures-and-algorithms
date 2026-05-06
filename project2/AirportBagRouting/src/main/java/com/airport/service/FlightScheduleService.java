package com.airport.service;

import com.airport.model.Flight;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Uçuş Takvimi Servisi.
 *
 * <p>Kullandığı veri yapısı: {@link PriorityQueue} (Min-Heap)
 * <ul>
 *   <li>Öncelik: En erken kalkış saati ({@link Flight#compareTo} → departureTime)</li>
 *   <li>Kalkış: {@code departNextFlight()} uçağı DEPARTED'a alır</li>
 *   <li>Varış: {@code landFlight()} uçağı ARRIVED'a alır</li>
 * </ul>
 */
public class FlightScheduleService {

    // Min-heap: Flight.compareTo → en erken kalkış en önce
    private final PriorityQueue<Flight> scheduledFlights;
    // O(1) uçuş numarasıyla arama için yardımcı HashMap
    private final Map<String, Flight> flightRegistry;

    // ==================== CONSTRUCTOR ====================

    public FlightScheduleService() {
        this.scheduledFlights = new PriorityQueue<>();
        this.flightRegistry   = new HashMap<>();
    }

    // ==================== UÇUŞ EKLEME / KALDIRMA ====================

    /**
     * Yeni uçuşu takvime ekler.
     */
    public void scheduleFlight(Flight flight) {
        scheduledFlights.offer(flight);
        flightRegistry.put(flight.getFlightNumber(), flight);
    }

    /**
     * Takvimden uçuşu iptal eder.
     */
    public boolean cancelFlight(String flightNumber) {
        Flight f = flightRegistry.get(flightNumber);
        if (f == null) return false;
        f.setStatus(Flight.FlightStatus.CANCELLED);
        scheduledFlights.remove(f); // O(n) — PQ'dan remove
        flightRegistry.remove(flightNumber);
        return true;
    }

    // ==================== KALKIŞ / VARIŞ ====================

    /**
     * Sıradaki (en erken) uçuşa bakar, çıkarmaz.
     */
    public Flight peekNextDeparture() {
        return scheduledFlights.peek();
    }

    /**
     * Sıradaki uçuşu takvimden alır ve DEPARTED durumuna geçirir (Kalkış).
     * @return Kalkan uçuş; kuyruk boşsa null.
     */
    public Flight departNextFlight() {
        Flight f = scheduledFlights.poll();
        if (f != null) {
            f.setStatus(Flight.FlightStatus.DEPARTED);
        }
        return f;
    }

    /**
     * Belirli uçuşu iner — ARRIVED durumuna alır.
     */
    public boolean landFlight(String flightNumber) {
        Flight f = flightRegistry.get(flightNumber);
        if (f == null) return false;
        f.setStatus(Flight.FlightStatus.ARRIVED);
        return true;
    }

    /**
     * Biniş başlatır — BOARDING durumuna alır.
     */
    public boolean startBoarding(String flightNumber) {
        Flight f = flightRegistry.get(flightNumber);
        if (f == null) return false;
        f.setStatus(Flight.FlightStatus.BOARDING);
        return true;
    }

    // ==================== SORGULAR ====================

    /**
     * Uçuş numarasıyla O(1) arama.
     */
    public Optional<Flight> getFlightByNumber(String flightNumber) {
        return Optional.ofNullable(flightRegistry.get(flightNumber));
    }

    /**
     * Takvim sırasına göre tüm planlanmış uçuşlar (Min-Heap sırası).
     */
    public List<Flight> getUpcomingFlights() {
        List<Flight> list = new ArrayList<>(scheduledFlights);
        Collections.sort(list); // departureTime sırası
        return list;
    }

    /**
     * Belirli bir zamandan önce kalkacak uçuşlar.
     */
    public List<Flight> getFlightsBefore(LocalDateTime time) {
        List<Flight> result = new ArrayList<>();
        for (Flight f : scheduledFlights) {
            if (f.getDepartureTime().isBefore(time)) result.add(f);
        }
        result.sort(null);
        return result;
    }

    /**
     * Belirli rota üzerindeki uçuşlar.
     */
    public List<Flight> getFlightsByRoute(String departureCode, String arrivalCode) {
        List<Flight> result = new ArrayList<>();
        for (Flight f : flightRegistry.values()) {
            if (f.getDepartureAirport().equalsIgnoreCase(departureCode)
                    && f.getArrivalAirport().equalsIgnoreCase(arrivalCode)) {
                result.add(f);
            }
        }
        result.sort(null);
        return result;
    }

    public int getScheduledCount()   { return scheduledFlights.size(); }
    public int getTotalFlightCount() { return flightRegistry.size(); }
}