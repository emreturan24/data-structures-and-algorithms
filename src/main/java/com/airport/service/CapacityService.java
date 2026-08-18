package com.airport.service;

import com.airport.datastructures.WaitingQueue;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.model.enums.BaggageStatus;

import java.util.*;

/**
 * Kapasite Yönetimi Servisi.
 *
 * <p>Kullandığı veri yapıları:
 * <ul>
 *   <li>{@link WaitingQueue} (FIFO) — Uçak dolduğunda taşan düşük öncelikli bagajlar</li>
 *   <li>{@link HashMap}           — Her uçuşun anlık yük durumu (O(1) erişim)</li>
 * </ul>
 *
 * <p>Yükleme mantığı:
 * <ol>
 *   <li>Bagajlar öncelik sırasında (VIP→Economy) yüklenmeye çalışılır</li>
 *   <li>Kapasite dolarsa düşük öncelikli bagajlar WaitingQueue'ya aktarılır</li>
 *   <li>Sonraki uçuşta {@code tryAssignWaitingBaggage()} ile yeniden denenebilir</li>
 * </ol>
 */
public class CapacityService {

    private final Map<String, WaitingQueue> waitingQueues; // flightNumber -> kuyruk
    // Her uçuşun mevcut toplam yükü: flightNumber → kg
    private final Map<String, Double> flightCurrentLoad;

    // ==================== CONSTRUCTOR ====================
    
    public CapacityService() {
        this.waitingQueues = new HashMap<>();
        this.flightCurrentLoad = new HashMap<>();
    }

    private WaitingQueue getQueue(String flightNumber) {
        return waitingQueues.computeIfAbsent(flightNumber, k -> new WaitingQueue());
    }
    // ==================== TEK BAGAJ YÜKLEME ====================

    /**
     * Tek bagajı uçağa yüklemeye çalışır.
     * Kapasite yeterliyse yükler; doluysa WaitingQueue'ya ekler.
     *
     * @return true: uçağa yüklendi | false: bekleme kuyruğuna alındı
     */
    public boolean tryLoad(Baggage baggage, Flight flight) {
        double currentLoad = flightCurrentLoad.getOrDefault(flight.getFlightNumber(), 0.0);

        if (currentLoad + baggage.getWeightKg() <= flight.getMaxCapacityKg()) {
            // Kapasite müsait → yükle
            double newLoad = currentLoad + baggage.getWeightKg();
            flightCurrentLoad.put(flight.getFlightNumber(), newLoad);
            flight.setCurrentLoadKg(newLoad);
            baggage.setStatus(BaggageStatus.CARGO);
            return true;
        } else {
            // Kapasite dolu → bekleme kuyruğuna al
            getQueue(flight.getFlightNumber()).enqueue(baggage);
            baggage.setStatus(BaggageStatus.WAITING_QUEUE);
            return false;
        }
    }

    // ==================== TOPLU YÜKLEME ====================

    /**
     * Bagaj listesini sırayla yüklemeye çalışır.
     * Liste önceden öncelik sırasına göre sıralanmış olmalı.
     *
     * @param baggageList Yüklenecek bagajlar (öncelik sırasında)
     * @param flight      Hedef uçuş
     * @return Yükleme sonuç raporu ({@link LoadingResult})
     */
    public LoadingResult loadBaggageList(List<Baggage> baggageList, Flight flight) {
        List<Baggage> loaded = new ArrayList<>();
        List<Baggage> queued = new ArrayList<>();

        for (Baggage baggage : baggageList) {
            if (tryLoad(baggage, flight)) {
                loaded.add(baggage);
            } else {
                queued.add(baggage);
            }
        }

        return new LoadingResult(
                flight.getFlightNumber(),
                loaded,
                queued,
                flightCurrentLoad.getOrDefault(flight.getFlightNumber(), 0.0),
                flight.getMaxCapacityKg()
        );
    }

    // ==================== BEKLEME KUYRUĞU ====================

    /**
     * Bekleme kuyruğundaki bagajları sonraki uçuşa atamaya çalışır.
     */
    public List<Baggage> tryAssignWaitingBaggage(Flight nextFlight) {
        double available = getRemainingCapacity(nextFlight);
        List<Baggage> assigned = getQueue(nextFlight.getFlightNumber()).drainForFlight(
                nextFlight.getFlightNumber(), available);

        for (Baggage b : assigned) {
            double currentLoad = flightCurrentLoad.getOrDefault(nextFlight.getFlightNumber(), 0.0);
            flightCurrentLoad.put(nextFlight.getFlightNumber(), currentLoad + b.getWeightKg());
            nextFlight.setCurrentLoadKg(nextFlight.getCurrentLoadKg() + b.getWeightKg());
            b.setStatus(BaggageStatus.CARGO);
        }
        return assigned;
    }

    public Baggage dequeueWaiting(String flightNumber) {
        return getQueue(flightNumber).dequeue();
    }

    // ==================== SORGULAR ====================

    public List<Baggage> getAllWaiting(String flightNumber) {
        return getQueue(flightNumber).getAllWaiting();
    }
    public int getWaitingCount(String flightNumber) {
        return getQueue(flightNumber).size();
    }
    public WaitingQueue getWaitingQueue(String flightNumber) {
        return getQueue(flightNumber);
    }

    public double getCurrentLoad(String flightNumber) {
        return flightCurrentLoad.getOrDefault(flightNumber, 0.0);
    }

    public double getRemainingCapacity(Flight flight) {
        return flight.getMaxCapacityKg() - getCurrentLoad(flight.getFlightNumber());
    }

    public boolean isFlightFull(Flight flight) {
        return getRemainingCapacity(flight) < 0.001;
    }

    public double getLoadPercentage(Flight flight) {
        if (flight.getMaxCapacityKg() == 0) return 0.0;
        return getCurrentLoad(flight.getFlightNumber()) / flight.getMaxCapacityKg() * 100.0;
    }

    // ==================== İÇ SINIF: SONUÇ RAPORU ====================

    /**
     * Yükleme işleminin sonuç raporu.
     * Controller bunu UI katmanına iletir.
     */
    public static class LoadingResult {

        private final String flightNumber;
        private final List<Baggage> loadedBaggage;
        private final List<Baggage> queuedBaggage;
        private final double totalLoadKg;
        private final double maxCapacityKg;

        public LoadingResult(String flightNumber, List<Baggage> loadedBaggage,
                             List<Baggage> queuedBaggage,
                             double totalLoadKg, double maxCapacityKg) {
            this.flightNumber  = flightNumber;
            this.loadedBaggage = Collections.unmodifiableList(loadedBaggage);
            this.queuedBaggage = Collections.unmodifiableList(queuedBaggage);
            this.totalLoadKg   = totalLoadKg;
            this.maxCapacityKg = maxCapacityKg;
        }

        public String getFlightNumber()        { return flightNumber; }
        public List<Baggage> getLoadedBaggage(){ return loadedBaggage; }
        public List<Baggage> getQueuedBaggage(){ return queuedBaggage; }
        public double getTotalLoadKg()         { return totalLoadKg; }
        public double getMaxCapacityKg()       { return maxCapacityKg; }
        public int getLoadedCount()            { return loadedBaggage.size(); }
        public int getQueuedCount()            { return queuedBaggage.size(); }

        public double getLoadPercentage() {
            if (maxCapacityKg == 0) return 0.0;
            return totalLoadKg / maxCapacityKg * 100.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "LoadingResult{flight='%s', loaded=%d, queued=%d, load=%.1f/%.1fkg (%.1f%%)}",
                    flightNumber, loadedBaggage.size(), queuedBaggage.size(),
                    totalLoadKg, maxCapacityKg, getLoadPercentage()
            );
        }
    }
}