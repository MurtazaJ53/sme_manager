package com.lax.sme_manager.service;

import com.lax.sme_manager.dto.ChequeData;
import com.lax.sme_manager.repository.model.ChequeConfig;
import com.lax.sme_manager.util.IndianNumberToWords;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ChequePrintService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChequePrintService.class);

    // 1 mm = 2.83465 points
    private static final float MM_TO_POINTS = 2.83465f;

    // Standard Indian Cheque: 203mm x 95mm — synced with ChequeSettingsView
    private static final float CHEQUE_WIDTH_POINTS = 206f * MM_TO_POINTS;
    private static final float CHEQUE_HEIGHT_POINTS = 98f * MM_TO_POINTS;

    public void printSilent(ChequeConfig config, ChequeData data) throws Exception {
        printBatch(config, java.util.List.of(data));
    }

    public void printBatch(ChequeConfig config, java.util.List<ChequeData> batchData) throws Exception {
        if (batchData == null || batchData.isEmpty())
            return;

        try (PDDocument document = new PDDocument()) {
            for (ChequeData data : batchData) {
                PDRectangle pageSize = new PDRectangle(CHEQUE_WIDTH_POINTS, CHEQUE_HEIGHT_POINTS);
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    drawChequeContent(document, contentStream, config, data);
                }
            }

            // Send to Printer — EXACT SIZE, NO ROTATION, NO SCALING
            PrinterJob job = PrinterJob.getPrinterJob();
            java.awt.print.PageFormat pf = job.defaultPage();

            // Set Paper to exact CTS-2010 cheque dimensions
            java.awt.print.Paper paper = new java.awt.print.Paper();
            paper.setSize(CHEQUE_WIDTH_POINTS, CHEQUE_HEIGHT_POINTS);
            paper.setImageableArea(0, 0, CHEQUE_WIDTH_POINTS, CHEQUE_HEIGHT_POINTS);
            pf.setPaper(paper);
            pf.setOrientation(java.awt.print.PageFormat.LANDSCAPE);

            // LANDSCAPE orientation = no rotation. false = no shrink-to-fit.
            job.setPageable(new PDFPageable(document, org.apache.pdfbox.printing.Orientation.LANDSCAPE, false, 0));
            PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultService != null) {
                job.setPrintService(defaultService);
                job.print();
                LOGGER.info("Batch print job sent for {} cheques", batchData.size());
            } else {
                throw new RuntimeException("No default printer found.");
            }
        }
    }

    private void drawChequeContent(PDDocument document, PDPageContentStream contentStream, ChequeConfig config,
            ChequeData data) throws IOException {
        // Standardize Font to Courier (Monospaced)
        contentStream.setFont(PDType1Font.COURIER_BOLD, config.getFontSize() > 0 ? config.getFontSize() : 12);

        // 1. Date
        try {
            // Default to today if null (matches Preview behavior)
            java.time.LocalDate d = data.date() != null ? data.date() : java.time.LocalDate.now();
            String dateStr = d.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
            String[] posArray = config.getDatePositions() != null ? config.getDatePositions().split(";") : null;

            for (int i = 0; i < dateStr.length(); i++) {
                float x, y;
                if (posArray != null && i < posArray.length) {
                    String[] xy = posArray[i].split(",");
                    x = (float) ((Float.parseFloat(xy[0]) + config.getOffsetX()) * MM_TO_POINTS);
                    y = CHEQUE_HEIGHT_POINTS - (float) ((Float.parseFloat(xy[1]) + config.getOffsetY()) * MM_TO_POINTS);
                } else {
                    x = (float) ((config.getDateX() + (i * ChequeConfig.DATE_DIGIT_SPACING_MM) + config.getOffsetX())
                            * MM_TO_POINTS);
                    y = CHEQUE_HEIGHT_POINTS - (float) ((config.getDateY() + config.getOffsetY()) * MM_TO_POINTS);
                }
                float charOffset = 1.8f * MM_TO_POINTS;
                streamDrawText(contentStream, String.valueOf(dateStr.charAt(i)), x + charOffset, y);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to print date", e);
        }

        // 2. Payee
        try {
            if (data.payeeName() != null) {
                drawText(contentStream, data.payeeName(), config.getPayeeX(), config.getPayeeY(), config);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to print payee", e);
        }

        // 3 & 4. Amounts
        try {
            String amountWords = IndianNumberToWords.convert(data.amount());
            drawText(contentStream, amountWords, config.getAmountWordsX(), config.getAmountWordsY(), config);
            String amountDigits = String.format("**%.2f/-", data.amount());
            drawText(contentStream, amountDigits, config.getAmountDigitsX(), config.getAmountDigitsY(), config);
        } catch (Exception e) {
            LOGGER.error("Failed to print amount", e);
        }

        // 5. AC Payee
        if (data.isAcPayee()) {
            try {
                double acX = config.getAcPayeeX() > 0 ? config.getAcPayeeX() : 31;
                double acY = config.getAcPayeeY() > 0 ? config.getAcPayeeY() : 14;
                drawAcPayee(contentStream, acX, acY, config);
            } catch (Exception e) {
                LOGGER.error("Failed to print AC Pay", e);
            }
        }

        // 6. Signature
        try {
            var sigRepo = new com.lax.sme_manager.repository.SignatureRepository();
            com.lax.sme_manager.repository.model.SignatureConfig sigCfg = config.getActiveSignatureId() > 0
                    ? sigRepo.getSignatureById(config.getActiveSignatureId())
                    : null;

            // Fallback: if no active signature, use the first one from DB
            if (sigCfg == null) {
                var allSigs = sigRepo.getAllSignatures();
                if (!allSigs.isEmpty()) {
                    sigCfg = allSigs.get(0);
                }
            }

            String sigPath = (sigCfg != null) ? sigCfg.getPath() : config.getSignaturePath();

            if (sigPath != null && !sigPath.isEmpty()) {
                PDImageXObject pdImage = PDImageXObject.createFromFile(sigPath, document);
                float scale = (sigCfg != null) ? (float) sigCfg.getScale() : 1.0f;
                float baseWidthMm = 40f;
                float sigWidth = baseWidthMm * scale * MM_TO_POINTS;
                float sigHeight = (pdImage.getHeight() / (float) pdImage.getWidth()) * sigWidth;

                org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState gs = new org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState();
                gs.setBlendMode(org.apache.pdfbox.pdmodel.graphics.blend.BlendMode.MULTIPLY);
                if (sigCfg != null)
                    gs.setNonStrokingAlphaConstant((float) sigCfg.getOpacity());
                contentStream.setGraphicsStateParameters(gs);

                drawImage(contentStream, pdImage, config.getSignatureX(), config.getSignatureY(), sigWidth, sigHeight,
                        config);
                contentStream.setGraphicsStateParameters(
                        new org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to print signature", e);
        }

        // 7. MICR Line
        try {
            if (config.getMicrCode() != null && !config.getMicrCode().isEmpty()) {
                drawMicrLine(contentStream, config.getMicrX(), config.getMicrY(), config.getMicrCode(), config);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to print MICR line", e);
        }
    }

    private void drawAcPayee(PDPageContentStream stream, double xMm, double yMm, ChequeConfig config)
            throws IOException {
        float x = (float) ((xMm + config.getOffsetX()) * MM_TO_POINTS);
        float y = CHEQUE_HEIGHT_POINTS - (float) ((yMm + config.getOffsetY()) * MM_TO_POINTS);

        stream.saveGraphicsState();
        stream.transform(org.apache.pdfbox.util.Matrix.getTranslateInstance(x, y));
        stream.transform(org.apache.pdfbox.util.Matrix.getRotateInstance(Math.toRadians(15), 0, 0));

        stream.setLineWidth(1.2f);
        stream.setStrokingColor(0, 0, 0);
        // Shorter lines (80pt) keep rotated stamp within page bounds
        stream.moveTo(0, 10);
        stream.lineTo(80, 10);
        stream.stroke();
        stream.moveTo(0, -4);
        stream.lineTo(80, -4);
        stream.stroke();

        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
        stream.newLineAtOffset(8, 0);
        stream.showText("A/C PAY");
        stream.endText();

        stream.restoreGraphicsState();
    }

    private void drawMicrLine(PDPageContentStream stream, double xMm, double yMm, String micr, ChequeConfig config)
            throws IOException {
        float x = (float) ((xMm + config.getOffsetX()) * MM_TO_POINTS);
        float yFromBottom = (float) (95.0 - yMm);
        if (yFromBottom < 4.0 || yFromBottom > 15.0) { // MICR clear band: 4-15mm from bottom (95mm height)
            yFromBottom = 4.76f;
        }
        float y = yFromBottom * MM_TO_POINTS;

        stream.beginText();
        stream.setFont(PDType1Font.COURIER_BOLD, 12);
        stream.newLineAtOffset(x, y);
        stream.showText(micr);
        stream.endText();
    }

    private void drawText(PDPageContentStream stream, String text, double xMm, double yMm, ChequeConfig config)
            throws IOException {
        if (text == null || text.isEmpty())
            return;
        float x = (float) ((xMm + config.getOffsetX()) * MM_TO_POINTS);
        float y = CHEQUE_HEIGHT_POINTS - (float) ((yMm + config.getOffsetY()) * MM_TO_POINTS);
        streamDrawText(stream, text, x, y);
    }

    private void streamDrawText(PDPageContentStream stream, String text, float x, float y) throws IOException {
        stream.beginText();
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private void drawImage(PDPageContentStream stream, PDImageXObject image, double xMm, double yMm, float widthPoints,
            float heightPoints, ChequeConfig config) throws IOException {
        float x = (float) ((xMm + config.getOffsetX()) * MM_TO_POINTS);
        float y = CHEQUE_HEIGHT_POINTS - (float) ((yMm + config.getOffsetY()) * MM_TO_POINTS) - heightPoints;
        stream.drawImage(image, x, y, widthPoints, heightPoints);
    }
}
