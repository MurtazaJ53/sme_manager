package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.service.MetricsService;
import com.lax.sme_manager.service.UpdateService;
import com.lax.sme_manager.ui.component.UIStyles;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.util.i18n.AppLabel;
import com.lax.sme_manager.viewmodel.DashboardViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.scene.chart.*;

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
        content.setPadding(new Insets(LaxTheme.Layout.MAIN_CONTAINER_PADDING));

        // Title
        Label title = new Label(AppLabel.TITLE_DASHBOARD.get());
        title.setStyle(UIStyles.getTitleStyle());

        // --- SECTION 1: METRIC CARDS ---
        HBox metricsRow = new HBox(LaxTheme.Spacing.SPACE_32);
        metricsRow.getChildren().addAll(
                createMetricCard(AppLabel.METRIC_BAGS_TODAY.get(), viewModel.bagsToday, "BLUE"),
                createMetricCard("Avg Rate/Bag", viewModel.avgRateThisMonth, "BLUE"),
                createMetricCard("Pending Clearing", viewModel.pendingClearingCount, "ORANGE"),
                createMetricCard(AppLabel.METRIC_UNPAID_CHEQUES.get(), viewModel.unpaidChequesCount.asString(), "RED"));

        // --- SECTION 2: VISUAL ANALYTICS ---
        HBox visualRow = new HBox(LaxTheme.Spacing.SPACE_32);

        // Weekly Trend Chart
        VBox trendCard = createChartCard("📊 Weekly Purchase Trend (Bags)", createTrendChart());
        HBox.setHgrow(trendCard, Priority.ALWAYS);

        // Payment Distribution Chart
        VBox paymentCard = createChartCard("💳 Payment Mode Split", createPaymentChart());
        paymentCard.setMinWidth(350);

        visualRow.getChildren().addAll(trendCard, paymentCard);

        // --- SECTION 3: TOP VENDORS & SYSTEM ---
        HBox bottomRow = new HBox(LaxTheme.Spacing.SPACE_32);

        VBox vendorsCard = createVendorsCard();
        HBox.setHgrow(vendorsCard, Priority.ALWAYS);

        VBox updateHub = createUpdateHub();
        updateHub.setMinWidth(400);

        bottomRow.getChildren().addAll(vendorsCard, updateHub);

        content.getChildren().addAll(title, metricsRow, visualRow, bottomRow);
        setContent(content);
    }

    private Chart createTrendChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date");
        yAxis.setLabel("Bags");

        BarChart<String, Number> bc = new BarChart<>(xAxis, yAxis);
        bc.setData(viewModel.weeklyTrendData);
        bc.setAnimated(true);
        bc.setLegendVisible(false);
        bc.setPrefHeight(300);
        return bc;
    }

    private Chart createPaymentChart() {
        PieChart pc = new PieChart(viewModel.paymentModeData);
        pc.setAnimated(true);
        pc.setLegendVisible(true);
        pc.setLegendSide(javafx.geometry.Side.BOTTOM);
        pc.setLabelsVisible(true);
        pc.setPrefHeight(300);
        return pc;
    }

    private VBox createChartCard(String title, Chart chart) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(24));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 20, 0, 0, 8); -fx-border-color: #E2E8F0; -fx-border-radius: 12;");

        Label lbl = new Label(title.toUpperCase());
        lbl.setStyle("-fx-font-size: 13; -fx-font-weight: 800; -fx-text-fill: #64748B; -fx-letter-spacing: 1px;");

        card.getChildren().addAll(lbl, chart);
        return card;
    }

    private VBox createVendorsCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(24));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 20, 0, 0, 8); -fx-border-color: #E2E8F0; -fx-border-radius: 12;");

        Label lbl = new Label("🏆 TOP VENDORS (BY VOL)");
        lbl.setStyle("-fx-font-size: 13; -fx-font-weight: 800; -fx-text-fill: #64748B; -fx-letter-spacing: 1px;");

        ListView<String> listView = new ListView<>(viewModel.topVendorsData);
        listView.setPrefHeight(200);
        listView.setStyle(
                "-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle(
                            "-fx-font-size: 14px; -fx-padding: 10; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;");
                }
            }
        });

        card.getChildren().addAll(lbl, listView);
        return card;
    }

    private VBox createUpdateHub() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(24));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 20, 0, 0, 8); -fx-border-color: #E2E8F0; -fx-border-radius: 12;");

        Label vLabel = new Label("SME Manager v2.2");
        vLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 16px; -fx-text-fill: #0F172A;");
        Label statusLabel = new Label("Your system is up to date");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        Button btnCheck = new Button("Check for Updates");
        btnCheck.setMaxWidth(Double.MAX_VALUE);
        btnCheck.setStyle(
                "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-cursor: hand;");

        btnCheck.setOnAction(e -> {
            btnCheck.setDisable(true);
            statusLabel.setText("Checking for updates...");
            new UpdateService().checkForUpdates().thenAccept(updateInfo -> {
                Platform.runLater(() -> {
                    btnCheck.setDisable(false);
                    if (updateInfo.isUpdateAvailable) {
                        statusLabel.setText("New Version Available: v" + updateInfo.latestVersion);
                        statusLabel.setStyle("-fx-text-fill: #0D9488; -fx-font-weight: 700;");
                    } else {
                        statusLabel.setText("You are using the latest version.");
                        statusLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: 400;");
                    }
                });
            });
        });

        card.getChildren().addAll(vLabel, statusLabel, btnCheck);
        return card;
    }

    private VBox createMetricCard(String title, javafx.beans.value.ObservableStringValue valueProperty,
            String accentColor) {
        VBox card = new VBox(LaxTheme.Spacing.SPACE_12);
        card.setPadding(new Insets(24));
        card.setPrefWidth(280);
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
                "-fx-text-fill: " + LaxTheme.Colors.TEXT_SECONDARY + "; -fx-font-size: 13; -fx-font-weight: 600;");

        Label valueLabel = new Label();
        valueLabel.textProperty().bind(valueProperty);
        valueLabel.setStyle("-fx-text-fill: " + LaxTheme.Colors.TEXT_PRIMARY
                + "; -fx-font-size: 32; -fx-font-weight: 700; -fx-letter-spacing: -1px;");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    @Override
    public void refresh() {
        if (viewModel != null) {
            viewModel.loadMetrics();
        }
    }
}
