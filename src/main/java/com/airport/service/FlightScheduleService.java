package com.airport.service;

import com.airport.db.FlightDAO;
import com.airport.model.Flight;

import java.util.*;

/**
 * Uçuş Takvimi Servisi.
 *
 * Kullandığı veri yapısı: PriorityQueue (Min-Heap) — değişmedi.
 *
 * MySQL entegrasyonu:
 *   - Constructor: DB'den tüm uçuşları yükler (DB → bellek).
 *   - Yazma işlemleri: önce belleğe, ardından DB'ye (write-through).
 *   - Okuma işlemleri: bellekten → hız korunur.
 */
public class FlightScheduleService {

    private final PriorityQueue<Flight> scheduledFlights;
    private final Map<String, Flight>   flightRegistry;

    // ==================== CONSTRUCTOR ====================

    public FlightScheduleService() {
        this.scheduledFlights = new PriorityQueue<>();
        this.flightRegistry   = new HashMap<>();

        // DB'deki uçuşları belleğe yükle
        loadFromDatabase();
    }

    /**
     * Uygulama başlangıcında DB'deki tüm uçuşları PriorityQueue'ya yükler.
     */
    private void loadFromDatabase() {
        List<Flight> flights = FlightDAO.findAll();
        for (Flight f : flights) {
            scheduledFlights.offer(f);
            flightRegistry.put(f.getFlightNumber(), f);
        }
        System.out.println("✓ FlightScheduleService: " + flights.size() + " uçuş DB'den yüklendi.");
    }

    // ==================== UÇUŞ EKLEME / KALDIRMA ====================

    /**
     * Yeni uçuşu takvime ekler.
     * Belleğe ve DB'ye yazar.
     */
    public boolean scheduleFlight(Flight flight) {
        scheduledFlights.offer(flight);
        flightRegistry.put(flight.getFlightNumber(), flight);

        boolean dbOk = FlightDAO.insert(flight);
        if (!dbOk) {
            // Geri al
            scheduledFlights.remove(flight);
            flightRegistry.remove(flight.getFlightNumber());
            return false;
        }
        return true;
    }

    /**
     * Uçuşu iptal eder.
     * Belleği ve DB'yi günceller.
     */
    public boolean cancelFlight(String flightNumber) {
        Flight f = flightRegistry.get(flightNumber);
        if (f == null) return false;

        Flight.FlightStatus oldStatus = f.getStatus();

        f.setStatus(Flight.FlightStatus.CANCELLED);
        scheduledFlights.remove(f);
        flightRegistry.remove(flightNumber);

        boolean dbOk = FlightDAO.updateStatus(flightNumber, Flight.FlightStatus.CANCELLED);
        if (!dbOk) {
            // Geri al
            f.setStatus(oldStatus);
            scheduledFlights.offer(f);
            flightRegistry.put(flightNumber, f);
            return false;
        }
        return true;
    }

    // ==================== KALKIŞ / VARIŞ ====================

    public Flight peekNextDeparture() {
        return scheduledFlights.peek();
    }

    /**
     * Sıradaki uçuşu alır ve DEPARTED yapar.
     * Belleği ve DB'yi günceller.
     */
    public boolean departNextFlight() {
        Flight f = scheduledFlights.poll();
        if (f == null) return false;

        Flight.FlightStatus oldStatus = f.getStatus();
        f.setStatus(Flight.FlightStatus.DEPARTED);

        boolean dbOk = FlightDAO.updateStatus(f.getFlightNumber(), Flight.FlightStatus.DEPARTED);
        if (!dbOk) {
            f.setStatus(oldStatus);
            scheduledFlights.offer(f);
            return false;
        }
        return true;
    }

    /**
     * Uçuşu ARRIVED yapar.
     */
    public boolean landFlight(String flightNumber) {
        Flight f = flightRegistry.get(flightNumber);
        if (f == null) return false;

        Flight.FlightStatus oldStatus = f.getStatus();
        f.setStatus(Flight.FlightStatus.ARRIVED);

        boolean dbOk = FlightDAO.updateStatus(flightNumber, Flight.FlightStatus.ARRIVED);
        if (!dbOk) {
            f.setStatus(oldStatus);
            return false;
        }
        return true;
    }

    /**
     * Uçuşu BOARDING yapar.
     */
    public boolean startBoarding(String flightNumber) {
        Flight f = flightRegistry.get(flightNumber);
        if (f == null) return false;

        Flight.FlightStatus oldStatus = f.getStatus();
        f.setStatus(Flight.FlightStatus.BOARDING);

        boolean dbOk = FlightDAO.updateStatus(flightNumber, Flight.FlightStatus.BOARDING);
        if (!dbOk) {
            f.setStatus(oldStatus);
            return false;
        }
        return true;
    }

    // ==================== SORGULAR ====================

    public List<Flight> getUpcomingFlights() {
        List<Flight> list = new ArrayList<>(scheduledFlights);
        list.sort(null);  // Flight.compareTo → departureTime'a göre sıralar
        return list;
    }

    public Optional<Flight> getFlightByNumber(String flightNumber) {
        return Optional.ofNullable(flightRegistry.get(flightNumber));
    }

    public int getFlightCount() {
        return flightRegistry.size();
    }
}