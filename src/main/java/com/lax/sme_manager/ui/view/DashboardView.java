package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.service.MetricsService;
import com.lax.sme_manager.service.UpdateService;
import com.lax.sme_manager.ui.component.UIStyles;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.ui.view.UpdateDialog;
import com.lax.sme_manager.util.i18n.AppLabel;
import com.lax.sme_manager.viewmodel.DashboardViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;

public class DashboardView extends ScrollPane implements RefreshableView {

    private final DashboardViewModel viewModel;

    public DashboardView(MetricsService metricsService) {
        this.viewModel = new DashboardViewModel(metricsService);
        initializeUI();
        viewModel.loadMetrics();
    }

    private void initializeUI() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: " + LaxTheme.Colors.LIGHT_GRAY + ";");

        VBox content = new VBox(LaxTheme.Spacing.SPACE_32);
        content.setPadding(new Insets(LaxTheme.Layout.MAIN_CONTAINER_PADDING)); // Design Manifest: 25px

        // Title
        Label title = new Label(AppLabel.TITLE_DASHBOARD.get());
        title.setStyle(UIStyles.getTitleStyle());

        // Sections
        VBox purchaseSection = createSection("📦 Purchase Overview");
        GridPane purchaseGrid = createPaddingGrid();
        purchaseGrid.add(createMetricCard(AppLabel.METRIC_BAGS_TODAY.get(), viewModel.bagsToday, "BLUE"), 0, 0);
        purchaseGrid.add(createMetricCard(AppLabel.METRIC_BAGS_WEEK.get(), viewModel.bagsThisWeek, "BLUE"), 1, 0);
        purchaseGrid.add(createMetricCard(AppLabel.METRIC_BAGS_MONTH.get(), viewModel.bagsThisMonth, "BLUE"), 2, 0);
        purchaseSection.getChildren().add(purchaseGrid);

        VBox financialSection = createSection("💰 Financial Status");
        GridPane financialGrid = createPaddingGrid();
        financialGrid.add(createMetricCard("Total (Month)", viewModel.amountThisMonth, "BLUE"), 0, 0);
        financialGrid.add(createMetricCard("Avg Rate/Bag", viewModel.avgRateThisMonth, "BLUE"), 1, 0);
        financialSection.getChildren().add(financialGrid);

        VBox criticalSection = createSection("⚠️ Critical Handlers");
        GridPane criticalGrid = createPaddingGrid();
        criticalGrid.add(createMetricCard(AppLabel.METRIC_UNPAID_CHEQUES.get(),
                viewModel.unpaidChequesCount.asString(), "RED"), 0, 0);
        criticalGrid.add(createMetricCard("Cheques Clearing Today", viewModel.clearingTodayCount, "ORANGE"), 1, 0);
        criticalSection.getChildren().add(criticalGrid);

        VBox updateSection = createUpdateHub();

        content.getChildren().addAll(title, purchaseSection, financialSection, criticalSection, updateSection);
        setContent(content);
    }

    private VBox createUpdateHub() {
        VBox section = createSection("🚀 System Update");
        
        HBox card = new HBox(20);
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 20, 0, 0, 8); -fx-border-color: #E2E8F0; -fx-border-radius: 12;");
        card.setPrefWidth(600);

        VBox info = new VBox(4);
        Label vLabel = new Label("SME Manager v2.0");
        vLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 16px; -fx-text-fill: #0F172A;");
        Label statusLabel = new Label("Your system is up to date");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        info.getChildren().addAll(vLabel, statusLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnCheck = new Button("Check for Updates");
        btnCheck.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 8 16; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-cursor: hand;");
        
        btnCheck.setOnAction(e -> {
            btnCheck.setDisable(true);
            statusLabel.setText("Checking for updates...");
            new UpdateService().checkForUpdates().thenAccept(updateInfo -> {
                Platform.runLater(() -> {
                    btnCheck.setDisable(false);
                    if (updateInfo.isUpdateAvailable) {
                        statusLabel.setText("New Version Available: v" + updateInfo.latestVersion);
                        statusLabel.setStyle("-fx-text-fill: #0D9488; -fx-font-weight: 700;");
                        new UpdateDialog(updateInfo).show();
                    } else {
                        statusLabel.setText("You are using the latest version.");
                        statusLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: 400;");
                    }
                });
            });
        });

        card.getChildren().addAll(info, spacer, btnCheck);
        section.getChildren().add(card);
        return section;
    }

    private VBox createSection(String title) {
        VBox section = new VBox(LaxTheme.Spacing.SPACE_16);
        Label lbl = new Label(title.toUpperCase());
        lbl.setStyle("-fx-font-size: 13; -fx-font-weight: 700; -fx-text-fill: " + LaxTheme.Colors.TEXT_SECONDARY
                + "; -fx-letter-spacing: 1px;");
        section.getChildren().add(lbl);
        return section;
    }

    private GridPane createPaddingGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(LaxTheme.Spacing.SPACE_32); // Increased gutter
        grid.setVgap(LaxTheme.Spacing.SPACE_32);
        return grid;
    }

    private VBox createMetricCard(String title, javafx.beans.value.ObservableStringValue valueProperty,
            String accentColor) {
        VBox card = new VBox(LaxTheme.Spacing.SPACE_12);
        card.setPadding(new Insets(24));
        card.setPrefWidth(280);
        card.setPrefHeight(140);
        card.setAlignment(Pos.CENTER_LEFT);

        String accentHex;
        String bgHex;
        switch (accentColor) {
            case "RED":
                accentHex = LaxTheme.Colors.ERROR;
                bgHex = "#FEF2F2";
                break;
            case "ORANGE":
                accentHex = LaxTheme.Colors.WARNING;
                bgHex = "#FFFBEB";
                break;
            case "GREEN":
                accentHex = LaxTheme.Colors.SUCCESS;
                bgHex = "#F0FDF4";
                break;
            default:
                accentHex = LaxTheme.Colors.PRIMARY_TEAL;
                bgHex = "#FFFFFF";
                break;
        }

        card.setStyle(String.format("-fx-background-color: %s; " +
                "-fx-background-radius: " + LaxTheme.BorderRadius.RADIUS_LG + "; " +
                "-fx-border-color: %s; -fx-border-width: 0 0 0 6; -fx-border-radius: 2; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 40, 0, 0, 10);",
                bgHex, accentHex));

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: " + LaxTheme.Colors.TEXT_SECONDARY + "; -fx-font-size: 13; -fx-font-weight: 500;");

        Label valueLabel = new Label();
        valueLabel.textProperty().bind(valueProperty);
        valueLabel.setStyle("-fx-text-fill: " + LaxTheme.Colors.TEXT_PRIMARY
                + "; -fx-font-size: 32; -fx-font-weight: 700; -fx-letter-spacing: -1px;");

        card.getChildren().addAll(titleLabel, valueLabel);

        // Add indicator for updates
        viewModel.isLoading.addListener((obs, old, loading) -> {
            Platform.runLater(() -> card.setOpacity(loading ? 0.7 : 1.0));
        });

        return card;
    }

    @Override
    public void refresh() {
        if (viewModel != null) {
            viewModel.loadMetrics();
        }
    }
}
