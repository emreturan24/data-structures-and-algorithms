package com.airport.service;

import com.airport.datastructures.AirportGraph;

import java.util.List;
import java.util.Set;

/**
 * Rotalama Servisi — AirportGraph üzerindeki işlemleri UI'dan soyutlar.
 *
 * <p>Kullandığı veri yapısı: {@link AirportGraph} (adjacency list + BFS/DFS)
 * <p>Controller bu servis üzerinden graf kurulumu ve rota sorguları yapar.
 */
public class RoutingService {

    private final AirportGraph graph;

    // ==================== CONSTRUCTOR ====================

    public RoutingService() {
        this.graph = new AirportGraph();
    }

    // ==================== HAVAALANI YÖNETİMİ ====================

    public void addAirport(String code, String name) {
        graph.addAirport(code, name);
    }

    public boolean airportExists(String code) {
        return graph.containsAirport(code);
    }

    public Set<String> getAllAirports() {
        return graph.getAllAirportCodes();
    }

    public String getAirportName(String code) {
        return graph.getAirportName(code);
    }

    // ==================== ROTA YÖNETİMİ ====================

    public void addRoute(String from, String to) {
        graph.addRoute(from, to);
    }

    public void addBidirectionalRoute(String a, String b) {
        graph.addBidirectionalRoute(a, b);
    }

    public boolean removeRoute(String from, String to) {
        return graph.removeRoute(from, to);
    }

    public boolean hasDirectFlight(String from, String to) {
        return graph.hasDirectConnection(from, to);
    }

    public Set<String> getConnectedAirports(String airportCode) {
        return graph.getNeighbors(airportCode);
    }

    // ==================== ROTA SORGULAMA ====================

    /**
     * BFS ile en az aktarmalı rotayı bulur.
     * @return [IST, ESB, ADB] gibi rota node listesi; rota yoksa boş liste.
     */
    public List<String> findShortestRoute(String from, String to) {
        return graph.bfsShortestPath(from, to);
    }

    /**
     * DFS ile tüm olası rotaları bulur.
     * @return Her biri tam yol olan liste listesi.
     */
    public List<List<String>> findAllRoutes(String from, String to) {
        return graph.dfsAllPaths(from, to);
    }

    /**
     * BFS üzerinden aktarma sayısını hesaplar.
     * Direkt uçuşta 0 döner. Rota yoksa -1 döner.
     */
    public int getTransferCount(String from, String to) {
        List<String> path = graph.bfsShortestPath(from, to);
        if (path.isEmpty()) return -1;
        return path.size() - 2; // start ve end node'ları çıkar
    }

    /**
     * İki havaalanı arasında herhangi bir rota var mı?
     */
    public boolean isReachable(String from, String to) {
        return !graph.bfsShortestPath(from, to).isEmpty();
    }

    /**
     * Graf durumunu string olarak döner (debug/log için).
     */
    public String getGraphSummary() {
        return graph.toAdjacencyString();
    }
}