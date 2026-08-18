package com.airport.db;

import com.airport.model.Flight;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FlightDAO {

    public static List<Flight> findAll() {
        List<Flight> list = new ArrayList<>();
        String sql = "SELECT * FROM flights";

        try (Connection con = DatabaseManager.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) {
            System.err.println("❌ FlightDAO.findAll: " + e.getMessage());
        }
        return list;
    }

    public static boolean insert(Flight f) {
        String sql = "INSERT INTO flights " +
                "(flight_number, origin, destination, departure, arrival, " +
                "max_capacity, current_load, status, gate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, f.getFlightNumber());
            ps.setString(2, f.getDepartureAirport());
            ps.setString(3, f.getArrivalAirport());
            ps.setTimestamp(4, Timestamp.valueOf(f.getDepartureTime()));
            ps.setTimestamp(5, Timestamp.valueOf(f.getArrivalTime()));
            ps.setDouble(6, f.getMaxCapacityKg());
            ps.setDouble(7, f.getCurrentLoadKg());
            ps.setString(8, f.getStatus().name());
            ps.setString(9, f.getGate());
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.err.println("❌ FlightDAO.insert: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateStatus(String flightNumber, Flight.FlightStatus status) {
        String sql = "UPDATE flights SET status = ? WHERE flight_number = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, flightNumber);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (Exception e) {
            System.err.println("❌ FlightDAO.updateStatus: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateLoad(String flightNumber, double currentLoadKg) {
        String sql = "UPDATE flights SET current_load = ? WHERE flight_number = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDouble(1, currentLoadKg);
            ps.setString(2, flightNumber);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (Exception e) {
            System.err.println("❌ FlightDAO.updateLoad: " + e.getMessage());
            return false;
        }
    }

    public static boolean delete(String flightNumber) {
        String sql = "DELETE FROM flights WHERE flight_number = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, flightNumber);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (Exception e) {
            System.err.println("❌ FlightDAO.delete: " + e.getMessage());
            return false;
        }
    }

    private static Flight mapRow(ResultSet rs) throws SQLException {
        String no     = rs.getString("flight_number");
        String origin = rs.getString("origin");
        String dest   = rs.getString("destination");
        LocalDateTime dep = rs.getTimestamp("departure").toLocalDateTime();
        LocalDateTime arr = rs.getTimestamp("arrival").toLocalDateTime();
        double maxCap = rs.getDouble("max_capacity");
        double curLoad= rs.getDouble("current_load");
        String statusStr = rs.getString("status");
        String gate   = rs.getString("gate");

        Flight f = new Flight(no, origin, dest, dep, arr, maxCap);
        f.setCurrentLoadKg(curLoad);
        f.setStatus(Flight.FlightStatus.valueOf(statusStr));
        f.setGate(gate);
        return f;
    }
}