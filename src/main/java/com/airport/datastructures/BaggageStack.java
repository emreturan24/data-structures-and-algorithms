package com.airport.datastructures;

import com.airport.model.Baggage;

import java.util.*;

/**
 * Ağırlık Bazlı Yükleme Stack'i (LIFO).
 *
 * <p>Yükleme kuralı:
 * <ol>
 *   <li>WeightLoadingService bagajları <b>ağır → hafif</b> sıralar.</li>
 *   <li>Sıralı liste sırayla push edilir → ağır bagajlar altta, hafifler üstte.</li>
 *   <li>İnişte pop (LIFO) → <b>hafif bagajlar önce çıkar</b>, uçak dengesi bozulmaz.</li>
 * </ol>
 *
 * <p>Dahili yapı: {@link ArrayDeque} — Java'nın önerdiği Stack implementasyonu.
 * (java.util.Stack thread-safe olduğu için yavaştır; ArrayDeque daha performanslıdır.)
 */
public class BaggageStack {

    private final Deque<Baggage> stack;
    private final String flightNumber;
    private int totalPushed; // istatistik

    // ==================== CONSTRUCTOR ====================

    public BaggageStack(String flightNumber) {
        this.stack        = new ArrayDeque<>();
        this.flightNumber = flightNumber;
        this.totalPushed  = 0;
    }

    // ==================== STACK OPERASYONLARI ====================

    /**
     * Bagajı stack'e ekler (en üste).
     */
    public void push(Baggage baggage) {
        stack.push(baggage);
        totalPushed++;
    }

    /**
     * En üstteki bagajı çıkarır (LIFO).
     * @throws NoSuchElementException stack boşsa
     */
    public Baggage pop() {
        if (stack.isEmpty()) {
            throw new NoSuchElementException(
                    "BaggageStack boş! Uçuş: " + flightNumber);
        }
        return stack.pop();
    }

    /**
     * En üstteki bagaja bakar, çıkarmaz.
     */
    public Baggage peek() {
        if (stack.isEmpty()) {
            throw new NoSuchElementException(
                    "BaggageStack boş! Uçuş: " + flightNumber);
        }
        return stack.peek();
    }

    /**
     * Tüm bagajları LIFO sırasıyla boşaltır (iniş operasyonu).
     * @return Çıkarılan bagajlar (hafiften ağıra)
     */
    public List<Baggage> unloadAll() {
        List<Baggage> unloaded = new ArrayList<>(stack.size());
        while (!stack.isEmpty()) {
            unloaded.add(stack.pop());
        }
        return unloaded;
    }

    /**
     * Stack içeriğini kopyasını döner (stack değişmez — UI gösterimi için).
     * Sıra: üstten alta (hafiften ağıra)
     */
    public List<Baggage> peekAll() {
        return new ArrayList<>(stack); // ArrayDeque iterasyonu üstten başlar
    }

    // ==================== SORGU ====================

    public boolean isEmpty()          { return stack.isEmpty(); }
    public int size()                 { return stack.size(); }
    public String getFlightNumber()   { return flightNumber; }
    public int getTotalPushed()       { return totalPushed; }

    /**
     * Stack'teki toplam ağırlık.
     */
    public double getTotalWeightKg() {
        return stack.stream().mapToDouble(Baggage::getWeightKg).sum();
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public String toString() {
        return String.format("BaggageStack{flight='%s', size=%d, totalWeight=%.1fkg}",
                flightNumber, stack.size(), getTotalWeightKg());
    }
}