package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.dto.ChequeData;
import com.lax.sme_manager.ui.component.UIStyles;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.util.IndianNumberToWords;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ChequeWriterView extends VBox {

    private Label previewDate;
    private Label previewPayee;
    private Label previewAmountWords;
    private Label previewAmountNumeric;
    private VBox acPayeeLine;

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

        // Fields Initialization (already done in createFormPart but mentioned for clarity)
        payeeField = (TextField) findNodeById(formPart, "payeeField");
        amountField = (TextField) findNodeById(formPart, "amountField");
        datePicker = (DatePicker) findNodeById(formPart, "datePicker");
        acPayeeCheck = (CheckBox) findNodeById(formPart, "acPayeeCheck");
        amountWordsLabel = (Label) findNodeById(formPart, "amountWordsLabel");

        setupLiveSync();

        getChildren().addAll(header, content);
    }

    private VBox createFormPart() {
        VBox form = new VBox(20);
        form.setPrefWidth(450);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); -fx-border-color: #E2E8F0; -fx-border-radius: 12;");

        Label sectionTitle = new Label("CHEQUE DETAILS");
        sectionTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #94A3B8; -fx-letter-spacing: 0.1em;");

        VBox payeeBox = createLabeledInput("Payee Name", "payeeField", "Enter recipient name...");
        VBox amountBox = createLabeledInput("Amount (₹)", "amountField", "0.00");
        
        amountWordsLabel = new Label("");
        amountWordsLabel.setId("amountWordsLabel");
        amountWordsLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-style: italic; -fx-wrap-text: true;");
        amountWordsLabel.setPadding(new Insets(-10, 0, 0, 0));

        VBox dateBox = new VBox(8);
        Label dateLbl = new Label("Cheque Date");
        dateLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #475569; -fx-font-size: 13px;");
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setId("datePicker");
        datePicker.setPrefHeight(40);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        dateBox.getChildren().addAll(dateLbl, datePicker);

        acPayeeCheck = new CheckBox("A/C Payee Only (Crossed)");
        acPayeeCheck.setId("acPayeeCheck");
        acPayeeCheck.setSelected(true);
        acPayeeCheck.setStyle("-fx-font-weight: 600; -fx-text-fill: #1E293B;");

        HBox actionRow = new HBox(12);
        Button btnPrint = new Button("🖨️ Preview & Print");
        btnPrint.setPrefHeight(45);
        btnPrint.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnPrint, Priority.ALWAYS);
        btnPrint.setStyle("-fx-background-color: #0D9488; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8; -fx-cursor: hand;");
        btnPrint.setOnAction(e -> handlePreview());

        Button btnClear = new Button("Clear");
        btnClear.setPrefHeight(45);
        btnClear.setPrefWidth(100);
        btnClear.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: 600; -fx-background-radius: 8; -fx-cursor: hand;");
        btnClear.setOnAction(e -> clearForm());

        actionRow.getChildren().addAll(btnPrint, btnClear);

        form.getChildren().addAll(sectionTitle, payeeBox, amountBox, amountWordsLabel, dateBox, acPayeeCheck, actionRow);
        return form;
    }

    private VBox createPreviewPart() {
        VBox preview = new VBox(20);
        preview.setAlignment(Pos.TOP_CENTER);

        Label previewTitle = new Label("LIVE VISUAL PREVIEW");
        previewTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #94A3B8; -fx-letter-spacing: 0.1em;");

        // The Cheque Card
        StackPane chequeCard = new StackPane();
        chequeCard.setPrefSize(700, 320);
        chequeCard.setMaxSize(700, 320);
        chequeCard.setStyle("-fx-background-color: #FCFDFE; -fx-background-radius: 4; -fx-border-color: #CBD5E1; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 20, 0, 0, 10);");

        AnchorPane chequeLayer = new AnchorPane();
        
        // A/C Payee crossing
        acPayeeLine = new VBox(2);
        acPayeeLine.setAlignment(Pos.CENTER);
        Label line1 = new Label("------------------");
        Label line2 = new Label("A/C PAYEE ONLY");
        Label line3 = new Label("------------------");
        line1.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        line2.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        line3.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        acPayeeLine.getChildren().addAll(line1, line2, line3);
        acPayeeLine.setRotate(-45);
        AnchorPane.setTopAnchor(acPayeeLine, 10.0);
        AnchorPane.setLeftAnchor(acPayeeLine, -10.0);

        // Date
        VBox dateBox = new VBox(2);
        dateBox.setAlignment(Pos.CENTER);
        Label dateLbl = new Label("DATE");
        dateLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        previewDate = new Label(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        previewDate.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 18px; -fx-font-weight: bold; -fx-letter-spacing: 2px; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0; -fx-padding: 0 5 0 5;");
        dateBox.getChildren().addAll(dateLbl, previewDate);
        AnchorPane.setTopAnchor(dateBox, 20.0);
        AnchorPane.setRightAnchor(dateBox, 30.0);

        // Payee
        HBox payeeBox = new HBox(10);
        payeeBox.setAlignment(Pos.BOTTOM_LEFT);
        Label payLbl = new Label("PAY");
        payLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        previewPayee = new Label("--------------------------------------------------");
        previewPayee.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 20px; -fx-font-weight: bold; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
        HBox.setHgrow(previewPayee, Priority.ALWAYS);
        payeeBox.getChildren().addAll(payLbl, previewPayee);
        AnchorPane.setTopAnchor(payeeBox, 80.0);
        AnchorPane.setLeftAnchor(payeeBox, 30.0);
        AnchorPane.setRightAnchor(payeeBox, 30.0);

        // Words
        VBox wordsBox = new VBox(5);
        Label wordsLbl = new Label("RUPEES");
        wordsLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        previewAmountWords = new Label("----------------------------------------------------------------------------------");
        previewAmountWords.setWrapText(true);
        previewAmountWords.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
        wordsBox.getChildren().addAll(wordsLbl, previewAmountWords);
        AnchorPane.setTopAnchor(wordsBox, 130.0);
        AnchorPane.setLeftAnchor(wordsBox, 30.0);
        AnchorPane.setRightAnchor(wordsBox, 200.0);

        // Numeric Amount
        HBox numBox = new HBox(5);
        numBox.setAlignment(Pos.CENTER);
        numBox.setPrefSize(160, 50);
        numBox.setStyle("-fx-border-color: #94A3B8; -fx-border-width: 2; -fx-padding: 5;");
        Label rSymbol = new Label("₹");
        rSymbol.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        previewAmountNumeric = new Label("0.00");
        previewAmountNumeric.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 22px; -fx-font-weight: bold;");
        numBox.getChildren().addAll(rSymbol, previewAmountNumeric);
        AnchorPane.setTopAnchor(numBox, 140.0);
        AnchorPane.setRightAnchor(numBox, 30.0);

        // Signature Area
        VBox sigBox = new VBox(2);
        sigBox.setAlignment(Pos.CENTER);
        Label authLbl = new Label("AUTHORISED SIGNATORY");
        authLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        Region sigLine = new Region();
        sigLine.setPrefHeight(1);
        sigLine.setStyle("-fx-background-color: #94A3B8;");
        sigLine.setPrefWidth(150);
        sigBox.getChildren().addAll(sigLine, authLbl);
        AnchorPane.setBottomAnchor(sigBox, 30.0);
        AnchorPane.setRightAnchor(sigBox, 30.0);

        chequeLayer.getChildren().addAll(acPayeeLine, dateBox, payeeBox, wordsBox, numBox, sigBox);
        chequeCard.getChildren().add(chequeLayer);

        preview.getChildren().addAll(previewTitle, chequeCard);
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
        field.setStyle("-fx-background-color: #F8FAF7; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 12;");
        
        box.getChildren().addAll(lbl, field);
        return box;
    }

    private void setupLiveSync() {
        payeeField.textProperty().addListener((o, old, n) -> {
            previewPayee.setText(n.isEmpty() ? "--------------------------------------------------" : n.toUpperCase());
        });

        amountField.textProperty().addListener((o, old, n) -> {
            updateAmountWords();
            previewAmountNumeric.setText(n.isEmpty() ? "0.00" : n);
            previewAmountWords.setText(amountWordsLabel.getText().isEmpty() ? "--------------------------------------------------" : amountWordsLabel.getText().toUpperCase() + " ONLY");
        });

        datePicker.valueProperty().addListener((o, old, n) -> {
            if (n != null) {
                previewDate.setText(n.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
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
        if (id.equals(root.getId())) return root;
        if (root instanceof javafx.scene.layout.Pane) {
            for (javafx.scene.Node node : ((javafx.scene.layout.Pane) root).getChildren()) {
                if (id.equals(node.getId())) return node;
                if (node instanceof javafx.scene.Parent) {
                    javafx.scene.Node found = findNodeById((javafx.scene.Parent) node, id);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }
}
