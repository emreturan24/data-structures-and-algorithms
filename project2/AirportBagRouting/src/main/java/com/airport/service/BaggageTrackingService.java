package com.airport.service;

import com.airport.model.Baggage;
import com.airport.model.enums.BaggageStatus;

import java.util.*;

/**
 * Bagaj Takip Servisi.
 *
 * <p>Kullandığı veri yapısı: {@link HashMap}
 * <ul>
 *   <li>{@code baggageMap}         — baggageId → Baggage         (O(1) durum sorgulama)</li>
 *   <li>{@code flightBaggageIndex} — flightNumber → List&lt;baggageId&gt;</li>
 *   <li>{@code passengerBaggageIndex} — passengerId → List&lt;baggageId&gt;</li>
 * </ul>
 *
 * <p>Tüm durum güncellemeleri bu servis üzerinden yapılır.
 * UI bagajın nerede olduğunu anlık olarak bu servisten O(1) ile sorgular.
 */
public class BaggageTrackingService {

    // Ana harita: baggageId → Baggage (O(1) erişim)
    private final HashMap<String, Baggage> baggageMap;

    // Yardımcı indeks: uçuşa göre hızlı filtreleme
    private final HashMap<String, List<String>> flightBaggageIndex;

    // Yardımcı indeks: yolcuya göre hızlı filtreleme
    private final HashMap<String, List<String>> passengerBaggageIndex;

    // ==================== CONSTRUCTOR ====================

    public BaggageTrackingService() {
        this.baggageMap             = new HashMap<>();
        this.flightBaggageIndex     = new HashMap<>();
        this.passengerBaggageIndex  = new HashMap<>();
    }

    // ==================== KAYIT ====================

    /**
     * Yeni bagajı sisteme kaydeder (check-in).
     * Hem ana haritaya hem de yardımcı indekslere eklenir.
     */
    public void registerBaggage(Baggage baggage) {
        baggageMap.put(baggage.getBaggageId(), baggage);

        flightBaggageIndex
                .computeIfAbsent(baggage.getFlightNumber(), k -> new ArrayList<>())
                .add(baggage.getBaggageId());

        passengerBaggageIndex
                .computeIfAbsent(baggage.getPassengerId(), k -> new ArrayList<>())
                .add(baggage.getBaggageId());
    }

    /**
     * Bagajı sistemden kaldırır (gerekirse).
     */
    public boolean removeBaggage(String baggageId) {
        Baggage b = baggageMap.remove(baggageId);
        if (b == null) return false;

        List<String> fList = flightBaggageIndex.get(b.getFlightNumber());
        if (fList != null) fList.remove(baggageId);

        List<String> pList = passengerBaggageIndex.get(b.getPassengerId());
        if (pList != null) pList.remove(baggageId);

        return true;
    }

    // ==================== DURUM SORGULAMA (O(1)) ====================

    /**
     * Bagajın anlık durumunu O(1) ile döner.
     */
    public Optional<BaggageStatus> getStatus(String baggageId) {
        Baggage b = baggageMap.get(baggageId);
        return Optional.ofNullable(b).map(Baggage::getStatus);
    }

    /**
     * Bagaj nesnesinin tamamını O(1) ile döner.
     */
    public Optional<Baggage> getBaggage(String baggageId) {
        return Optional.ofNullable(baggageMap.get(baggageId));
    }

    // ==================== DURUM GÜNCELLEME (O(1)) ====================

    /**
     * Bagajın durumunu O(1) ile günceller.
     * @return true: güncellendi, false: bagaj bulunamadı
     */
    public boolean updateStatus(String baggageId, BaggageStatus newStatus) {
        Baggage b = baggageMap.get(baggageId);
        if (b == null) return false;
        b.setStatus(newStatus);
        return true;
    }

    /**
     * Birden fazla bagajın durumunu toplu günceller.
     */
    public int bulkUpdateStatus(List<String> baggageIds, BaggageStatus newStatus) {
        int updated = 0;
        for (String id : baggageIds) {
            if (updateStatus(id, newStatus)) updated++;
        }
        return updated;
    }

    // ==================== FİLTRELEME SORGULARI ====================

    /**
     * Belirli uçuşa ait tüm bagajlar.
     */
    public List<Baggage> getBaggageForFlight(String flightNumber) {
        List<String> ids = flightBaggageIndex.getOrDefault(flightNumber, Collections.emptyList());
        List<Baggage> result = new ArrayList<>();
        for (String id : ids) {
            Baggage b = baggageMap.get(id);
            if (b != null) result.add(b);
        }
        return result;
    }

    /**
     * Belirli yolcuya ait tüm bagajlar.
     */
    public List<Baggage> getBaggageForPassenger(String passengerId) {
        List<String> ids = passengerBaggageIndex.getOrDefault(passengerId, Collections.emptyList());
        List<Baggage> result = new ArrayList<>();
        for (String id : ids) {
            Baggage b = baggageMap.get(id);
            if (b != null) result.add(b);
        }
        return result;
    }

    /**
     * Belirli statüdeki tüm bagajlar.
     */
    public List<Baggage> getByStatus(BaggageStatus status) {
        List<Baggage> result = new ArrayList<>();
        for (Baggage b : baggageMap.values()) {
            if (b.getStatus() == status) result.add(b);
        }
        return result;
    }

    // ==================== İSTATİSTİK ====================

    public int getTotalCount()                   { return baggageMap.size(); }
    public boolean exists(String baggageId)      { return baggageMap.containsKey(baggageId); }

    /**
     * Durum bazlı özet: kaç bagaj hangi aşamada?
     */
    public Map<BaggageStatus, Long> getStatusSummary() {
        Map<BaggageStatus, Long> summary = new EnumMap<>(BaggageStatus.class);
        for (BaggageStatus s : BaggageStatus.values()) summary.put(s, 0L);
        for (Baggage b : baggageMap.values()) {
            summary.merge(b.getStatus(), 1L, Long::sum);
        }
        return summary;
    }
}