package com.airport.datastructures;

import java.util.*;

/**
 * Yönlü (Directed) Graf — Havaalanı ağını modeller.
 *
 * <p>Node  = Havaalanı IATA kodu (IST, ESB, ADB...)
 * <p>Edge  = İki havaalanı arasındaki uçuş rotası
 *
 * <p>İçerdiği algoritmalar:
 * <ul>
 *   <li><b>BFS</b> → En az aktarmalı (en kısa) rota bulma</li>
 *   <li><b>DFS</b> → Tüm olası rotaları bulma (backtracking)</li>
 * </ul>
 *
 * <p>Dahili yapı: {@code HashMap<String, Set<String>>} (adjacency list)
 */
public class AirportGraph {

    // Node listesi: havaalanı kodu → Set<komşu kod>
    private final Map<String, Set<String>> adjacencyList;
    // Havaalanı tam adları (UI katmanı için)
    private final Map<String, String> airportNames;
    private final Map<String, Map<String, Double>> distanceMap;

    // ==================== CONSTRUCTOR ====================

    public AirportGraph() {
        this.adjacencyList = new HashMap<>();
        this.distanceMap   = new HashMap<>();
        this.airportNames  = new HashMap<>();
    }

    // ==================== GRAPH KURULUM ====================

    /**
     * Yeni bir havaalanı (node) ekler.
     */
    public void addAirport(String code, String name) {
        String up = code.toUpperCase();
        adjacencyList.putIfAbsent(up, new LinkedHashSet<>());
        distanceMap.putIfAbsent(up, new HashMap<>());
        airportNames.put(up, name);
    }

    /**
     * Tek yönlü rota ekler: from → to
     */
    public void addRoute(String fromCode, String toCode) {
        addRoute(fromCode, toCode, 0.0);
    }

    public void addRoute(String fromCode, String toCode, double distanceKm) {
        String from = fromCode.toUpperCase();
        String to   = toCode.toUpperCase();
        adjacencyList.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
        adjacencyList.putIfAbsent(to, new LinkedHashSet<>());
        distanceMap.computeIfAbsent(from, k -> new HashMap<>()).put(to, distanceKm);
        distanceMap.putIfAbsent(to, new HashMap<>());
    }

    public double getDistance(String from, String to) {
        Map<String, Double> map = distanceMap.get(from.toUpperCase());
        return map != null ? map.getOrDefault(to.toUpperCase(), 0.0) : 0.0;
    }

    /** Rota mesafesini sonradan ayarla */
    public void setDistance(String from, String to, double km) {
        String f = from.toUpperCase();
        String t = to.toUpperCase();
        distanceMap.computeIfAbsent(f, k -> new HashMap<>()).put(t, km);
    }
    /**
     * Çift yönlü rota ekler: a ↔ b
     */
    public void addBidirectionalRoute(String codeA, String codeB) {
        addBidirectionalRoute(codeA, codeB, 0.0);
    }

    public void addBidirectionalRoute(String codeA, String codeB, double distanceKm) {
        addRoute(codeA, codeB, distanceKm);
        addRoute(codeB, codeA, distanceKm);
    }


    /**
     * Rota kaldır (tek yönlü).
     */
    public boolean removeRoute(String fromCode, String toCode) {
        Set<String> neighbors = adjacencyList.get(fromCode.toUpperCase());
        if (neighbors == null) return false;
        return neighbors.remove(toCode.toUpperCase());
    }

    // ==================== BFS — EN KISA ROTA ====================

    /**
     * BFS ile start → end arası en az durak içeren rotayı bulur.
     *
     * @return Rota node listesi (start ve end dahil). Rota yoksa boş liste.
     */
    public List<String> bfsShortestPath(String start, String end) {
        String s = start.toUpperCase();
        String e = end.toUpperCase();

        if (!adjacencyList.containsKey(s) || !adjacencyList.containsKey(e)) {
            return Collections.emptyList();
        }
        if (s.equals(e)) {
            return List.of(s);
        }

        // Her node için o node'a ulaşan tam yolu tutuyoruz
        Queue<List<String>> queue   = new LinkedList<>();
        Set<String>          visited = new HashSet<>();

        queue.offer(List.of(s));
        visited.add(s);

        while (!queue.isEmpty()) {
            List<String> path    = queue.poll();
            String        current = path.get(path.size() - 1);

            for (String neighbor : adjacencyList.getOrDefault(current, Collections.emptySet())) {
                if (visited.contains(neighbor)) continue;

                List<String> newPath = new ArrayList<>(path);
                newPath.add(neighbor);

                if (neighbor.equals(e)) {
                    return Collections.unmodifiableList(newPath);
                }
                visited.add(neighbor);
                queue.offer(newPath);
            }
        }
        return Collections.emptyList(); // rota yok
    }

    // ==================== DIJKSTRA ====================

    /**
     * Dijkstra ile en kısa mesafeli rotayı bulur.
     * @return PathResult (yol listesi + toplam mesafe), rota yoksa boş yol ve -1 mesafe
     */
    public PathResult dijkstraShortestPath(String start, String end) {
        String s = start.toUpperCase();
        String e = end.toUpperCase();

        if (!adjacencyList.containsKey(s) || !adjacencyList.containsKey(e)) {
            return new PathResult(Collections.emptyList(), -1);
        }
        if (s.equals(e)) {
            return new PathResult(List.of(s), 0.0);
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        for (String node : adjacencyList.keySet()) {
            dist.put(node, Double.MAX_VALUE);
        }
        dist.put(s, 0.0);
        pq.add(s);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (u.equals(e)) break;

            for (String v : adjacencyList.getOrDefault(u, Collections.emptySet())) {
                double weight = getDistance(u, v);
                if (weight <= 0) continue; // mesafesiz kenarı atla

                double alt = dist.get(u) + weight;
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }

        if (dist.get(e) == Double.MAX_VALUE) {
            return new PathResult(Collections.emptyList(), -1);
        }

        // Yolu geriye doğru oluştur
        LinkedList<String> path = new LinkedList<>();
        for (String at = e; at != null; at = prev.get(at)) {
            path.addFirst(at);
        }
        return new PathResult(Collections.unmodifiableList(path), dist.get(e));
    }

    public static class PathResult {
        private final List<String> path;
        private final double totalDistance;

        public PathResult(List<String> path, double totalDistance) {
            this.path = path;
            this.totalDistance = totalDistance;
        }
        public List<String> getPath() { return path; }
        public double getTotalDistance() { return totalDistance; }
        public boolean hasPath() { return !path.isEmpty(); }
    }

    // ==================== DFS — TÜM ROTALAR ====================

    /**
     * DFS + backtracking ile start → end arası tüm döngüsüz rotaları bulur.
     *
     * @return Her biri tam yol olan liste listesi. Rota yoksa boş.
     */
    public List<List<String>> dfsAllPaths(String start, String end) {
        String s = start.toUpperCase();
        String e = end.toUpperCase();

        List<List<String>> allPaths   = new ArrayList<>();
        List<String>        currentPath = new ArrayList<>();
        Set<String>         visited    = new HashSet<>();

        currentPath.add(s);
        visited.add(s);
        dfsHelper(s, e, currentPath, visited, allPaths);
        return allPaths;
    }

    private void dfsHelper(String current, String end,
                           List<String> path, Set<String> visited,
                           List<List<String>> allPaths) {
        if (current.equals(end)) {
            allPaths.add(new ArrayList<>(path));
            return;
        }
        for (String neighbor : adjacencyList.getOrDefault(current, Collections.emptySet())) {
            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                path.add(neighbor);
                dfsHelper(neighbor, end, path, visited, allPaths);
                // Backtrack
                path.remove(path.size() - 1);
                visited.remove(neighbor);
            }
        }
    }

    // ==================== SORGU METODLARI ====================

    /**
     * İki havaalanı arasında direkt bağlantı var mı?
     */
    public boolean hasDirectConnection(String from, String to) {
        Set<String> neighbors = adjacencyList.get(from.toUpperCase());
        return neighbors != null && neighbors.contains(to.toUpperCase());
    }

    /**
     * Bir havaalanından uçuş yapılan tüm havaalanları.
     */
    public Set<String> getNeighbors(String airportCode) {
        return Collections.unmodifiableSet(
                adjacencyList.getOrDefault(airportCode.toUpperCase(), Collections.emptySet())
        );
    }

    /**
     * Grafttaki tüm havaalanı kodları.
     */
    public Set<String> getAllAirportCodes() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }

    public String getAirportName(String code) {
        return airportNames.getOrDefault(code.toUpperCase(), "Bilinmeyen Havaalanı (" + code + ")");
    }

    public int getAirportCount() { return adjacencyList.size(); }

    public boolean containsAirport(String code) {
        return adjacencyList.containsKey(code.toUpperCase());
    }

    /**
     * Grafı okunabilir şekilde yazdırır (debug için).
     */
    public String toAdjacencyString() {
        StringBuilder sb = new StringBuilder("=== Havaalanı Graf ===\n");
        for (Map.Entry<String, Set<String>> entry : adjacencyList.entrySet()) {
            String name = airportNames.getOrDefault(entry.getKey(), "");
            sb.append(String.format("  %s (%s) → %s%n",
                    entry.getKey(), name, entry.getValue()));
        }
        return sb.toString();
    }
}