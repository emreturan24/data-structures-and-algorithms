package com.airport.db;

import com.airport.model.Baggage;
import com.airport.model.enums.BaggageStatus;
import com.airport.model.enums.DangerousGoodsCategory;
import com.airport.model.enums.PassengerClass;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Bagaj tablosunun tüm veritabanı işlemleri.
 *
 * <p>Yazma işlemleri (insert, update, delete) boolean döner.
 * false → veritabanı hatası, üst katman işlemi geri almalı.
 */
public class BaggageDAO {

    // ==================== OKUMA ====================

    public static List<Baggage> findAll() {
        List<Baggage> list = new ArrayList<>();
        String sql = "SELECT * FROM baggage";

        try (Connection con = DatabaseManager.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) {
            System.err.println("❌ BaggageDAO.findAll: " + e.getMessage());
        }
        return list;
    }

    public static List<Baggage> findByStatus(BaggageStatus status) {
        List<Baggage> list = new ArrayList<>();
        String sql = "SELECT * FROM baggage WHERE status = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (Exception e) {
            System.err.println("❌ BaggageDAO.findByStatus: " + e.getMessage());
        }
        return list;
    }

    // ==================== YAZMA (boolean dönüşlü) ====================

    /** @return true = veritabanına başarıyla yazıldı */
    public static boolean insert(Baggage b) {
        String sql = "INSERT INTO baggage " +
                "(baggage_id, passenger_id, flight_number, weight_kg, " +
                "owner_class, status, has_dangerous, danger_category) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, b.getBaggageId());
            ps.setString(2, b.getPassengerId());
            ps.setString(3, b.getFlightNumber());
            ps.setDouble(4, b.getWeightKg());
            ps.setString(5, b.getOwnerClass().name());
            ps.setString(6, b.getStatus().name());
            ps.setBoolean(7, b.isHasDangerousGoods());
            ps.setString(8, b.getDangerousCategory() != null
                    ? b.getDangerousCategory().name() : null);

            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.err.println("❌ BaggageDAO.insert: " + e.getMessage());
            return false;
        }
    }

    /** @return true = en az bir satır güncellendi */
    public static boolean updateStatus(String baggageId, BaggageStatus status) {
        String sql = "UPDATE baggage SET status = ? WHERE baggage_id = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, baggageId);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (Exception e) {
            System.err.println("❌ BaggageDAO.updateStatus: " + e.getMessage());
            return false;
        }
    }

    /** @return true = en az bir satır güncellendi */
    public static boolean updateDangerous(String baggageId,
                                          boolean dangerous,
                                          DangerousGoodsCategory category,
                                          BaggageStatus status) {
        String sql = "UPDATE baggage SET has_dangerous=?, danger_category=?, status=? " +
                "WHERE baggage_id=?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, dangerous);
            ps.setString(2, category != null ? category.name() : null);
            ps.setString(3, status.name());
            ps.setString(4, baggageId);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (Exception e) {
            System.err.println("❌ BaggageDAO.updateDangerous: " + e.getMessage());
            return false;
        }
    }

    /** @return true = en az bir satır silindi */
    public static boolean delete(String baggageId) {
        String sql = "DELETE FROM baggage WHERE baggage_id = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, baggageId);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (Exception e) {
            System.err.println("❌ BaggageDAO.delete: " + e.getMessage());
            return false;
        }
    }

    // ==================== YARDIMCI ====================

    private static Baggage mapRow(ResultSet rs) throws SQLException {
        String id         = rs.getString("baggage_id");
        String passId     = rs.getString("passenger_id");
        String flightNo   = rs.getString("flight_number");
        double weight     = rs.getDouble("weight_kg");
        PassengerClass pc = PassengerClass.valueOf(rs.getString("owner_class"));
        BaggageStatus st  = BaggageStatus.valueOf(rs.getString("status"));
        boolean dangerous = rs.getBoolean("has_dangerous");
        String catStr     = rs.getString("danger_category");

        Baggage b = new Baggage(id, passId, flightNo, weight, pc);
        b.setStatus(st);
        b.setHasDangerousGoods(dangerous);
        if (catStr != null) {
            b.setDangerousCategory(DangerousGoodsCategory.valueOf(catStr));
        }
        return b;
    }
}