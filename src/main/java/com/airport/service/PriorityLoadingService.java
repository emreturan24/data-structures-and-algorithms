package com.airport.service;

import com.airport.model.Baggage;
import com.airport.model.enums.PassengerClass;

import java.util.*;

/**
 * Yolcu Öncelik Hiyerarşisi Servisi.
 *
 * <p>Kullandığı veri yapısı: Her uçuş için ayrı {@link PriorityQueue} (Min-Heap)
 * <ul>
 *   <li>VIP bagajlar her zaman ilk işleme alınır</li>
 *   <li>Öncelik: VIP (1) &gt; Business (2) &gt; Economy (3)</li>
 *   <li>Aynı sınıfta: ağır bagaj önce (Baggage.compareTo)</li>
 * </ul>
 *
 * <p>Her uçuşun kendi PriorityQueue'su vardır → flightQueues Map'inde tutulur.
 */
public class PriorityLoadingService {

    // Her uçuş için bağımsız öncelik kuyruğu
    // Baggage.compareTo → VIP önce, sonra Business, sonra Economy
    private final Map<String, PriorityQueue<Baggage>> flightQueues;

    // ==================== CONSTRUCTOR ====================

    public PriorityLoadingService() {
        this.flightQueues = new HashMap<>();
    }

    // ==================== BAGAJ EKLEME ====================

    /**
     * Bagajı ilgili uçuşun öncelik kuyruğuna ekler.
     */
    public void addBaggage(Baggage baggage) {
        flightQueues
                .computeIfAbsent(baggage.getFlightNumber(), k -> new PriorityQueue<>())
                .offer(baggage);
    }

    /**
     * Birden fazla bagajı aynı anda ekler.
     */
    public void addAll(List<Baggage> baggageList) {
        for (Baggage b : baggageList) {
            addBaggage(b);
        }
    }

    // ==================== İŞLEME ALMA ====================

    /**
     * Sıradaki en yüksek öncelikli bagajı işler ve kuyruktan çıkarır.
     * @return VIP varsa VIP bagaj; yoksa Business; yoksa Economy. Kuyruk boşsa null.
     */
    public Baggage processNext(String flightNumber) {
        PriorityQueue<Baggage> pq = flightQueues.get(flightNumber);
        if (pq == null || pq.isEmpty()) return null;
        return pq.poll();
    }

    /**
     * Sıradaki bagaja bakar, çıkarmaz.
     */
    public Baggage peekNext(String flightNumber) {
        PriorityQueue<Baggage> pq = flightQueues.get(flightNumber);
        if (pq == null || pq.isEmpty()) return null;
        return pq.peek();
    }

    /**
     * Uçuşun tüm bagajlarını öncelik sırasıyla işler.
     * @return Öncelik sırasına göre sıralı bagaj listesi
     */
    public List<Baggage> processAllForFlight(String flightNumber) {
        List<Baggage> processed = new ArrayList<>();
        PriorityQueue<Baggage> pq = flightQueues.get(flightNumber);
        if (pq != null) {
            while (!pq.isEmpty()) {
                processed.add(pq.poll());
            }
        }
        return processed;
    }

    /**
     * Sadece VIP bagajları çeker (diğerleri kuyrukta kalır).
     */
    public List<Baggage> processVipOnly(String flightNumber) {
        return processByClass(flightNumber, PassengerClass.VIP);
    }

    /**
     * Belirli sınıfa ait tüm bagajları kuyruktan çeker.
     */
    public List<Baggage> processAllByClass(String flightNumber, PassengerClass targetClass) {
        return processByClass(flightNumber, targetClass);
    }

    private List<Baggage> processByClass(String flightNumber, PassengerClass targetClass) {
        PriorityQueue<Baggage> pq = flightQueues.get(flightNumber);
        if (pq == null || pq.isEmpty()) return Collections.emptyList();

        List<Baggage> target  = new ArrayList<>();
        List<Baggage> leftover = new ArrayList<>();

        while (!pq.isEmpty()) {
            Baggage b = pq.poll();
            if (b.getOwnerClass() == targetClass) {
                target.add(b);
            } else {
                leftover.add(b);
            }
        }
        // Geride kalanları geri koy
        pq.addAll(leftover);
        return target;
    }

    // ==================== SORGULAR ====================

    public int getQueueSize(String flightNumber) {
        PriorityQueue<Baggage> pq = flightQueues.get(flightNumber);
        return pq == null ? 0 : pq.size();
    }

    /**
     * Kuyruk içeriğini kopyasını döner (UI için, sıra bozulmaz).
     */
    public List<Baggage> peekQueue(String flightNumber) {
        PriorityQueue<Baggage> pq = flightQueues.get(flightNumber);
        if (pq == null) return Collections.emptyList();
        // PriorityQueue kopyalanıp sıraya sokulur
        List<Baggage> copy = new ArrayList<>(pq);
        copy.sort(null);
        return copy;
    }

    /**
     * Sınıf dağılımı: kaç VIP, Business, Economy var?
     */
    public Map<PassengerClass, Long> getClassDistribution(String flightNumber) {
        PriorityQueue<Baggage> pq = flightQueues.get(flightNumber);
        if (pq == null) return Collections.emptyMap();

        Map<PassengerClass, Long> dist = new EnumMap<>(PassengerClass.class);
        for (PassengerClass pc : PassengerClass.values()) dist.put(pc, 0L);
        for (Baggage b : pq) {
            dist.merge(b.getOwnerClass(), 1L, Long::sum);
        }
        return dist;
    }

    public boolean hasVipBaggage(String flightNumber) {
        PriorityQueue<Baggage> pq = flightQueues.get(flightNumber);
        if (pq == null || pq.isEmpty()) return false;
        // PQ'nun peek'i zaten en yüksek önceliği gösterir
        Baggage top = pq.peek();
        return top != null && top.getOwnerClass() == PassengerClass.VIP;
    }
}