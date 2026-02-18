package com.lax.sme_manager.repository;

import com.lax.sme_manager.repository.model.PurchaseEntity;
import com.lax.sme_manager.util.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PurchaseRepository - Implementation of IPurchaseRepository
 */
public class PurchaseRepository implements IPurchaseRepository {

    @Override
    public PurchaseEntity save(PurchaseEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    private PurchaseEntity insert(PurchaseEntity entity) {
        String sql = "INSERT INTO purchase_entries (" +
                "entry_date, vendor_id, bags, rate, weight_kg, is_lumpsum, " +
                "market_fee_percent, commission_percent, market_fee_amount, commission_amount, " +
                "base_amount, grand_total, notes, payment_mode, " +
                "advance_paid, status, created_at, updated_at, " +
                "cheque_number, cheque_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int idx = 1;
            idx = setCommonFields(pstmt, entity, idx);
            pstmt.setObject(idx++, entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now());
            pstmt.setObject(idx++, LocalDateTime.now()); // updated_at
            pstmt.setString(idx++, entity.getChequeNumber());
            pstmt.setObject(idx++, entity.getChequeDate());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating purchase failed, no rows affected.");
            }
            try (Statement s = conn.createStatement();
                    ResultSet rs = s.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                entity.setId(rs.getInt(1));
            }
            return entity;

        } catch (SQLException e) {
            System.err.println("Error inserting purchase: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save purchase", e);
        }
    }

    private PurchaseEntity update(PurchaseEntity entity) {
        String sql = "UPDATE purchase_entries SET " +
                "entry_date = ?, vendor_id = ?, bags = ?, rate = ?, weight_kg = ?, is_lumpsum = ?, " +
                "market_fee_percent = ?, commission_percent = ?, market_fee_amount = ?, commission_amount = ?, " +
                "base_amount = ?, grand_total = ?, notes = ?, payment_mode = ?, " +
                "advance_paid = ?, status = ?, updated_at = ?, " +
                "cheque_number = ?, cheque_date = ? " +
                "WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int idx = 1;
            idx = setCommonFields(pstmt, entity, idx);
            pstmt.setObject(idx++, LocalDateTime.now()); // updated_at
            pstmt.setString(idx++, entity.getChequeNumber());
            pstmt.setObject(idx++, entity.getChequeDate());
            pstmt.setInt(idx++, entity.getId()); // WHERE id = ?

            pstmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            System.err.println("Error updating purchase: " + e.getMessage());
            throw new RuntimeException("Failed to update purchase", e);
        }
    }

    private int setCommonFields(PreparedStatement pstmt, PurchaseEntity entity, int startIndex)
            throws SQLException {
        int index = startIndex;
        pstmt.setObject(index++, entity.getEntryDate());
        pstmt.setInt(index++, entity.getVendorId());
        pstmt.setInt(index++, entity.getBags());
        pstmt.setBigDecimal(index++, entity.getRate() != null ? entity.getRate() : BigDecimal.ZERO);
        pstmt.setObject(index++, entity.getWeightKg());
        pstmt.setBoolean(index++, entity.getIsLumpsum() != null && entity.getIsLumpsum());
        pstmt.setBigDecimal(index++,
                entity.getMarketFeePercent() != null ? entity.getMarketFeePercent() : BigDecimal.valueOf(0.70));
        pstmt.setBigDecimal(index++,
                entity.getCommissionPercent() != null ? entity.getCommissionPercent() : BigDecimal.valueOf(2.00));
        pstmt.setBigDecimal(index++,
                entity.getMarketFeeAmount() != null ? entity.getMarketFeeAmount() : BigDecimal.ZERO);
        pstmt.setBigDecimal(index++,
                entity.getCommissionFeeAmount() != null ? entity.getCommissionFeeAmount() : BigDecimal.ZERO);
        pstmt.setBigDecimal(index++, entity.getBaseAmount() != null ? entity.getBaseAmount() : BigDecimal.ZERO);
        pstmt.setBigDecimal(index++, entity.getGrandTotal() != null ? entity.getGrandTotal() : BigDecimal.ZERO);
        pstmt.setString(index++, entity.getNotes() != null ? entity.getNotes() : "");
        pstmt.setString(index++, entity.getPaymentMode() != null ? entity.getPaymentMode() : "CHEQUE");
        pstmt.setBoolean(index++, entity.getAdvancePaid() != null && entity.getAdvancePaid());
        pstmt.setString(index++, entity.getStatus() != null ? entity.getStatus() : "UNPAID");
        return index;
    }

    @Override
    public Optional<PurchaseEntity> findById(Integer id) {
        String sql = "SELECT * FROM purchase_entries WHERE id = ? AND is_deleted = 0";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding purchase by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<PurchaseEntity> findAll() {
        List<PurchaseEntity> purchases = new ArrayList<>();
        String sql = "SELECT * FROM purchase_entries WHERE is_deleted = 0 ORDER BY entry_date DESC, id DESC";

        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                purchases.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all purchases: " + e.getMessage());
        }
        return purchases;
    }

    @Override
    public List<PurchaseEntity> findByDate(LocalDate date) {
        List<PurchaseEntity> purchases = new ArrayList<>();
        String sql = "SELECT * FROM purchase_entries WHERE DATE(entry_date) = ? AND is_deleted = 0 ORDER BY entry_date DESC, id DESC";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, date);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                purchases.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching purchases by date: " + e.getMessage());
        }
        return purchases;
    }

    @Override
    public List<PurchaseEntity> findByVendorId(Integer vendorId) {
        List<PurchaseEntity> purchases = new ArrayList<>();
        String sql = "SELECT * FROM purchase_entries WHERE vendor_id = ? AND is_deleted = 0 ORDER BY entry_date DESC, id DESC";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vendorId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                purchases.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching purchases by vendor: " + e.getMessage());
        }
        return purchases;
    }

    @Override
    public List<PurchaseEntity> findByDateRange(LocalDate startDate, LocalDate endDate) {
        List<PurchaseEntity> purchases = new ArrayList<>();
        String sql = "SELECT * FROM purchase_entries WHERE entry_date >= ? AND entry_date <= ? AND is_deleted = 0 ORDER BY entry_date DESC, id DESC";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, startDate);
            pstmt.setObject(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                purchases.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching purchases by date range: " + e.getMessage());
        }
        return purchases;
    }

    @Override
    public List<PurchaseEntity> findTodaysPurchases() {
        return findByDate(LocalDate.now());
    }

    @Override
    public List<PurchaseEntity> findThisMonthPurchases() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        return findByDateRange(startDate, endDate);
    }

    @Override
    public Integer getBagsCount(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COALESCE(SUM(bags), 0) as total_bags FROM purchase_entries " +
                "WHERE entry_date BETWEEN ? AND ? AND is_deleted = 0";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, startDate);
            pstmt.setObject(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total_bags");
            }
        } catch (SQLException e) {
            System.err.println("Error getting bags count: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public Double getTotalAmount(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COALESCE(SUM(grand_total), 0.0) as total_amount FROM purchase_entries " +
                "WHERE entry_date BETWEEN ? AND ? AND is_deleted = 0";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, startDate);
            pstmt.setObject(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total_amount");
            }
        } catch (SQLException e) {
            System.err.println("Error getting total amount: " + e.getMessage());
        }
        return 0.0;
    }

    @Override
    public Integer countPendingCheques(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COUNT(*) as pending_count FROM purchase_entries " +
                "WHERE UPPER(payment_mode) = 'CHEQUE' AND status != 'PAID' " +
                "AND entry_date BETWEEN ? AND ? AND is_deleted = 0";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, startDate);
            pstmt.setObject(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("pending_count");
            }
        } catch (SQLException e) {
            System.err.println("Error counting pending cheques: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public List<PurchaseEntity> findByVendorAndStatus(Integer vendorId, String status) {
        List<PurchaseEntity> purchases = new ArrayList<>();
        String sql = "SELECT * FROM purchase_entries " +
                "WHERE vendor_id = ? AND status = ? AND is_deleted = 0 " +
                "ORDER BY entry_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vendorId);
            pstmt.setString(2, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                purchases.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching purchases by vendor and status: " + e.getMessage());
        }
        return purchases;
    }

    @Override
    public Integer countChequesByClearingDate(LocalDate date) {
        String sql = "SELECT COUNT(*) as clearing_count FROM purchase_entries " +
                "WHERE UPPER(payment_mode) = 'CHEQUE' AND cheque_date = ? AND is_deleted = 0";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, date);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("clearing_count");
            }
        } catch (SQLException e) {
            System.err.println("Error counting clearing cheques: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public int getLastInsertedId() {
        String sql = "SELECT MAX(id) FROM purchase_entries";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void delete(Integer id) {
        String sql = "UPDATE purchase_entries SET is_deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error soft deleting purchase: " + e.getMessage());
            throw new RuntimeException("Failed to delete purchase", e);
        }
    }

    private PurchaseEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        return PurchaseEntity.builder()
                .id(rs.getInt("id"))
                .entryDate(rs.getObject("entry_date", LocalDate.class))
                .vendorId(rs.getInt("vendor_id"))
                .bags(rs.getInt("bags"))
                .rate(rs.getObject("rate", BigDecimal.class))
                .weightKg(rs.getObject("weight_kg", BigDecimal.class))
                .isLumpsum(rs.getBoolean("is_lumpsum"))
                .marketFeePercent(rs.getObject("market_fee_percent", BigDecimal.class))
                .commissionPercent(rs.getObject("commission_percent", BigDecimal.class))
                .marketFeeAmount(rs.getObject("market_fee_amount", BigDecimal.class))
                .commissionFeeAmount(rs.getObject("commission_amount", BigDecimal.class))
                .baseAmount(rs.getObject("base_amount", BigDecimal.class))
                .grandTotal(rs.getObject("grand_total", BigDecimal.class))
                .paymentMode(rs.getString("payment_mode"))
                .notes(rs.getString("notes"))
                .advancePaid(rs.getBoolean("advance_paid"))
                .status(rs.getString("status"))
                .createdAt(rs.getObject("created_at", LocalDateTime.class))
                .updatedAt(rs.getObject("updated_at", LocalDateTime.class))
                .chequeNumber(rs.getString("cheque_number"))
                .chequeDate(rs.getObject("cheque_date", LocalDate.class))
                .isDeleted(rs.getBoolean("is_deleted"))
                .build();
    }
}
