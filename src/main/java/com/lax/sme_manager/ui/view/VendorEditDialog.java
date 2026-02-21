package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.repository.model.VendorEntity;
import com.lax.sme_manager.ui.theme.LaxTheme;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.time.LocalDateTime;

/**
 * Modal dialog for adding/editing vendors
 */
public class VendorEditDialog extends Dialog<VendorEntity> {

    private final VendorEntity existingVendor;

    private TextField nameField;
    private TextField contactPersonField;
    private TextField phoneField;
    private TextField emailField;
    private TextField defaultAmountField;
    private TextArea addressArea;
    private TextArea notesArea;

    public VendorEditDialog(VendorEntity vendor) {
        this.existingVendor = vendor;

        setTitle(vendor == null ? "Add New Vendor" : "Edit Vendor");
        setHeaderText(vendor == null ? "Register a new vendor to your network" : "Update information for " + vendor.getName());

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().getStyleClass().add("dialog-pane");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(30));
        grid.setPrefWidth(650);

        // Styling helpers
        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #64748B; -fx-font-size: 13px;";
        String fieldStyle = "-fx-pref-height: 40px;";

        // Name (required)
        Label nameLbl = new Label("VENDOR NAME *");
        nameLbl.setStyle(labelStyle);
        nameField = new TextField();
        nameField.setPromptText("Enter full legal name of the vendor");
        nameField.setStyle(fieldStyle);
        GridPane.setHgrow(nameField, Priority.ALWAYS);

        // Contact Person
        Label contactPersonLbl = new Label("CONTACT PERSON");
        contactPersonLbl.setStyle(labelStyle);
        contactPersonField = new TextField();
        contactPersonField.setPromptText("Primary contact person name");
        contactPersonField.setStyle(fieldStyle);
        GridPane.setHgrow(contactPersonField, Priority.ALWAYS);

        // Phone
        Label phoneLbl = new Label("PHONE NUMBER");
        phoneLbl.setStyle(labelStyle);
        phoneField = new TextField();
        phoneField.setPromptText("e.g. +1 234 567 890");
        phoneField.setStyle(fieldStyle);
        GridPane.setHgrow(phoneField, Priority.ALWAYS);

        // Email
        Label emailLbl = new Label("EMAIL ADDRESS");
        emailLbl.setStyle(labelStyle);
        emailField = new TextField();
        emailField.setPromptText("e.g. vendor@example.com");
        emailField.setStyle(fieldStyle);
        GridPane.setHgrow(emailField, Priority.ALWAYS);

        // Address
        Label addressLbl = new Label("OFFICE ADDRESS");
        addressLbl.setStyle(labelStyle);
        addressArea = new TextArea();
        addressArea.setPromptText("Enter full physical or mailing address");
        addressArea.setPrefRowCount(3);
        GridPane.setHgrow(addressArea, Priority.ALWAYS);

        // Default Amount
        Label defaultAmountLbl = new Label("DEFAULT AMOUNT (₹)");
        defaultAmountLbl.setStyle(labelStyle);
        defaultAmountField = new TextField();
        defaultAmountField.setPromptText("Auto-populate in Cheque Writer");
        defaultAmountField.setStyle(fieldStyle);
        GridPane.setHgrow(defaultAmountField, Priority.ALWAYS);

        // Notes
        Label notesLbl = new Label("INTERNAL NOTES");
        notesLbl.setStyle(labelStyle);
        notesArea = new TextArea();
        notesArea.setPromptText("Any additional terms, bank details, or internal remarks");
        notesArea.setPrefRowCount(3);
        GridPane.setHgrow(notesArea, Priority.ALWAYS);

        // Add all to grid
        int row = 0;
        grid.add(nameLbl, 0, row);
        grid.add(nameField, 1, row++);

        grid.add(contactPersonLbl, 0, row);
        grid.add(contactPersonField, 1, row++);

        grid.add(phoneLbl, 0, row);
        grid.add(phoneField, 1, row++);

        grid.add(emailLbl, 0, row);
        grid.add(emailField, 1, row++);

        grid.add(addressLbl, 0, row);
        grid.add(addressArea, 1, row++);

        grid.add(defaultAmountLbl, 0, row);
        grid.add(defaultAmountField, 1, row++);

        grid.add(notesLbl, 0, row);
        grid.add(notesArea, 1, row);

        getDialogPane().setContent(grid);

        // Populate fields if editing
        if (vendor != null) {
            nameField.setText(vendor.getName());
            contactPersonField.setText(vendor.getContactPerson());
            phoneField.setText(vendor.getPhone());
            emailField.setText(vendor.getEmail());
            addressArea.setText(vendor.getAddress());
            defaultAmountField.setText(vendor.getDefaultAmount() != null ? vendor.getDefaultAmount().toPlainString() : "");
            notesArea.setText(vendor.getNotes());
        }

        // Validation: name is required
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(vendor == null); // Disable for new vendor until name entered

        nameField.textProperty().addListener((obs, old, val) -> {
            okButton.setDisable(val == null || val.trim().isEmpty());
        });

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                VendorEntity result;
                if (existingVendor != null) {
                    // Update existing
                    result = existingVendor;
                } else {
                    // Create new
                    result = new VendorEntity();
                    result.setCreatedAt(LocalDateTime.now());
                }

                result.setName(nameField.getText().trim());
                result.setContactPerson(contactPersonField.getText().trim());
                result.setPhone(phoneField.getText().trim());
                result.setEmail(emailField.getText().trim());
                result.setAddress(addressArea.getText().trim());
                try {
                    String amt = defaultAmountField.getText().trim();
                    result.setDefaultAmount(amt.isEmpty() ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(amt));
                } catch (Exception e) {
                    result.setDefaultAmount(java.math.BigDecimal.ZERO);
                }
                result.setNotes(notesArea.getText().trim());
                result.setUpdatedAt(LocalDateTime.now());

                return result;
            }
            return null;
        });
    }
}
