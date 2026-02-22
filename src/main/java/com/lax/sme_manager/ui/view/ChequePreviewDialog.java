package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.dto.ChequeData;
import com.lax.sme_manager.repository.ChequeConfigRepository;
import com.lax.sme_manager.repository.model.ChequeConfig;
import com.lax.sme_manager.service.ChequePrintService;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.util.DatabaseManager;
import com.lax.sme_manager.util.IndianNumberToWords;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.effect.BlendMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lax.sme_manager.ui.component.AlertUtils;

/**
 * Clean Cheque Preview Dialog — Only for previewing and printing.
 * No editing here (use Cheque Settings in sidebar for that).
 */
public class ChequePreviewDialog extends Dialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChequePreviewDialog.class);
    private final ChequeData chequeData;
    private final ChequeConfigRepository configRepo;
    private final ChequePrintService printService;
    private final com.lax.sme_manager.repository.ChequeBookRepository bookRepo;

    // Use current saved config
    private ChequeConfig config;

    private Pane chequePane;
    private ImageView bgView;
    private com.lax.sme_manager.repository.SignatureRepository sigRepo;
    private com.lax.sme_manager.repository.model.SignatureConfig activeSig;

    private ComboBox<String> bankSelector;
    private ComboBox<com.lax.sme_manager.repository.model.ChequeBook> bookSelector;
    private TextField chequeNumberField;
    private Label warningLabel;
    private Button btnPrint;
    private Runnable onPrintComplete;

    // Standard Indian Cheque: 203mm x 95mm — sized to fit within dialog
    private static final double ACTUAL_WIDTH_MM = 203.0;
    private static final double ACTUAL_HEIGHT_MM = 95.0;
    private static final double PREVIEW_WIDTH_PX = 850;
    private static final double MM_TO_PX = PREVIEW_WIDTH_PX / ACTUAL_WIDTH_MM;
    private static final double PREVIEW_HEIGHT_PX = ACTUAL_HEIGHT_MM * MM_TO_PX;

    public ChequePreviewDialog(ChequeData chequeData) {
        this(chequeData, null);
    }

    public ChequePreviewDialog(ChequeData chequeData, Runnable onPrintComplete) {
        this.chequeData = chequeData;
        this.onPrintComplete = onPrintComplete;
        this.configRepo = new ChequeConfigRepository();
        this.sigRepo = new com.lax.sme_manager.repository.SignatureRepository();
        this.printService = new ChequePrintService();
        this.bookRepo = new com.lax.sme_manager.repository.ChequeBookRepository();

        // Load config from DB (or factory defaults if missing)
        this.config = configRepo.getConfig();
        if (this.config == null)
            this.config = ChequeConfig.getFactoryDefaults();

        if (this.config.getActiveSignatureId() > 0) {
            this.activeSig = sigRepo.getSignatureById(this.config.getActiveSignatureId());
        }

        // Fallback: if no active signature, use the first one from DB for preview
        if (this.activeSig == null) {
            java.util.List<com.lax.sme_manager.repository.model.SignatureConfig> allSigs = sigRepo.getAllSignatures();
            if (!allSigs.isEmpty()) {
                this.activeSig = allSigs.get(0);
            }
        }

        setTitle("Cheque Preview");
        initUI();
    }

    private void initUI() {
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().setPrefSize(950, 600);

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: white;");

        // --- Top Toolbar ---
        HBox topToolbar = new HBox(15);
        topToolbar.setAlignment(Pos.CENTER_LEFT);

        Label bankLbl = new Label("Bank Template:");
        bankLbl.setStyle("-fx-font-weight: bold;");

        bankSelector = new ComboBox<>();
        bankSelector.setEditable(false); // Read-only selection here
        bankSelector.setPromptText("Select Bank");
        bankSelector.setPrefWidth(200);
        loadBankTemplates();
        bankSelector.setOnAction(e -> handleBankSelection());

        Label bookLbl = new Label("Cheque Book:");
        bookLbl.setStyle("-fx-font-weight: bold;");

        bookSelector = new ComboBox<>();
        bookSelector.setPrefWidth(200);
        loadChequeBooks();

        Label chqLbl = new Label("Cheque No:");
        chqLbl.setStyle("-fx-font-weight: bold;");

        chequeNumberField = new TextField();
        chequeNumberField.setPromptText("Enter 6-digit number");
        chequeNumberField.setPrefWidth(120);

        if (chequeData.chequeNumber() != null && !chequeData.chequeNumber().isEmpty()) {
            chequeNumberField.setText(chequeData.chequeNumber());
            chequeNumberField.setEditable(false); // If passed in (like bulk print), lock it
        } else if (bookSelector.getValue() != null) {
            chequeNumberField.setText(String.format("%06d", bookSelector.getValue().getNextNumber()));
        }

        bookSelector.setOnAction(e -> {
            com.lax.sme_manager.repository.model.ChequeBook selected = bookSelector.getValue();
            if (selected != null && (chequeData.chequeNumber() == null || chequeData.chequeNumber().isEmpty())) {
                chequeNumberField.setText(String.format("%06d", selected.getNextNumber()));
                updateWarningLabel(selected);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topToolbar.getChildren().addAll(bankLbl, bankSelector, new Label("  |  "), bookLbl, bookSelector,
                new Label("  |  "), chqLbl, chequeNumberField, spacer);

        warningLabel = new Label();
        warningLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        if (bookSelector.getValue() != null) {
            updateWarningLabel(bookSelector.getValue());
        }

        VBox topSection = new VBox(5, topToolbar, warningLabel);
        topSection.setAlignment(Pos.CENTER_LEFT);

        // --- Cheque Canvas ---
        StackPane canvasContainer = new StackPane();
        canvasContainer.setPadding(new Insets(10));
        canvasContainer.setStyle(
                "-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8;");

        chequePane = new Pane();
        chequePane.setPrefSize(PREVIEW_WIDTH_PX, PREVIEW_HEIGHT_PX);
        chequePane.setMaxSize(PREVIEW_WIDTH_PX, PREVIEW_HEIGHT_PX);

        // Ghost Background logic
        try {
            Image bg = null;
            var is = getClass().getResourceAsStream("/images/standard_cheque.png");
            if (is != null)
                bg = new Image(is);
            if (bg == null) {
                File file = new File("src/main/resources/images/standard_cheque.png");
                if (file.exists())
                    bg = new Image(file.toURI().toString());
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

        renderChequeElements();
        canvasContainer.getChildren().add(chequePane);

        // --- Bottom Toolbar ---
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER);

        btnPrint = new Button("🖨️ Print Cheque");
        btnPrint.setStyle(
                LaxTheme.getButtonStyle(LaxTheme.ButtonType.PRIMARY) + "; -fx-padding: 8 24; -fx-font-size: 14px;");
        btnPrint.setOnAction(e -> print());

        // Update initial state of print button based on book selection
        if (bookSelector.getValue() != null) {
            btnPrint.setDisable(bookSelector.getValue().isExhausted());
        }

        toolbar.getChildren().addAll(btnPrint);

        root.getChildren().addAll(topSection, canvasContainer, toolbar);
        getDialogPane().setContent(root);

        if (config.getBankName() != null && !config.getBankName().isEmpty()) {
            bankSelector.getSelectionModel().select(config.getBankName());
        } else {
            bankSelector.getSelectionModel().select(0);
        }
    }

    private void renderChequeElements() {
        // Clear previous elements (except BG)
        chequePane.getChildren().removeIf(n -> n != bgView);

        String dStr = chequeData.date() != null ? chequeData.date().format(DateTimeFormatter.ofPattern("ddMMyyyy"))
                : "14022026";
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
            Label l = createLabel(String.valueOf(dStr.charAt(i)), x, y, 18);
            // Center in box
            l.setMinWidth(ChequeConfig.DATE_DIGIT_SPACING_MM * MM_TO_PX);
            l.setAlignment(Pos.CENTER);
            chequePane.getChildren().add(l);
        }

        chequePane.getChildren().add(createLabel(chequeData.payeeName(),
                config.getPayeeX() > 0 ? config.getPayeeX() : 37,
                config.getPayeeY() > 0 ? config.getPayeeY() : 21, 14));

        // Amount Words (auto-sized)
        String words = IndianNumberToWords.convert(chequeData.amount());
        double awX = config.getAmountWordsX() > 0 ? config.getAmountWordsX() : 37;
        Label lblWords = createLabel(words, awX, config.getAmountWordsY() > 0 ? config.getAmountWordsY() : 30, 12);
        double availW = Math.max(50, (197 - awX) * MM_TO_PX);
        lblWords.setPrefWidth(availW);
        lblWords.setMaxWidth(availW);
        lblWords.setWrapText(true);
        // Recalculate font size for fit
        double singleLine = availW / (words.length() * 0.6);
        double fSize = (singleLine >= 12) ? Math.min(singleLine, 16)
                : Math.min(Math.max((availW * 2) / (words.length() * 0.6), 8), 14);
        lblWords.setStyle("-fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-font-size: " + Math.round(fSize)
                + "px; -fx-text-fill: #1e1b4b; -fx-padding: 2 4;");
        lblWords.setFont(Font.font("Courier New", FontWeight.BOLD, fSize));
        chequePane.getChildren().add(lblWords);

        String digits = String.format("%.2f/-", chequeData.amount());
        chequePane.getChildren().add(createLabel(digits,
                config.getAmountDigitsX() > 0 ? config.getAmountDigitsX() : 163.55,
                config.getAmountDigitsY() > 0 ? config.getAmountDigitsY() : 37.78, 14));

        // Signature
        if (activeSig != null && activeSig.getPath() != null) {
            try {
                File f = new File(activeSig.getPath());
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString());
                    ImageView sigView = new ImageView(img);
                    double sigW = 40 * MM_TO_PX * activeSig.getScale();
                    sigView.setFitWidth(sigW);
                    sigView.setPreserveRatio(true);
                    sigView.setLayoutX((config.getSignatureX() > 0 ? config.getSignatureX() : 152.52) * MM_TO_PX);
                    sigView.setLayoutY((config.getSignatureY() > 0 ? config.getSignatureY() : 48.13) * MM_TO_PX);
                    sigView.setOpacity(activeSig.getOpacity());
                    sigView.setBlendMode(BlendMode.MULTIPLY);
                    chequePane.getChildren().add(sigView);
                }
            } catch (Exception e) {
                LOGGER.error("Error loading signature", e);
            }
        }

        if (chequeData.isAcPayee()) {
            double acX = config.getAcPayeeX() > 0 ? config.getAcPayeeX() : 31;
            double acY = config.getAcPayeeY() > 0 ? config.getAcPayeeY() : 14;
            Label ac = new Label("A/C PAY");
            ac.setStyle(
                    "-fx-border-color: #1e293b; -fx-border-width: 1.5 0 1.5 0; -fx-padding: 4 12; -fx-font-weight: bold; -fx-rotate: -15; -fx-text-fill: #1e293b; -fx-font-family: 'Courier New'; -fx-font-size: 14px;");
            ac.setLayoutX(acX * MM_TO_PX);
            ac.setLayoutY(acY * MM_TO_PX);
            chequePane.getChildren().add(ac);
        }
    }

    private Label createLabel(String text, double xMm, double yMm, double fontSize) {
        if (text == null)
            text = "";
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-font-size: " + fontSize
                + "px; -fx-text-fill: #1e1b4b; -fx-padding: 2 4;");
        l.setFont(Font.font("Courier New", FontWeight.BOLD, fontSize));
        l.setLayoutX(xMm * MM_TO_PX);
        l.setLayoutY(yMm * MM_TO_PX);
        return l;
    }

    private void loadBankTemplates() {
        List<String> banks = new ArrayList<>();
        String sql = "SELECT bank_name FROM bank_templates ORDER BY bank_name";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next())
                banks.add(rs.getString("bank_name"));
        } catch (SQLException e) {
            LOGGER.error("Failed to load bank templates", e);
        }
        bankSelector.getItems().setAll(banks);
    }

    private void handleBankSelection() {
        String selectedBank = bankSelector.getValue();
        if (selectedBank == null || selectedBank.isBlank())
            return;

        // Fetch new config from DB for this bank
        String sql = "SELECT * FROM bank_templates WHERE bank_name = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, selectedBank);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Update local config object
                config.setPayeeX(rs.getDouble("payee_x"));
                config.setPayeeY(rs.getDouble("payee_y"));
                config.setAmountWordsX(rs.getDouble("amount_words_x"));
                config.setAmountWordsY(rs.getDouble("amount_words_y"));
                config.setAmountDigitsX(rs.getDouble("amount_digits_x"));
                config.setAmountDigitsY(rs.getDouble("amount_digits_y"));
                config.setDateX(rs.getDouble("date_x"));
                config.setDateY(rs.getDouble("date_y"));
                // Generate positions from dateX/Y (simplified for preview switch)
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 8; i++) {
                    double x = config.getDateX() + (i * ChequeConfig.DATE_DIGIT_SPACING_MM);
                    sb.append(String.format("%.2f,%.2f", x, config.getDateY()));
                    if (i < 7)
                        sb.append(";");
                }
                config.setDatePositions(sb.toString());

                config.setSignatureX(rs.getDouble("signature_x"));
                config.setSignatureY(rs.getDouble("signature_y"));

                // Re-render
                renderChequeElements();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to load template", e);
        }
    }

    private void print() {
        String chqNo = chequeNumberField.getText().trim();
        if (chequeData.purchaseId() != null) {
            if (chqNo.isEmpty() || chqNo.length() < 6) {
                AlertUtils.showError("Error", "Please enter a valid Cheque Number.");
                return;
            }
            if (isChequeNumberDuplicate(chqNo)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                AlertUtils.styleDialog(alert);
                alert.setTitle("Security Warning");
                alert.setHeaderText("Duplicate Cheque Number");
                alert.setContentText("Cheque number " + chqNo + " has already been used.");
                alert.show();
                return;
            }
        }
        try {
            printService.printSilent(config, chequeData);

            // Increment the cheque book counter if a book is selected and we actually
            // printed
            if (bookSelector.getValue() != null
                    && (chequeData.chequeNumber() == null || chequeData.chequeNumber().isEmpty())) {
                // If it was a single standalone print that consumed a leaf
                bookRepo.consumeLeaves(bookSelector.getValue().getId(), 1);
            }

            if (chequeData.purchaseId() != null) {
                markPurchaseAsPaid(chequeData.purchaseId(), chqNo);
                AlertUtils.showInfo("Information", "Print successful! Purchase marked as PAID.");
            } else {
                AlertUtils.showInfo("Information", "Print job sent successfully.");
            }
            if (onPrintComplete != null)
                javafx.application.Platform.runLater(onPrintComplete);
            close();
        } catch (Exception e) {
            AlertUtils.showError("Error", "Printing Failed: " + e.getMessage());
        }
    }

    private void loadChequeBooks() {
        java.util.List<com.lax.sme_manager.repository.model.ChequeBook> books = bookRepo.getAllBooks();
        bookSelector.setItems(javafx.collections.FXCollections.observableArrayList(books));

        bookSelector.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(com.lax.sme_manager.repository.model.ChequeBook book) {
                if (book == null)
                    return "Select Book";
                return book.getBookName() + (book.isExhausted() ? " (EXHAUSTED)" : "");
            }

            @Override
            public com.lax.sme_manager.repository.model.ChequeBook fromString(String string) {
                return null;
            }
        });

        com.lax.sme_manager.repository.model.ChequeBook activeBook = bookRepo.getActiveBook();
        if (activeBook != null) {
            // Find the object instance in the list that matches the ID
            books.stream().filter(b -> b.getId() == activeBook.getId()).findFirst().ifPresent(b -> {
                bookSelector.setValue(b);
            });
        } else if (!books.isEmpty()) {
            bookSelector.setValue(books.get(0));
        }
    }

    private void updateWarningLabel(com.lax.sme_manager.repository.model.ChequeBook book) {
        if (book.isExhausted()) {
            warningLabel.setText("⚠️ This cheque book is EXHAUSTED. Please select a different book.");
            warningLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Red
            if (btnPrint != null)
                btnPrint.setDisable(true);
        } else if (book.getRemainingLeaves() <= 5) {
            warningLabel.setText("⚠️ Only " + book.getRemainingLeaves() + " leaves remaining in this book!");
            warningLabel.setStyle("-fx-text-fill: #eab308; -fx-font-weight: bold;"); // Yellow
            if (btnPrint != null)
                btnPrint.setDisable(false);
        } else {
            warningLabel.setText("");
            if (btnPrint != null)
                btnPrint.setDisable(false);
        }
    }

    private boolean isChequeNumberDuplicate(String chqNo) {
        String sql = "SELECT COUNT(*) FROM purchase_entries WHERE cheque_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chqNo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.error("Failed to check duplicate cheque", e);
        }
        return false;
    }

    private void markPurchaseAsPaid(int purchaseId, String chqNo) {
        String sql = "UPDATE purchase_entries SET status = 'PAID', cheque_number = ?, cheque_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chqNo);
            stmt.setObject(2, java.time.LocalDate.now());
            stmt.setInt(3, purchaseId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to auto-reconcile purchase", e);
            javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.WARNING,
                    "Cheque printed but failed to update status: " + e.getMessage()).show());
        }
    }
}
