package com.airport.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RouteDAO {

    public static List<String[]> findAllAirports() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT code, name FROM airports";

        try (Connection con = DatabaseManager.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{ rs.getString("code"), rs.getString("name") });
            }
        } catch (Exception e) {
            System.err.println("❌ RouteDAO.findAllAirports: " + e.getMessage());
        }
        return list;
    }

    public static List<String[]> findAllRoutes() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT from_code, to_code, bidirectional, distance_km FROM routes";

        try (Connection con = DatabaseManager.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("from_code"),
                        rs.getString("to_code"),
                        String.valueOf(rs.getBoolean("bidirectional")),
                        String.valueOf(rs.getDouble("distance_km"))   // <-- yeni
                });
            }
        } catch (Exception e) {
            System.err.println("❌ RouteDAO.findAllRoutes: " + e.getMessage());
        }
        return list;
    }

    public static boolean insertAirport(String code, String name) {
        String sql = "INSERT IGNORE INTO airports (code, name) VALUES (?, ?)";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);
            ps.setString(2, name);
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.err.println("❌ RouteDAO.insertAirport: " + e.getMessage());
            return false;
        }
    }

    public static boolean insertRoute(String from, String to, boolean bidirectional, double distanceKm) {
        String sql = "INSERT IGNORE INTO routes (from_code, to_code, bidirectional, distance_km) VALUES (?, ?, ?, ?)";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, from);
            ps.setString(2, to);
            ps.setBoolean(3, bidirectional);
            ps.setDouble(4, distanceKm);
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.err.println("❌ RouteDAO.insertRoute: " + e.getMessage());
            return false;
        }
    }
}