package com.lax.sme_manager.ui.component;

import com.lax.sme_manager.domain.Vendor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import com.lax.sme_manager.ui.theme.LaxTheme;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable UI components following LaxSmeManager design system
 * - Consistent styling via LaxTheme
 * - Accessible, keyboard-friendly
 * - Numeric validation built-in
 * - Perfect 80px field height alignment
 */
public class UIComponents {
    // Constants for consistent sizing
    private static final double STANDARD_INPUT_WIDTH = 340;
    private static final double STANDARD_INPUT_HEIGHT = 44; // Increased from 40
    private static final double STANDARD_FIELD_GROUP_HEIGHT = 88; // Increased from 80
    private static final DateTimeFormatter INDIA_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Generic grey container for 2-column content (fees, metrics, settings)
     */
    public static VBox createGreyPanelContainer(Node leftContent, Node rightContent) {
        VBox panel = new VBox(LaxTheme.Spacing.SPACE_12);
        panel.setStyle("-fx-background-color: rgba(248, 250, 251, 0.9); " +
                "-fx-border-color: " + LaxTheme.Colors.BORDER_GRAY + "; " +
                "-fx-border-radius: " + LaxTheme.BorderRadius.RADIUS_LG + "; " +
                "-fx-border-width: 1; " +
                "-fx-padding: 16 20;");

        HBox contentRow = new HBox(LaxTheme.Spacing.SPACE_32);
        contentRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(leftContent, Priority.ALWAYS);
        HBox.setHgrow(rightContent, Priority.ALWAYS);
        contentRow.getChildren().addAll(leftContent, rightContent);
        panel.getChildren().add(contentRow);
        return panel;
    }

    /**
     * 2-column fee field: Label(top) + [TextField][Button](bottom)
     */
    public static VBox createFeeColumn(String labelText, TextField percentField, Button toggleButton) {
        VBox column = new VBox(LaxTheme.Spacing.SPACE_6);
        column.setPrefWidth(170);

        // TOP: Bilingual label with asterisk
        Label label = new Label(labelText + " *");
        label.setStyle(getLabelStyle());
        label.setPrefHeight(24); // Increased

        // BOTTOM: Aligned TextField + Button
        HBox inputRow = new HBox(LaxTheme.Spacing.SPACE_12);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        percentField.setPrefWidth(80);
        percentField.setPrefHeight(STANDARD_INPUT_HEIGHT);
        percentField.setPromptText("%");
        percentField.setStyle(getInputStyle() + " -fx-background-color: #f8fafc;");
        applyNumericValidation(percentField, true); // Decimal validation

        toggleButton.setPrefWidth(70);
        toggleButton.setPrefHeight(STANDARD_INPUT_HEIGHT);
        toggleButton.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY));

        inputRow.getChildren().addAll(percentField, toggleButton);
        column.getChildren().addAll(label, inputRow);
        return column;
    }

    /**
     * Labeled numeric text field with validation (bags=integer,
     * rate/weight=decimal)
     */
    public static VBox createLabeledTextField(String labelText, boolean integerOnly, TextField textField) {
        return createLabeledTextField(labelText, integerOnly, textField, false);
    }

    /**
     * Labeled numeric text field with validation and optional required indicator.
     */
    public static VBox createLabeledTextField(String labelText, boolean integerOnly, TextField textField, boolean required) {
        VBox fieldGroup = new VBox(LaxTheme.Spacing.SPACE_6);
        fieldGroup.setPrefHeight(STANDARD_FIELD_GROUP_HEIGHT);
        fieldGroup.setMinHeight(STANDARD_FIELD_GROUP_HEIGHT);
        fieldGroup.setPadding(new Insets(0, 0, 0, 0)); // No extra padding - handled by parent

        Node labelNode = required ? createRequiredLabel(labelText) : createPlainLabel(labelText);

        textField.setPrefHeight(STANDARD_INPUT_HEIGHT);
        textField.setPrefWidth(STANDARD_INPUT_WIDTH);
        textField.setMinWidth(STANDARD_INPUT_WIDTH);
        textField.setMaxWidth(STANDARD_INPUT_WIDTH);
        textField.setStyle(getInputStyle() + " -fx-background-color: #f8fafc;");
        applyNumericValidation(textField, !integerOnly); // true=decimal, false=integer

        fieldGroup.getChildren().addAll(labelNode, textField);
        return fieldGroup;
    }

    /**
     * Labeled DatePicker with Indian format (DD-MM-YYYY)
     */
    public static VBox createLabeledDatePicker(String labelText, DatePicker datePicker) {
        return createLabeledDatePicker(labelText, datePicker, false);
    }

    /**
     * Labeled DatePicker with Indian format and optional required indicator.
     */
    public static VBox createLabeledDatePicker(String labelText, DatePicker datePicker, boolean required) {
        VBox fieldGroup = new VBox(LaxTheme.Spacing.SPACE_6);
        fieldGroup.setPrefHeight(STANDARD_FIELD_GROUP_HEIGHT);

        Node labelNode = required ? createRequiredLabel(labelText) : createPlainLabel(labelText);

        datePicker.setPrefHeight(STANDARD_INPUT_HEIGHT);
        datePicker.setPrefWidth(STANDARD_INPUT_WIDTH);
        datePicker.setStyle(getInputStyle() + " -fx-background-color: #f8fafc;");
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? INDIA_DATE_FORMAT.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isEmpty())
                    return null;
                try {
                    return LocalDate.parse(string, INDIA_DATE_FORMAT);
                } catch (Exception e) {
                    return null;
                }
            }
        });
        datePicker.getEditor().setStyle("-fx-background-color: transparent; -fx-padding: 0 8 0 8; -fx-font-size: 14;");

        fieldGroup.getChildren().addAll(labelNode, datePicker);
        return fieldGroup;
    }

    /**
     * Labeled ComboBox with bilingual label
     */
    public static VBox createLabeledComboBox(String labelText, ComboBox<?> comboBox) {
        return createLabeledComboBox(labelText, comboBox, false);
    }

    /**
     * Labeled ComboBox with optional required indicator.
     */
    public static VBox createLabeledComboBox(String labelText, ComboBox<?> comboBox, boolean required) {
        VBox fieldGroup = new VBox(LaxTheme.Spacing.SPACE_6);
        fieldGroup.setPrefHeight(STANDARD_FIELD_GROUP_HEIGHT);

        Node labelNode = required ? createRequiredLabel(labelText) : createPlainLabel(labelText);

        comboBox.setPrefHeight(STANDARD_INPUT_HEIGHT);
        comboBox.setPrefWidth(STANDARD_INPUT_WIDTH);
        comboBox.setStyle(getInputStyle() + " -fx-background-color: #f8fafc;");

        fieldGroup.getChildren().addAll(labelNode, comboBox);
        return fieldGroup;
    }

    // /**
    // * Labeled ComboBox with bilingual label
    // */
    // public static VBox createVendorComboBox(String englishLabel, String
    // gujaratiLabel,
    // ComboBox<Vendor> comboBox) {
    // VBox fieldGroup = new VBox(LaxTheme.Spacing.SPACE_6);
    // fieldGroup.setPrefHeight(STANDARD_FIELD_GROUP_HEIGHT);
    //
    // Label label = new Label(englishLabel + " (" + gujaratiLabel + ")");
    // label.setStyle(getLabelStyle());
    // label.setPrefHeight(20);
    //
    // // ✅ NOTHING TOUCHES comboBox.getItems() or 'items' here
    // // PurchaseEntryScreen already sets vendorList and listeners
    //
    // comboBox.setPrefHeight(STANDARD_INPUT_HEIGHT);
    // comboBox.setPrefWidth(STANDARD_INPUT_WIDTH);
    // comboBox.setStyle(getInputStyle());
    //
    // fieldGroup.getChildren().addAll(label, comboBox);
    // return fieldGroup;
    // }

    public static VBox createVendorAutoCompleteField(
            String labelEn,
            String labelGu,
            List<Vendor> vendors,
            Consumer<Vendor> onSelect) {
        Label label = new Label(labelEn + " / " + labelGu);

        TextField textField = new TextField();
        textField.setPromptText("Type vendor name");

        AutoCompletionBinding<Vendor> binding = TextFields.bindAutoCompletion(
                textField,
                request -> vendors.stream()
                        .filter(v -> v.getName().toLowerCase()
                                .contains(request.getUserText().toLowerCase()))
                        .toList());

        binding.setOnAutoCompleted(event -> {
            Vendor selected = event.getCompletion();
            textField.setText(selected.getName());
            onSelect.accept(selected);
        });

        VBox box = new VBox(4, label, textField);
        return box;
    }

    /**
     * Labeled checkbox with proper alignment
     */
    public static VBox createLabeledCheckbox(String labelText, CheckBox checkBox) {
        VBox fieldGroup = new VBox(LaxTheme.Spacing.SPACE_6);
        fieldGroup.setPrefHeight(STANDARD_FIELD_GROUP_HEIGHT);

        Label label = new Label(labelText);
        label.setStyle(getLabelStyle());
        label.setPrefHeight(20);

        HBox checkboxContainer = new HBox(LaxTheme.Spacing.SPACE_8);
        checkboxContainer.setAlignment(Pos.CENTER_LEFT);
        checkboxContainer.setPrefHeight(STANDARD_INPUT_HEIGHT);
        checkboxContainer.setPrefWidth(STANDARD_INPUT_WIDTH);
        checkBox.setStyle(getCheckboxStyle());
        checkBox.setPrefHeight(20);
        checkboxContainer.getChildren().add(checkBox);

        fieldGroup.getChildren().addAll(label, checkboxContainer);
        return fieldGroup;
    }

    /**
     * Single-line notes field with tab-safe navigation
     */
    public static VBox createNotesField(String labelText, TextField notesField) {
        VBox fieldGroup = new VBox(LaxTheme.Spacing.SPACE_6);
        fieldGroup.setPrefHeight(STANDARD_FIELD_GROUP_HEIGHT);

        Label label = new Label(labelText);
        label.setStyle(getLabelStyle());
        label.setPrefHeight(20);

        notesField.setPromptText("Optional notes / comments...");
        notesField.setPrefHeight(STANDARD_INPUT_HEIGHT);
        notesField.setPrefWidth(STANDARD_INPUT_WIDTH);
        notesField.setStyle(getInputStyle() + " -fx-background-color: #f8fafc;");
        // Tab-safe: navigate without inserting tab character
        notesField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB)
                event.consume();
        });

        fieldGroup.getChildren().addAll(label, notesField);
        return fieldGroup;
    }

    /**
     * 2-column form row (Date|Vendor, Bags|Rate, etc.)
     */
    public static HBox createTwoColumnRow(Node leftColumn, Node rightColumn) {
        HBox row = new HBox(LaxTheme.Spacing.SPACE_32);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPrefHeight(STANDARD_FIELD_GROUP_HEIGHT);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        row.getChildren().addAll(leftColumn, rightColumn);
        return row;
    }

    /**
     * Summary row for Entry/Calculation panels (label | value)
     */
    public static HBox createSummaryRow(String label, String value) {
        HBox row = new HBox(LaxTheme.Spacing.SPACE_16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(28);

        Label labelControl = new Label(label);
        labelControl.setStyle(getSummaryLabelStyle());
        labelControl.setPrefWidth(140);
        labelControl.setMinWidth(140);
        labelControl.setMaxWidth(140);

        Label valueControl = new Label(value);
        valueControl.setStyle(getSummaryValueStyle());
        HBox.setHgrow(valueControl, Priority.ALWAYS);

        row.getChildren().addAll(labelControl, valueControl);
        return row;
    }

    // === VALIDATION & UTILITIES ===

    /**
     * Apply numeric validation to TextField (integer or decimal)
     */
    private static void applyNumericValidation(TextField textField, boolean allowDecimal) {
        String regex = allowDecimal ? "\\d*\\.?\\d{0,2}" : "\\d*";

        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches(regex)) {
                textField.setText(oldValue); // Always revert to previous (known valid)
            }
        });
    }

    /**
     * Adds a listener to clear the field if it contains "0" or "0.0" when focused.
     */
    public static void addAutoClearOnFocus(TextField textField) {
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                String text = textField.getText();
                if ("0".equals(text) || "0.0".equals(text)) {
                    textField.setText("");
                }
            }
        });
    }

    // === STYLING HELPERS ===
    private static String getLabelStyle() {
        return String.format("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: %s; -fx-letter-spacing: -0.2px;",
                LaxTheme.Colors.TEXT_SECONDARY);
    }

    /**
     * Creates a plain label node (no asterisk).
     */
    private static Label createPlainLabel(String labelText) {
        Label label = new Label(labelText);
        label.setStyle(getLabelStyle());
        label.setPrefHeight(20);
        return label;
    }

    /**
     * Creates a label with a red asterisk (*) for required fields.
     */
    private static TextFlow createRequiredLabel(String labelText) {
        Text labelPart = new Text(labelText + " ");
        labelPart.setStyle(getLabelStyle());
        Text asterisk = new Text("*");
        asterisk.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-fill: #EF4444;");
        TextFlow flow = new TextFlow(labelPart, asterisk);
        flow.setPrefHeight(20);
        return flow;
    }

    private static String getInputStyle() {
        return LaxTheme.getInputStyle() + " -fx-font-size: 14;";
    }

    private static String getCheckboxStyle() {
        return String.format("-fx-font-size: 13; -fx-text-fill: %s;", LaxTheme.Colors.TEXT_PRIMARY);
    }

    public static String getSummaryLabelStyle() {
        return String.format("-fx-font-size: 13; -fx-text-fill: %s;", LaxTheme.Colors.TEXT_SECONDARY);
    }

    public static String getSummaryValueStyle() {
        return String.format("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: %s;",
                LaxTheme.Colors.PRIMARY_TEAL);
    }
}
