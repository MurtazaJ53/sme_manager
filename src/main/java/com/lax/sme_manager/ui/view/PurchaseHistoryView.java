package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.domain.Vendor;
import com.lax.sme_manager.repository.VendorRepository;
import com.lax.sme_manager.repository.model.PurchaseEntity;
import com.lax.sme_manager.service.ExportService;
import com.lax.sme_manager.ui.component.UIStyles;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.util.i18n.AppLabel;
import com.lax.sme_manager.util.NumericTextFormatter;
import com.lax.sme_manager.viewmodel.PurchaseHistoryViewModel;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class PurchaseHistoryView extends VBox implements RefreshableView {
    private static final Logger LOGGER = LoggerFactory.getLogger(PurchaseHistoryView.class);
    private final PurchaseHistoryViewModel viewModel;
    private final VendorRepository vendorRepository;
    private final ExportService exportService;
    private Consumer<PurchaseEntity> onPurchaseSelected;
    private Consumer<PurchaseEntity> onPurchaseEdit;
    private TableView<PurchaseEntity> purchaseTable;

    public PurchaseHistoryView(PurchaseHistoryViewModel viewModel, VendorRepository vendorRepository) {
        this.viewModel = viewModel;
        this.vendorRepository = vendorRepository;
        this.exportService = new ExportService(vendorRepository);
        initializeUI();
        viewModel.loadPurchases();
    }

    public void setOnPurchaseSelected(Consumer<PurchaseEntity> listener) {
        this.onPurchaseSelected = listener;
    }

    public void setOnPurchaseEdit(Consumer<PurchaseEntity> listener) {
        this.onPurchaseEdit = listener;
    }

    private void initializeUI() {
        setPadding(new Insets(24, 32, 24, 32));
        setSpacing(24);
        setStyle("-fx-background-color: #f8fafc;");

        // --- TOP HEADER ---
        HBox topHeader = new HBox(20);
        topHeader.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Purchase History");
        titleLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitleLbl = new Label("Manage and track all business transactions");
        subtitleLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");
        titleBox.getChildren().addAll(titleLbl, subtitleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- BULK ACTIONS (Dynamic visibility) ---
        HBox bulkActions = new HBox(12);
        bulkActions.setAlignment(Pos.CENTER_LEFT);
        
        Button btnBatchPrint = new Button("🖨️ Batch Print");
        btnBatchPrint.setStyle("-fx-background-color: #0D9488; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnBatchPrint.setOnAction(e -> handleBatchPrint());

        Button btnDeleteSelected = new Button("Delete Selected");
        btnDeleteSelected.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-font-weight: 700; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: #FCA5A5; -fx-border-radius: 8;");
        btnDeleteSelected.setOnAction(e -> handleBulkDelete());

        // Bind visibility to selection
        javafx.beans.binding.BooleanBinding hasSelection = javafx.beans.binding.Bindings.isNotEmpty(viewModel.selectedPurchases);
        bulkActions.visibleProperty().bind(hasSelection);
        bulkActions.managedProperty().bind(hasSelection);
        bulkActions.getChildren().addAll(btnBatchPrint, btnDeleteSelected);

        // Search Container
        HBox searchContainer = new HBox(10);
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.setPadding(new Insets(0, 16, 0, 16));
        searchContainer.setPrefHeight(45);
        searchContainer.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-border-width: 1;");
        
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
        
        TextField searchInput = new TextField();
        searchInput.setPromptText("Search by vendor or cheque...");
        searchInput.setPrefWidth(220);
        searchInput.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-text-fill: #1E293B; -fx-font-size: 13px;");
        searchInput.textProperty().bindBidirectional(viewModel.getFilterState().searchQuery);
        searchContainer.getChildren().addAll(searchIcon, searchInput);

        Button exportBtn = new Button("Export Excel");
        exportBtn.setPrefHeight(45);
        exportBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: 700; -fx-padding: 0 20; -fx-background-radius: 8; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-cursor: hand;");
        exportBtn.setOnAction(e -> handleExport());

        topHeader.getChildren().addAll(titleBox, spacer, bulkActions, searchContainer, exportBtn);

        // --- FILTER CARD ---
        VBox filterCard = createExpertFilterCard();

        // --- TABLE ---
        purchaseTable = createTable();
        VBox.setVgrow(purchaseTable, Priority.ALWAYS);
        purchaseTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // --- FOOTER ---
        HBox footer = createUnifiedFooter();

        getChildren().addAll(topHeader, filterCard, purchaseTable, footer);
    }

    private HBox createUnifiedFooter() {
        HBox footer = new HBox(24);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 16, 8, 16));
        footer.setStyle(UIStyles.getCardStyle() + "; -fx-background-color: #f8fafc;");

        Label lblBags = new Label("Total Bags: 0");
        lblBags.setStyle("-fx-font-weight: bold;");
        Label lblAmount = new Label("Total Amount: ₹0.00");
        lblAmount.setStyle("-fx-font-weight: bold; -fx-text-fill: " + LaxTheme.Colors.PRIMARY_TEAL + ";");

        viewModel.purchaseList.addListener((javafx.collections.ListChangeListener<PurchaseEntity>) c -> {
            int totalBags = viewModel.purchaseList.stream().mapToInt(PurchaseEntity::getBags).sum();
            double totalAmount = viewModel.purchaseList.stream()
                    .mapToDouble(p -> p.getGrandTotal() != null ? p.getGrandTotal().doubleValue() : 0.0).sum();
            lblBags.setText("Total Bags: " + totalBags);
            lblAmount.setText("Total Amount: ₹" + String.format("%,.2f", totalAmount));
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox pagination = new HBox(12);
        pagination.setAlignment(Pos.CENTER);
        Button prev = new Button("←");
        prev.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY));
        prev.setOnAction(e -> viewModel.prevPage());

        Label pageLabel = new Label();
        pageLabel.textProperty().bind(viewModel.paginationLabel);

        Button next = new Button("→");
        next.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY));
        next.setOnAction(e -> viewModel.nextPage());
        pagination.getChildren().addAll(prev, pageLabel, next);

        footer.getChildren().addAll(lblBags, lblAmount, spacer, pagination);
        return footer;
    }

    private VBox createExpertFilterCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 10, 0, 0, 4); -fx-border-color: #F1F5F9; -fx-border-width: 1; -fx-border-radius: 12;");

        // --- ROW 1: PRIMARY FILTERS ---
        HBox row1 = new HBox(20);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label filterLbl = new Label("LIVE FILTERS:");
        filterLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: 800; -fx-font-size: 11px; -fx-letter-spacing: 0.1em;");

        // Date Selectors
        HBox datePair = new HBox(10);
        datePair.setAlignment(Pos.CENTER_LEFT);
        
        DatePicker startP = new DatePicker();
        startP.setPrefWidth(140);
        startP.setPromptText("Start Date");
        startP.valueProperty().bindBidirectional(viewModel.getFilterState().filterStartDate);
        
        Label toText = new Label("to");
        toText.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        
        DatePicker endP = new DatePicker();
        endP.setPrefWidth(140);
        endP.setPromptText("End Date");
        endP.valueProperty().bindBidirectional(viewModel.getFilterState().filterEndDate);
        
        datePair.getChildren().addAll(startP, toText, endP);

        // Quick Presets
        HBox presetsGrp = new HBox(6);
        String pStyle = "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 6 12; -fx-background-radius: 4; -fx-cursor: hand;";
        
        Button bToday = new Button("Today"); bToday.setStyle(pStyle);
        bToday.setOnAction(e -> viewModel.getFilterState().applyPresetToday());
        
        Button bYesterday = new Button("Yesterday"); bYesterday.setStyle(pStyle);
        bYesterday.setOnAction(e -> viewModel.getFilterState().applyPresetYesterday());
        
        Button bMonth = new Button("This Month"); bMonth.setStyle(pStyle);
        bMonth.setOnAction(e -> viewModel.getFilterState().applyPresetLastMonth());
        
        presetsGrp.getChildren().addAll(bToday, bYesterday, bMonth);

        Region row1Spacer = new Region();
        HBox.setHgrow(row1Spacer, Priority.ALWAYS);

        Button resetBtn = new Button("Reset All");
        resetBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> viewModel.resetFilters());

        row1.getChildren().addAll(filterLbl, datePair, presetsGrp, row1Spacer, resetBtn);

        // --- ROW 2: VENDOR & AMOUNT ---
        HBox row2 = new HBox(30);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.setPadding(new Insets(5, 0, 0, 0));

        // Vendor Selector (Improved with in-context "Select All/Clear")
        VBox vBox = new VBox(4);
        Label vLbl = new Label("Vendor Selection:");
        vLbl.setStyle("-fx-text-fill: #64748B; -fx-font-weight: 600; -fx-font-size: 11px;");
        
        org.controlsfx.control.CheckComboBox<Vendor> vCombo = new org.controlsfx.control.CheckComboBox<>();
        vCombo.setPrefWidth(250);
        vCombo.getItems().addAll(vendorRepository.findAllVendors());
        vCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Vendor v) { return v == null ? "All Vendors" : v.getName(); }
            @Override public Vendor fromString(String s) { return null; }
        });
        vCombo.getCheckModel().getCheckedItems().addListener((javafx.collections.ListChangeListener<Vendor>) c -> {
            List<Integer> ids = vCombo.getCheckModel().getCheckedItems().stream().map(Vendor::getId).toList();
            if (!ids.equals(viewModel.getFilterState().filterVendorIds)) {
                viewModel.getFilterState().filterVendorIds.setAll(ids);
            }
        });

        // State -> UI Sync (Fix for Reset/Presets)
        viewModel.getFilterState().filterVendorIds.addListener((javafx.collections.ListChangeListener<Integer>) c -> {
            List<Integer> currentCheckedIds = vCombo.getCheckModel().getCheckedItems().stream().map(Vendor::getId).toList();
            if (!currentCheckedIds.equals(viewModel.getFilterState().filterVendorIds)) {
                Platform.runLater(() -> {
                    vCombo.getCheckModel().clearChecks();
                    for (Vendor v : vCombo.getItems()) {
                        if (viewModel.getFilterState().filterVendorIds.contains(v.getId())) {
                            vCombo.getCheckModel().check(v);
                        }
                    }
                });
            }
        });

        // "Select All" / "Clear" inside the dropdown context
        ContextMenu vCtx = new ContextMenu();
        MenuItem mAll = new MenuItem("✓ Select All Vendors");
        mAll.setOnAction(e -> vCombo.getCheckModel().checkAll());
        MenuItem mNone = new MenuItem("✕ Clear Selection");
        mNone.setOnAction(e -> vCombo.getCheckModel().clearChecks());
        vCtx.getItems().addAll(mAll, mNone);
        vCombo.setContextMenu(vCtx);
        Tooltip.install(vCombo, new Tooltip("Right-click for Select All / Clear"));

        vBox.getChildren().addAll(vLbl, vCombo);

        // Amount Range
        VBox aBox = new VBox(4);
        Label aLbl = new Label("Amount Range (₹):");
        aLbl.setStyle("-fx-text-fill: #64748B; -fx-font-weight: 600; -fx-font-size: 11px;");
        
        HBox aFields = new HBox(8);
        aFields.setAlignment(Pos.CENTER_LEFT);
        TextField minF = new TextField(); minF.setPromptText("Min"); minF.setPrefWidth(100);
        minF.setTextFormatter(NumericTextFormatter.decimalOnly(2));
        minF.textProperty().addListener((o, old, n) -> {
            try {
                BigDecimal val = n.isEmpty() ? BigDecimal.ZERO : new BigDecimal(n);
                if (viewModel.getFilterState().filterMinAmount.get().compareTo(val) != 0) {
                    viewModel.getFilterState().filterMinAmount.set(val);
                }
            } catch (Exception e) {}
        });
        
        TextField maxF = new TextField(); maxF.setPromptText("Max"); maxF.setPrefWidth(100);
        maxF.setTextFormatter(NumericTextFormatter.decimalOnly(2));
        maxF.textProperty().addListener((o, old, n) -> {
            try {
                BigDecimal val = n.isEmpty() ? null : new BigDecimal(n);
                BigDecimal current = viewModel.getFilterState().filterMaxAmount.get();
                if ((val == null && current != null) || (val != null && (current == null || current.compareTo(val) != 0))) {
                    viewModel.getFilterState().filterMaxAmount.set(val);
                }
            } catch (Exception e) {}
        });

        // State -> UI Sync for Amounts
        viewModel.getFilterState().filterMinAmount.addListener((o, old, n) -> {
            String txt = (n == null || n.compareTo(BigDecimal.ZERO) == 0) ? "" : n.toPlainString();
            if (!minF.getText().equals(txt)) minF.setText(txt);
        });
        viewModel.getFilterState().filterMaxAmount.addListener((o, old, n) -> {
            String txt = (n == null) ? "" : n.toPlainString();
            if (!maxF.getText().equals(txt)) maxF.setText(txt);
        });
        
        aFields.getChildren().addAll(minF, new Label("-"), maxF);
        aBox.getChildren().addAll(aLbl, aFields);

        row2.getChildren().addAll(vBox, aBox);

        card.getChildren().addAll(row1, row2);
        return card;
    }

    private TableView<PurchaseEntity> createTable() {
        TableView<PurchaseEntity> table = new TableView<>();
        table.setItems(viewModel.purchaseList);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Checkbox
        TableColumn<PurchaseEntity, Boolean> selectCol = new TableColumn<>("");
        CheckBox selectAll = new CheckBox();
        selectCol.setGraphic(selectAll);
        selectCol.setPrefWidth(40);
        selectCol.setCellValueFactory(data -> {
            PurchaseEntity p = data.getValue();
            BooleanProperty prop = new SimpleBooleanProperty(viewModel.selectedPurchases.contains(p));

            // When user clicks individual checkbox -> update the shared list
            prop.addListener((obs, old, n) -> {
                if (n) {
                    if (!viewModel.selectedPurchases.contains(p))
                        viewModel.selectedPurchases.add(p);
                } else {
                    viewModel.selectedPurchases.remove(p);
                }
            });

            // When the shared list changes (e.g. Select All) -> update this checkbox
            viewModel.selectedPurchases.addListener((javafx.collections.ListChangeListener<PurchaseEntity>) c -> {
                boolean selected = viewModel.selectedPurchases.contains(p);
                if (prop.get() != selected) {
                    Platform.runLater(() -> prop.set(selected));
                }
            });

            return prop;
        });
        selectCol.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(selectCol));

        selectAll.setOnAction(e -> {
            if (selectAll.isSelected())
                viewModel.selectedPurchases.setAll(viewModel.purchaseList);
            else
                viewModel.selectedPurchases.clear();
        });

        // Columns
        TableColumn<PurchaseEntity, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getEntryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        TableColumn<PurchaseEntity, String> vendorCol = new TableColumn<>("Vendor");
        vendorCol.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(getVendorName(d.getValue().getVendorId())));

        TableColumn<PurchaseEntity, Number> bagsCol = new TableColumn<>("Bags");
        bagsCol.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getBags()));

        TableColumn<PurchaseEntity, String> rateCol = new TableColumn<>("Rate");
        rateCol.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(String.format("%.2f", d.getValue().getRate())));

        TableColumn<PurchaseEntity, String> amountCol = new TableColumn<>("Amount");
        amountCol.setPrefWidth(120);
        amountCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                String.format("₹%,.2f", d.getValue().getGrandTotal())));

        TableColumn<PurchaseEntity, String> chequeCol = new TableColumn<>("Cheque No");
        chequeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getChequeNumber() != null ? d.getValue().getChequeNumber() : "-"));

        TableColumn<PurchaseEntity, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("PAID"))
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    else if ("UNPAID".equalsIgnoreCase(item))
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                }
            }
        });

        // Actions with Crisp SVG Icons
        TableColumn<PurchaseEntity, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(cf -> new ActionButtonsTableCell(
                p -> {
                    if (onPurchaseSelected != null) onPurchaseSelected.accept(p);
                },
                p -> {
                    if (onPurchaseEdit != null) onPurchaseEdit.accept(p);
                },
                this::handlePrintCheque,
                this::handleDelete
        ));
        actionCol.setMinWidth(180);

        table.getColumns().addAll(selectCol, dateCol, vendorCol, bagsCol, rateCol, amountCol, chequeCol, statusCol,
                actionCol);
        return table;
    }

    private Button createSvgButton(String svgPath, String colorHex, String tooltip) {
        SVGPath path = new SVGPath();
        path.setContent(svgPath);
        path.setFill(Color.web(colorHex));
        path.setScaleX(1.1); // Slightly larger
        path.setScaleY(1.1);

        Button btn = new Button();
        btn.setGraphic(path);
        btn.setTooltip(new Tooltip(tooltip));
        btn.getStyleClass().add("button");
        btn.getStyleClass().add("icon-only"); // New class from theme.css
        // Override default button padding for icons
        btn.setStyle("-fx-padding: 6; -fx-background-color: transparent; -fx-cursor: hand;");
        
        // Add hover effect manually to ensure visibility
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-padding: 6; -fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 50%; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-padding: 6; -fx-background-color: transparent; -fx-cursor: hand;"));
        
        return btn;
    }

    private void handlePrintCheque(PurchaseEntity p) {
        String vendorName = getVendorName(p.getVendorId());
        com.lax.sme_manager.dto.ChequeData data = new com.lax.sme_manager.dto.ChequeData(
                vendorName, p.getGrandTotal(), p.getChequeDate() != null ? p.getChequeDate() : LocalDate.now(), true, p.getId());
        // Pass refresh callback so table updates immediately after successful print
        new ChequePreviewDialog(data, () -> viewModel.loadPurchases()).show();
    }

    private void handleBatchPrint() {
        List<PurchaseEntity> selected = viewModel.selectedPurchases;
        if (selected.isEmpty())
            return;

        // Filter out already-PAID cheques
        List<PurchaseEntity> unpaid = selected.stream()
                .filter(p -> !"PAID".equalsIgnoreCase(p.getStatus()))
                .toList();

        if (unpaid.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "All selected cheques are already PAID. Nothing to print.").show();
            return;
        }

        int skipped = selected.size() - unpaid.size();
        String msg = "Print " + unpaid.size() + " cheque(s)?";
        if (skipped > 0) {
            msg += "\n(" + skipped + " already-PAID cheque(s) will be skipped.)";
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                // Ask for a starting cheque number for the batch
                TextInputDialog chqDialog = new TextInputDialog();
                chqDialog.setTitle("Batch Cheque Numbers");
                chqDialog.setHeaderText("Enter Starting Cheque Number");
                chqDialog.setContentText("First leaf number (6+ digits):");
                chqDialog.showAndWait().ifPresent(startChqStr -> {
                    if (startChqStr.trim().length() < 6) {
                        new Alert(Alert.AlertType.ERROR, "Cheque number must be at least 6 digits.").show();
                        return;
                    }

                    var printService = new com.lax.sme_manager.service.ChequePrintService();
                    var config = new com.lax.sme_manager.repository.ChequeConfigRepository().getConfig();
                    int count = 0;
                    int duplicates = 0;

                    try {
                        long chqNum = Long.parseLong(startChqStr.trim());

                        for (PurchaseEntity p : unpaid) {
                            String currentChqNo = String.valueOf(chqNum);

                            // Fraud check for each cheque number
                            if (isBatchChequeNumberDuplicate(currentChqNo)) {
                                duplicates++;
                                chqNum++;
                                continue;
                            }

                            try {
                                String vName = getVendorName(p.getVendorId());
                                var data = new com.lax.sme_manager.dto.ChequeData(vName, p.getGrandTotal(),
                                        p.getChequeDate() != null ? p.getChequeDate() : LocalDate.now(), true, p.getId());
                                printService.printSilent(config, data);

                                // Mark as PAID in DB
                                markBatchPurchaseAsPaid(p.getId(), currentChqNo);
                                count++;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            chqNum++;
                        }
                    } catch (NumberFormatException e) {
                        new Alert(Alert.AlertType.ERROR, "Invalid cheque number format. Use digits only.").show();
                        return;
                    }

                    String result = "Sent " + count + " cheque(s) to printer and marked as PAID.";
                    if (duplicates > 0) {
                        result += "\n⚠️ " + duplicates + " duplicate cheque number(s) were skipped.";
                    }
                    new Alert(Alert.AlertType.INFORMATION, result).show();

                    // Refresh the table immediately
                    viewModel.loadPurchases();
                    viewModel.selectedPurchases.clear();
                });
            }
        });
    }

    /** Fraud check for batch printing */
    private boolean isBatchChequeNumberDuplicate(String chqNo) {
        String sql = "SELECT COUNT(*) FROM purchase_entries WHERE cheque_number = ?";
        try (var conn = com.lax.sme_manager.util.DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chqNo);
            var rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Mark a single purchase as PAID during batch printing */
    private void markBatchPurchaseAsPaid(int purchaseId, String chqNo) {
        String sql = "UPDATE purchase_entries SET status = 'PAID', cheque_number = ?, cheque_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (var conn = com.lax.sme_manager.util.DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chqNo);
            stmt.setObject(2, LocalDate.now());
            stmt.setInt(3, purchaseId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleBulkDelete() {
        int count = viewModel.selectedPurchases.size();
        if (count == 0)
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + count + " selected entries?", ButtonType.YES,
                ButtonType.NO);
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES)
                viewModel.deleteSelectedPurchases();
        });
    }

    private void handleDelete(PurchaseEntity p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete this purchase entry?", ButtonType.YES,
                ButtonType.NO);
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES)
                viewModel.deletePurchase(p);
        });
    }

    private String getVendorName(int id) {
        return vendorRepository.findAllVendors().stream().filter(v -> v.getId() == id).map(Vendor::getName).findFirst()
                .orElse("Unknown");
    }

    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Purchase History");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx"));
        fileChooser.setInitialFileName("Purchase_History_" + LocalDate.now() + ".xlsx");
        
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                exportService.exportToExcel(viewModel.purchaseList, file);
                new Alert(Alert.AlertType.INFORMATION, "Export successful!").show();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).show();
                LOGGER.error("Export error", e);
            }
        }
    }

    @Override
    public void refresh() {
        viewModel.loadPurchases();
    }
}
