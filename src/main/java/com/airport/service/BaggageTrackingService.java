package com.airport.service;

import com.airport.db.BaggageDAO;
import com.airport.model.Baggage;
import com.airport.model.enums.BaggageStatus;

import java.util.*;

/**
 * Bagaj Takip Servisi.
 * HashMap tabanlı O(1) erişim; DB ile write-through senkronizasyonu.
 *
 * reloadFromDatabase() EKLENDİ:
 *   "Senaryoyu Sıfırla" butonu DB'yi sıfırladıktan sonra bu metodu çağırır.
 *   Bellekteki HashMap DB ile yeniden senkronize edilir.
 */
public class BaggageTrackingService {

    private final HashMap<String, Baggage>      baggageMap;
    private final HashMap<String, List<String>> flightBaggageIndex;
    private final HashMap<String, List<String>> passengerBaggageIndex;

    // ==================== CONSTRUCTOR ====================

    public BaggageTrackingService() {
        this.baggageMap            = new HashMap<>();
        this.flightBaggageIndex    = new HashMap<>();
        this.passengerBaggageIndex = new HashMap<>();
        loadFromDatabase();
    }

    private void loadFromDatabase() {
        List<Baggage> all = BaggageDAO.findAll();
        for (Baggage b : all) {
            baggageMap.put(b.getBaggageId(), b);
            flightBaggageIndex
                    .computeIfAbsent(b.getFlightNumber(), k -> new ArrayList<>())
                    .add(b.getBaggageId());
            passengerBaggageIndex
                    .computeIfAbsent(b.getPassengerId(), k -> new ArrayList<>())
                    .add(b.getBaggageId());
        }
        System.out.println("✓ BaggageTrackingService: " + all.size() + " bagaj yüklendi.");
    }

    /**
     * DB sıfırlandıktan sonra belleği yeniden yükler.
     * "Senaryoyu Sıfırla" butonu tarafından çağrılır.
     *
     * Mevcut HashMap tamamen temizlenir, DB'den yeniden okunur.
     * SecurityService paylaşımlı nesne referanslarını kaybeder;
     * onReset() içinde SecurityService.reload() ayrıca çağrılmalıdır.
     */
    public void reloadFromDatabase() {
        baggageMap.clear();
        flightBaggageIndex.clear();
        passengerBaggageIndex.clear();
        loadFromDatabase();
        System.out.println("✓ BaggageTrackingService: bellek DB ile senkronize edildi.");
    }

    // ==================== KAYIT ====================

    public boolean registerBaggage(Baggage baggage) {
        baggageMap.put(baggage.getBaggageId(), baggage);
        flightBaggageIndex
                .computeIfAbsent(baggage.getFlightNumber(), k -> new ArrayList<>())
                .add(baggage.getBaggageId());
        passengerBaggageIndex
                .computeIfAbsent(baggage.getPassengerId(), k -> new ArrayList<>())
                .add(baggage.getBaggageId());
        BaggageDAO.insert(baggage);
        return false;
    }

    public boolean removeBaggage(String baggageId) {
        Baggage b = baggageMap.remove(baggageId);
        if (b == null) return false;
        List<String> fl = flightBaggageIndex.get(b.getFlightNumber());
        if (fl != null) fl.remove(baggageId);
        List<String> pl = passengerBaggageIndex.get(b.getPassengerId());
        if (pl != null) pl.remove(baggageId);
        BaggageDAO.delete(baggageId);
        return true;
    }

    // ==================== SORGULAR ====================

    public Optional<BaggageStatus> getStatus(String baggageId) {
        return Optional.ofNullable(baggageMap.get(baggageId)).map(Baggage::getStatus);
    }

    public Optional<Baggage> getBaggage(String baggageId) {
        return Optional.ofNullable(baggageMap.get(baggageId));
    }

    // ==================== GÜNCELLEME ====================

    public boolean updateStatus(String baggageId, BaggageStatus newStatus) {
        Baggage b = baggageMap.get(baggageId);
        if (b == null) return false;
        b.setStatus(newStatus);
        BaggageDAO.updateStatus(baggageId, newStatus);
        return true;
    }

    public int bulkUpdateStatus(List<String> ids, BaggageStatus newStatus) {
        int n = 0;
        for (String id : ids) if (updateStatus(id, newStatus)) n++;
        return n;
    }

    // ==================== FİLTRELEME ====================

    public List<Baggage> getBaggageForFlight(String flightNumber) {
        List<String> ids = flightBaggageIndex.getOrDefault(flightNumber, Collections.emptyList());
        List<Baggage> result = new ArrayList<>();
        for (String id : ids) { Baggage b = baggageMap.get(id); if (b != null) result.add(b); }
        return result;
    }

    public List<Baggage> getBaggageForPassenger(String passengerId) {
        List<String> ids = passengerBaggageIndex.getOrDefault(passengerId, Collections.emptyList());
        List<Baggage> result = new ArrayList<>();
        for (String id : ids) { Baggage b = baggageMap.get(id); if (b != null) result.add(b); }
        return result;
    }

    public List<Baggage> getByStatus(BaggageStatus status) {
        List<Baggage> result = new ArrayList<>();
        for (Baggage b : baggageMap.values()) if (b.getStatus() == status) result.add(b);
        return result;
    }

    // ==================== İSTATİSTİK ====================

    public int getTotalCount()               { return baggageMap.size(); }
    public boolean exists(String baggageId)  { return baggageMap.containsKey(baggageId); }

    public Map<BaggageStatus, Long> getStatusSummary() {
        Map<BaggageStatus, Long> summary = new EnumMap<>(BaggageStatus.class);
        for (BaggageStatus s : BaggageStatus.values()) summary.put(s, 0L);
        for (Baggage b : baggageMap.values()) summary.merge(b.getStatus(), 1L, Long::sum);
        return summary;
    }
}
