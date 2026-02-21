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

    // Global offsets for printer calibration (in mm)
    private double offsetX;
    private double offsetY;

    // Element-specific calibration offsets (Date)
    private double dateOffsetX;
    private double dateOffsetY;

    private double acPayeeX;
    private double acPayeeY;

    // Orientation: "LANDSCAPE" (default) or "PORTRAIT"
    @Builder.Default
    private String printOrientation = "LANDSCAPE";

    public String getPrintOrientation() {
        return printOrientation != null ? printOrientation : "LANDSCAPE";
    }

    // MICR (Magnetic Ink Character Recognition) Settings
    private String micrCode; // e.g., "⑆123456⑈ 123456789⑉ 12"
    private double micrX; // X position in mm
    private double micrY; // Y position (Relative to BOTTOM if implemented as such, but here we stay
                          // consistent with Y=0 at TOP)

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
     * Cheque Size: 205mm x 95mm (20.5cm x 9.5cm)
     */
    public static ChequeConfig getFactoryDefaults() {
        ChequeConfig cfg = new ChequeConfig();
        cfg.setFontSize(12);
        cfg.setAcPayee(true);
        cfg.setAcPayeeX(31); // User-calibrated
        cfg.setAcPayeeY(14); // User-calibrated

        // Date: X=160mm, Y=10.0mm — user-calibrated
        cfg.setDateX(159.79);
        cfg.setDateY(9.04);

        // Build date positions for all 8 digits to match exact user calibration
        cfg.setDatePositions(
                "159.69,9.03;165.28,9.03;170.25,9.03;175.92,9.03;181.77,9.03;186.91,9.03;192.76,9.03;197.90,9.03");

        // Offsets: 0 by default — user can adjust per printer
        cfg.setOffsetX(0);
        cfg.setOffsetY(0);
        cfg.setPrintOrientation("LANDSCAPE");

        // Payee Name: X=37mm, Y=21mm — user-calibrated
        cfg.setPayeeX(37);
        cfg.setPayeeY(21);

        // Amount (Words): X=37mm, Y=30mm — user-calibrated
        cfg.setAmountWordsX(37);
        cfg.setAmountWordsY(30);

        // Amount (Digits): X=163.5mm, Y=38.8mm — user-calibrated
        cfg.setAmountDigitsX(163.55);
        cfg.setAmountDigitsY(37.78);

        cfg.setSignatureX(152.52);
        cfg.setSignatureY(48.13);

        // MICR Standards (Section 4.1): 3/16 inch from bottom
        // 3/16" = 4.76mm. Since height is 95mm, Y = 95 - 4.76 = 90.24mm roughly.
        // We set it to 88mm to be safe and centered in the band.
        cfg.setMicrCode("⑆000000⑈ 000000000⑉ 00");
        cfg.setMicrX(13); // Standard MICR starting position
        cfg.setMicrY(88); // Positioned near the "clear band" for 95mm height

        return cfg;
    }
}
