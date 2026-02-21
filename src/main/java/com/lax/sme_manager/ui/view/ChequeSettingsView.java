package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.repository.ChequeConfigRepository;
import com.lax.sme_manager.repository.SignatureRepository;
import com.lax.sme_manager.repository.model.ChequeConfig;
import com.lax.sme_manager.repository.model.SignatureConfig;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.util.IndianNumberToWords;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
    private final SignatureRepository sigRepo;
    private ChequeConfig config;
    private ChequeConfig savedConfig; // backup before reset
    private Runnable onSaveCallback;

    // Preview elements
    private Label[] lblDateDigits = new Label[8];
    private Label lblPayee;
    private Label lblAmountWords;
    private Label lblAmountDigits;
    private Label lblAcPayee;
    private ImageView sigPreview;
    private SignatureConfig activeSig;
    private Pane chequePane;
    private ImageView bgView;
    private final List<javafx.scene.Node> draggableNodes = new ArrayList<>();

    private Spinner<Double> offsetXSpinner;
    private Spinner<Double> offsetYSpinner;
    private Spinner<Double> dateOffsetXSpinner;
    private Spinner<Double> dateOffsetYSpinner;

    // Standard Indian Cheque: 203mm (+/- 1mm) x 95mm
    private static final double ACTUAL_WIDTH_MM = 210.0;
    private static final double ACTUAL_HEIGHT_MM = 95.0;

    // Force width to fit sidebar (850px), and let the multiplier derive from that
    // width
    private static final double PREVIEW_WIDTH_PX = 850;
    private static final double MM_TO_PX = PREVIEW_WIDTH_PX / ACTUAL_WIDTH_MM;
    private static final double PREVIEW_HEIGHT_PX = ACTUAL_HEIGHT_MM * MM_TO_PX;
    // Sample data for preview
    private static final String SAMPLE_PAYEE = "M/S Sample Vendor";
    private static final BigDecimal SAMPLE_AMOUNT = new BigDecimal("125000.00");
    private static final String SAMPLE_DATE = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"));

    public ChequeSettingsView() {
        this.configRepo = new ChequeConfigRepository();
        this.sigRepo = new SignatureRepository();
        this.config = configRepo.getConfig();
        if (this.config == null)
            this.config = ChequeConfig.getFactoryDefaults();

        initializeUI();
    }

    private void initializeUI() {
        setSpacing(20);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: " + LaxTheme.Colors.LIGHT_GRAY + ";");

        // --- Title ---
        Label title = new Label("🖨️ Cheque Settings");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: " + LaxTheme.Colors.DARK_NAVY + ";");

        Label subtitle = new Label(
                "Drag the labels on the cheque below to align them with your cheque leaf. Click Save when done.");
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
            String imagePath = "/images/standard_cheque.png";
            java.net.URL imageUrl = getClass().getResource(imagePath);
            Image bg = null;

            if (imageUrl != null) {
                // Method 1: Safely load from classpath (Best practice)
                bg = new Image(imageUrl.toExternalForm());
            } else {
                // Method 2: Fallback to your direct file path (Works during development)
                File file = new File("src/main/resources/images/standard_cheque.png");
                if (file.exists()) {
                    bg = new Image(file.toURI().toString());
                }
            }

            if (bg != null && !bg.isError()) {
                bgView = new ImageView(bg);
                bgView.setFitWidth(PREVIEW_WIDTH_PX);
                bgView.setFitHeight(PREVIEW_HEIGHT_PX);
                bgView.setPreserveRatio(false);
                bgView.setLayoutY(-12); // Tweak this number (e.g., -10, -15) until the lines hit the text
                bgView.setLayoutX(-5);
                chequePane.getChildren().add(bgView);
            } else {
                drawFallbackChequeTemplate();
            }
        } catch (Exception e) {
            drawFallbackChequeTemplate();
        }

        addSampleElements();
        canvasContainer.getChildren().add(chequePane);

        // --- Coordinate Info Panel ---
        HBox coordPanel = createCoordPanel();

        // --- Calibration Panel ---
        VBox calibrationPanel = createCalibrationPanel();

        // --- Buttons ---
        HBox buttonBar = new HBox(15);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        Button saveBtn = new Button("💾 Save Alignment");
        saveBtn.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.PRIMARY) + "; -fx-padding: 10 24;");
        saveBtn.setOnAction(e -> saveAlignment());

        Button resetBtn = new Button("🔄 Reset to Default");
        resetBtn.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY) + "; -fx-padding: 10 24;");
        resetBtn.setOnAction(e -> resetToDefaults());

        Button restoreBtn = new Button("♻️ Restore Previous");
        restoreBtn.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY) + "; -fx-padding: 10 24;");
        restoreBtn.setOnAction(e -> restorePrevious());

        Button helpBtn = new Button("❓ Feed Guide");
        helpBtn.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY) + "; -fx-padding: 10 24;");
        helpBtn.setOnAction(e -> showFeedGuide());

        buttonBar.getChildren().addAll(saveBtn, resetBtn, restoreBtn, helpBtn);

        // --- Layout ---
        VBox content = new VBox(16, title, subtitle, canvasContainer, coordPanel, calibrationPanel, buttonBar);
        content.setPadding(new Insets(0));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    private void drawFallbackChequeTemplate() {
        chequePane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #94a3b8; -fx-border-width: 1;");

        // Draw Date Boxes (DD MM YYYY) at standard position
        double startX = 159.79 * MM_TO_PX;
        double startY = 9.04 * MM_TO_PX;
        double boxSize = 5 * MM_TO_PX; // 5mm boxes

        HBox dateBoxes = new HBox(2); // 2px gap between boxes
        dateBoxes.setLayoutX(startX);
        dateBoxes.setLayoutY(startY);

        for (int i = 0; i < 8; i++) {
            Rectangle rect = new Rectangle(boxSize, boxSize);
            rect.setFill(Color.TRANSPARENT);
            rect.setStroke(Color.LIGHTGRAY);
            dateBoxes.getChildren().add(rect);

            // // Add extra space after DD and MM (i==1 and i==3)
            // if (i == 1 || i == 3) {
            // Rectangle spacer = new Rectangle(boxSize / 2, boxSize);
            // spacer.setFill(Color.TRANSPARENT);
            // dateBoxes.getChildren().add(spacer);
            // }
        }

        // "Pay" line at ~37mm X, 21mm Y
        Label payLabel = new Label("Pay _____________________________________________");
        payLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: lightgray; -fx-font-family: 'Courier New';");
        payLabel.setLayoutX(31 * MM_TO_PX);
        payLabel.setLayoutY(24 * MM_TO_PX);

        // "Amount" words line at ~37mm X, 30mm Y
        Label amountLabel = new Label("Rupees _________________________________________");
        amountLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;-fx-text-fill: lightgray; -fx-font-family: 'Courier New';");
        amountLabel.setLayoutX(31 * MM_TO_PX);
        amountLabel.setLayoutY(33 * MM_TO_PX);

        // Draw MICR Band (Bottom 19mm is blank white space)
        Rectangle micrBand = new Rectangle(PREVIEW_WIDTH_PX, 19 * MM_TO_PX);
        micrBand.setLayoutY(PREVIEW_HEIGHT_PX - (19 * MM_TO_PX));
        micrBand.setFill(Color.web("#f8fafc"));
        micrBand.setStroke(Color.LIGHTGRAY);

        // Draw Rupee Symbol Box at ~157mm X, 37mm Y
        Label rupeeSymbol = new Label("₹");
        rupeeSymbol.setStyle("-fx-border-color: lightgray; -fx-padding: 4; -fx-font-size: 14px;");
        rupeeSymbol.setLayoutX(160 * MM_TO_PX);
        rupeeSymbol.setLayoutY(37 * MM_TO_PX);
        rupeeSymbol.setPrefSize(35 * MM_TO_PX, 8 * MM_TO_PX);

        chequePane.getChildren().addAll(dateBoxes, payLabel, amountLabel, micrBand, rupeeSymbol);
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
                x = (config.getDateX() > 0 ? config.getDateX() : 159.79) + (i * ChequeConfig.DATE_DIGIT_SPACING_MM)
                        + config.getDateOffsetX();
                y = (config.getDateY() > 0 ? config.getDateY() : 9.04) + config.getDateOffsetY();
            }
            lblDateDigits[i] = createDraggableLabel(String.valueOf(SAMPLE_DATE.charAt(i)), x, y,
                    "Date Digit " + (i + 1), 18); // adjusted font size slightly for scale
            lblDateDigits[i].setMinWidth(ChequeConfig.DATE_DIGIT_SPACING_MM * MM_TO_PX);
            lblDateDigits[i].setAlignment(Pos.CENTER);
            chequePane.getChildren().add(lblDateDigits[i]);
        }

        // Payee
        lblPayee = createDraggableLabel(SAMPLE_PAYEE,
                config.getPayeeX() > 0 ? config.getPayeeX() : 37,
                config.getPayeeY() > 0 ? config.getPayeeY() : 21,
                "Payee Name", 14);

        // Amount Words
        String words = IndianNumberToWords.convert(SAMPLE_AMOUNT);
        double awX = config.getAmountWordsX() > 0 ? config.getAmountWordsX() : 37;
        double awY = config.getAmountWordsY() > 0 ? config.getAmountWordsY() : 30;
        lblAmountWords = createDraggableLabel(words, awX, awY, "Amount Words", 12);
        lblAmountWords.setWrapText(true);
        double availW = Math.max(50, (197 - awX) * MM_TO_PX);
        lblAmountWords.setPrefWidth(availW);
        lblAmountWords.setMaxWidth(availW);
        lblAmountWords.setMinHeight(30);

        // Amount Digits
        String digits = String.format("**%.2f/-", SAMPLE_AMOUNT);
        lblAmountDigits = createDraggableLabel(digits,
                config.getAmountDigitsX() > 0 ? config.getAmountDigitsX() : 163.55,
                config.getAmountDigitsY() > 0 ? config.getAmountDigitsY() : 37.78,
                "Amount Digits", 14);

        chequePane.getChildren().addAll(lblPayee, lblAmountWords, lblAmountDigits);

        // A/C Payee stamp
        double acX = config.getAcPayeeX() > 0 ? config.getAcPayeeX() : 31;
        double acY = config.getAcPayeeY() > 0 ? config.getAcPayeeY() : 14;
        lblAcPayee = createDraggableLabel("A/C PAY", acX, acY, "A/C Payee Stamp", 14);
        updateAcPayeeStyle(lblAcPayee, false);
        chequePane.getChildren().add(lblAcPayee);

        // Signature
        activeSig = config.getActiveSignatureId() > 0 ? sigRepo.getSignatureById(config.getActiveSignatureId()) : null;

        // Fallback: if no active signature, use the first one found in DB for alignment
        // purposes
        if (activeSig == null) {
            List<SignatureConfig> allSigs = sigRepo.getAllSignatures();
            if (!allSigs.isEmpty()) {
                activeSig = allSigs.get(0);
            }
        }

        if (activeSig != null && activeSig.getPath() != null) {
            try {
                File f = new File(activeSig.getPath());
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString());
                    sigPreview = new ImageView(img);
                    double sigW = 40 * MM_TO_PX * (activeSig.getScale() > 0 ? activeSig.getScale() : 1.0);
                    sigPreview.setFitWidth(sigW);
                    sigPreview.setPreserveRatio(true);
                    sigPreview.setLayoutX((config.getSignatureX() > 0 ? config.getSignatureX() : 152.52) * MM_TO_PX);
                    sigPreview.setLayoutY((config.getSignatureY() > 0 ? config.getSignatureY() : 48.13) * MM_TO_PX);
                    sigPreview.setOpacity(activeSig.getOpacity());
                    sigPreview.setBlendMode(BlendMode.MULTIPLY);
                    makeDraggable(sigPreview, "Signature");
                    chequePane.getChildren().add(sigPreview);
                }
            } catch (Exception e) {
                LOGGER.error("Error loading signature in settings", e);
            }
        }
    }

    private void updateAcPayeeStyle(Label l, boolean hover) {
        String base = "-fx-text-fill: #1e293b; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-font-size: 14px; "
                + "-fx-border-color: #1e293b; -fx-border-width: 1.5 0 1.5 0; -fx-padding: 4 12; -fx-rotate: -15; ";
        if (hover) {
            l.setStyle(base
                    + "-fx-background-color: rgba(13,148,136,0.1); -fx-border-color: #0d9488; -fx-border-style: dotted;");
        } else {
            l.setStyle(base + "-fx-background-color: transparent;");
        }
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
        l.setOnMousePressed(e -> {
            delta[0] = l.getLayoutX() - e.getSceneX();
            delta[1] = l.getLayoutY() - e.getSceneY();
            l.toFront();
        });
        l.setOnMouseDragged(e -> {
            l.setLayoutX(e.getSceneX() + delta[0]);
            l.setLayoutY(e.getSceneY() + delta[1]);
        });

        // Hover highlight
        l.setOnMouseEntered(e -> {
            if (l == lblAcPayee)
                updateAcPayeeStyle(l, true);
            else
                l.setStyle(baseStyle
                        + " -fx-border-color: #0d9488; -fx-border-width: 1; -fx-border-style: dashed; -fx-background-color: rgba(13,148,136,0.08);");
        });
        l.setOnMouseExited(e -> {
            if (l == lblAcPayee)
                updateAcPayeeStyle(l, false);
            else
                l.setStyle(baseStyle);
        });

        draggableNodes.add(l);
        return l;
    }

    private void makeDraggable(javafx.scene.Node n, String hint) {
        Tooltip.install(n, new Tooltip(hint + " — Drag to move"));
        n.setCursor(Cursor.MOVE);

        final double[] delta = new double[2];
        n.setOnMousePressed(e -> {
            delta[0] = n.getLayoutX() - e.getSceneX();
            delta[1] = n.getLayoutY() - e.getSceneY();
            n.toFront();
        });
        n.setOnMouseDragged(e -> {
            n.setLayoutX(e.getSceneX() + delta[0]);
            n.setLayoutY(e.getSceneY() + delta[1]);
        });

        if (n instanceof ImageView) {
            n.setOnMouseEntered(e -> n.setStyle("-fx-effect: dropshadow(three-pass-box, #0d9488, 10, 0.5, 0, 0);"));
            n.setOnMouseExited(e -> n.setStyle(""));
        }

        draggableNodes.add(n);
    }

    private HBox createCoordPanel() {
        HBox panel = new HBox(20);
        panel.setPadding(new Insets(12, 16, 12, 16));
        panel.setStyle(
                "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");
        panel.setAlignment(Pos.CENTER_LEFT);

        Label info = new Label("📐 Current Coordinates (mm):");
        info.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        Label dateCoord = new Label();
        Label payeeCoord = new Label();
        Label wordsCoord = new Label();
        Label digitsCoord = new Label();
        Label sigCoord = new Label();

        String coordStyle = "-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-family: 'Courier New';";
        dateCoord.setStyle(coordStyle);
        payeeCoord.setStyle(coordStyle);
        wordsCoord.setStyle(coordStyle);
        digitsCoord.setStyle(coordStyle);
        sigCoord.setStyle(coordStyle);

        // Optimized Property Listeners instead of AnimationTimer
        Runnable updateCoords = () -> {
            if (lblDateDigits[0] != null) {
                dateCoord.setText(String.format("Date: %.0f,%.0f", lblDateDigits[0].getLayoutX() / MM_TO_PX,
                        lblDateDigits[0].getLayoutY() / MM_TO_PX));
                payeeCoord.setText(String.format("Payee: %.0f,%.0f", lblPayee.getLayoutX() / MM_TO_PX,
                        lblPayee.getLayoutY() / MM_TO_PX));
                wordsCoord.setText(String.format("Words: %.0f,%.0f", lblAmountWords.getLayoutX() / MM_TO_PX,
                        lblAmountWords.getLayoutY() / MM_TO_PX));
                digitsCoord.setText(String.format("Digits: %.0f,%.0f", lblAmountDigits.getLayoutX() / MM_TO_PX,
                        lblAmountDigits.getLayoutY() / MM_TO_PX));
                if (sigPreview != null) {
                    sigCoord.setText(String.format("Sign: %.0f,%.0f", sigPreview.getLayoutX() / MM_TO_PX,
                            sigPreview.getLayoutY() / MM_TO_PX));
                } else {
                    sigCoord.setText("Sign: N/A");
                }
            }
        };

        // Fire once to set initial values
        updateCoords.run();

        // Attach listeners to update coordinates only when dragged
        for (javafx.scene.Node node : draggableNodes) {
            node.layoutXProperty().addListener((obs, oldVal, newVal) -> updateCoords.run());
            node.layoutYProperty().addListener((obs, oldVal, newVal) -> updateCoords.run());
        }

        panel.getChildren().addAll(info, dateCoord, payeeCoord, wordsCoord, digitsCoord, sigCoord);
        return panel;
    }

    private VBox createCalibrationPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16));
        panel.setStyle(
                "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label title = new Label("🎛️ Printer Calibration (Fine Tune Only)");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        Label help = new Label(
                "⚠️ Use SMALL offsets only (±1 to ±3mm). Large offsets will push text off the cheque edge. "
                        + "If your print is heavily misaligned, fix your printer's paper feed, not these numbers.");
        help.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
        help.setWrapText(true);

        offsetXSpinner = new Spinner<>(-5.0, 15.0, config.getOffsetX(), 0.5);
        offsetXSpinner.setEditable(true);
        offsetXSpinner.setPrefWidth(80);

        offsetYSpinner = new Spinner<>(-5.0, 15.0, config.getOffsetY(), 0.5);
        offsetYSpinner.setEditable(true);
        offsetYSpinner.setPrefWidth(80);

        HBox inputs = new HBox(15);
        inputs.setAlignment(Pos.CENTER_LEFT);

        inputs.getChildren().addAll(
                new Label("X Offset (mm):"), offsetXSpinner,
                new Label("Y Offset (mm):"), offsetYSpinner);

        Label dateTitle = new Label("📅 Date Calibration (Shift Entire Block)");
        dateTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155; -fx-padding: 10 0 0 0;");

        dateOffsetXSpinner = new Spinner<>(-10.0, 10.0, config.getDateOffsetX(), 0.1);
        dateOffsetXSpinner.setEditable(true);
        dateOffsetXSpinner.setPrefWidth(80);

        dateOffsetYSpinner = new Spinner<>(-10.0, 10.0, config.getDateOffsetY(), 0.1);
        dateOffsetYSpinner.setEditable(true);
        dateOffsetYSpinner.setPrefWidth(80);

        HBox dateInputs = new HBox(15);
        dateInputs.setAlignment(Pos.CENTER_LEFT);
        dateInputs.getChildren().addAll(
                new Label("Date X Offset (mm):"), dateOffsetXSpinner,
                new Label("Date Y Offset (mm):"), dateOffsetYSpinner);

        panel.getChildren().addAll(title, help, inputs, dateTitle, dateInputs);
        return panel;
    }

    private void saveAlignment() {
        config.setPayeeX(lblPayee.getLayoutX() / MM_TO_PX);
        config.setPayeeY(lblPayee.getLayoutY() / MM_TO_PX);
        config.setAmountWordsX(lblAmountWords.getLayoutX() / MM_TO_PX);
        config.setAmountWordsY(lblAmountWords.getLayoutY() / MM_TO_PX);
        config.setAmountDigitsX(lblAmountDigits.getLayoutX() / MM_TO_PX);
        config.setAmountDigitsY(lblAmountDigits.getLayoutY() / MM_TO_PX);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(String.format("%.2f,%.2f", lblDateDigits[i].getLayoutX() / MM_TO_PX,
                    lblDateDigits[i].getLayoutY() / MM_TO_PX));
            if (i < 7)
                sb.append(";");
        }
        config.setDatePositions(sb.toString());
        config.setDateX(lblDateDigits[0].getLayoutX() / MM_TO_PX);
        config.setDateY(lblDateDigits[0].getLayoutY() / MM_TO_PX);

        config.setAcPayeeX(lblAcPayee.getLayoutX() / MM_TO_PX);
        config.setAcPayeeY(lblAcPayee.getLayoutY() / MM_TO_PX);

        if (sigPreview != null) {
            config.setSignatureX(sigPreview.getLayoutX() / MM_TO_PX);
            config.setSignatureY(sigPreview.getLayoutY() / MM_TO_PX);
        }

        config.setOffsetX(offsetXSpinner.getValue());
        config.setOffsetY(offsetYSpinner.getValue());
        config.setDateOffsetX(dateOffsetXSpinner.getValue());
        config.setDateOffsetY(dateOffsetYSpinner.getValue());

        configRepo.saveConfig(config);

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        new Alert(Alert.AlertType.INFORMATION, "✅ Alignment saved! All future cheque prints will use these positions.")
                .show();
    }

    private void resetToDefaults() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Reset all positions to factory defaults?\n\n" +
                        "Your current settings will be backed up.\n" +
                        "Use ♻️ Restore Previous to undo this.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Reset Alignment");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                // Backup current config before resetting
                savedConfig = config;

                config = ChequeConfig.getFactoryDefaults();
                configRepo.saveConfig(config);

                offsetXSpinner.getValueFactory().setValue(0.0);
                offsetYSpinner.getValueFactory().setValue(0.0);

                rebuildPreview();

                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }

                new Alert(Alert.AlertType.INFORMATION,
                        "🔄 Reset to factory defaults.\nUse ♻️ Restore Previous to undo.").show();
            }
        });
    }

    private void restorePrevious() {
        if (savedConfig == null) {
            new Alert(Alert.AlertType.WARNING, "No previous settings to restore.\n\n" +
                    "This button works after you use Reset to Default.\n" +
                    "It restores whatever settings you had before the reset.").show();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Restore your previous cheque settings?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Restore Previous Settings");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                config = savedConfig;
                savedConfig = null;
                configRepo.saveConfig(config);

                offsetXSpinner.getValueFactory().setValue(config.getOffsetX());
                offsetYSpinner.getValueFactory().setValue(config.getOffsetY());

                rebuildPreview();

                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }

                new Alert(Alert.AlertType.INFORMATION, "♻️ Previous settings restored!").show();
            }
        });
    }

    private void rebuildPreview() {
        chequePane.getChildren().clear();
        draggableNodes.clear();
        sigPreview = null;
        if (bgView != null) {
            chequePane.getChildren().add(bgView);
        } else {
            drawFallbackChequeTemplate();
        }
        addSampleElements();

        for (javafx.scene.Node node : draggableNodes) {
            node.layoutXProperty().addListener((obs, oldVal, newVal) -> updateCoordLabels());
            node.layoutYProperty().addListener((obs, oldVal, newVal) -> updateCoordLabels());
        }
        updateCoordLabels();
    }

    // Helper method separated to allow calling from resetToDefaults cleanly
    private void updateCoordLabels() {
        // Find the coord panel labels. Since it's a bit rigid to dig through the scene
        // graph,
        // the property listeners are attached fresh in the reset logic.
    }

    private void showFeedGuide() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Printer Feed Guide");
        alert.setHeaderText("How to print a cheque correctly");
        alert.setContentText("CRITICAL PRINTER SETTINGS:\n\n" +
                "1. Scale: Set to '100%' or 'Actual Size' (NEVER 'Fit to Page')\n" +
                "2. Auto-Rotate: DISABLE in your print dialog\n" +
                "3. Feed Direction: Long edge first (Landscape)\n" +
                "4. Centering: Slide paper guides to center the cheque\n" +
                "5. Face: Usually Face Down for Laser printers\n\n" +
                "If text is slightly shifted, use X/Y Offset (±1 to ±3mm max).\n" +
                "If text is heavily misaligned, fix paper feed — NOT the offsets.");
        alert.show();
    }
}