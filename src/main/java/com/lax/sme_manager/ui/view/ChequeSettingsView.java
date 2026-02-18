package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.repository.ChequeConfigRepository;
import com.lax.sme_manager.repository.model.ChequeConfig;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.util.IndianNumberToWords;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cheque Settings — Sidebar view for editing and saving cheque alignment.
 * Drag labels on a cheque template to align them, then Save.
 */
public class ChequeSettingsView extends VBox {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChequeSettingsView.class);

    private final ChequeConfigRepository configRepo;
    private ChequeConfig config;

    // Preview elements
    private Label[] lblDateDigits = new Label[8];
    private Label lblPayee;
    private Label lblAmountWords;
    private Label lblAmountDigits;
    private Pane chequePane;
    private ImageView bgView;
    private final List<javafx.scene.Node> draggableNodes = new ArrayList<>();

    // CTS-2010 Dimension: 202mm x 92mm
    private static final double PREVIEW_WIDTH_PX = 850;
    private static final double PREVIEW_HEIGHT_PX = PREVIEW_WIDTH_PX * (92.0 / 202.0);
    private static final double MM_TO_PX = PREVIEW_WIDTH_PX / 202.0;

    // Sample data for preview
    private static final String SAMPLE_PAYEE = "M/S Sample Vendor";
    private static final BigDecimal SAMPLE_AMOUNT = new BigDecimal("125000.00");
    private static final String SAMPLE_DATE = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"));

    public ChequeSettingsView() {
        this.configRepo = new ChequeConfigRepository();
        this.config = configRepo.getConfig();
        if (this.config == null) this.config = ChequeConfig.getFactoryDefaults();

        initializeUI();
    }

    private void initializeUI() {
        setSpacing(20);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: " + LaxTheme.Colors.LIGHT_GRAY + ";");

        // --- Title ---
        Label title = new Label("🖨️ Cheque Settings");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: " + LaxTheme.Colors.DARK_NAVY + ";");

        Label subtitle = new Label("Drag the labels on the cheque below to align them with your cheque leaf. Click Save when done.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-wrap-text: true;");
        subtitle.setWrapText(true);

        // --- Cheque Canvas ---
        StackPane canvasContainer = new StackPane();
        canvasContainer.setPadding(new Insets(10));
        canvasContainer.setStyle(
                "-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8;");

        chequePane = new Pane();
        chequePane.setPrefSize(PREVIEW_WIDTH_PX, PREVIEW_HEIGHT_PX);
        chequePane.setMaxSize(PREVIEW_WIDTH_PX, PREVIEW_HEIGHT_PX);

        // Background — cheque template
        try {
            Image bg = null;
            var is = getClass().getResourceAsStream("/images/standard_cheque.png");
            if (is != null) bg = new Image(is);
            if (bg == null) {
                File file = new File("src/main/resources/images/standard_cheque.png");
                if (file.exists()) bg = new Image(file.toURI().toString());
            }
            if (bg != null) {
                bgView = new ImageView(bg);
                bgView.setFitWidth(PREVIEW_WIDTH_PX);
                bgView.setFitHeight(PREVIEW_HEIGHT_PX);
                bgView.setPreserveRatio(false);
                chequePane.getChildren().add(bgView);
            }
        } catch (Exception e) {
            chequePane.setStyle("-fx-background-color: #f8fafc;");
        }

        addSampleElements();
        canvasContainer.getChildren().add(chequePane);

        // --- Coordinate Info Panel ---
        HBox coordPanel = createCoordPanel();

        // --- Buttons ---
        HBox buttonBar = new HBox(15);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        Button saveBtn = new Button("💾 Save Alignment");
        saveBtn.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.PRIMARY) + "; -fx-padding: 10 24;");
        saveBtn.setOnAction(e -> saveAlignment());

        Button resetBtn = new Button("🔄 Reset to Default");
        resetBtn.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY) + "; -fx-padding: 10 24;");
        resetBtn.setOnAction(e -> resetToDefaults());

        buttonBar.getChildren().addAll(saveBtn, resetBtn);

        // --- Layout ---
        VBox content = new VBox(16, title, subtitle, canvasContainer, coordPanel, buttonBar);
        content.setPadding(new Insets(0));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
    }

    private void addSampleElements() {
        // Date Digits
        String[] positions = config.getDatePositions() != null ? config.getDatePositions().split(";") : null;

        for (int i = 0; i < 8; i++) {
            double x, y;
            if (positions != null && i < positions.length) {
                String[] xy = positions[i].split(",");
                x = Double.parseDouble(xy[0]);
                y = Double.parseDouble(xy[1]);
            } else {
                x = (config.getDateX() > 0 ? config.getDateX() : 163) + (i * ChequeConfig.DATE_DIGIT_SPACING_MM);
                y = (config.getDateY() > 0 ? config.getDateY() : 6);
            }
            lblDateDigits[i] = createDraggableLabel(String.valueOf(SAMPLE_DATE.charAt(i)), x, y, "Date Digit " + (i + 1), 22);
            chequePane.getChildren().add(lblDateDigits[i]);
        }

        // Payee
        lblPayee = createDraggableLabel(SAMPLE_PAYEE,
                config.getPayeeX() > 0 ? config.getPayeeX() : 32,
                config.getPayeeY() > 0 ? config.getPayeeY() : 22,
                "Payee Name", 18);

        // Amount Words
        String words = IndianNumberToWords.convert(SAMPLE_AMOUNT);
        double awX = config.getAmountWordsX() > 0 ? config.getAmountWordsX() : 28;
        double awY = config.getAmountWordsY() > 0 ? config.getAmountWordsY() : 36;
        lblAmountWords = createDraggableLabel(words, awX, awY, "Amount Words", 14);
        lblAmountWords.setWrapText(true);
        double availW = Math.max(50, (197 - awX) * MM_TO_PX);
        lblAmountWords.setPrefWidth(availW);
        lblAmountWords.setMaxWidth(availW);
        lblAmountWords.setMinHeight(42);

        // Amount Digits
        String digits = String.format("**%.2f/-", SAMPLE_AMOUNT);
        lblAmountDigits = createDraggableLabel(digits,
                config.getAmountDigitsX() > 0 ? config.getAmountDigitsX() : 164,
                config.getAmountDigitsY() > 0 ? config.getAmountDigitsY() : 37,
                "Amount Digits", 18);

        chequePane.getChildren().addAll(lblPayee, lblAmountWords, lblAmountDigits);

        // A/C Payee stamp
        Label ac = new Label("A/C PAYEE");
        ac.setStyle("-fx-border-color: #1e293b; -fx-border-width: 1.5 0 1.5 0; -fx-padding: 4 12; -fx-font-weight: bold; -fx-rotate: -15; -fx-text-fill: #1e293b;");
        ac.setLayoutX(15 * MM_TO_PX);
        ac.setLayoutY(10 * MM_TO_PX);
        chequePane.getChildren().add(ac);
    }

    private Label createDraggableLabel(String text, double xMm, double yMm, String hint, double fontSize) {
        Label l = new Label(text);
        String baseStyle = "-fx-text-fill: #1e1b4b; -fx-font-family: 'Courier New'; -fx-font-weight: bold; "
                + "-fx-padding: 2 4; -fx-background-color: transparent; -fx-font-size: " + fontSize + "px;";
        l.setStyle(baseStyle);
        l.setFont(Font.font("Courier New", FontWeight.BOLD, fontSize));
        l.setTooltip(new Tooltip(hint + " — Drag to move"));
        l.setCursor(Cursor.MOVE);
        l.setLayoutX(xMm * MM_TO_PX);
        l.setLayoutY(yMm * MM_TO_PX);

        // Drag handlers
        final double[] delta = new double[2];
        l.setOnMousePressed(e -> { delta[0] = l.getLayoutX() - e.getSceneX(); delta[1] = l.getLayoutY() - e.getSceneY(); l.toFront(); });
        l.setOnMouseDragged(e -> { l.setLayoutX(e.getSceneX() + delta[0]); l.setLayoutY(e.getSceneY() + delta[1]); });

        // Hover highlight
        l.setOnMouseEntered(e -> l.setStyle(baseStyle + " -fx-border-color: #0d9488; -fx-border-width: 1; -fx-border-style: dashed; -fx-background-color: rgba(13,148,136,0.08);"));
        l.setOnMouseExited(e -> l.setStyle(baseStyle));

        draggableNodes.add(l);
        return l;
    }

    private HBox createCoordPanel() {
        HBox panel = new HBox(20);
        panel.setPadding(new Insets(12, 16, 12, 16));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");
        panel.setAlignment(Pos.CENTER_LEFT);

        Label info = new Label("📐 Current Coordinates (mm):");
        info.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        Label dateCoord = new Label();
        Label payeeCoord = new Label();
        Label wordsCoord = new Label();
        Label digitsCoord = new Label();

        String coordStyle = "-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-family: 'Courier New';";
        dateCoord.setStyle(coordStyle);
        payeeCoord.setStyle(coordStyle);
        wordsCoord.setStyle(coordStyle);
        digitsCoord.setStyle(coordStyle);

        // Live update coordinates on drag
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                dateCoord.setText(String.format("Date: %.0f,%.0f", lblDateDigits[0].getLayoutX() / MM_TO_PX, lblDateDigits[0].getLayoutY() / MM_TO_PX));
                payeeCoord.setText(String.format("Payee: %.0f,%.0f", lblPayee.getLayoutX() / MM_TO_PX, lblPayee.getLayoutY() / MM_TO_PX));
                wordsCoord.setText(String.format("Words: %.0f,%.0f", lblAmountWords.getLayoutX() / MM_TO_PX, lblAmountWords.getLayoutY() / MM_TO_PX));
                digitsCoord.setText(String.format("Digits: %.0f,%.0f", lblAmountDigits.getLayoutX() / MM_TO_PX, lblAmountDigits.getLayoutY() / MM_TO_PX));
            }
        };
        timer.start();

        panel.getChildren().addAll(info, dateCoord, payeeCoord, wordsCoord, digitsCoord);
        return panel;
    }

    private void saveAlignment() {
        // Read positions from UI
        config.setPayeeX(lblPayee.getLayoutX() / MM_TO_PX);
        config.setPayeeY(lblPayee.getLayoutY() / MM_TO_PX);
        config.setAmountWordsX(lblAmountWords.getLayoutX() / MM_TO_PX);
        config.setAmountWordsY(lblAmountWords.getLayoutY() / MM_TO_PX);
        config.setAmountDigitsX(lblAmountDigits.getLayoutX() / MM_TO_PX);
        config.setAmountDigitsY(lblAmountDigits.getLayoutY() / MM_TO_PX);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(String.format("%.2f,%.2f", lblDateDigits[i].getLayoutX() / MM_TO_PX, lblDateDigits[i].getLayoutY() / MM_TO_PX));
            if (i < 7) sb.append(";");
        }
        config.setDatePositions(sb.toString());
        config.setDateX(lblDateDigits[0].getLayoutX() / MM_TO_PX);
        config.setDateY(lblDateDigits[0].getLayoutY() / MM_TO_PX);

        configRepo.saveConfig(config);

        new Alert(Alert.AlertType.INFORMATION, "✅ Alignment saved! All future cheque prints will use these positions.").show();
    }

    private void resetToDefaults() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Reset all positions to factory defaults?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Reset Alignment");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                config = ChequeConfig.getFactoryDefaults();
                configRepo.saveConfig(config);

                // Reload UI
                chequePane.getChildren().clear();
                draggableNodes.clear();
                if (bgView != null) chequePane.getChildren().add(bgView);
                addSampleElements();

                new Alert(Alert.AlertType.INFORMATION, "🔄 Reset to factory defaults.").show();
            }
        });
    }
}
