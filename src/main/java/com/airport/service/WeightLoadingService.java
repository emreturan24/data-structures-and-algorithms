package com.airport.service;

import com.airport.datastructures.BaggageStack;
import com.airport.model.Baggage;
import com.airport.model.enums.BaggageStatus;

import java.util.*;

/**
 * Ağırlık Bazlı Yükleme Servisi.
 *
 * <p>Kullandığı veri yapıları:
 * <ul>
 *   <li><b>Sorting</b> — Bagajlar ağır→hafif sıralanır (uçak dengesi için)</li>
 *   <li><b>{@link BaggageStack}</b> — Sıralı liste Stack'e yüklenir:
 *       ağır bagajlar altta, hafif bagajlar üstte</li>
 *   <li><b>LIFO (pop)</b> — İnişte hafif bagajlar önce çıkar</li>
 * </ul>
 *
 * <p>Yükleme akışı:
 * <ol>
 *   <li>Bagaj listesi Collections.sort() ile ağır→hafif sıralanır</li>
 *   <li>Sırayla push edilir (ağır ilk push = altta, hafif son push = üstte)</li>
 *   <li>İnişte unloadAll() → LIFO: hafif önce çıkar</li>
 * </ol>
 */
public class WeightLoadingService {

    // Her uçuş için bir Stack: flightNumber → BaggageStack
    private final Map<String, BaggageStack> flightStacks;

    // ==================== CONSTRUCTOR ====================

    public WeightLoadingService() {
        this.flightStacks = new HashMap<>();
    }

    // ==================== YÜKLEME ====================

    /**
     * Bagaj listesini ağır→hafif sıralar ve Stack'e yükler.
     *
     * <p>Sonuç: Stack'in altında ağır, üstünde hafif bagajlar.
     *
     * @param flightNumber Uçuş numarası
     * @param baggageList  Yüklenecek bagajlar
     * @return Oluşturulan BaggageStack
     */
    public BaggageStack loadBaggageForFlight(String flightNumber, List<Baggage> baggageList) {
        // 1) Ağır → hafif sıralama (descending weight)
        List<Baggage> sorted = new ArrayList<>(baggageList);
        sorted.sort((a, b) -> Double.compare(b.getWeightKg(), a.getWeightKg()));

        // 2) Stack oluştur ve yükle
        BaggageStack stack = new BaggageStack(flightNumber);
        for (Baggage baggage : sorted) {
            baggage.setStatus(BaggageStatus.LOADED);
            stack.push(baggage);
        }

        flightStacks.put(flightNumber, stack);
        return stack;
    }

    /**
     * Mevcut Stack'e tek bir bagaj ekler (geç check-in durumu).
     * Not: Denge bozulabilir; kullanılırken dikkat.
     */
    public boolean addSingleBaggage(String flightNumber, Baggage baggage) {
        BaggageStack stack = flightStacks.get(flightNumber);
        if (stack == null) return false;
        baggage.setStatus(BaggageStatus.LOADED);
        stack.push(baggage);
        return true;
    }

    // ==================== BOŞALTMA (İNİŞ) ====================

    /**
     * İnişte tüm bagajları LIFO sırasıyla boşaltır.
     * Üstteki (hafif) bagajlar önce çıkar.
     *
     * @return Boşaltılan bagajlar (hafiften ağıra sıralı)
     */
    public List<Baggage> unloadFlight(String flightNumber) {
        BaggageStack stack = flightStacks.get(flightNumber);
        if (stack == null) return Collections.emptyList();

        List<Baggage> unloaded = stack.unloadAll();
        for (Baggage b : unloaded) {
            b.setStatus(BaggageStatus.DELIVERED);
        }

        flightStacks.remove(flightNumber);
        return unloaded;
    }

    /**
     * Tek bagaj boşalt (adım adım iniş simülasyonu için).
     */
    public Baggage unloadNext(String flightNumber) {
        BaggageStack stack = flightStacks.get(flightNumber);
        if (stack == null || stack.isEmpty()) return null;
        Baggage b = stack.pop();
        b.setStatus(BaggageStatus.DELIVERED);
        return b;
    }

    // ==================== SORGULAR ====================

    public BaggageStack getStack(String flightNumber) {
        return flightStacks.get(flightNumber);
    }

    /**
     * Stack'in en üstündeki (en hafif) bagaja bak.
     */
    public Baggage peekTopBaggage(String flightNumber) {
        BaggageStack stack = flightStacks.get(flightNumber);
        if (stack == null || stack.isEmpty()) return null;
        return stack.peek();
    }

    public boolean isLoaded(String flightNumber) {
        BaggageStack stack = flightStacks.get(flightNumber);
        return stack != null && !stack.isEmpty();
    }

    public int getLoadedCount(String flightNumber) {
        BaggageStack stack = flightStacks.get(flightNumber);
        return stack == null ? 0 : stack.size();
    }

    public double getLoadedWeightKg(String flightNumber) {
        BaggageStack stack = flightStacks.get(flightNumber);
        return stack == null ? 0.0 : stack.getTotalWeightKg();
    }

    /**
     * Stack içeriğini sıralı liste olarak döner (UI preview için).
     * Üstten alta: hafiften ağıra
     */
    public List<Baggage> previewLoadOrder(String flightNumber) {
        BaggageStack stack = flightStacks.get(flightNumber);
        if (stack == null) return Collections.emptyList();
        return stack.peekAll();
    }
}