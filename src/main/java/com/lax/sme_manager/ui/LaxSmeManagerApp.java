package com.lax.sme_manager.ui;

import com.lax.sme_manager.repository.VendorRepository;
import com.lax.sme_manager.util.VendorCache;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import com.lax.sme_manager.util.BackupService;
import com.lax.sme_manager.repository.PurchaseRepository;
import com.lax.sme_manager.service.MetricsService;
import com.lax.sme_manager.service.PurchaseHistoryService;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.ui.view.DashboardView;
import com.lax.sme_manager.ui.view.PurchaseEntryView;
import com.lax.sme_manager.ui.view.PurchaseHistoryView;
import com.lax.sme_manager.viewmodel.PurchaseHistoryViewModel;
import com.lax.sme_manager.ui.view.PurchaseDetailView;
import com.lax.sme_manager.domain.Vendor;
import com.lax.sme_manager.repository.model.PurchaseEntity;
import com.lax.sme_manager.util.i18n.AppLabel;
import com.lax.sme_manager.ui.view.PurchaseEditView;
import com.lax.sme_manager.ui.view.ChequeWriterView;
import com.lax.sme_manager.ui.view.ChequeSettingsView; // Import
import com.lax.sme_manager.ui.view.SettingsView;
import com.lax.sme_manager.ui.view.VendorManagementView;
import com.lax.sme_manager.ui.view.LoginView;
import com.lax.sme_manager.ui.view.RecycleBinView;
import com.lax.sme_manager.viewmodel.RecycleBinViewModel;
import javafx.scene.control.TextInputDialog;
import com.lax.sme_manager.ui.component.AlertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LaxSmeManagerApp - Main application window
 * Features:
 * - Dark sidebar navigation with themed colors
 * - Dynamic content area (changes based on selection)
 * - All styling uses LaxTheme constants
 */
public class LaxSmeManagerApp {
    private final VendorCache vendorCache;
    private final PurchaseHistoryService historyService;
    private final MetricsService metricsService;
    private final VendorRepository vendorRepository;

    private Stage stage;
    private BorderPane root;
    private StackPane contentArea;
    private Button currentActiveButton;

    // Cached view instances for lazy loading
    private DashboardView dashboardView;
    private PurchaseEntryView purchaseEntryView;
    private PurchaseHistoryView purchaseHistoryView;
    private SettingsView settingsView;

    private static final Logger LOGGER = LoggerFactory.getLogger(LaxSmeManagerApp.class);
    private VendorManagementView vendorManagementView;
    private ChequeWriterView chequeWriterView;
    private ChequeSettingsView chequeSettingsView; // Declaration
    private RecycleBinView recycleBinView;

    public LaxSmeManagerApp(Stage stage) {
        this.stage = stage;

        // Initialize Core Services
        this.vendorRepository = new VendorRepository();
        this.vendorCache = new VendorCache(vendorRepository);
        this.vendorCache.initialize();

        PurchaseRepository purchaseRepository = new PurchaseRepository();
        this.historyService = new PurchaseHistoryService(purchaseRepository, vendorRepository);
        this.metricsService = new MetricsService(purchaseRepository);

        initialize();
    }

    private void initialize() {
        // Build the main app layout
        root = new BorderPane();
        root.setStyle(getBackgroundStyle());
        root.setLeft(createSidebar());
        contentArea = new StackPane();
        contentArea.setStyle(getContentAreaStyle());
        root.setCenter(contentArea);
        root.setBottom(createStatusBar());

        // Show Purchase Entry by default
        showPurchaseEntry();

        // Scene setup
        Scene scene = new Scene(root, 1400, 800);
        stage.setScene(scene);
        stage.setTitle("Lax Yard & SME Manager v2.0");

        // Set App Icon
        setAppIcon(stage);

        // Backup on close
        stage.setOnCloseRequest(e -> {
            new BackupService().performBackup();
            System.exit(0);
        });

        // Show login dialog FIRST (blocks until success)
        showLoginDialog();

        stage.show();
    }

    private void showLoginDialog() {
        Stage loginStage = new Stage();
        loginStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        loginStage.initOwner(stage);
        loginStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        loginStage.setTitle("Login");
        loginStage.setResizable(false);
        setAppIcon(loginStage);

        LoginView loginView = new LoginView(() -> loginStage.close());

        Scene loginScene = new Scene(loginView, 420, 480);
        loginScene.setFill(Color.TRANSPARENT);
        loginStage.setScene(loginScene);

        // Center on screen
        loginStage.centerOnScreen();
        loginStage.showAndWait(); // Blocks until closed
    }

    // ============ SIDEBAR NAVIGATION ============
    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setStyle(getSidebarStyle());
        sidebar.setPrefWidth(220);
        sidebar.setSpacing(0);

        // Header with Refresh Icon
        HBox headerContainer = new HBox();
        headerContainer.setAlignment(Pos.CENTER_LEFT);
        headerContainer.setStyle(getSidebarHeaderStyle());
        headerContainer.setPadding(new Insets(
                LaxTheme.Spacing.SPACE_20,
                LaxTheme.Spacing.SPACE_16,
                LaxTheme.Spacing.SPACE_20,
                LaxTheme.Spacing.SPACE_16));

        Label header = new Label("LAX MANAGER");
        header.setStyle("-fx-font-size: " + LaxTheme.Typography.FONT_SIZE_LG
                + "; -fx-font-weight: bold; -fx-text-fill: white;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄");
        refreshBtn.setTooltip(new Tooltip("Global Refresh"));
        refreshBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 16px; -fx-cursor: hand;");
        refreshBtn.setOnMouseEntered(e -> refreshBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 4;"));
        refreshBtn.setOnMouseExited(e -> refreshBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 16px; -fx-cursor: hand;"));
        refreshBtn.setOnAction(e -> globalRefresh());

        headerContainer.getChildren().addAll(header, headerSpacer, refreshBtn);
        VBox.setVgrow(headerContainer, Priority.NEVER);

        // Navigation items
        VBox navItems = new VBox();
        navItems.setStyle("-fx-spacing: " + LaxTheme.Spacing.SPACE_8 + ";");
        navItems.setPadding(new Insets(LaxTheme.Spacing.SPACE_12, 0, 0, 0));
        VBox.setVgrow(navItems, Priority.ALWAYS);

        // Navigation buttons
        Button dashboardBtn = createNavButton(AppLabel.TITLE_DASHBOARD.get(), false);
        dashboardBtn.setOnAction(e -> {
            updateActiveButton(dashboardBtn);
            showDashboard();
        });

        Button entryBtn = createNavButton(AppLabel.TITLE_PURCHASE_ENTRY.get(), true);
        entryBtn.setOnAction(e -> {
            updateActiveButton(entryBtn);
            showPurchaseEntry();
        });
        this.currentActiveButton = entryBtn;

        Button historyBtn = createNavButton(AppLabel.TITLE_PURCHASE_HISTORY.get(), false);
        historyBtn.setOnAction(e -> {
            updateActiveButton(historyBtn);
            showPurchaseHistory();
        });

        // Button reportsBtn = createNavButton("📈 Reports", false);
        // reportsBtn.setOnAction(e -> {
        // updateActiveButton(reportsBtn);
        // showReports();
        // });

        Button settingsBtn = createNavButton(AppLabel.TITLE_SETTINGS.get(), false);
        settingsBtn.setOnAction(e -> {
            updateActiveButton(settingsBtn);
            showSettings();
        });

        Button vendorsBtn = createNavButton("👥 Vendors", false);
        vendorsBtn.setOnAction(e -> {
            updateActiveButton(vendorsBtn);
            showVendorManagement();
        });

        Button chequeWriterBtn = createNavButton("Cheque Writer", false);
        chequeWriterBtn.setOnAction(e -> {
            updateActiveButton(chequeWriterBtn);
            showChequeWriter();
        });

        Button chequeSettingsBtn = createNavButton("Cheque Settings", false);
        chequeSettingsBtn.setOnAction(e -> {
            updateActiveButton(chequeSettingsBtn);
            showChequeSettings();
        });

        Button recycleBinBtn = createNavButton("🗑️ Recycle Bin", false);
        recycleBinBtn.setOnAction(e -> {
            updateActiveButton(recycleBinBtn);
            showRecycleBin();
        });

        navItems.getChildren().addAll(dashboardBtn, entryBtn, historyBtn,
                vendorsBtn, chequeWriterBtn, chequeSettingsBtn, recycleBinBtn, settingsBtn);
        sidebar.getChildren().addAll(headerContainer, navItems);

        return sidebar;
    }

    private Button createNavButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setPrefWidth(220);
        btn.setMinHeight(LaxTheme.Sidebar.BUTTON_MIN_HEIGHT);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(getSidebarButtonStyle(active));
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(
                LaxTheme.Sidebar.BUTTON_PADDING_V,
                LaxTheme.Sidebar.BUTTON_PADDING_H,
                LaxTheme.Sidebar.BUTTON_PADDING_V,
                LaxTheme.Sidebar.BUTTON_PADDING_H));
        btn.setCursor(javafx.scene.Cursor.HAND);

        btn.setOnMouseEntered(e -> {
            if (btn != currentActiveButton) {
                btn.setStyle(getSidebarButtonHoverStyle());
            }
        });

        btn.setOnMouseExited(e -> {
            if (btn != currentActiveButton) {
                btn.setStyle(getSidebarButtonStyle(false));
            }
        });

        return btn;
    }

    private void updateActiveButton(Button newActiveButton) {
        if (currentActiveButton != null) {
            currentActiveButton.setStyle(getSidebarButtonStyle(false));
        }
        newActiveButton.setStyle(getSidebarButtonStyle(true));
        currentActiveButton = newActiveButton;
    }

    // ============ SCREEN METHODS ============
    private void showDashboard() {
        if (dashboardView == null) {
            dashboardView = new DashboardView(metricsService);
        } else {
            dashboardView.refresh();
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(dashboardView);
    }

    private void showPurchaseEntry() {
        if (purchaseEntryView == null) {
            purchaseEntryView = new PurchaseEntryView(vendorCache);
        } else {
            purchaseEntryView.refresh();
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(purchaseEntryView);
    }

    private void showPurchaseHistory() {
        if (purchaseHistoryView == null) {
            purchaseHistoryView = new PurchaseHistoryView(
                    new PurchaseHistoryViewModel(historyService), vendorRepository);
            purchaseHistoryView.setOnPurchaseSelected(this::showPurchaseDetailsDialog);
            purchaseHistoryView.setOnPurchaseEdit(this::showPurchaseEditDialog);
        } else {
            purchaseHistoryView.refresh();
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(purchaseHistoryView);
    }

    private void showPurchaseDetailsDialog(PurchaseEntity purchase) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Purchase Details - #" + purchase.getId());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(800);
        dialog.getDialogPane().setPrefHeight(600);

        // Fetch vendor name directly from cache
        Vendor v = vendorCache.findById(purchase.getVendorId());
        String vName = (v != null) ? v.getName() : "Unknown (" + purchase.getVendorId() + ")";

        PurchaseDetailView detailView = new PurchaseDetailView(purchase, vName);
        dialog.getDialogPane().setContent(detailView);

        dialog.showAndWait();
    }

    private void showPurchaseEditDialog(PurchaseEntity purchase) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Purchase - #" + purchase.getId());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(800);
        dialog.getDialogPane().setPrefHeight(750);

        PurchaseEditView editView = new PurchaseEditView(purchase, vendorCache);
        editView.setOnSave(() -> {
            dialog.close();
            showPurchaseHistory(); // Refresh list after save
        });
        editView.setOnCancel(dialog::close);

        dialog.getDialogPane().setContent(editView);
        dialog.showAndWait();
    }

    // private void showReports() {
    // if (reportsView == null) {
    // reportsView = new VBox();
    // reportsView.setStyle(getScreenContentStyle());
    // reportsView.setPadding(new Insets(LaxTheme.Spacing.SPACE_24));
    // reportsView.setSpacing(LaxTheme.Spacing.SPACE_16);

    // Label title = new Label("📈 Reports");
    // title.setStyle(getScreenTitleStyle());

    // Label placeholder = new Label("Reports coming soon...");
    // placeholder.setStyle(getPlaceholderStyle());

    // reportsView.getChildren().addAll(title, placeholder);
    // }
    // contentArea.getChildren().clear();
    // contentArea.getChildren().add(reportsView);
    // }

    private void showChequeWriter() {
        if (chequeWriterView == null) {
            chequeWriterView = new ChequeWriterView();
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(chequeWriterView);
    }

    private void showSettings() {
        if (settingsView == null) {
            settingsView = new SettingsView();
        }
        contentArea.getChildren().clear();

        // CRITICAL: Must wrap in ScrollPane for accessibility
        ScrollPane scrollSettings = new ScrollPane(settingsView);
        scrollSettings.setFitToWidth(true);
        scrollSettings.setStyle("-fx-background-color: transparent;");
        contentArea.getChildren().add(scrollSettings);
    }

    private void showVendorManagement() {
        if (vendorManagementView == null) {
            vendorManagementView = new VendorManagementView();
        } else {
            vendorManagementView.refresh();
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(vendorManagementView);
    }

    private void showChequeSettings() {
        if (chequeSettingsView == null) {
            chequeSettingsView = new ChequeSettingsView();
            chequeSettingsView.setOnSaveCallback(() -> {
                if (chequeWriterView != null) {
                    chequeWriterView.refreshLayout();
                }
            });
        } else {
            chequeSettingsView.refreshBooksTable();
        }
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(chequeSettingsView);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        contentArea.getChildren().add(scrollPane);
    }

    private void showRecycleBin() {
        Dialog<String> passwordDialog = new Dialog<>();
        AlertUtils.styleDialog(passwordDialog);
        passwordDialog.setTitle("Secure Access");
        passwordDialog.setHeaderText("Recycle Bin is password protected.");

        ButtonType accessType = new ButtonType("Access", ButtonBar.ButtonData.OK_DONE);
        passwordDialog.getDialogPane().getButtonTypes().addAll(accessType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        PasswordField pf = new PasswordField();
        pf.setPromptText("Enter Management Password...");
        pf.setStyle(com.lax.sme_manager.ui.theme.LaxTheme.getInputStyle());

        Hyperlink forgotLink = new Hyperlink("Forgot Password?");
        forgotLink.setStyle("-fx-text-fill: #0d9488; -fx-font-size: 12px; -fx-underline: false;");
        forgotLink.setOnAction(e -> {
            if (!com.lax.sme_manager.util.PasswordManager.hasSecurityQuestions()) {
                AlertUtils.showInfo("Recovery Not Set",
                        "You need to set up BOTH security questions in Settings to use this feature.\n\n" +
                                "To manual reset, delete 'config.properties' from the AppData folder and restart.");
                return;
            }

            // Phase 1
            TextInputDialog q1Dialog = new TextInputDialog();
            AlertUtils.styleDialog(q1Dialog);
            q1Dialog.setTitle("Recovery: Phase 1");
            q1Dialog.setHeaderText("Question 1: " + com.lax.sme_manager.util.PasswordManager.getSecurityQuestion1());
            q1Dialog.setContentText("Secret Answer 1:");
            java.util.Optional<String> ans1 = q1Dialog.showAndWait();

            if (ans1.isPresent()) {
                // Phase 2
                TextInputDialog q2Dialog = new TextInputDialog();
                AlertUtils.styleDialog(q2Dialog);
                q2Dialog.setTitle("Recovery: Phase 2");
                q2Dialog.setHeaderText(
                        "Question 2: " + com.lax.sme_manager.util.PasswordManager.getSecurityQuestion2());
                q2Dialog.setContentText("Secret Answer 2:");
                java.util.Optional<String> ans2 = q2Dialog.showAndWait();

                if (ans2.isPresent()) {
                    if (com.lax.sme_manager.util.PasswordManager.validateSecurityAnswers(ans1.get(), ans2.get())) {
                        TextInputDialog newPwDialog = new TextInputDialog();
                        AlertUtils.styleDialog(newPwDialog);
                        newPwDialog.setTitle("Reset Password");
                        newPwDialog.setHeaderText("Identity Verified! Setup your new password.");
                        newPwDialog.setContentText("New Password:");
                        java.util.Optional<String> newPw = newPwDialog.showAndWait();
                        if (newPw.isPresent() && !newPw.get().isEmpty()) {
                            com.lax.sme_manager.util.PasswordManager.resetRecyclePasswordWithAnswers(ans1.get(),
                                    ans2.get(), newPw.get());
                            AlertUtils.showInfo("Password Reset", "Management password reset successfully!");
                        }
                    } else {
                        AlertUtils.showError("Access Denied", "Incorrect security answers. Verification failed.");
                    }
                }
            }
        });

        content.getChildren().addAll(new Label("Enter Management Password:"), pf, forgotLink);
        passwordDialog.getDialogPane().setContent(content);

        // Auto-focus password field
        javafx.application.Platform.runLater(pf::requestFocus);

        passwordDialog.setResultConverter(button -> {
            if (button == accessType) {
                return pf.getText();
            }
            return null;
        });

        java.util.Optional<String> result = passwordDialog.showAndWait();
        if (result.isPresent() && com.lax.sme_manager.util.PasswordManager.validateRecycleAccess(result.get())) {
            if (recycleBinView == null) {
                recycleBinView = new RecycleBinView(new RecycleBinViewModel(new PurchaseRepository()));
            } else {
                recycleBinView.refresh();
            }
            contentArea.getChildren().clear();
            contentArea.getChildren().add(recycleBinView);
        } else {
            if (result.isPresent()) {
                AlertUtils.showError("Error", "Incorrect password. Access denied.");
            }
            // Switch back to Dashboard or previous screen to avoid staying on empty
            // selection
            showDashboard();
            updateActiveButton((Button) ((VBox) root.getLeft()).getChildren().get(1)); // Hacky way to reset visual
                                                                                       // state
        }
    }

    private void globalRefresh() {
        // 1. Reset Caches
        vendorCache.refreshCache();

        // 2. Clear all cached views to force redesign as requested
        dashboardView = null;
        purchaseEntryView = null;
        purchaseHistoryView = null;
        settingsView = null;
        vendorManagementView = null;
        chequeWriterView = null;
        chequeSettingsView = null;
        recycleBinView = null;

        // 3. Reload current screen
        if (currentActiveButton != null) {
            currentActiveButton.fire();
        }

        // Show toast-like feedback in status bar if we had one,
        // for now just log it.
        System.out.println("Global Refresh Complete");
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(12);
        statusBar.setPadding(new Insets(6, 16, 6, 16));
        statusBar.setAlignment(Pos.CENTER_RIGHT);
        statusBar.setStyle(
                "-fx-background-color: #F1F5F9; -fx-border-color: #E2E8F0 transparent transparent transparent;");

        // DB Health Indicator
        HBox healthBox = new HBox(8);
        healthBox.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(4);
        dot.setFill(Color.web("#22C55E")); // Green

        Label healthLbl = new Label("Database Connected");
        healthLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        healthBox.getChildren().addAll(dot, healthLbl);

        statusBar.getChildren().add(healthBox);
        return statusBar;
    }

    // ============ STYLING (ALL USING LaxTheme) ============

    private String getBackgroundStyle() {
        return String.format(
                "-fx-background-color: %s;",
                LaxTheme.Colors.LIGHT_GRAY);
    }

    private String getContentAreaStyle() {
        return String.format(
                "-fx-background-color: %s;",
                LaxTheme.Colors.LIGHT_GRAY);
    }

    private String getSidebarStyle() {
        return String.format(
                "-fx-background-color: %s; -fx-text-fill: %s;",
                LaxTheme.Colors.DARK_NAVY,
                LaxTheme.Colors.WHITE);
    }

    private String getSidebarHeaderStyle() {
        return String.format(
                "-fx-font-size: %d; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: %s; " +
                        "-fx-border-color: rgba(255,255,255,0.1) transparent transparent transparent; " +
                        "-fx-border-width: 0 0 1 0;",
                LaxTheme.Typography.FONT_SIZE_LG,
                LaxTheme.Colors.WHITE);
    }

    private String getSidebarButtonStyle(boolean active) {
        if (active) {
            return String.format(
                    "-fx-background-color: rgba(13, 148, 136, 0.1); " +
                            "-fx-text-fill: white; " +
                            "-fx-border-color: %s transparent transparent transparent; " +
                            "-fx-border-width: 0 0 0 4; " +
                            "-fx-font-weight: %d; " +
                            "-fx-font-size: %d; " +
                            "-fx-min-height: %d;",
                    LaxTheme.Colors.PRIMARY_TEAL,
                    LaxTheme.Typography.WEIGHT_SEMIBOLD,
                    LaxTheme.Sidebar.FONT_SIZE,
                    LaxTheme.Sidebar.BUTTON_MIN_HEIGHT);
        }
        return String.format(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: rgba(255,255,255,0.8); " +
                        "-fx-border-color: transparent; " +
                        "-fx-font-size: %d; " +
                        "-fx-min-height: %d;",
                LaxTheme.Sidebar.FONT_SIZE,
                LaxTheme.Sidebar.BUTTON_MIN_HEIGHT);
    }

    private String getSidebarButtonHoverStyle() {
        return String.format(
                "-fx-background-color: rgba(255,255,255,0.05); " +
                        "-fx-text-fill: rgba(255,255,255,0.9); " +
                        "-fx-font-size: %d; " +
                        "-fx-min-height: %d;",
                LaxTheme.Sidebar.FONT_SIZE,
                LaxTheme.Sidebar.BUTTON_MIN_HEIGHT);
    }

    public static void setAppIcon(Stage targetStage) {
        try {
            if (targetStage == null)
                return;
            targetStage.getIcons().add(new Image(LaxSmeManagerApp.class.getResourceAsStream("/images/app_icon.png")));
        } catch (Exception e) {
            // Silently fail icon load
        }
    }
}