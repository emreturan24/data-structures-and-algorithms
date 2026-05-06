package com.airport.service;

import com.airport.model.Baggage;
import com.airport.model.enums.BaggageStatus;
import com.airport.model.enums.DangerousGoodsCategory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Güvenlik Denetimi Servisi.
 *
 * <p>Kullandığı veri yapısı:
 * <ul>
 *   <li>{@code ArrayList<Baggage>} — Güvenlik Havuzu (tehlikeli bagajlar burada tutulur)</li>
 *   <li>{@code List} filtreleme — tehlikeli madde içerenleri uçuş listesinden çıkarır</li>
 * </ul>
 *
 * <p>İşlem akışı:
 * <ol>
 *   <li>{@code screenBaggageList()} → listeyi tara, temiz bagajları döndür</li>
 *   <li>Tehlikeli bagajlar → {@code SECURITY_HOLD} statüsü + Güvenlik Havuzu'na eklenir</li>
 *   <li>Güvenlik personeli inceledikten sonra {@code clearBaggage()} veya {@code confirmHold()}</li>
 * </ol>
 */
public class SecurityService {

    /**
     * Güvenlik Havuzu — tehlikeli madde içeren bagajlar.
     * Özellikle ArrayList seçildi: indeks erişimi + dinamik büyüme.
     */
    private final ArrayList<Baggage> securityPool;

    // İstatistik
    private int totalScreened;
    private int totalFlagged;

    // ==================== CONSTRUCTOR ====================

    public SecurityService() {
        this.securityPool  = new ArrayList<>();
        this.totalScreened = 0;
        this.totalFlagged  = 0;
    }

    // ==================== TARAMA ====================

    /**
     * Bir uçuşa ait bagaj listesini güvenlik taramasından geçirir.
     *
     * <p>Tehlikeli bagaj → {@code SECURITY_HOLD} + Güvenlik Havuzu'na taşınır.
     * <p>Temiz bagaj → {@code SECURITY_SCREENING} statüsü alır, dönen listeye eklenir.
     *
     * @param baggageList Taranacak bagaj listesi
     * @return Güvenlik kontrolünden geçen (temiz) bagajlar
     */
    public List<Baggage> screenBaggageList(List<Baggage> baggageList) {
        List<Baggage> cleared = new ArrayList<>();

        for (Baggage baggage : baggageList) {
            totalScreened++;
            if (baggage.isHasDangerousGoods()) {
                baggage.setStatus(BaggageStatus.SECURITY_HOLD);
                if (!securityPool.contains(baggage)) {
                    securityPool.add(baggage);
                    totalFlagged++;
                }
            } else {
                baggage.setStatus(BaggageStatus.SECURITY_SCREENING);
                cleared.add(baggage);
            }
        }
        return cleared;
    }

    // ==================== MANUEL BAYRAKLAMA ====================

    /**
     * Güvenlik personelinin manuel olarak tehlikeli işaretlediği bagaj.
     */
    public void flagBaggage(Baggage baggage, DangerousGoodsCategory category) {
        baggage.setHasDangerousGoods(true);
        baggage.setDangerousCategory(category);
        baggage.setStatus(BaggageStatus.SECURITY_HOLD);
        if (!securityPool.contains(baggage)) {
            securityPool.add(baggage);
            totalFlagged++;
        }
    }

    // ==================== TEMİZLEME / ONAYLAMA ====================

    /**
     * Güvenlik incelemesi sonucu bagaj temiz çıktı → havuzdan çıkar, tekrar sisteme al.
     * @return true: temizlendi, false: havuzda bulunamadı
     */
    public boolean clearBaggage(String baggageId) {
        Iterator<Baggage> it = securityPool.iterator();
        while (it.hasNext()) {
            Baggage b = it.next();
            if (b.getBaggageId().equals(baggageId)) {
                b.setHasDangerousGoods(false);
                b.setDangerousCategory(null);
                b.setStatus(BaggageStatus.CHECK_IN);
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Bagaj gerçekten tehlikeli → havuzda tut, uçuştan kalıcı olarak çıkarıldığını işaretle.
     */
    public boolean confirmHold(String baggageId) {
        return securityPool.stream()
                .anyMatch(b -> b.getBaggageId().equals(baggageId));
    }

    // ==================== SORGULAR ====================

    /**
     * Güvenlik Havuzu'ndaki tüm bagajların kopyası (UI için).
     */
    public ArrayList<Baggage> getSecurityPool() {
        return new ArrayList<>(securityPool);
    }

    /**
     * Belirli tehlike kategorisindeki bagajlar.
     */
    public List<Baggage> getByCategory(DangerousGoodsCategory category) {
        return securityPool.stream()
                .filter(b -> category.equals(b.getDangerousCategory()))
                .collect(Collectors.toList());
    }

    public Optional<Baggage> findInPool(String baggageId) {
        return securityPool.stream()
                .filter(b -> b.getBaggageId().equals(baggageId))
                .findFirst();
    }

    public int getSecurityPoolSize()  { return securityPool.size(); }
    public int getTotalScreened()     { return totalScreened; }
    public int getTotalFlagged()      { return totalFlagged; }

    public double getFlagRate() {
        if (totalScreened == 0) return 0.0;
        return (double) totalFlagged / totalScreened * 100.0;
    }
}