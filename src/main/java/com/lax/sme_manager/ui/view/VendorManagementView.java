package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.domain.Vendor;
import com.lax.sme_manager.repository.VendorRepository;
import com.lax.sme_manager.repository.model.VendorEntity;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.util.i18n.AppLabel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Vendor Management View - Full CRUD operations for vendors
 * Features:
 * - Paginated table with all vendors
 * - Search/Filter by name or contact
 * - Add/Edit/Soft Delete operations
 */
public class VendorManagementView extends VBox implements RefreshableView {
    private static final Logger LOGGER = LoggerFactory.getLogger(VendorManagementView.class);

    private final VendorRepository repository;
    private final ObservableList<VendorEntity> vendors = FXCollections.observableArrayList();

    private TableView<VendorEntity> vendorTable;
    private TextField searchField;
    private Pagination pagination;
    private static final int ROWS_PER_PAGE = 20;

    public VendorManagementView() {
        this.repository = new VendorRepository();
        initializeUI();
        loadVendors();
    }

    private void initializeUI() {
        setPadding(new Insets(24, 32, 24, 32));
        setSpacing(24);
        setStyle("-fx-background-color: #f8fafc;");

        // --- HERO HEADER & TOP SEARCH ---
        HBox topHeader = new HBox(20);
        topHeader.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Vendor Management");
        titleLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        
        HBox statsBox = new HBox(10);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        Region statsPill = createModernStatsPill();
        statsBox.getChildren().add(statsPill);
        
        titleBox.getChildren().addAll(titleLbl, statsBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search Container (Expert-level custom styling)
        HBox searchContainer = new HBox(10);
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.setPadding(new Insets(0, 16, 0, 16));
        searchContainer.setPrefHeight(45);
        searchContainer.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-border-width: 1;");
        
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
        
        searchField = new TextField();
        searchField.setPromptText("Search vendors or contacts...");
        searchField.setPrefWidth(300);
        searchField.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-text-fill: #1E293B; -fx-font-size: 13px;");
        searchField.textProperty().addListener((obs, old, val) -> filterVendors(val));
        
        searchContainer.getChildren().addAll(searchIcon, searchField);

        Button addButton = new Button("➕ Add New Vendor");
        addButton.setPrefHeight(45);
        addButton.setStyle("-fx-background-color: #0D9488; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 0 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(13,148,136,0.3), 8, 0, 0, 4);");
        addButton.setOnAction(e -> handleAddVendor());

        topHeader.getChildren().addAll(titleBox, spacer, searchContainer, addButton);

        // --- TABLE ---
        vendorTable = createVendorTable();
        VBox.setVgrow(vendorTable, Priority.ALWAYS);

        getChildren().addAll(topHeader, vendorTable);
    }

    private HBox createModernStatsPill() {
        HBox pill = new HBox(8);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.setPadding(new Insets(4, 12, 4, 12));
        pill.setStyle("-fx-background-color: #E0F2FE; -fx-background-radius: 20; -fx-border-color: #7DD3FC; -fx-border-radius: 20;");

        Label label = new Label("Total Registered:");
        label.setStyle("-fx-text-fill: #0369A1; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-transform: uppercase;");
        
        Label value = new Label(String.valueOf(vendors.size()));
        value.setStyle("-fx-text-fill: #0369A1; -fx-font-size: 11px; -fx-font-weight: 800;");
        
        vendors.addListener((javafx.collections.ListChangeListener<VendorEntity>) c -> {
            value.setText(String.valueOf(vendors.size()));
        });

        pill.getChildren().addAll(label, value);
        return pill;
    }

    private TableView<VendorEntity> createVendorTable() {
        TableView<VendorEntity> table = new TableView<>(vendors);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ID Column
        TableColumn<VendorEntity, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getId()));
        idCol.setPrefWidth(60);
        idCol.setResizable(false);

        // Name Column
        TableColumn<VendorEntity, String> nameCol = new TableColumn<>("VENDOR NAME");
        nameCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(250);

        // Contact Person Column
        TableColumn<VendorEntity, String> contactCol = new TableColumn<>("CONTACT PERSON");
        contactCol.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getContactPerson()));

        // Phone Column
        TableColumn<VendorEntity, String> phoneCol = new TableColumn<>("PHONE");
        phoneCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    VendorEntity v = getTableRow().getItem();
                    Label l = new Label("📞 " + (v.getPhone() != null ? v.getPhone() : "N/A"));
                    l.getStyleClass().add("cell-icon-label");
                    setGraphic(l);
                }
            }
        });

        // Email Column
        TableColumn<VendorEntity, String> emailCol = new TableColumn<>("EMAIL");
        emailCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    VendorEntity v = getTableRow().getItem();
                    Label l = new Label("📧 " + (v.getEmail() != null ? v.getEmail() : "N/A"));
                    l.getStyleClass().add("cell-icon-label");
                    setGraphic(l);
                }
            }
        });

        // Actions Column
        TableColumn<VendorEntity, Void> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setPrefWidth(180);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final HBox container = new HBox(12);
            private final Button editBtn;
            private final Button deleteBtn;

            {
                editBtn = createModernActionButton(
                    "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
                    "#FFF3E0", "#F57C00", "#FFE0B2", "Edit Vendor"
                );

                deleteBtn = createModernActionButton(
                    "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
                    "#FFEBEE", "#D32F2F", "#FFCDD2", "Soft Delete"
                );

                editBtn.setOnAction(e -> {
                    VendorEntity v = getTableView().getItems().get(getIndex());
                    handleEditVendor(v);
                });

                deleteBtn.setOnAction(e -> {
                    VendorEntity v = getTableView().getItems().get(getIndex());
                    handleDeleteVendor(v);
                });

                container.getChildren().addAll(editBtn, deleteBtn);
                container.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, contactCol, phoneCol, emailCol, actionsCol);
        return table;
    }

    private Button createModernActionButton(String svgPath, String bgColor, String iconColor, String hoverColor, String tooltip) {
        javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
        path.setContent(svgPath);
        path.setFill(javafx.scene.paint.Color.web(iconColor));
        path.setScaleX(1.0);
        path.setScaleY(1.0);

        Button btn = new Button();
        btn.setGraphic(path);
        btn.setTooltip(new Tooltip(tooltip));
        
        String baseStyle = String.format("-fx-background-color: %s; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;", bgColor);
        btn.setStyle(baseStyle);

        btn.setOnMouseEntered(e -> btn.setStyle(baseStyle.replace(bgColor, hoverColor)));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));

        return btn;
    }

    private void loadVendors() {
        try {
            List<VendorEntity> allVendors = repository.findAll();
            vendors.setAll(allVendors);
            LOGGER.info("Loaded {} vendors", allVendors.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load vendors", e);
            new Alert(Alert.AlertType.ERROR, "Failed to load vendors: " + e.getMessage()).show();
        }
    }

    private void filterVendors(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadVendors();
            return;
        }

        try {
            List<VendorEntity> allVendors = repository.findAll();
            String lowerQuery = query.toLowerCase();
            List<VendorEntity> filtered = allVendors.stream()
                    .filter(v -> v.getName().toLowerCase().contains(lowerQuery) ||
                            (v.getContactPerson() != null && v.getContactPerson().toLowerCase().contains(lowerQuery)))
                    .toList();
            vendors.setAll(filtered);
            LOGGER.debug("Filtered to {} vendors matching '{}'", filtered.size(), query);
        } catch (Exception e) {
            LOGGER.error("Failed to filter vendors", e);
        }
    }

    private void handleAddVendor() {
        VendorEditDialog dialog = new VendorEditDialog(null);
        Optional<VendorEntity> result = dialog.showAndWait();
        result.ifPresent(vendor -> {
            try {
                repository.insert(vendor);
                loadVendors();
                new Alert(Alert.AlertType.INFORMATION, "Vendor added successfully!").show();
                LOGGER.info("Added new vendor: {}", vendor.getName());
            } catch (Exception e) {
                LOGGER.error("Failed to add vendor", e);
                new Alert(Alert.AlertType.ERROR, "Failed to add vendor: " + e.getMessage()).show();
            }
        });
    }

    private void handleEditVendor(VendorEntity vendor) {
        VendorEditDialog dialog = new VendorEditDialog(vendor);
        Optional<VendorEntity> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            try {
                repository.update(updated);
                loadVendors();
                new Alert(Alert.AlertType.INFORMATION, "Vendor updated successfully!").show();
                LOGGER.info("Updated vendor ID: {}", updated.getId());
            } catch (Exception e) {
                LOGGER.error("Failed to update vendor", e);
                new Alert(Alert.AlertType.ERROR, "Failed to update vendor: " + e.getMessage()).show();
            }
        });
    }

    private void handleDeleteVendor(VendorEntity vendor) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete vendor '" + vendor.getName() + "'?\n\n" +
                        "This will not remove the vendor from the database, just mark as deleted.",
                ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText("Soft Delete Vendor");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    repository.softDelete(vendor.getId());
                    loadVendors();
                    new Alert(Alert.AlertType.INFORMATION, "Vendor deleted successfully!").show();
                    LOGGER.info("Soft-deleted vendor ID: {}", vendor.getId());
                } catch (Exception e) {
                    LOGGER.error("Failed to delete vendor", e);
                    new Alert(Alert.AlertType.ERROR, "Failed to delete vendor: " + e.getMessage()).show();
                }
            }
        });
    }

    @Override
    public void refresh() {
        loadVendors();
    }
}
