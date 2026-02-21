package com.lax.sme_manager.repository;

import com.lax.sme_manager.domain.Vendor;
import com.lax.sme_manager.repository.model.VendorEntity;
import com.lax.sme_manager.util.DatabaseManager;
import com.lax.sme_manager.util.DateUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Vendor CRUD operations
 */
public class VendorRepository {

    public List<VendorEntity> findAll() {
        String sql = """
                SELECT id, name, contact_person, address,
                       phone, email, notes, default_amount, is_deleted,
                       created_at, updated_at
                FROM vendors
                WHERE (is_deleted = 0 OR is_deleted IS NULL)
                ORDER BY name
                """;

        List<VendorEntity> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(sql)) {

            while (rs.next()) {
                list.add(map(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch vendors", e);
        }
    }

    public List<Vendor> findAllVendors() {
        String sql = """
                SELECT id, name, is_deleted
                FROM vendors
                WHERE (is_deleted = 0 OR is_deleted IS NULL)
                ORDER BY name
                """;

        List<Vendor> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(sql)) {

            while (rs.next()) {
                Vendor e = new Vendor(rs.getInt("id"), rs.getString("name"));
                list.add(e);
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch vendors", e);
        }
    }

    public Optional<VendorEntity> findById(int id) {
        String sql = "SELECT * FROM vendors WHERE id = ?";

        try (Connection c = DatabaseManager.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find vendor by id", e);
        }
    }

    public Optional<VendorEntity> findByName(String name) {
        String sql = "SELECT * FROM vendors WHERE LOWER(name) = LOWER(?)";

        try (Connection c = DatabaseManager.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find vendor by name", e);
        }
    }

    public VendorEntity insert(VendorEntity e) {
        String sql = """
                INSERT INTO vendors
                (name, contact_person, address, phone,
                 email, notes, default_amount, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = DatabaseManager.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, e.getName());
            ps.setString(2, e.getContactPerson());
            ps.setString(3, e.getAddress());
            ps.setString(4, e.getPhone());
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getNotes());
            ps.setObject(7, e.getDefaultAmount());
            ps.setObject(8, e.getCreatedAt());
            ps.setObject(9, e.getUpdatedAt());

            ps.executeUpdate();

            try (Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                e.setId(rs.getInt(1));
            }
            return e;

        } catch (SQLException ex) {
            throw new RuntimeException("Failed to insert vendor", ex);
        }
    }

    public void update(VendorEntity e) {
        String sql = """
                UPDATE vendors SET
                  name=?, contact_person=?, address=?,
                  phone=?, email=?, notes=?, default_amount=?,
                  updated_at=?
                WHERE id=?
                """;

        try (Connection c = DatabaseManager.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, e.getName());
            ps.setString(2, e.getContactPerson());
            ps.setString(3, e.getAddress());
            ps.setString(4, e.getPhone());
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getNotes());
            ps.setObject(7, e.getDefaultAmount());
            ps.setObject(8, LocalDateTime.now());
            ps.setInt(9, e.getId());

            ps.executeUpdate();

        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update vendor", ex);
        }
    }

    public void delete(int id) {
        try (Connection c = DatabaseManager.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM vendors WHERE id=?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete vendor", e);
        }
    }

    /**
     * Soft delete a vendor - marks as deleted without removing from database
     */
    public void softDelete(int id) {
        String sql = "UPDATE vendors SET is_deleted = 1, updated_at = ? WHERE id = ?";

        try (Connection c = DatabaseManager.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, LocalDateTime.now());
            ps.setInt(2, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to soft delete vendor", e);
        }
    }

    private VendorEntity map(ResultSet rs) throws SQLException {

        return VendorEntity.builder()
                .id(rs.getInt("id"))
                .name(rs.getString("name"))
                .contactPerson(rs.getString("contact_person"))
                .address(rs.getString("address"))
                .phone(rs.getString("phone"))
                .email(rs.getString("email"))
                .notes(rs.getString("notes"))
                .defaultAmount(rs.getBigDecimal("default_amount"))
                .isDeleted(rs.getBoolean("is_deleted"))
                .createdAt(rs.getObject("created_at", LocalDateTime.class))
                .updatedAt(rs.getObject("updated_at", LocalDateTime.class))
                .build();
    }
}
