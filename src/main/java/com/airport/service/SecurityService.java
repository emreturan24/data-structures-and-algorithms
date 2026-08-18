package com.airport.service;

import com.airport.db.BaggageDAO;
import com.airport.model.Baggage;
import com.airport.model.enums.BaggageStatus;
import com.airport.model.enums.DangerousGoodsCategory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Güvenlik Denetimi Servisi.
 *
 * Kullandığı veri yapısı: ArrayList (Güvenlik Havuzu).
 *
 * DÜZELTME — Güvenlik Havuzu Başlangıç Yüklemesi:
 *   Önceki versiyonda SecurityService DB'den SECURITY_HOLD bagajlarını
 *   bağımsız yükleyip yeni Baggage örnekleri oluşturuyordu. Bu,
 *   BaggageTrackingService'teki örneklerden farklı nesnelerdi → güncelleme
 *   senkronizasyonu bozuluyordu.
 *
 *   Güncel versiyonda SecurityService, BaggageTrackingService referansını
 *   constructor'da alır ve SECURITY_HOLD bagajlarını doğrudan oradan çeker.
 *   Bu sayede her iki servis aynı Baggage nesnelerini paylaşır.
 *
 * DÜZELTME v2 — Tehlikeli Bayraklı Bagajların Kaçırılması:
 *   Sadece SECURITY_HOLD statüsü kontrol edildiğinden, DB'de has_dangerous=true
 *   olan ama statüsü henüz SECURITY_HOLD'a güncellenmemiş bagajlar (örn. CHECK_IN
 *   veya CARGO statüsündeyken tehlikeli işaretlenmiş) güvenlik havuzuna eklenemiyordu.
 *   loadFromTrackingService() artık tüm has_dangerous=true bagajları da tarar ve
 *   statüsünü SECURITY_HOLD'a çekerek havuza ekler.
 */
public class SecurityService {

    private final ArrayList<Baggage>      securityPool;
    private final BaggageTrackingService  trackingService;

    private int totalScreened;
    private int totalFlagged;

    // ==================== CONSTRUCTOR ====================

    /**
     * @param trackingService Bagaj nesnelerini paylaşmak için gerekli.
     *                        SecurityService bu servisten bagajları alarak havuzu oluşturur.
     */
    public SecurityService(BaggageTrackingService trackingService) {
        this.trackingService = trackingService;
        this.securityPool    = new ArrayList<>();
        this.totalScreened   = 0;
        this.totalFlagged    = 0;

        // BaggageTrackingService'ten paylaşılan nesneleri al
        loadFromTrackingService();
    }

    /**
     * Uygulama başlangıcında güvenlik havuzunu iki kaynaktan doldurur:
     *
     * <ol>
     *   <li>SECURITY_HOLD statüsündeki tüm bagajlar (önceki oturumlardan DB'de kalanlar).</li>
     *   <li>has_dangerous=true bayrağı taşıyıp statüsü henüz SECURITY_HOLD olmayan bagajlar
     *       (DB'de el ile tehlikeli işaretlenmiş ama statüsü güncellenmemiş kayıtlar).
     *       Bu bagajların statüsü burada SECURITY_HOLD'a çekilir ve DB'ye yazılır.</li>
     * </ol>
     *
     * <p>Neden DB'den değil TrackingService'ten?<br>
     * DB'den yüklemek farklı Baggage örnekleri oluşturur. TrackingService
     * zaten DB'den tüm bagajları yüklemiş; aynı nesneleri kullanırsak
     * bir servisteki değişiklik diğerinde de görünür (referans paylaşımı).
     */
    private void loadFromTrackingService() {

        // ── 1. SECURITY_HOLD statüsündeki bagajları yükle ────────────────────
        List<Baggage> held = trackingService.getByStatus(BaggageStatus.SECURITY_HOLD);
        securityPool.addAll(held);
        System.out.println("✓ SecurityService: " + held.size()
                + " bagaj güvenlik havuzuna yüklendi (SECURITY_HOLD statüsünden).");

        // ── 2. has_dangerous=true ama SECURITY_HOLD olmayan bagajları da ekle ─
        //    Durum: DB'ye ekleme sırasında has_dangerous=true yazılmış ama
        //    statüs sütunu güncellenmeyi atlamış kayıtlar (örn. doğrudan DB'ye
        //    eklenen test verisi veya eski versiyon uyumsuzluğu).
        int recovered = 0;
        for (BaggageStatus st : BaggageStatus.values()) {
            if (st == BaggageStatus.SECURITY_HOLD) continue; // zaten eklendi

            for (Baggage b : trackingService.getByStatus(st)) {
                if (b.isHasDangerousGoods() && !securityPool.contains(b)) {
                    // Statüsü SECURITY_HOLD'a çek — bellek + DB senkron
                    b.setStatus(BaggageStatus.SECURITY_HOLD);
                    BaggageDAO.updateStatus(b.getBaggageId(), BaggageStatus.SECURITY_HOLD);
                    securityPool.add(b);
                    totalFlagged++;
                    recovered++;
                    System.out.println("⚠ SecurityService: " + b.getBaggageId()
                            + " has_dangerous=true bayrağı var → SECURITY_HOLD'a alındı.");
                }
            }
        }

        if (recovered > 0) {
            System.out.println("✓ SecurityService: " + recovered
                    + " tehlikeli bagaj eksik statüsünden kurtarıldı.");
        }

        System.out.println("✓ SecurityService: Toplam havuz büyüklüğü = "
                + securityPool.size() + " bagaj.");
    }

    // ==================== TARAMA ====================

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
                BaggageDAO.updateStatus(baggage.getBaggageId(), BaggageStatus.SECURITY_HOLD);
            } else {
                baggage.setStatus(BaggageStatus.SECURITY_SCREENING);
                cleared.add(baggage);
                BaggageDAO.updateStatus(baggage.getBaggageId(), BaggageStatus.SECURITY_SCREENING);
            }
        }
        return cleared;
    }

    // ==================== MANUEL BAYRAKLAMA ====================

    public void flagBaggage(Baggage baggage, DangerousGoodsCategory category) {
        baggage.setHasDangerousGoods(true);
        baggage.setDangerousCategory(category);
        baggage.setStatus(BaggageStatus.SECURITY_HOLD);
        if (!securityPool.contains(baggage)) {
            securityPool.add(baggage);
            totalFlagged++;
        }
        BaggageDAO.updateDangerous(baggage.getBaggageId(), true, category, BaggageStatus.SECURITY_HOLD);
    }

    // ==================== TEMİZLEME ====================

    public boolean clearBaggage(String baggageId) {
        Iterator<Baggage> it = securityPool.iterator();
        while (it.hasNext()) {
            Baggage b = it.next();
            if (b.getBaggageId().equals(baggageId)) {
                b.setHasDangerousGoods(false);
                b.setDangerousCategory(null);
                b.setStatus(BaggageStatus.CHECK_IN);
                it.remove();
                BaggageDAO.updateDangerous(baggageId, false, null, BaggageStatus.CHECK_IN);
                return true;
            }
        }
        return false;
    }

    public boolean confirmHold(String baggageId) {
        return securityPool.stream()
                .anyMatch(b -> b.getBaggageId().equals(baggageId));
    }

    // ==================== SORGULAR ====================

    public ArrayList<Baggage> getSecurityPool() {
        return new ArrayList<>(securityPool);
    }

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