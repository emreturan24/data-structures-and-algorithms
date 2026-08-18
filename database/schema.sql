-- ============================================================
--  AirportRouter — Havaalanı Bagaj Yönetim Sistemi
--  MySQL şema + başlangıç verisi
--
--  Kurulum:
--      mysql -u root -p < database/schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS airport_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_turkish_ci;

USE airport_db;

-- Tabloları sıfırdan kurmak için (bağımlılık sırasına dikkat)
DROP TABLE IF EXISTS baggage;
DROP TABLE IF EXISTS routes;
DROP TABLE IF EXISTS flights;
DROP TABLE IF EXISTS airports;

-- ==================== HAVAALANLARI (Graph düğümleri) ====================

CREATE TABLE airports (
    code VARCHAR(4)   NOT NULL,          -- IATA kodu: IST, ESB, ADB...
    name VARCHAR(120) NOT NULL,
    PRIMARY KEY (code)
) ENGINE=InnoDB;

-- ==================== ROTALAR (Graph kenarları) ====================

CREATE TABLE routes (
    from_code     VARCHAR(4)  NOT NULL,
    to_code       VARCHAR(4)  NOT NULL,
    bidirectional BOOLEAN     NOT NULL DEFAULT TRUE,
    distance_km   DOUBLE      NOT NULL DEFAULT 0,
    PRIMARY KEY (from_code, to_code),
    CONSTRAINT fk_routes_from FOREIGN KEY (from_code) REFERENCES airports(code) ON DELETE CASCADE,
    CONSTRAINT fk_routes_to   FOREIGN KEY (to_code)   REFERENCES airports(code) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==================== UÇUŞLAR (Min-Heap kaynağı) ====================

CREATE TABLE flights (
    flight_number VARCHAR(10) NOT NULL,
    origin        VARCHAR(4)  NOT NULL,
    destination   VARCHAR(4)  NOT NULL,
    departure     DATETIME    NOT NULL,
    arrival       DATETIME    NOT NULL,
    max_capacity  DOUBLE      NOT NULL,          -- kg
    current_load  DOUBLE      NOT NULL DEFAULT 0,-- kg
    status        VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
                  -- SCHEDULED | BOARDING | DEPARTED | ARRIVED | CANCELLED
    gate          VARCHAR(10),
    PRIMARY KEY (flight_number),
    INDEX idx_flights_departure (departure)
) ENGINE=InnoDB;

-- ==================== BAGAJLAR ====================

CREATE TABLE baggage (
    baggage_id      VARCHAR(50) NOT NULL,        -- BAG-xxxxxxxx
    passenger_id    VARCHAR(50) NOT NULL,
    flight_number   VARCHAR(10) NOT NULL,
    weight_kg       DOUBLE      NOT NULL,
    owner_class     VARCHAR(20) NOT NULL,        -- VIP | BUSINESS | ECONOMY
    status          VARCHAR(30) NOT NULL DEFAULT 'CHECK_IN',
                    -- CHECK_IN | SECURITY_SCREENING | SECURITY_HOLD | CARGO
                    -- LOADED | IN_TRANSIT | DELIVERED | WAITING_QUEUE
    has_dangerous   BOOLEAN     NOT NULL DEFAULT FALSE,
    danger_category VARCHAR(30) NULL,            -- EXPLOSIVE | FLAMMABLE_LIQUID | ...
    PRIMARY KEY (baggage_id),
    INDEX idx_baggage_flight    (flight_number),
    INDEX idx_baggage_passenger (passenger_id),
    INDEX idx_baggage_status    (status)
) ENGINE=InnoDB;

-- ============================================================
--  BAŞLANGIÇ VERİSİ
-- ============================================================

INSERT INTO airports (code, name) VALUES
    ('IST', 'İstanbul Havalimanı'),
    ('SAW', 'Sabiha Gökçen Havalimanı'),
    ('ESB', 'Ankara Esenboğa Havalimanı'),
    ('ADB', 'İzmir Adnan Menderes Havalimanı'),
    ('AYT', 'Antalya Havalimanı'),
    ('TZX', 'Trabzon Havalimanı'),
    ('DIY', 'Diyarbakır Havalimanı'),
    ('GZT', 'Gaziantep Havalimanı');

INSERT INTO routes (from_code, to_code, bidirectional, distance_km) VALUES
    ('IST', 'ESB', TRUE,  351),
    ('IST', 'ADB', TRUE,  330),
    ('IST', 'AYT', TRUE,  482),
    ('IST', 'TZX', TRUE,  920),
    ('SAW', 'ESB', TRUE,  340),
    ('SAW', 'ADB', TRUE,  320),
    ('ESB', 'ADB', TRUE,  520),
    ('ESB', 'DIY', TRUE,  770),
    ('ESB', 'GZT', TRUE,  570),
    ('ESB', 'TZX', TRUE,  590),
    ('ADB', 'AYT', TRUE,  330),
    ('AYT', 'GZT', TRUE,  680),
    ('DIY', 'GZT', TRUE,  240),
    ('TZX', 'DIY', TRUE,  480);

INSERT INTO flights
    (flight_number, origin, destination, departure, arrival,
     max_capacity, current_load, status, gate) VALUES
    ('TK1001', 'IST', 'ESB', '2026-06-01 08:00:00', '2026-06-01 09:15:00', 3000, 0, 'SCHEDULED', 'A12'),
    ('TK1002', 'IST', 'ADB', '2026-06-01 09:30:00', '2026-06-01 10:35:00', 2500, 0, 'SCHEDULED', 'A05'),
    ('PC2201', 'SAW', 'ESB', '2026-06-01 10:00:00', '2026-06-01 11:10:00', 2000, 0, 'SCHEDULED', 'B03'),
    ('TK1003', 'ESB', 'DIY', '2026-06-01 12:00:00', '2026-06-01 13:40:00', 1800, 0, 'SCHEDULED', 'C07'),
    ('AJ3301', 'IST', 'AYT', '2026-06-01 14:15:00', '2026-06-01 15:30:00', 2200, 0, 'SCHEDULED', 'A21'),
    ('TK1004', 'ADB', 'AYT', '2026-06-01 16:00:00', '2026-06-01 16:55:00', 1500, 0, 'SCHEDULED', 'D02');

INSERT INTO baggage
    (baggage_id, passenger_id, flight_number, weight_kg,
     owner_class, status, has_dangerous, danger_category) VALUES
    ('BAG-1001a2b3', 'P-1001', 'TK1001', 23.5, 'ECONOMY',  'CHECK_IN', FALSE, NULL),
    ('BAG-1002c4d5', 'P-1002', 'TK1001', 31.0, 'BUSINESS', 'CHECK_IN', FALSE, NULL),
    ('BAG-1003e6f7', 'P-1003', 'TK1001', 18.2, 'VIP',      'CHECK_IN', FALSE, NULL),
    ('BAG-1004g8h9', 'P-1004', 'TK1002', 27.8, 'ECONOMY',  'CHECK_IN', FALSE, NULL),
    ('BAG-1005i0j1', 'P-1005', 'TK1002', 12.4, 'ECONOMY',  'CHECK_IN', TRUE,  'FLAMMABLE_LIQUID'),
    ('BAG-1006k2l3', 'P-1006', 'PC2201', 20.0, 'BUSINESS', 'CHECK_IN', FALSE, NULL),
    ('BAG-1007m4n5', 'P-1007', 'PC2201', 33.6, 'ECONOMY',  'CHECK_IN', FALSE, NULL),
    ('BAG-1008o6p7', 'P-1008', 'TK1003', 15.9, 'VIP',      'CHECK_IN', FALSE, NULL),
    ('BAG-1009q8r9', 'P-1009', 'TK1003', 29.3, 'ECONOMY',  'CHECK_IN', TRUE,  'COMPRESSED_GAS'),
    ('BAG-1010s0t1', 'P-1010', 'AJ3301', 24.7, 'BUSINESS', 'CHECK_IN', FALSE, NULL),
    ('BAG-1011u2v3', 'P-1011', 'AJ3301', 19.1, 'ECONOMY',  'CHECK_IN', FALSE, NULL),
    ('BAG-1012w4x5', 'P-1012', 'TK1004', 26.4, 'ECONOMY',  'CHECK_IN', FALSE, NULL);
