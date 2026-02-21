package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.repository.model.PurchaseEntity;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.viewmodel.RecycleBinViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.format.DateTimeFormatter;

public class RecycleBinView extends VBox {

    private final RecycleBinViewModel viewModel;
    private TableView<PurchaseEntity> tableView;

    public RecycleBinView(RecycleBinViewModel viewModel) {
        this.viewModel = viewModel;
        initialize();
        viewModel.loadDeletedPurchases();
    }

    private void initialize() {
        setPadding(new Insets(24));
        setSpacing(20);
        setStyle("-fx-background-color: #f8fafc;");

        // Header
        VBox header = new VBox(4);
        Label title = new Label("Recycle Bin");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitle = new Label("Recover soft-deleted purchase entries (Protected Section)");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");
        header.getChildren().addAll(title, subtitle);

        // Actions
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> viewModel.loadDeletedPurchases());
        
        Label statusLabel = new Label();
        statusLabel.textProperty().bind(viewModel.statusMessage);
        statusLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");

        actions.getChildren().addAll(refreshBtn, statusLabel);

        // Table
        createTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        getChildren().addAll(header, actions, tableView);
    }

    private void createTable() {
        tableView = new TableView<>();
        tableView.setItems(viewModel.deletedPurchases);
        tableView.setPlaceholder(new Label("No deleted records found."));
        tableView.setStyle("-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #E2E8F0; -fx-overflow-x: hidden;");

        // Columns
        TableColumn<PurchaseEntity, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getId()));
        idCol.setPrefWidth(60);

        TableColumn<PurchaseEntity, String> dateCol = new TableColumn<>("ENTRY DATE");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEntryDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
        ));
        dateCol.setPrefWidth(120);

        TableColumn<PurchaseEntity, String> vendorCol = new TableColumn<>("VENDOR ID");
        vendorCol.setCellValueFactory(data -> new SimpleStringProperty("Vendor " + data.getValue().getVendorId()));
        vendorCol.setPrefWidth(100);

        TableColumn<PurchaseEntity, String> amountCol = new TableColumn<>("TOTAL AMOUNT");
        amountCol.setCellValueFactory(data -> new SimpleStringProperty("₹" + data.getValue().getGrandTotal()));
        amountCol.setPrefWidth(120);

        TableColumn<PurchaseEntity, String> deletedDateCol = new TableColumn<>("DELETED ON");
        deletedDateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getUpdatedAt().format(DateTimeFormatter.ofPattern("dd-MMM HH:mm"))
        ));
        deletedDateCol.setPrefWidth(150);

        // Action Column
        TableColumn<PurchaseEntity, Void> actionCol = new TableColumn<>("ACTIONS");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button restoreBtn = new Button("⤴ Restore");
            {
                restoreBtn.setStyle("-fx-background-color: #0D9488; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
                restoreBtn.setOnAction(event -> {
                    PurchaseEntity p = getTableView().getItems().get(getIndex());
                    viewModel.restorePurchase(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(restoreBtn);
                }
            }
        });
        actionCol.setPrefWidth(100);

        tableView.getColumns().addAll(idCol, dateCol, vendorCol, amountCol, deletedDateCol, actionCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void refresh() {
        viewModel.loadDeletedPurchases();
    }
}
