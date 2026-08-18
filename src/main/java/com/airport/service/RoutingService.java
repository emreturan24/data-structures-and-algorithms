package com.airport.service;

import com.airport.datastructures.AirportGraph;
import com.airport.db.RouteDAO;

import java.util.List;
import java.util.Set;

/**
 * Rotalama Servisi — AirportGraph üzerindeki işlemleri UI'dan soyutlar.
 *
 * Kullandığı veri yapısı: AirportGraph (adjacency list + BFS/DFS) — değişmedi.
 *
 * MySQL entegrasyonu:
 *   - Constructor: DB'deki airports ve routes tablosunu okuyarak
 *     graf yapısını otomatik oluşturur.
 *   - addAirport / addRoute çağrıları hem grafa hem DB'ye yazar.
 */
public class RoutingService {

    private final AirportGraph graph;


    public AirportGraph.PathResult findShortestRouteByDistance(String from, String to) {
        return graph.dijkstraShortestPath(from, to);
    }
    // ==================== CONSTRUCTOR ====================

    public RoutingService() {
        this.graph = new AirportGraph();

        // DB'deki havaalanları ve rotaları grafa yükle
        loadFromDatabase();
    }

    /**
     * Uygulama başlangıcında DB'deki havaalanı ve rota verilerini
     * bellek içi AirportGraph'a yükler.
     */
    private void loadFromDatabase() {
        // Havaalanları yükle
        List<String[]> airports = RouteDAO.findAllAirports();
        for (String[] a : airports) {
            graph.addAirport(a[0], a[1]);
        }

        // Rotaları yükle (mesafe dahil)
        List<String[]> routes = RouteDAO.findAllRoutes();
        for (String[] r : routes) {
            String from   = r[0];
            String to     = r[1];
            boolean bidir = Boolean.parseBoolean(r[2]);
            double  dist  = Double.parseDouble(r[3]);  // ← mesafe

            if (bidir) graph.addBidirectionalRoute(from, to, dist);
            else       graph.addRoute(from, to, dist);
        }

        System.out.println("✓ RoutingService: " + airports.size()
                + " havaalanı, " + routes.size() + " rota DB'den yüklendi.");
    }

    // ==================== HAVAALANI YÖNETİMİ ====================

    /**
     * Yeni havaalanı ekler. Hem grafa hem DB'ye yazar.
     */
    public void addAirport(String code, String name) {
        graph.addAirport(code, name);
        RouteDAO.insertAirport(code, name);
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

    /**
     * Tek yönlü rota ekler. Hem grafa hem DB'ye yazar.
     */
    public void addRoute(String from, String to) {
        addRoute(from, to, 0.0); // mesafesiz ekleme varsayılan 0
    }
    public void addRoute(String from, String to, double distanceKm) {
        graph.addRoute(from, to, distanceKm);
        RouteDAO.insertRoute(from, to, false, distanceKm);
    }

    /**
     * Çift yönlü rota ekler. Hem grafa hem DB'ye yazar.
     */
    public void addBidirectionalRoute(String a, String b) {
        addBidirectionalRoute(a, b, 0.0);
    }

    public void addBidirectionalRoute(String a, String b, double distanceKm) {
        graph.addBidirectionalRoute(a, b, distanceKm);
        RouteDAO.insertRoute(a, b, true, distanceKm);
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

    public List<String> findShortestRoute(String from, String to) {
        return graph.bfsShortestPath(from, to);
    }

    public List<List<String>> findAllRoutes(String from, String to) {
        return graph.dfsAllPaths(from, to);
    }

    public int getTransferCount(String from, String to) {
        List<String> path = graph.bfsShortestPath(from, to);
        if (path.isEmpty()) return -1;
        return path.size() - 2;
    }

    public boolean isReachable(String from, String to) {
        return !graph.bfsShortestPath(from, to).isEmpty();
    }

    public String getGraphSummary() {
        return graph.toAdjacencyString();
    }
}