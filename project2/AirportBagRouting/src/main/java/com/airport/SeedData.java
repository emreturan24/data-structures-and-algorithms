package com.airport;

import com.airport.controller.AirportController;
import com.airport.model.Baggage;
import com.airport.model.Flight;
import com.airport.model.enums.DangerousGoodsCategory;
import com.airport.model.enums.PassengerClass;

import java.time.LocalDateTime;

/**
 * Demo verisi yükler — gerçek uygulamada veritabanı / API ile değiştirilir.
 */
public class SeedData {

    public static void populate(AirportController ctrl) {

        // ── Graf (Türkiye + komşu havaalanları) ──────────────────────────
        ctrl.addAirport("IST", "İstanbul Havalimanı");
        ctrl.addAirport("SAW", "Sabiha Gökçen");
        ctrl.addAirport("ESB", "Ankara Esenboğa");
        ctrl.addAirport("ADB", "İzmir Adnan Menderes");
        ctrl.addAirport("AYT", "Antalya Havalimanı");
        ctrl.addAirport("TZX", "Trabzon Havalimanı");
        ctrl.addAirport("VAN", "Van Ferit Melen");
        ctrl.addAirport("GZP", "Gazipaşa-Alanya");

        ctrl.addRoute("IST", "ESB", true);
        ctrl.addRoute("IST", "ADB", true);
        ctrl.addRoute("IST", "AYT", true);
        ctrl.addRoute("IST", "TZX", true);
        ctrl.addRoute("IST", "VAN", true);
        ctrl.addRoute("IST", "SAW", true);
        ctrl.addRoute("ESB", "ADB", true);
        ctrl.addRoute("ESB", "AYT", true);
        ctrl.addRoute("ESB", "TZX", true);
        ctrl.addRoute("ADB", "AYT", true);
        ctrl.addRoute("AYT", "GZP", true);
        ctrl.addRoute("VAN", "TZX", true);

        // ── Uçuşlar ──────────────────────────────────────────────────────
        LocalDateTime now = LocalDateTime.now();
        Flight[] flights = {
                new Flight("TK101", "IST", "ESB", now.plusMinutes(45),  now.plusMinutes(150), 8000),
                new Flight("TK202", "IST", "ADB", now.plusMinutes(90),  now.plusMinutes(210), 9500),
                new Flight("TK303", "IST", "AYT", now.plusMinutes(120), now.plusMinutes(255), 7500),
                new Flight("TK404", "ESB", "ADB", now.plusMinutes(180), now.plusMinutes(270), 6000),
                new Flight("TK505", "IST", "TZX", now.plusMinutes(30),  now.plusMinutes(165), 5500),
                new Flight("TK606", "AYT", "IST", now.plusHours(3),     now.plusHours(4),     8200),
        };
        for (Flight f : flights) ctrl.scheduleFlight(f);

        // ── Bagajlar ─────────────────────────────────────────────────────
        String[][] passengers = {
                {"P001","TK101"}, {"P002","TK101"}, {"P003","TK101"},
                {"P004","TK202"}, {"P005","TK202"},
                {"P006","TK303"}, {"P007","TK303"},
                {"P008","TK505"}, {"P009","TK505"},
        };
        PassengerClass[] classes = {
                PassengerClass.VIP,      PassengerClass.BUSINESS, PassengerClass.ECONOMY,
                PassengerClass.VIP,      PassengerClass.ECONOMY,
                PassengerClass.BUSINESS, PassengerClass.ECONOMY,
                PassengerClass.VIP,      PassengerClass.ECONOMY,
        };
        double[] weights = {32.5, 28.0, 18.5, 42.0, 15.0, 38.0, 22.0, 45.5, 19.0};

        for (int i = 0; i < passengers.length; i++) {
            Baggage b = new Baggage(passengers[i][0], passengers[i][1],
                    weights[i], classes[i]);
            ctrl.checkIn(b);
            ctrl.addToPriorityQueue(b);
        }

        // Tehlikeli iki bagaj (güvenlik uyarısı demo'su)
        Baggage danger1 = new Baggage("P010", "TK101", 12.0, PassengerClass.ECONOMY);
        danger1.setHasDangerousGoods(true);
        danger1.setDangerousCategory(DangerousGoodsCategory.FLAMMABLE_LIQUID);
        ctrl.checkIn(danger1);
        ctrl.addToPriorityQueue(danger1);

        Baggage danger2 = new Baggage("P011", "TK303", 8.5, PassengerClass.BUSINESS);
        danger2.setHasDangerousGoods(true);
        danger2.setDangerousCategory(DangerousGoodsCategory.EXPLOSIVE);
        ctrl.checkIn(danger2);
        ctrl.addToPriorityQueue(danger2);
    }
}