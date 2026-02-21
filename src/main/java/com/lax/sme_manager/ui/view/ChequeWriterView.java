package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.dto.ChequeData;
import com.lax.sme_manager.ui.component.UIStyles;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.util.IndianNumberToWords;
import com.lax.sme_manager.repository.ChequeConfigRepository;
import com.lax.sme_manager.repository.SignatureRepository;
import com.lax.sme_manager.repository.model.ChequeConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import com.lax.sme_manager.repository.VendorRepository;
import com.lax.sme_manager.repository.model.VendorEntity;
import org.controlsfx.control.textfield.TextFields;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ChequeWriterView extends VBox {
    private final VendorRepository vendorRepository = new VendorRepository();
    private final ChequeConfigRepository configRepo = new ChequeConfigRepository();

    private Label previewDate;
    private Label previewPayee;
    private Label previewAmountWords;
    private Label previewAmountNumeric;
    private VBox acPayeeLine;
    private HBox numBox;
    private VBox sigBox;
    private ImageView sigImageView;
    private double mmToPx;

    private TextField payeeField;
    private TextField amountField;
    private DatePicker datePicker;
    private CheckBox acPayeeCheck;
    private Label amountWordsLabel;

    public ChequeWriterView() {
        setPadding(new Insets(24, 32, 24, 32));
        setSpacing(24);
        setStyle("-fx-background-color: #f8fafc;");

        // --- HERO HEADER ---
        VBox header = new VBox(4);
        Label title = new Label("Cheque Writer");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitle = new Label("Issue and print professional cheques with live preview");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");
        header.getChildren().addAll(title, subtitle);

        // --- MAIN CONTENT (SPLIT VIEW) ---
        HBox content = new HBox(40);
        content.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(content, Priority.ALWAYS);

        // LEFT: FORM
        VBox formPart = createFormPart();
        HBox.setHgrow(formPart, Priority.SOMETIMES);

        // RIGHT: LIVE PREVIEW
        VBox previewPart = createPreviewPart();
        HBox.setHgrow(previewPart, Priority.ALWAYS);

        content.getChildren().addAll(formPart, previewPart);

        // Fields Initialization (already done in createFormPart but mentioned for
        // clarity)
        payeeField = (TextField) findNodeById(formPart, "payeeField");
        amountField = (TextField) findNodeById(formPart, "amountField");
        datePicker = (DatePicker) findNodeById(formPart, "datePicker");
        acPayeeCheck = (CheckBox) findNodeById(formPart, "acPayeeCheck");
        amountWordsLabel = (Label) findNodeById(formPart, "amountWordsLabel");

        setupLiveSync();
        refreshLayout(); // Initial sync with DB

        getChildren().addAll(header, content);
    }

    public void refreshLayout() {
        ChequeConfig config = configRepo.getConfig();
        if (config == null)
            config = ChequeConfig.getFactoryDefaults();

        acPayeeLine.setLayoutX((config.getAcPayeeX() > 0 ? config.getAcPayeeX() : 31) * mmToPx);
        acPayeeLine.setLayoutY((config.getAcPayeeY() > 0 ? config.getAcPayeeY() : 14) * mmToPx);

        // The following lines are modified to apply date offsets correctly
        // Assuming previewDate is a single label, not individual digits.
        // The original code already applies offsets. The instruction implies a change.
        // The provided snippet is syntactically incomplete and seems to be for a
        // different date rendering approach.
        // Reverting to the original logic for applying offsets to the single
        // previewDate label.
        previewDate.setLayoutX(
                (config.getDateX() > 0 ? config.getDateX() : 159.79) * mmToPx + config.getDateOffsetX() * mmToPx);
        previewDate.setLayoutY(
                (config.getDateY() > 0 ? config.getDateY() : 9.04) * mmToPx + config.getDateOffsetY() * mmToPx);

        previewPayee.setLayoutX((config.getPayeeX() > 0 ? config.getPayeeX() : 37) * mmToPx);
        previewPayee.setLayoutY((config.getPayeeY() > 0 ? config.getPayeeY() : 21) * mmToPx);

        previewAmountWords.setLayoutX((config.getAmountWordsX() > 0 ? config.getAmountWordsX() : 37) * mmToPx);
        previewAmountWords.setLayoutY((config.getAmountWordsY() > 0 ? config.getAmountWordsY() : 30) * mmToPx);

        numBox.setLayoutX((config.getAmountDigitsX() > 0 ? config.getAmountDigitsX() : 163.55) * mmToPx);
        numBox.setLayoutY((config.getAmountDigitsY() > 0 ? config.getAmountDigitsY() : 37.78) * mmToPx);

        sigBox.setLayoutX((config.getSignatureX() > 0 ? config.getSignatureX() : 152.52) * mmToPx);
        sigBox.setLayoutY((config.getSignatureY() > 0 ? config.getSignatureY() : 48.13) * mmToPx);

        // Signature Image Refresh
        sigImageView.setImage(null);
        sigImageView.setVisible(false);

        SignatureRepository sigRepo = new SignatureRepository();
        com.lax.sme_manager.repository.model.SignatureConfig activeSig = config.getActiveSignatureId() > 0
                ? sigRepo.getSignatureById(config.getActiveSignatureId())
                : null;

        // Fallback: if no active signature, use first one for preview
        if (activeSig == null) {
            List<com.lax.sme_manager.repository.model.SignatureConfig> allSigs = sigRepo.getAllSignatures();
            if (!allSigs.isEmpty()) {
                activeSig = allSigs.get(0);
            }
        }

        if (activeSig != null && activeSig.getPath() != null) {
            try {
                java.io.File f = new java.io.File(activeSig.getPath());
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString());
                    sigImageView.setImage(img);
                    double sigW = 40 * mmToPx * (activeSig.getScale() > 0 ? activeSig.getScale() : 1.0);
                    sigImageView.setFitWidth(sigW);
                    sigImageView.setPreserveRatio(true);
                    sigImageView.setLayoutX((config.getSignatureX() > 0 ? config.getSignatureX() : 152.52) * mmToPx);
                    sigImageView.setLayoutY((config.getSignatureY() > 0 ? config.getSignatureY() : 48.13) * mmToPx);
                    sigImageView.setOpacity(activeSig.getOpacity());
                    sigImageView.setBlendMode(BlendMode.MULTIPLY);
                    sigImageView.setVisible(true);
                }
            } catch (Exception e) {
                // Silently fail for preview
            }
        }
    }

    private VBox createFormPart() {
        VBox form = new VBox(20);
        form.setPrefWidth(450);
        form.setPadding(new Insets(24));
        form.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); -fx-border-color: #E2E8F0; -fx-border-radius: 12;");

        Label sectionTitle = new Label("CHEQUE DETAILS");
        sectionTitle.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #94A3B8; -fx-letter-spacing: 0.1em;");

        VBox payeeBox = createLabeledInput("Payee Name", "payeeField", "Enter recipient name...");
        VBox amountBox = createLabeledInput("Amount (₹)", "amountField", "0.00");

        amountWordsLabel = new Label("");
        amountWordsLabel.setId("amountWordsLabel");
        amountWordsLabel
                .setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-style: italic; -fx-wrap-text: true;");
        amountWordsLabel.setPadding(new Insets(-10, 0, 0, 0));

        VBox dateBox = new VBox(8);
        Label dateLbl = new Label("Cheque Date");
        dateLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #475569; -fx-font-size: 13px;");
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setId("datePicker");
        datePicker.setPrefHeight(40);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        dateBox.getChildren().addAll(dateLbl, datePicker);

        acPayeeCheck = new CheckBox("A/C Pay(Crossed)");
        acPayeeCheck.setId("acPayeeCheck");
        acPayeeCheck.setSelected(true);
        acPayeeCheck.setStyle("-fx-font-weight: 600; -fx-text-fill: #1E293B;");

        HBox actionRow = new HBox(12);
        Button btnPrint = new Button("🖨️ Preview & Print");
        btnPrint.setPrefHeight(45);
        btnPrint.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnPrint, Priority.ALWAYS);
        btnPrint.setStyle(
                "-fx-background-color: #0D9488; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8; -fx-cursor: hand;");
        btnPrint.setOnAction(e -> handlePreview());

        Button btnClear = new Button("Clear");
        btnClear.setPrefHeight(45);
        btnClear.setPrefWidth(100);
        btnClear.setStyle(
                "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: 600; -fx-background-radius: 8; -fx-cursor: hand;");
        btnClear.setOnAction(e -> clearForm());

        actionRow.getChildren().addAll(btnPrint, btnClear);

        form.getChildren().addAll(sectionTitle, payeeBox, amountBox, amountWordsLabel, dateBox, acPayeeCheck,
                actionRow);
        return form;
    }

    private VBox createPreviewPart() {
        VBox preview = new VBox(12);
        preview.setAlignment(Pos.TOP_CENTER);

        Label previewTitle = new Label("LIVE VISUAL PREVIEW");
        previewTitle.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #94A3B8; -fx-letter-spacing: 0.1em;");

        // Cheque card — 203mm x 95mm proportional (synced with Settings & Preview)
        double cardW = 480;
        this.mmToPx = cardW / 219.0;
        double cardH = 95.0 * mmToPx;

        Pane chequePane = new Pane();
        chequePane.setPrefSize(cardW, cardH);
        chequePane.setMaxSize(cardW, cardH);
        chequePane.setStyle("-fx-background-color: #FCFDFE; -fx-background-radius: 4; -fx-border-color: #CBD5E1; "
                + "-fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 20, 0, 0, 10);");

        // A/C Payee stamp (28mm, 16mm — rotation -15° matches print)
        acPayeeLine = new VBox(0);
        acPayeeLine.setAlignment(Pos.CENTER);
        Label line1 = new Label("//");
        Label line2 = new Label("A/C PAY");
        Label line3 = new Label("//");
        line1.setStyle("-fx-font-size: 7px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        line2.setStyle(
                "-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-family: 'Courier New';");
        line3.setStyle("-fx-font-size: 7px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        acPayeeLine.getChildren().addAll(line1, line2, line3);
        acPayeeLine.setRotate(-15);
        acPayeeLine.setLayoutX(31 * mmToPx);
        acPayeeLine.setLayoutY(14 * mmToPx);

        // Date (160mm, 10mm)
        previewDate = new Label(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy")));
        previewDate.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-font-weight: bold; "
                + "-fx-letter-spacing: 3px;");
        previewDate.setLayoutX(159.79 * mmToPx);
        previewDate.setLayoutY(9.04 * mmToPx);

        // Payee (37mm, 21mm)
        previewPayee = new Label("-------------------------------");
        previewPayee.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-font-weight: bold;");
        previewPayee.setLayoutX(37 * mmToPx);
        previewPayee.setLayoutY(21 * mmToPx);
        previewPayee.setMaxWidth(110 * mmToPx);

        // Amount Words (37mm, 30mm)
        previewAmountWords = new Label("-------------------------------");
        previewAmountWords.setWrapText(true);
        previewAmountWords.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 9px; -fx-font-weight: bold;");
        previewAmountWords.setLayoutX(37 * mmToPx);
        previewAmountWords.setLayoutY(30 * mmToPx);
        previewAmountWords.setMaxWidth(110 * mmToPx);

        // Amount Numeric (163.5mm, 38.8mm)
        numBox = new HBox(3);
        numBox.setAlignment(Pos.CENTER);
        numBox.setStyle("-fx-border-color: #94A3B8; -fx-border-width: 1; -fx-padding: 2 4;");
        String rSymbolStr = "\u20B9";
        Label rSymbol = new Label(rSymbolStr);
        rSymbol.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        previewAmountNumeric = new Label("0.00");
        previewAmountNumeric.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-font-weight: bold;");
        numBox.getChildren().addAll(rSymbol, previewAmountNumeric);
        numBox.setLayoutX(163.55 * mmToPx);
        numBox.setLayoutY(37.78 * mmToPx);

        // Signature (152.52mm, 48.13mm)
        sigBox = new VBox(1);
        sigBox.setAlignment(Pos.CENTER);
        Region sigLine = new Region();
        sigLine.setPrefHeight(1);
        sigLine.setStyle("-fx-background-color: #94A3B8;");
        sigLine.setPrefWidth(80);
        Label authLbl = new Label("AUTHORISED SIGNATORY");
        authLbl.setStyle("-fx-font-size: 7px; -fx-font-weight: bold;");
        sigBox.getChildren().addAll(sigLine, authLbl);
        sigBox.setLayoutX(152.52 * mmToPx);
        sigBox.setLayoutY(48.13 * mmToPx);

        // Signature Image (Overlay over sigBox)
        sigImageView = new ImageView();
        sigImageView.setMouseTransparent(true); // Don't block clicks to elements behind if any
        sigImageView.setVisible(false);

        chequePane.getChildren().addAll(acPayeeLine, previewDate, previewPayee,
                previewAmountWords, numBox, sigBox, sigImageView);

        preview.getChildren().addAll(previewTitle, chequePane);
        return preview;
    }

    private VBox createLabeledInput(String labelText, String id, String prompt) {
        VBox box = new VBox(8);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #475569; -fx-font-size: 13px;");

        TextField field = new TextField();
        field.setId(id);
        field.setPromptText(prompt);
        field.setPrefHeight(40);
        field.setStyle(
                "-fx-background-color: #F8FAF7; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 12;");

        box.getChildren().addAll(lbl, field);
        return box;
    }

    private void setupLiveSync() {
        // High-Performance Payee Lookup (Section 2.2.1)
        List<VendorEntity> payees = vendorRepository.findAll();
        org.controlsfx.control.textfield.AutoCompletionBinding<VendorEntity> binding = TextFields
                .bindAutoCompletion(payeeField, payees);

        binding.setOnAutoCompleted(event -> {
            VendorEntity selection = event.getCompletion();
            if (selection != null) {
                payeeField.setText(selection.getName().toUpperCase());
                if (selection.getDefaultAmount() != null
                        && selection.getDefaultAmount().compareTo(BigDecimal.ZERO) > 0) {
                    amountField.setText(selection.getDefaultAmount().toPlainString());
                }
            }
        });

        payeeField.textProperty().addListener((o, old, n) -> {
            previewPayee.setText(n.isEmpty() ? "--------------------------------------------------" : n.toUpperCase());
        });

        amountField.textProperty().addListener((o, old, n) -> {
            updateAmountWords();
            previewAmountNumeric.setText(n.isEmpty() ? "0.00" : n);
            previewAmountWords
                    .setText(amountWordsLabel.getText().isEmpty() ? "--------------------------------------------------"
                            : amountWordsLabel.getText().toUpperCase() + " ONLY");
        });

        datePicker.valueProperty().addListener((o, old, n) -> {
            if (n != null) {
                previewDate.setText(n.format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy")));
            }
        });

        acPayeeLine.visibleProperty().bind(acPayeeCheck.selectedProperty());
    }

    private void updateAmountWords() {
        try {
            if (amountField.getText().isEmpty()) {
                amountWordsLabel.setText("");
                return;
            }
            BigDecimal amount = new BigDecimal(amountField.getText());
            amountWordsLabel.setText(IndianNumberToWords.convert(amount));
        } catch (NumberFormatException e) {
            amountWordsLabel.setText("Invalid Amount");
        }
    }

    private void handlePreview() {
        try {
            String payee = payeeField.getText();
            BigDecimal amount = new BigDecimal(amountField.getText());
            LocalDate date = datePicker.getValue();
            boolean isAcPayee = acPayeeCheck.isSelected();

            ChequeData data = new ChequeData(payee, amount, date, isAcPayee, null);
            new ChequePreviewDialog(data).show();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Please enter a valid amount.").show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
        }
    }

    private void clearForm() {
        payeeField.clear();
        amountField.clear();
        datePicker.setValue(LocalDate.now());
        amountWordsLabel.setText("");
    }

    private javafx.scene.Node findNodeById(javafx.scene.Parent root, String id) {
        if (id.equals(root.getId()))
            return root;
        if (root instanceof javafx.scene.layout.Pane) {
            for (javafx.scene.Node node : ((javafx.scene.layout.Pane) root).getChildren()) {
                if (id.equals(node.getId()))
                    return node;
                if (node instanceof javafx.scene.Parent) {
                    javafx.scene.Node found = findNodeById((javafx.scene.Parent) node, id);
                    if (found != null)
                        return found;
                }
            }
        }
        return null;
    }
}
