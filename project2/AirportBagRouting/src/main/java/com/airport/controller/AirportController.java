package com.airport.controller;

import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.model.Passenger;
import com.airport.model.enums.BaggageStatus;
import com.airport.model.enums.DangerousGoodsCategory;
import com.airport.model.enums.PassengerClass;
import com.airport.service.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Havaalanı Bagaj Yönetim Sistemi — Facade Controller.
 *
 * <p>Bu sınıf tüm servisleri kapsülleyip UI katmanına <b>tek bir API yüzeyi</b> sunar.
 * JavaFX (veya başka herhangi bir UI framework) sadece bu controller ile konuşur;
 * servis sınıflarına doğrudan erişmez → tam decoupled MVC mimarisi.
 *
 * <p>Servisler:
 * <ul>
 *   <li>{@link RoutingService}        — Graf + BFS/DFS rota</li>
 *   <li>{@link FlightScheduleService} — Uçuş takvimi (Min-Heap)</li>
 *   <li>{@link SecurityService}       — Güvenlik filtreleme + Güvenlik Havuzu</li>
 *   <li>{@link PriorityLoadingService}— Yolcu öncelik hiyerarşisi (PriorityQueue)</li>
 *   <li>{@link WeightLoadingService}  — Ağırlık sıralama + Stack yükleme</li>
 *   <li>{@link BaggageTrackingService}— HashMap tabanlı O(1) bagaj takibi</li>
 *   <li>{@link CapacityService}       — Kapasite yönetimi + Bekleme Kuyruğu</li>
 * </ul>
 */
public class AirportController {

    private final RoutingService         routingService;
    private final FlightScheduleService  flightScheduleService;
    private final SecurityService        securityService;
    private final PriorityLoadingService priorityLoadingService;
    private final WeightLoadingService   weightLoadingService;
    private final BaggageTrackingService trackingService;
    private final CapacityService        capacityService;

    // ==================== CONSTRUCTOR ====================

    public AirportController() {
        this.routingService         = new RoutingService();
        this.flightScheduleService  = new FlightScheduleService();
        this.securityService        = new SecurityService();
        this.priorityLoadingService = new PriorityLoadingService();
        this.weightLoadingService   = new WeightLoadingService();
        this.trackingService        = new BaggageTrackingService();
        this.capacityService        = new CapacityService();
    }

    // ============================================================
    // 1. ROTALAMA (Graph — BFS/DFS)
    // ============================================================

    public void addAirport(String code, String name) {
        routingService.addAirport(code, name);
    }

    public void addRoute(String from, String to, boolean bidirectional) {
        if (bidirectional) routingService.addBidirectionalRoute(from, to);
        else               routingService.addRoute(from, to);
    }

    /** BFS ile en kısa rota. Örn: ["IST","ESB","ADB"] */
    public List<String> findShortestRoute(String from, String to) {
        return routingService.findShortestRoute(from, to);
    }

    /** DFS ile tüm olası rotalar. */
    public List<List<String>> findAllRoutes(String from, String to) {
        return routingService.findAllRoutes(from, to);
    }

    /** -1 = rota yok, 0 = direkt, 1+ = aktarma sayısı */
    public int getTransferCount(String from, String to) {
        return routingService.getTransferCount(from, to);
    }

    public boolean isReachable(String from, String to) {
        return routingService.isReachable(from, to);
    }

    public String getGraphSummary() {
        return routingService.getGraphSummary();
    }

    // ============================================================
    // 2. UÇUŞ TAKVİMİ (PriorityQueue Min-Heap)
    // ============================================================

    public void scheduleFlight(Flight flight) {
        flightScheduleService.scheduleFlight(flight);
    }

    public Flight peekNextDeparture() {
        return flightScheduleService.peekNextDeparture();
    }

    /** Bir sonraki uçuşu kaldırır ve DEPARTED statüsüne geçirir. */
    public Flight departNextFlight() {
        return flightScheduleService.departNextFlight();
    }

    public boolean landFlight(String flightNumber) {
        return flightScheduleService.landFlight(flightNumber);
    }

    public boolean startBoarding(String flightNumber) {
        return flightScheduleService.startBoarding(flightNumber);
    }

    public boolean cancelFlight(String flightNumber) {
        return flightScheduleService.cancelFlight(flightNumber);
    }

    public List<Flight> getUpcomingFlights() {
        return flightScheduleService.getUpcomingFlights();
    }

    public Optional<Flight> getFlightByNumber(String flightNumber) {
        return flightScheduleService.getFlightByNumber(flightNumber);
    }

    // ============================================================
    // 3. BAGAJ CHECK-IN & TAKİP (HashMap)
    // ============================================================

    /**
     * Hazır Baggage nesnesiyle check-in.
     * @return Atanan baggageId
     */
    public String checkIn(Baggage baggage) {
        trackingService.registerBaggage(baggage);
        return baggage.getBaggageId();
    }

    /**
     * Parametrelerle yeni Baggage oluşturup check-in.
     * @return Atanan baggageId
     */
    public String checkIn(String passengerId, String flightNumber,
                          double weightKg, PassengerClass ownerClass) {
        Baggage baggage = new Baggage(passengerId, flightNumber, weightKg, ownerClass);
        trackingService.registerBaggage(baggage);
        return baggage.getBaggageId();
    }

    /** O(1) durum sorgulama */
    public Optional<BaggageStatus> getBaggageStatus(String baggageId) {
        return trackingService.getStatus(baggageId);
    }

    /** O(1) bagaj getirme */
    public Optional<Baggage> getBaggage(String baggageId) {
        return trackingService.getBaggage(baggageId);
    }

    /** O(1) durum güncelleme */
    public boolean updateBaggageStatus(String baggageId, BaggageStatus status) {
        return trackingService.updateStatus(baggageId, status);
    }

    public List<Baggage> getFlightBaggage(String flightNumber) {
        return trackingService.getBaggageForFlight(flightNumber);
    }

    public List<Baggage> getPassengerBaggage(String passengerId) {
        return trackingService.getBaggageForPassenger(passengerId);
    }

    public Map<BaggageStatus, Long> getBaggageStatusSummary() {
        return trackingService.getStatusSummary();
    }

    // ============================================================
    // 4. GÜVENLİK DENETİMİ (Filtering + ArrayList Güvenlik Havuzu)
    // ============================================================

    /**
     * Uçuşun tüm bagajlarını tarar.
     * Tehlikeli olanlar Güvenlik Havuzu'na alınır.
     * @return Temiz bagajlar (kargo akışına devam edecekler)
     */
    public List<Baggage> runSecurityScreening(String flightNumber) {
        List<Baggage> allBaggage = trackingService.getBaggageForFlight(flightNumber);
        List<Baggage> cleared    = securityService.screenBaggageList(allBaggage);
        // Temiz bagajların tracking durumunu güncelle
        for (Baggage b : cleared) {
            trackingService.updateStatus(b.getBaggageId(), BaggageStatus.SECURITY_SCREENING);
        }
        return cleared;
    }

    /** Güvenlik personeli belirli bagajı tehlikeli olarak işaretler. */
    public void flagAsDangerous(String baggageId, DangerousGoodsCategory category) {
        trackingService.getBaggage(baggageId).ifPresent(b -> {
            securityService.flagBaggage(b, category);
            trackingService.updateStatus(baggageId, BaggageStatus.SECURITY_HOLD);
        });
    }

    /** İnceleme sonucu temiz çıktı → sistemene iade. */
    public boolean clearFromSecurityHold(String baggageId) {
        return securityService.clearBaggage(baggageId);
    }

    public List<Baggage> getSecurityPool() {
        return securityService.getSecurityPool();
    }

    public List<Baggage> getSecurityPoolByCategory(DangerousGoodsCategory category) {
        return securityService.getByCategory(category);
    }

    // ============================================================
    // 5. ÖNCELİK YÜKLEMESİ (PriorityQueue — VIP > Business > Economy)
    // ============================================================

    public void addToPriorityQueue(Baggage baggage) {
        priorityLoadingService.addBaggage(baggage);
    }

    public void addAllToPriorityQueue(List<Baggage> baggageList) {
        priorityLoadingService.addAll(baggageList);
    }

    /** Sıradaki en yüksek öncelikli bagajı işler. */
    public Baggage processNextPriorityBaggage(String flightNumber) {
        return priorityLoadingService.processNext(flightNumber);
    }

    /** Tüm öncelik kuyruğunu sırayla boşaltır. */
    public List<Baggage> processAllPriorityBaggage(String flightNumber) {
        return priorityLoadingService.processAllForFlight(flightNumber);
    }

    public boolean hasVipBaggage(String flightNumber) {
        return priorityLoadingService.hasVipBaggage(flightNumber);
    }

    public Map<PassengerClass, Long> getClassDistribution(String flightNumber) {
        return priorityLoadingService.getClassDistribution(flightNumber);
    }

    // ============================================================
    // 6. AĞIRLIK BAZLI STACK YÜKLEMESİ (Sorting + Stack)
    // ============================================================

    /**
     * Bagajları ağır→hafif sıralar ve Stack'e yükler.
     * Stack'in altında ağır, üstünde hafif bagajlar.
     */
    public void loadFlightStack(String flightNumber, List<Baggage> baggageList) {
        weightLoadingService.loadBaggageForFlight(flightNumber, baggageList);
    }

    /**
     * İnişte LIFO boşaltma: hafif bagajlar önce çıkar.
     */
    public List<Baggage> unloadFlight(String flightNumber) {
        List<Baggage> unloaded = weightLoadingService.unloadFlight(flightNumber);
        for (Baggage b : unloaded) {
            trackingService.updateStatus(b.getBaggageId(), BaggageStatus.DELIVERED);
        }
        return unloaded;
    }

    /** Tek adım boşaltma (animasyon/simülasyon için). */
    public Baggage unloadNextBaggage(String flightNumber) {
        Baggage b = weightLoadingService.unloadNext(flightNumber);
        if (b != null) {
            trackingService.updateStatus(b.getBaggageId(), BaggageStatus.DELIVERED);
        }
        return b;
    }

    public List<Baggage> previewStackOrder(String flightNumber) {
        return weightLoadingService.previewLoadOrder(flightNumber);
    }

    // ============================================================
    // 7. KAPASİTE YÖNETİMİ (Queue — Bekleme Kuyruğu)
    // ============================================================

    /**
     * Bagajları kapasite kontrolüyle yükler.
     * Taşanlar otomatik Bekleme Kuyruğu'na aktarılır.
     */
    public CapacityService.LoadingResult loadWithCapacityCheck(
            List<Baggage> baggageList, Flight flight) {
        return capacityService.loadBaggageList(baggageList, flight);
    }

    /**
     * Bekleme kuyruğundaki bagajları sonraki uçuşa ata.
     */
    public List<Baggage> assignWaitingBaggageToFlight(Flight nextFlight) {
        return capacityService.tryAssignWaitingBaggage(nextFlight);
    }

    public List<Baggage> getWaitingBaggage() {
        return capacityService.getAllWaiting();
    }

    public int getWaitingCount() {
        return capacityService.getWaitingCount();
    }

    public double getFlightLoadPercentage(Flight flight) {
        return capacityService.getLoadPercentage(flight);
    }

    public double getRemainingCapacity(Flight flight) {
        return capacityService.getRemainingCapacity(flight);
    }

    // ============================================================
    // SERVİS ERİŞİMİ (UI katmanı için — opsiyonel)
    // ============================================================

    public RoutingService         getRoutingService()         { return routingService; }
    public FlightScheduleService  getFlightScheduleService()  { return flightScheduleService; }
    public SecurityService        getSecurityService()        { return securityService; }
    public PriorityLoadingService getPriorityLoadingService() { return priorityLoadingService; }
    public WeightLoadingService   getWeightLoadingService()   { return weightLoadingService; }
    public BaggageTrackingService getTrackingService()        { return trackingService; }
    public CapacityService        getCapacityService()        { return capacityService; }
}