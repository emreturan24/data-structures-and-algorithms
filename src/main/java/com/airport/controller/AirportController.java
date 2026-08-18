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
 * DÜZELTME: SecurityService artık BaggageTrackingService referansı
 * alıyor (constructor injection). Bu sayede her iki servis aynı
 * Baggage nesnelerini paylaşır → güvenlik havuzu başlangıçta doğru
 * dolar, senkronizasyon sorunu ortadan kalkar.
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
        this.trackingService        = new BaggageTrackingService();

        // SecurityService'e trackingService geçiriliyor — aynı Baggage örneklerini paylaşmak için
        this.securityService        = new SecurityService(trackingService);

        this.priorityLoadingService = new PriorityLoadingService();
        this.weightLoadingService   = new WeightLoadingService();
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

    public List<String> findShortestRoute(String from, String to) {
        return routingService.findShortestRoute(from, to);
    }

    public List<List<String>> findAllRoutes(String from, String to) {
        return routingService.findAllRoutes(from, to);
    }

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

    public boolean departNextFlight() {
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

    public Optional<String> checkIn(Baggage baggage) {
        boolean ok = trackingService.registerBaggage(baggage);
        if (ok) {
            return Optional.of(baggage.getBaggageId());
        } else {
            return Optional.empty();
        }
    }

    public Optional<String> checkIn(String passengerId, String flightNumber,
                                    double weightKg, PassengerClass ownerClass) {
        Baggage baggage = new Baggage(passengerId, flightNumber, weightKg, ownerClass);
        return checkIn(baggage);  // yukarıdaki metodu kullan
    }

    public Optional<BaggageStatus> getBaggageStatus(String baggageId) {
        return trackingService.getStatus(baggageId);
    }

    public Optional<Baggage> getBaggage(String baggageId) {
        return trackingService.getBaggage(baggageId);
    }

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
    // 4. GÜVENLİK DENETİMİ
    // ============================================================

    public List<Baggage> runSecurityScreening(String flightNumber) {
        List<Baggage> allBaggage = trackingService.getBaggageForFlight(flightNumber);
        List<Baggage> cleared    = securityService.screenBaggageList(allBaggage);
        for (Baggage b : cleared) {
            trackingService.updateStatus(b.getBaggageId(), BaggageStatus.SECURITY_SCREENING);
        }
        return cleared;
    }

    public void flagAsDangerous(String baggageId, DangerousGoodsCategory category) {
        trackingService.getBaggage(baggageId).ifPresent(b -> {
            securityService.flagBaggage(b, category);
            trackingService.updateStatus(baggageId, BaggageStatus.SECURITY_HOLD);
        });
    }

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
    // 5. ÖNCELİK YÜKLEMESİ
    // ============================================================

    public void addToPriorityQueue(Baggage baggage) {
        priorityLoadingService.addBaggage(baggage);
    }

    public void addAllToPriorityQueue(List<Baggage> baggageList) {
        priorityLoadingService.addAll(baggageList);
    }

    public Baggage processNextPriorityBaggage(String flightNumber) {
        return priorityLoadingService.processNext(flightNumber);
    }

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
    // 6. AĞIRLIK BAZLI STACK YÜKLEMESİ
    // ============================================================

    public void loadFlightStack(String flightNumber, List<Baggage> baggageList) {
        weightLoadingService.loadBaggageForFlight(flightNumber, baggageList);
    }

    public List<Baggage> unloadFlight(String flightNumber) {
        List<Baggage> unloaded = weightLoadingService.unloadFlight(flightNumber);
        for (Baggage b : unloaded) {
            trackingService.updateStatus(b.getBaggageId(), BaggageStatus.DELIVERED);
        }
        return unloaded;
    }

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
    // 7. KAPASİTE YÖNETİMİ
    // ============================================================

    public CapacityService.LoadingResult loadWithCapacityCheck(
            List<Baggage> baggageList, Flight flight) {
        return capacityService.loadBaggageList(baggageList, flight);
    }

    public List<Baggage> assignWaitingBaggageToFlight(Flight nextFlight) {
        return capacityService.tryAssignWaitingBaggage(nextFlight);
    }

    public List<Baggage> getWaitingBaggage(String flightNumber) {
        return capacityService.getAllWaiting(flightNumber);
    }

    public int getWaitingCount(String flightNumber) {
        return capacityService.getWaitingCount(flightNumber);
    }

    public Baggage dequeueWaiting(String flightNumber) {
        return capacityService.dequeueWaiting(flightNumber);
    }
    public double getFlightLoadPercentage(Flight flight) {
        return capacityService.getLoadPercentage(flight);
    }

    public double getRemainingCapacity(Flight flight) {
        return capacityService.getRemainingCapacity(flight);
    }

    // ============================================================
    // SERVİS ERİŞİMİ
    // ============================================================

    public RoutingService         getRoutingService()         { return routingService; }
    public FlightScheduleService  getFlightScheduleService()  { return flightScheduleService; }
    public SecurityService        getSecurityService()        { return securityService; }
    public PriorityLoadingService getPriorityLoadingService() { return priorityLoadingService; }
    public WeightLoadingService   getWeightLoadingService()   { return weightLoadingService; }
    public BaggageTrackingService getTrackingService()        { return trackingService; }
    public CapacityService        getCapacityService()        { return capacityService; }
}