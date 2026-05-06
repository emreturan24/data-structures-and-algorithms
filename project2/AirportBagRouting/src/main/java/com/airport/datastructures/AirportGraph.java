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

    // ==================== CONSTRUCTOR ====================

    public AirportGraph() {
        this.adjacencyList = new HashMap<>();
        this.airportNames  = new HashMap<>();
    }

    // ==================== GRAPH KURULUM ====================

    /**
     * Yeni bir havaalanı (node) ekler.
     */
    public void addAirport(String code, String name) {
        adjacencyList.putIfAbsent(code.toUpperCase(), new LinkedHashSet<>());
        airportNames.put(code.toUpperCase(), name);
    }

    /**
     * Tek yönlü rota ekler: from → to
     */
    public void addRoute(String fromCode, String toCode) {
        String from = fromCode.toUpperCase();
        String to   = toCode.toUpperCase();
        adjacencyList.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
        // to node'u da graph'ta olsun
        adjacencyList.putIfAbsent(to, new LinkedHashSet<>());
    }

    /**
     * Çift yönlü rota ekler: a ↔ b
     */
    public void addBidirectionalRoute(String codeA, String codeB) {
        addRoute(codeA, codeB);
        addRoute(codeB, codeA);
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