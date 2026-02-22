package com.lax.sme_manager.repository;

import com.lax.sme_manager.repository.model.ChequeBook;
import com.lax.sme_manager.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChequeBookRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChequeBookRepository.class);

    public List<ChequeBook> getAllBooks() {
        List<ChequeBook> books = new ArrayList<>();
        String sql = "SELECT * FROM cheque_books ORDER BY id DESC";

        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                books.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to fetch all cheque books", e);
        }
        return books;
    }

    public ChequeBook getActiveBook() {
        String sql = "SELECT * FROM cheque_books WHERE is_active = 1 LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to fetch active cheque book", e);
        }
        return null; // Return null if no active book exists yet
    }

    public ChequeBook getBookById(int id) {
        String sql = "SELECT * FROM cheque_books WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to fetch cheque book by ID", e);
        }
        return null;
    }

    public void saveBook(ChequeBook book) {
        // If we're setting this to active, deactivate others
        if (book.isActive()) {
            deactivateAll();
        }

        if (book.getId() == 0) {
            insertBook(book);
        } else {
            updateBook(book);
        }
    }

    private void insertBook(ChequeBook book) {
        String sql = "INSERT INTO cheque_books (book_name, bank_name, start_number, end_number, next_number, is_active) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, book.getBookName());
            pstmt.setString(2, book.getBankName());
            pstmt.setLong(3, book.getStartNumber());
            pstmt.setLong(4, book.getEndNumber());
            pstmt.setLong(5, book.getNextNumber());
            pstmt.setBoolean(6, book.isActive());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        book.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to insert cheque book", e);
        }
    }

    private void updateBook(ChequeBook book) {
        String sql = "UPDATE cheque_books SET book_name=?, bank_name=?, start_number=?, end_number=?, next_number=?, is_active=? WHERE id=?";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book.getBookName());
            pstmt.setString(2, book.getBankName());
            pstmt.setLong(3, book.getStartNumber());
            pstmt.setLong(4, book.getEndNumber());
            pstmt.setLong(5, book.getNextNumber());
            pstmt.setBoolean(6, book.isActive());
            pstmt.setInt(7, book.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to update cheque book", e);
        }
    }

    public void activateHook(int bookId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Transact to ensure consistency
            try {
                try (PreparedStatement unset = conn.prepareStatement("UPDATE cheque_books SET is_active = 0")) {
                    unset.executeUpdate();
                }
                try (PreparedStatement set = conn
                        .prepareStatement("UPDATE cheque_books SET is_active = 1 WHERE id = ?")) {
                    set.setInt(1, bookId);
                    set.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to set active cheque book", e);
        }
    }

    private void deactivateAll() {
        String sql = "UPDATE cheque_books SET is_active = 0";
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            LOGGER.error("Failed to deactivate all cheque books", e);
        }
    }

    public void deleteBook(int id) {
        String sql = "DELETE FROM cheque_books WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to delete cheque book", e);
        }
    }

    /**
     * Atomically advances the next_number and returns the previously available (now
     * consumed) number.
     * Use this when actually printing a cheque.
     *
     * @param bookId the ID of the book to bump
     * @param count  the number of leaves to consume
     * @return the first allocated cheque number, or -1 if the book couldn't fulfill
     *         it
     */
    public synchronized long consumeLeaves(int bookId, int count) {
        String logCheckSql = "SELECT leaf_number FROM cheque_usage_log WHERE book_id = ? AND leaf_number = ? AND status IN ('CANCELLED', 'VOID', 'MISPRINT')";
        String selectSql = "SELECT next_number, end_number FROM cheque_books WHERE id = ?";
        String updateNextSql = "UPDATE cheque_books SET next_number = ? WHERE id = ?";
        String insertLogSql = "INSERT INTO cheque_usage_log (book_id, leaf_number, status) VALUES (?, ?, 'PRINTED')";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long startNum = -1;
                long endNum = -1;

                // 1. Get current state
                try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                    pstmt.setInt(1, bookId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            startNum = rs.getLong("next_number");
                            endNum = rs.getLong("end_number");
                        }
                    }
                }

                if (startNum == -1 || startNum > endNum) {
                    conn.rollback();
                    return -1;
                }

                // 2. Resolve 'count' valid leaves, skipping holes
                java.util.List<Long> toConsume = new java.util.ArrayList<>();
                long candidate = startNum;

                while (toConsume.size() < count && candidate <= endNum) {
                    boolean isBlocked = false;
                    try (PreparedStatement checkStmt = conn.prepareStatement(logCheckSql)) {
                        checkStmt.setInt(1, bookId);
                        checkStmt.setLong(2, candidate);
                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next())
                                isBlocked = true;
                        }
                    }

                    if (!isBlocked) {
                        toConsume.add(candidate);
                    }
                    candidate++;
                }

                if (toConsume.size() < count) {
                    conn.rollback();
                    return -1; // Not enough leaves left in this book
                }

                // 3. Mark them as PRINTED and update next_number to candidate
                try (PreparedStatement logStmt = conn.prepareStatement(insertLogSql)) {
                    for (long num : toConsume) {
                        logStmt.setInt(1, bookId);
                        logStmt.setLong(2, num);
                        logStmt.addBatch();
                    }
                    logStmt.executeBatch();
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(updateNextSql)) {
                    updateStmt.setLong(1, candidate);
                    updateStmt.setInt(2, bookId);
                    updateStmt.executeUpdate();
                }

                conn.commit();
                return toConsume.get(0); // Return first in batch

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to consume cheque leaves with skip logic", e);
            return -1;
        }
    }

    public boolean markLeafStatus(int bookId, long leafNumber, String status, String remarks) {
        String sql = "INSERT INTO cheque_usage_log (book_id, leaf_number, status, remarks) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(book_id, leaf_number) DO UPDATE SET status=excluded.status, remarks=excluded.remarks";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            pstmt.setLong(2, leafNumber);
            pstmt.setString(3, status);
            pstmt.setString(4, remarks);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Failed to mark leaf status", e);
            return false;
        }
    }

    public List<Long> getLeavesWithStatus(int bookId, List<String> statuses) {
        List<Long> leaves = new ArrayList<>();
        if (statuses == null || statuses.isEmpty())
            return leaves;

        StringBuilder sb = new StringBuilder(
                "SELECT leaf_number FROM cheque_usage_log WHERE book_id = ? AND status IN (");
        for (int i = 0; i < statuses.size(); i++) {
            sb.append("?").append(i == statuses.size() - 1 ? "" : ",");
        }
        sb.append(") ORDER BY leaf_number ASC");

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
            pstmt.setInt(1, bookId);
            for (int i = 0; i < statuses.size(); i++) {
                pstmt.setString(i + 2, statuses.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    leaves.add(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to fetch leaves with statuses", e);
        }
        return leaves;
    }

    private ChequeBook mapResultSet(ResultSet rs) throws SQLException {
        // Handle potential null or timestamp parsing issues safely
        String createdAtStr = rs.getString("created_at");
        LocalDateTime createdAt = null;
        if (createdAtStr != null && !createdAtStr.isEmpty()) {
            try {
                // SQLite uses 'YYYY-MM-DD HH:MM:SS' format typically
                createdAtStr = createdAtStr.replace(" ", "T");
                createdAt = LocalDateTime.parse(createdAtStr);
            } catch (Exception e) {
                // Ignore parse errors from DB timestamps if inconsistent
            }
        }

        return ChequeBook.builder()
                .id(rs.getInt("id"))
                .bookName(rs.getString("book_name"))
                .bankName(rs.getString("bank_name"))
                .startNumber(rs.getLong("start_number"))
                .endNumber(rs.getLong("end_number"))
                .nextNumber(rs.getLong("next_number"))
                .isActive(rs.getBoolean("is_active"))
                .createdAt(createdAt)
                .build();
    }
}
