package com.lax.sme_manager.repository;

import com.lax.sme_manager.repository.model.ChequeConfig;
import com.lax.sme_manager.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChequeConfigRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChequeConfigRepository.class);

    // We assume ID=1 is the single config for now
    public ChequeConfig getConfig() {
        String sql = "SELECT * FROM cheque_config WHERE id = 1";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                ChequeConfig cfg = mapResultSetToConfig(rs);
                // If all coordinates are zero, this is an empty row — use factory defaults
                if (cfg.getDateX() == 0 && cfg.getPayeeX() == 0 && cfg.getAmountWordsX() == 0) {
                    return ChequeConfig.getFactoryDefaults();
                }
                return cfg;
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching cheque config", e);
        }
        // No DB row → return Golden Coordinates
        return ChequeConfig.getFactoryDefaults();
    }

    /** Reset to factory Golden Coordinates */
    public void resetToFactory() {
        saveConfig(ChequeConfig.getFactoryDefaults());
    }

    public void saveConfig(ChequeConfig config) {
        String sql = """
                UPDATE cheque_config SET
                bank_name=?, is_ac_payee=?, font_size=?,
                date_x=?, date_y=?,
                payee_x=?, payee_y=?,
                amount_words_x=?, amount_words_y=?,
                amount_digits_x=?, amount_digits_y=?,
                signature_x=?, signature_y=?,
                signature_path=?, date_positions=?,
                active_signature_id=?
                WHERE id = 1
                """;

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, config.getBankName());
            pstmt.setBoolean(2, config.isAcPayee());
            pstmt.setInt(3, config.getFontSize());
            pstmt.setDouble(4, config.getDateX());
            pstmt.setDouble(5, config.getDateY());
            pstmt.setDouble(6, config.getPayeeX());
            pstmt.setDouble(7, config.getPayeeY());
            pstmt.setDouble(8, config.getAmountWordsX());
            pstmt.setDouble(9, config.getAmountWordsY());
            pstmt.setDouble(10, config.getAmountDigitsX());
            pstmt.setDouble(11, config.getAmountDigitsY());
            pstmt.setDouble(12, config.getSignatureX());
            pstmt.setDouble(13, config.getSignatureY());
            pstmt.setString(14, config.getSignaturePath());
            pstmt.setString(15, config.getDatePositions());
            pstmt.setInt(16, config.getActiveSignatureId());

            int rows = pstmt.executeUpdate();
            if (rows == 0) {
                // If ID 1 doesn't exist, insert it (Fallback/Init)
                insertDefaultConfig(config);
            }

        } catch (SQLException e) {
            LOGGER.error("Error saving cheque config", e);
        }
    }

    private void insertDefaultConfig(ChequeConfig config) {
        String sql = """
                INSERT INTO cheque_config (
                    id, bank_name, is_ac_payee, font_size,
                    date_x, date_y,
                    payee_x, payee_y,
                    amount_words_x, amount_words_y,
                    amount_digits_x, amount_digits_y,
                    signature_x, signature_y,
                    signature_path, date_positions,
                    active_signature_id
                ) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, config.getBankName());
            pstmt.setBoolean(2, config.isAcPayee());
            pstmt.setInt(3, config.getFontSize());
            pstmt.setDouble(4, config.getDateX());
            pstmt.setDouble(5, config.getDateY());
            pstmt.setDouble(6, config.getPayeeX());
            pstmt.setDouble(7, config.getPayeeY());
            pstmt.setDouble(8, config.getAmountWordsX());
            pstmt.setDouble(9, config.getAmountWordsY());
            pstmt.setDouble(10, config.getAmountDigitsX());
            pstmt.setDouble(11, config.getAmountDigitsY());
            pstmt.setDouble(12, config.getSignatureX());
            pstmt.setDouble(13, config.getSignatureY());
            pstmt.setString(14, config.getSignaturePath());
            pstmt.setString(15, config.getDatePositions());
            pstmt.setInt(16, config.getActiveSignatureId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error inserting default config", e);
        }
    }

    private ChequeConfig mapResultSetToConfig(ResultSet rs) throws SQLException {
        return ChequeConfig.builder()
                .id(rs.getInt("id"))
                .bankName(rs.getString("bank_name"))
                .isAcPayee(rs.getBoolean("is_ac_payee"))
                .fontSize(rs.getInt("font_size"))
                .activeSignatureId(rs.getInt("active_signature_id"))
                .dateX(rs.getDouble("date_x"))
                .dateY(rs.getDouble("date_y"))
                .payeeX(rs.getDouble("payee_x"))
                .payeeY(rs.getDouble("payee_y"))
                .amountWordsX(rs.getDouble("amount_words_x"))
                .amountWordsY(rs.getDouble("amount_words_y"))
                .amountDigitsX(rs.getDouble("amount_digits_x"))
                .amountDigitsY(rs.getDouble("amount_digits_y"))
                .signatureX(rs.getDouble("signature_x"))
                .signatureY(rs.getDouble("signature_y"))
                .signaturePath(rs.getString("signature_path"))
                .datePositions(rs.getString("date_positions"))
                .build();
    }
}
