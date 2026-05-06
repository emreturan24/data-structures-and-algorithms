package com.airport.datastructures;

import com.airport.model.Baggage;

import java.util.*;

/**
 * Bekleme Kuyruğu (FIFO) — Kapasite aşımı durumunda kullanılır.
 *
 * <p>Uçağın maksimum kargo kapasitesi dolduğunda düşük öncelikli bagajlar
 * bu kuyruğa alınır. Sıradaki uçuşta önce gelen, önce yüklenir (FIFO).
 *
 * <p>Dahili yapı: {@link LinkedList} — O(1) enqueue/dequeue.
 */
public class WaitingQueue {

    private final Queue<Baggage> queue;
    private int totalEnqueued; // istatistik: toplam kuyruğa giren sayısı

    // ==================== CONSTRUCTOR ====================

    public WaitingQueue() {
        this.queue         = new LinkedList<>();
        this.totalEnqueued = 0;
    }

    // ==================== KUYRUK OPERASYONLARI ====================

    /**
     * Bagajı kuyruğun sonuna ekler (FIFO).
     */
    public void enqueue(Baggage baggage) {
        queue.offer(baggage);
        totalEnqueued++;
    }

    /**
     * Kuyruğun başından bagaj çıkarır.
     * @throws NoSuchElementException kuyruk boşsa
     */
    public Baggage dequeue() {
        Baggage b = queue.poll();
        if (b == null) {
            throw new NoSuchElementException("Bekleme kuyruğu boş!");
        }
        return b;
    }

    /**
     * Öne bakan; çıkarmaz.
     */
    public Baggage peek() {
        return queue.peek();
    }

    /**
     * Kuyruktan belirli uçuşa ait tüm bagajları çeker (next-flight assignment).
     */
    public List<Baggage> drainForFlight(String flightNumber, double availableCapacityKg) {
        List<Baggage> assigned = new ArrayList<>();
        double loaded = 0.0;

        Iterator<Baggage> it = ((LinkedList<Baggage>) queue).iterator();
        while (it.hasNext()) {
            Baggage b = it.next();
            if (loaded + b.getWeightKg() <= availableCapacityKg) {
                assigned.add(b);
                loaded += b.getWeightKg();
                it.remove();
            }
        }
        return assigned;
    }

    // ==================== SORGU ====================

    public boolean isEmpty()          { return queue.isEmpty(); }
    public int size()                 { return queue.size(); }
    public int getTotalEnqueued()     { return totalEnqueued; }

    /**
     * Kuyruktaki tüm bagajların kopyasını döner (UI gösterimi için).
     */
    public List<Baggage> getAllWaiting() {
        return new ArrayList<>(queue);
    }

    /**
     * Kuyruktaki toplam ağırlık.
     */
    public double getTotalWeightKg() {
        return queue.stream().mapToDouble(Baggage::getWeightKg).sum();
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public String toString() {
        return String.format("WaitingQueue{size=%d, totalWeight=%.1fkg, totalEnqueued=%d}",
                queue.size(), getTotalWeightKg(), totalEnqueued);
    }
}