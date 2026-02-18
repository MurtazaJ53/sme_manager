package com.lax.sme_manager.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChequeConfig {
    private int id;
    private String bankName;
    private boolean isAcPayee;
    private int fontSize;
    private int activeSignatureId;

    // Coordinates in mm (Millimeters)
    private double dateX;
    private double dateY;

    private double payeeX;
    private double payeeY;

    private double amountWordsX;
    private double amountWordsY;

    private double amountDigitsX;
    private double amountDigitsY;

    private double signatureX;
    private double signatureY;

    private String signaturePath; // Path to signature image

    /**
     * Stores individual digit positions as JSON or CSV
     * Format: "x,y;x,y;..." for 8 digits
     */
    private String datePositions;

    /** Standard CTS-2010 date digit spacing in mm */
    public static final double DATE_DIGIT_SPACING_MM = 6.2;

    /**
     * Golden Coordinates — Calibrated factory defaults for Indian CTS-2010 cheques.
     * These are the exact measurements from successful test prints.
     */
    public static ChequeConfig getFactoryDefaults() {
        ChequeConfig cfg = new ChequeConfig();
        cfg.setFontSize(12);
        cfg.setAcPayee(true);

        // Date: X=163mm, Y=6mm, Spacing=6.2mm
        cfg.setDateX(163);
        cfg.setDateY(6);

        // Build date positions for all 8 digits
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            double x = 163.0 + (i * DATE_DIGIT_SPACING_MM);
            sb.append(String.format("%.2f,%.2f", x, 6.0));
            if (i < 7) sb.append(";");
        }
        cfg.setDatePositions(sb.toString());

        // Payee Name: X=32mm, Y=22mm (+10mm right to clear "Pay" label)
        cfg.setPayeeX(32);
        cfg.setPayeeY(22);

        // Amount (Words): X=28mm, Y=36mm
        cfg.setAmountWordsX(28);
        cfg.setAmountWordsY(36);

        // Amount (Digits): X=164mm, Y=37mm (+8mm right to clear ₹ symbol)
        cfg.setAmountDigitsX(164);
        cfg.setAmountDigitsY(37);

        // Signature: default position
        cfg.setSignatureX(150);
        cfg.setSignatureY(65);

        return cfg;
    }
}
