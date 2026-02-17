package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.util.NumericTextFormatter;

import com.lax.sme_manager.domain.Vendor;
import com.lax.sme_manager.ui.component.UIComponents;
import com.lax.sme_manager.ui.component.UIStyles;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.util.i18n.AppLabel;
import com.lax.sme_manager.util.AppLogger;
import com.lax.sme_manager.util.VendorCache;
import com.lax.sme_manager.viewmodel.PurchaseEntryViewModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import javafx.util.StringConverter;
import org.slf4j.Logger;

/**
 * Modern MVVM View for Purchase Entry.
 * Decoupled from logic, purely handles UI binding and layout.
 */
public class PurchaseEntryView extends VBox implements RefreshableView {
    private static final Logger LOGGER = AppLogger.getLogger(PurchaseEntryView.class);

    private final PurchaseEntryViewModel viewModel;
    private final VendorCache vendorCache;

    // UI Controls (kept for binding references if needed, though mostly bound in
    // init)
    private ComboBox<Vendor> vendorComboBox;
    private Label statusLabel;

    public PurchaseEntryView(VendorCache vendorCache) {
        this.vendorCache = vendorCache;
        this.viewModel = new PurchaseEntryViewModel(vendorCache);

        initializeUI();
        setupBindings();
    }

    private void initializeUI() {
        setSpacing(LaxTheme.Spacing.SPACE_12); // Compact spacing
        setPadding(new Insets(12)); // Compact padding
        setStyle("-fx-background-color: transparent; -fx-background: " + LaxTheme.Colors.LIGHT_GRAY + ";");

        // Header
        Label titleLabel = new Label(AppLabel.TITLE_PURCHASE_ENTRY.get());
        titleLabel.setStyle(UIStyles.getTitleStyle());

        // Status Banner (Top)
        statusLabel = new Label();
        statusLabel.setStyle(UIStyles.getStatusStyle());
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setVisible(false); // Hidden by default
        statusLabel.managedProperty().bind(statusLabel.visibleProperty()); // Auto-collapse

        // Main Layout
        HBox mainContent = new HBox(LaxTheme.Spacing.SPACE_20); // Reduced gutter
        VBox formSection = createFormSection();
        HBox.setHgrow(formSection, Priority.ALWAYS);

        VBox summarySection = createSummarySection();
        summarySection.setPrefWidth(320); // Slightly more compact
        summarySection.setMinWidth(320);
        HBox.setHgrow(summarySection, Priority.NEVER);

        mainContent.getChildren().addAll(formSection, summarySection);

        // Global ScrollPane
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true); // Ensure full height logic if needed, but fitToWidth is key
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(titleLabel, statusLabel, scrollPane);
    }

    private VBox createFormSection() {
        VBox form = new VBox();
        form.setStyle(UIStyles.getCardStyle());
        // Outer padding for card border distance + inner padding for field spacing
        form.setPadding(new Insets(
                LaxTheme.Layout.SUB_PANEL_PADDING, // top: 20px
                LaxTheme.Layout.SUB_PANEL_PADDING, // right: 20px
                LaxTheme.Layout.SUB_PANEL_PADDING, // bottom: 20px
                LaxTheme.Layout.SUB_PANEL_PADDING)); // left: 20px
        form.setSpacing(LaxTheme.Spacing.SPACE_20); // Vertical spacing between rows (increased from 24)

        // -- Row 1: Date & Vendor --
        DatePicker datePicker = new DatePicker();
        // Date Restriction: No future dates
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
        VBox dateBox = UIComponents.createLabeledDatePicker(AppLabel.LBL_PURCHASE_DATE.get(), datePicker);
        // Bind Date
        datePicker.valueProperty().bindBidirectional(viewModel.entryDate);

        vendorComboBox = new ComboBox<>();
        VBox vendorBox = createVendorSearchBox(vendorComboBox);

        HBox row1 = UIComponents.createTwoColumnRow(dateBox, vendorBox);

        // -- Row 2: Bags & Rate --
        TextField bagsField = new TextField();
        bagsField.setTextFormatter(NumericTextFormatter.integerOnly());
        bagsField.textProperty().bindBidirectional(viewModel.bags);
        UIComponents.addAutoClearOnFocus(bagsField); // Auto-clear
        VBox bagsBox = UIComponents.createLabeledTextField(AppLabel.LBL_BAGS.get(), true, bagsField);

        TextField rateField = new TextField();
        rateField.setTextFormatter(NumericTextFormatter.decimalOnly(2));
        rateField.textProperty().bindBidirectional(viewModel.rate);
        UIComponents.addAutoClearOnFocus(rateField); // Auto-clear
        VBox rateBox = UIComponents.createLabeledTextField(AppLabel.LBL_RATE.get(), false,
                rateField);

        HBox row2 = UIComponents.createTwoColumnRow(bagsBox, rateBox);

        // -- Row 3: Weight & Lumpsum --
        TextField weightField = new TextField();
        weightField.setTextFormatter(NumericTextFormatter.decimalOnly(2));
        weightField.textProperty().bindBidirectional(viewModel.weight);
        weightField.disableProperty().bind(viewModel.isLumpsum);
        UIComponents.addAutoClearOnFocus(weightField); // Auto-clear
        VBox weightBox = UIComponents.createLabeledTextField(AppLabel.LBL_WEIGHT.get(), false,
                weightField);

        CheckBox lumpsumCheck = new CheckBox();
        lumpsumCheck.selectedProperty().bindBidirectional(viewModel.isLumpsum);
        VBox lumpsumBox = UIComponents.createLabeledCheckbox(AppLabel.LBL_LUMPSUM.get(), lumpsumCheck);

        HBox row3 = UIComponents.createTwoColumnRow(weightBox, lumpsumBox);

        // -- Row 4: Payment & Advance --
        ComboBox<String> paymentCombo = new ComboBox<>();
        paymentCombo.getItems().addAll("CHEQUE", "CASH", "BANK TRANSFER", "UPI", "ADVANCE");
        paymentCombo.valueProperty().bindBidirectional(viewModel.paymentMode);
        VBox paymentBox = UIComponents.createLabeledComboBox(AppLabel.LBL_PAYMENT_MODE.get(),
                paymentCombo);

        CheckBox advanceCheck = new CheckBox();
        advanceCheck.selectedProperty().bindBidirectional(viewModel.advancePaid);
        VBox advanceBox = UIComponents.createLabeledCheckbox(AppLabel.LBL_PAID_IN_ADVANCE.get(),
                advanceCheck);

        HBox row4 = UIComponents.createTwoColumnRow(paymentBox, advanceBox);

        // -- Row 5: Fees --
        VBox feePanel = createFeePanel();

        // -- Row 6: Notes --
        TextField notesField = new TextField();
        notesField.textProperty().bindBidirectional(viewModel.notes);
        VBox notesBox = UIComponents.createNotesField(AppLabel.LBL_NOTES.get(), notesField);

        // -- Row 7: Buttons --
        HBox textBtnRow = createButtons();

        form.getChildren().addAll(row1, row2, row3, row4, feePanel, notesBox, new Separator(), textBtnRow);
        return form;
    }

    private VBox createVendorSearchBox(ComboBox<Vendor> comboBox) {
        comboBox.setEditable(true);
        comboBox.setPromptText("Type vendor name...");

        // Initial items
        comboBox.setItems(vendorCache.getAllVendors());

        // Converter to handle text display
        comboBox.setConverter(new StringConverter<Vendor>() {
            @Override
            public String toString(Vendor object) {
                return object == null ? "" : object.getName();
            }

            @Override
            public Vendor fromString(String string) {
                // Returning null here forces 'value' to be set manually or logic elsewhere
                // handles it.
                // Actually, returning null allows the 'valueProperty' to be loosely bound.
                // We will handle 'new vendor' creation via the editor logic.
                if (string == null || string.isEmpty())
                    return null;

                // Return existing if exact match
                Vendor existing = vendorCache.findByName(string);
                if (existing != null)
                    return existing;

                // Return TEMP vendor for new entry
                return new Vendor(-1, string);
            }
        });

        // Search Filter Logic
        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null)
                return;

            // If the text matches the currently selected item, don't filter (it's just
            // display)
            Vendor current = comboBox.getValue();
            if (current != null && current.getName().equalsIgnoreCase(newText))
                return;

            // Otherwise, filter items
            Platform.runLater(() -> {
                // If user clears text, reset list
                if (newText.isEmpty()) {
                    comboBox.setItems(vendorCache.getAllVendors());
                    return;
                }

                // Filter
                List<Vendor> filtered = vendorCache.searchByName(newText);
                comboBox.getItems().setAll(filtered);

                // If the dropdown was hidden, show it
                if (!comboBox.isShowing()) {
                    comboBox.show();
                }
            });
        });

        // Bind to ViewModel
        comboBox.valueProperty().bindBidirectional(viewModel.selectedVendor);

        // Sync reset
        viewModel.selectedVendor.addListener((obs, o, n) -> {
            if (n == null) {
                comboBox.getEditor().clear();
                comboBox.getSelectionModel().clearSelection();
            } else {
                // Ensure item is selected
                if (comboBox.getValue() != n) {
                    comboBox.setValue(n);
                }
            }
        });

        return UIComponents.createLabeledComboBox(AppLabel.LBL_VENDOR.get(), comboBox); // Reuse generic combo creator
    }

    private VBox createFeePanel() {
        // Market Fee
        TextField mktField = new TextField();
        mktField.setTextFormatter(NumericTextFormatter.percentage()); // MANDATORY: Block alphabets
        mktField.textProperty().bindBidirectional(viewModel.marketFeePercent);
        mktField.setEditable(false);
        mktField.setStyle(
                "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 4; -fx-padding: 8;");

        Button mktBtn = new Button(AppLabel.ACTION_EDIT.get());
        mktBtn.setOnAction(e -> toggleEdit(mktField, mktBtn, "0.70"));

        VBox mktCol = UIComponents.createFeeColumn(AppLabel.LBL_RATE.get() + " (Market)", mktField, mktBtn);

        // Commission
        TextField comField = new TextField();
        comField.setTextFormatter(NumericTextFormatter.percentage()); // MANDATORY: Block alphabets
        comField.textProperty().bindBidirectional(viewModel.commissionPercent);
        comField.setEditable(false);
        comField.setStyle(
                "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 4; -fx-padding: 8;");

        Button comBtn = new Button(AppLabel.ACTION_EDIT.get());
        comBtn.setOnAction(e -> toggleEdit(comField, comBtn, "2.00"));

        VBox comCol = UIComponents.createFeeColumn("Commission", comField, comBtn);

        return UIComponents.createGreyPanelContainer(mktCol, comCol);
    }

    private void toggleEdit(TextField field, Button btn, String defVal) {
        if (!field.isEditable()) {
            field.setEditable(true);
            field.setStyle(LaxTheme.getInputStyle() + "; -fx-background-color: white");
            btn.setText("Cancel");
            field.requestFocus();
        } else {
            field.setEditable(false);
            field.setStyle(
                    "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 4; -fx-padding: 8;");
            btn.setText(AppLabel.ACTION_EDIT.get());
            // Reset to default if cancelled - or maybe just keep current?
            // Original app reset to default.
            field.setText(defVal);
        }
    }

    private HBox createButtons() {
        Button submitBtn = new Button(AppLabel.ACTION_SUBMIT.get());
        submitBtn.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.PRIMARY));
        submitBtn.setPrefHeight(LaxTheme.ComponentSizes.BUTTON_HEIGHT_MD);
        submitBtn.setPrefWidth(160);
        submitBtn.setOnAction(e -> viewModel.submitEntry());

        Button resetBtn = new Button(AppLabel.ACTION_RESET.get());
        resetBtn.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY));
        resetBtn.setPrefHeight(LaxTheme.ComponentSizes.BUTTON_HEIGHT_MD);
        resetBtn.setOnAction(e -> {
            viewModel.resetForm();
            vendorComboBox.getEditor().clear();
        });

        HBox box = new HBox(12, submitBtn, resetBtn);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private VBox createSummarySection() {
        VBox summary = new VBox(LaxTheme.Spacing.SPACE_16);

        // 1. Entry Summary Box
        VBox entryBox = new VBox(LaxTheme.Spacing.SPACE_12);
        entryBox.setStyle(UIStyles.getSummaryBoxStyle() + "; -fx-background-color: white;");
        entryBox.setPadding(new Insets(24));

        Label h1 = new Label("Entry Summary");
        h1.setStyle(UIStyles.getCalcSectionHeaderStyle());

        entryBox.getChildren().add(h1);
        entryBox.getChildren().add(summaryRow(AppLabel.LBL_BAGS.get(), viewModel.bags));
        entryBox.getChildren().add(summaryRow(AppLabel.LBL_WEIGHT.get(), viewModel.weight));
        entryBox.getChildren().add(summaryRow(AppLabel.LBL_RATE.get(), viewModel.rate));

        Label lumpsumVal = new Label("-");
        lumpsumVal.setStyle(UIComponents.getSummaryValueStyle());
        lumpsumVal.textProperty().bind(viewModel.isLumpsum.map(b -> b ? "Yes" : "No"));
        entryBox.getChildren().add(createBoundSummaryRow(AppLabel.LBL_LUMPSUM.get(), lumpsumVal));

        Label vendorVal = new Label("-");
        vendorVal.setStyle(UIComponents.getSummaryValueStyle());
        viewModel.selectedVendor.addListener((obs, o, n) -> vendorVal.setText(n != null ? n.getName() : "-"));
        entryBox.getChildren().add(createBoundSummaryRow(AppLabel.LBL_VENDOR.get(), vendorVal));

        // 2. Calculation Panel
        VBox calcBox = new VBox(LaxTheme.Spacing.SPACE_16);
        calcBox.setStyle(UIStyles.getCalcPanelStyle()
                + "; -fx-background-color: #f8fafc; -fx-border-width: 0 0 0 4; -fx-border-color: "
                + LaxTheme.Colors.PRIMARY_TEAL + ";");
        calcBox.setPadding(new Insets(24));

        Label h2 = new Label("Calculation Summary");
        h2.setStyle(UIStyles.getCalcTitleStyle());

        calcBox.getChildren().add(h2);
        calcBox.getChildren().add(summaryRowNum("Base Amount", viewModel.baseAmountProperty()));
        calcBox.getChildren().add(new Separator());
        calcBox.getChildren().add(summaryRowNum("Market Fee", viewModel.marketFeeAmountProperty()));
        calcBox.getChildren().add(summaryRowNum("Commission", viewModel.commissionFeeAmountProperty()));

        Label totalLbl = new Label();
        totalLbl.setStyle(UIStyles.getLargeAmountStyle());
        totalLbl.textProperty().bind(viewModel.grandTotalProperty().asString("₹%.2f"));

        Label totalRowLabel = new Label(AppLabel.LBL_TOTAL.get());
        totalRowLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + LaxTheme.Colors.TEXT_SECONDARY + ";");

        VBox totalRow = new VBox(8, totalRowLabel, totalLbl);
        totalRow.setPadding(new Insets(12, 0, 0, 0));
        calcBox.getChildren().addAll(new Separator(), totalRow);

        summary.getChildren().addAll(entryBox, calcBox);
        return summary;
    }

    private HBox summaryRow(String label, javafx.beans.value.ObservableValue<String> val) {
        Label v = new Label();
        v.setStyle(UIComponents.getSummaryValueStyle());
        v.textProperty().bind(val.map(s -> s.isEmpty() ? "-" : s));
        return createBoundSummaryRow(label, v);
    }

    private HBox summaryRowNum(String label, javafx.beans.value.ObservableValue<Number> val) {
        Label v = new Label();
        v.setStyle(UIComponents.getSummaryValueStyle());
        v.textProperty().bind(val.map(n -> String.format("₹%.2f", n.doubleValue())));
        return createBoundSummaryRow(label, v);
    }

    private HBox createBoundSummaryRow(String label, Label valueLabel) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(28);
        Label key = new Label(label);
        key.setStyle(UIComponents.getSummaryLabelStyle());
        key.setPrefWidth(140);
        row.getChildren().addAll(key, valueLabel);
        return row;
    }

    private void setupBindings() {
        // Status Bar Binding
        statusLabel.textProperty().bind(viewModel.statusMessage);

        viewModel.isStatusError.addListener((obs, o, isError) -> {
            if (isError)
                statusLabel.setStyle(UIStyles.getErrorStatusStyle());
            else
                statusLabel.setStyle(UIStyles.getSuccessStatusStyle());

            // Show status when message changes (and not empty)
            statusLabel.setVisible(!statusLabel.getText().isEmpty());
        });

        // Hide status when clicked (dismiss)
        statusLabel.setOnMouseClicked(e -> statusLabel.setVisible(false));
    }

    @Override
    public void refresh() {
        LOGGER.info("Refreshing PurchaseEntryView data");
        if (vendorComboBox != null && vendorCache != null) {
            vendorComboBox.setItems(vendorCache.getAllVendors());
        }
    }

    public PurchaseEntryViewModel getViewModel() {
        return viewModel;
    }
}
