package com.lax.sme_manager.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ChequeImageGenerator {

    public static void main(String[] args) {
        // Cheque Size: 205mm x 95mm (20.5cm x 9.5cm)
        // Using 10 px/mm scale for high resolution
        int width = 2050;
        int height = 950;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Background (Light Blue/Teal with wave pattern)
        g2d.setColor(new Color(240, 253, 250)); // Very light teal
        g2d.fillRect(0, 0, width, height);

        // 2. Wave Pattern (Simple sine waves)
        g2d.setColor(new Color(204, 251, 241)); // Slightly darker teal
        g2d.setStroke(new BasicStroke(2));
        for (int y = 0; y < height; y += 40) {
            drawWave(g2d, y, width);
        }

        // 3. Border
        g2d.setColor(new Color(15, 118, 110)); // Teal 700
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(20, 20, width - 40, height - 40);

        // 4. Bank Name Placeholder (Top Left)
        g2d.setFont(new Font("Serif", Font.BOLD, 40));
        g2d.setColor(new Color(15, 118, 110));
        g2d.drawString("Standard Bank of India", 60, 80);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g2d.drawString("Any Branch, Any City - 110001", 60, 110);
        g2d.drawString("IFSC: SBIN0001234", 60, 135);

        // 5. CTS-2010 Watermark
        g2d.setFont(new Font("SansSerif", Font.BOLD, 30));
        g2d.setColor(new Color(200, 200, 200));
        g2d.rotate(Math.toRadians(-15), 300, 500);
        g2d.drawString("CTS-2010", 100, 600);
        g2d.rotate(Math.toRadians(15), 300, 500); // Reset rotation

        // 6. Date Box (Top Right)
        int dateX = 1530;
        int dateY = 50; // 5mm from top
        g2d.setColor(new Color(15, 118, 110));
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < 8; i++) {
            g2d.drawRect(dateX + (i * 62), dateY, 45, 50);
        }
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g2d.drawString("D     D     M     M     Y     Y     Y     Y", dateX + 15, dateY - 5);

        // 7. Payee Line (Y=22mm → 220px)
        g2d.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2d.drawString("Pay", 60, 260);
        g2d.drawLine(140, 260, 1900, 260);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g2d.drawString("Or Bearer", 1800, 220);

        // 8. Amount in Words Line (Y=36mm → 360px)
        g2d.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2d.drawString("Rupees", 60, 400);
        g2d.drawLine(200, 400, 1300, 400); // Line 1
        g2d.drawLine(60, 480, 1300, 480); // Line 2

        // 9. Amount Box (Right, Y=37mm → 370px)
        g2d.setColor(new Color(13, 148, 136)); // Teal 600
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(1450, 410, 500, 90);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 40));
        g2d.setColor(new Color(15, 118, 110));
        g2d.drawString("₹", 1470, 470);

        // 10. Account Number Box (Y=55mm → 550px)
        g2d.setColor(new Color(15, 118, 110));
        g2d.drawRect(250, 560, 500, 60);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString("A/c No.", 140, 600);

        // 11. Signature Area (Y=72mm → 720px)
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g2d.drawString("Please sign above", 1600, 750);

        g2d.dispose();

        try {
            File outputfile = new File("src/main/resources/images/standard_cheque.png");
            outputfile.getParentFile().mkdirs();
            ImageIO.write(image, "png", outputfile);
            System.out.println("Cheque image generated successfully: " + outputfile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void drawWave(Graphics2D g2d, int yOffset, int width) {
        Polygon p = new Polygon();
        p.addPoint(0, yOffset);
        for (int x = 0; x <= width; x += 50) {
            p.addPoint(x, yOffset + (x % 100 == 0 ? 10 : -10));
        }
        p.addPoint(width, yOffset);
        g2d.drawPolyline(p.xpoints, p.ypoints, p.npoints);
    }
}
