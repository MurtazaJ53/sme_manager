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
import com.lax.sme_manager.ui.view.PurchaseEditView;
import com.lax.sme_manager.ui.view.ChequeWriterView;
import com.lax.sme_manager.ui.view.ChequeSettingsView; // Import
import com.lax.sme_manager.ui.view.SettingsView;
import com.lax.sme_manager.ui.view.VendorManagementView;
import com.lax.sme_manager.ui.view.LoginView;
import com.lax.sme_manager.ui.view.RecycleBinView;
import com.lax.sme_manager.viewmodel.RecycleBinViewModel;
import com.lax.sme_manager.ui.view.PrintLedgerView;
import com.lax.sme_manager.ui.view.ReportsView;
import javafx.scene.control.TextInputDialog;
import com.lax.sme_manager.ui.component.AlertUtils;

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
    private com.lax.sme_manager.domain.User currentUser;

    // Cached view instances for lazy loading
    private DashboardView dashboardView;
    private PurchaseEntryView purchaseEntryView;
    private PurchaseHistoryView purchaseHistoryView;
    private SettingsView settingsView;

    private VendorManagementView vendorManagementView;
    private ChequeWriterView chequeWriterView;
    private ChequeSettingsView chequeSettingsView; // Declaration
    private PrintLedgerView printLedgerView;
    private ReportsView reportsView;
    private RecycleBinView recycleBinView;
    private Label queueCountBadge;

    public LaxSmeManagerApp(Stage stage) {
        this.stage = stage;

        // Initialize Core Services
        this.vendorRepository = new VendorRepository();
        this.vendorCache = new VendorCache(vendorRepository);
        this.vendorCache.initialize();

        PurchaseRepository purchaseRepository = new PurchaseRepository();
        com.lax.sme_manager.repository.TrendRepository trendRepository = new com.lax.sme_manager.repository.TrendRepository();
        this.historyService = new PurchaseHistoryService(purchaseRepository, vendorRepository);
        this.metricsService = new MetricsService(purchaseRepository, trendRepository);

        // Show login dialog FIRST (blocks until success)
        showLoginDialog();

        // If no user (dialog closed without login), exit
        if (currentUser == null) {
            System.exit(0);
        }

        initialize();
        stage.show();
    }

    private void initialize() {
        // Build the main app layout
        root = new BorderPane();
        root.setStyle(getBackgroundStyle());
        root.setLeft(createSidebar()); // Sidebar now respects currentUser role
        contentArea = new StackPane();
        contentArea.setStyle(getContentAreaStyle());
        root.setCenter(contentArea);
        root.setBottom(createStatusBar());

        // Show Purchase Entry by default
        showPurchaseEntry();

        // Scene setup
        Scene scene = new Scene(root, 1400, 800);
        stage.setScene(scene);
        stage.setTitle("Lax Yard & SME Manager v2.2");

        // Set App Icon
        setAppIcon(stage);

        // Backup on close
        stage.setOnCloseRequest(e -> {
            new BackupService().performBackup();
            System.exit(0);
        });

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

        LoginView loginView = new LoginView(user -> {
            this.currentUser = user;
            loginStage.close();
        });

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

        Button btnDashboard = createNavButton("\uD83D\uDCCA  Dashboard", false);
        btnDashboard.setOnAction(e -> {
            updateActiveButton(btnDashboard);
            showDashboard();
        });

        Button btnPurchase = createNavButton("\uD83D\uDCE6  Purchase Entry", true);
        btnPurchase.setOnAction(e -> {
            updateActiveButton(btnPurchase);
            showPurchaseEntry();
        });
        this.currentActiveButton = btnPurchase;

        Button btnVendors = createNavButton("\uD83D\uDC64  Manage Vendors", false);
        btnVendors.setOnAction(e -> {
            updateActiveButton(btnVendors);
            showVendorManagement();
        });

        Button btnHistory = createNavButton("\uD83D\uDCDC  Purchase History", false);
        btnHistory.setOnAction(e -> {
            updateActiveButton(btnHistory);
            showPurchaseHistory();
        });

        Button btnReports = createNavButton("\uD83D\uDCC8  Accountant Reports", false);
        btnReports.setOnAction(e -> {
            updateActiveButton(btnReports);
            showReports();
        });

        Button btnLedger = createNavButton("\uD83D\uDCD4  Print Ledger", false);
        btnLedger.setOnAction(e -> {
            updateActiveButton(btnLedger);
            showPrintLedger();
        });

        Button btnChequeWriter = createNavButton("\u270F\uFE0F  Cheque Writer", false);
        btnChequeWriter.setOnAction(e -> {
            updateActiveButton(btnChequeWriter);
            showChequeWriter();
        });

        Button btnChequeSettings = createNavButton("\u2699\uFE0F  Cheque Settings", false);
        btnChequeSettings.setOnAction(e -> {
            updateActiveButton(btnChequeSettings);
            showChequeSettings();
        });

        Button btnRecycle = createNavButton("\uD83D\uDDD1\uFE0F  Recycle Bin", false);
        btnRecycle.setOnAction(e -> {
            updateActiveButton(btnRecycle);
            showRecycleBin();
        });

        Button btnSettings = createNavButton("\u2699\uFE0F  Admin Settings", false);
        btnSettings.setOnAction(e -> {
            updateActiveButton(btnSettings);
            showSettings();
        });

        // --- PRINT QUEUE HUB (With Badge) ---
        StackPane queueContainer = new StackPane();
        Button btnQueue = createNavButton("\uD83D\uDDA5\uFE0F  Print Queue Hub", false);
        btnQueue.setOnAction(e -> showPrintQueueHub());

        queueCountBadge = new Label("0");
        queueCountBadge.setStyle(
                "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 5; -fx-background-radius: 10;");
        queueCountBadge.setMouseTransparent(true);
        StackPane.setAlignment(queueCountBadge, Pos.CENTER_RIGHT);
        StackPane.setMargin(queueCountBadge, new Insets(0, 15, 0, 0));
        queueCountBadge.setVisible(false); // Hide if 0

        queueContainer.getChildren().addAll(btnQueue, queueCountBadge);
        refreshQueueBadge();

        // Visibility Rule: Operator only sees Dashboard, Entry, Vendors, History,
        // Writer
        if (currentUser != null && !currentUser.isAdmin()) {
            btnChequeSettings.setManaged(false);
            btnChequeSettings.setVisible(false);
            btnRecycle.setManaged(false);
            btnRecycle.setVisible(false);
            btnSettings.setManaged(false);
            btnSettings.setVisible(false);
            btnLedger.setManaged(false);
            btnLedger.setVisible(false);
            btnReports.setManaged(false);
            btnReports.setVisible(false);
        }

        sidebar.getChildren().addAll(
                headerContainer,
                btnDashboard,
                btnPurchase,
                btnVendors,
                btnHistory,
                btnReports,
                btnLedger,
                btnChequeWriter,
                btnChequeSettings,
                btnRecycle,
                btnSettings,
                new Separator() {
                    {
                        setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10 0;");
                    }
                },
                queueContainer);

        // --- FOOTER SECTIONS (User Info & Logout) ---
        VBox footer = new VBox(5);
        footer.setPadding(new Insets(10, 16, 20, 16));
        footer.setStyle("-fx-border-color: rgba(255,255,255,0.05) transparent transparent transparent;");

        // User Badge
        HBox userBadge = new HBox(8);
        userBadge.setAlignment(Pos.CENTER_LEFT);
        userBadge.setPadding(new Insets(10, 0, 10, 0));

        javafx.scene.shape.Circle userDot = new javafx.scene.shape.Circle(4,
                currentUser != null && currentUser.isAdmin() ? javafx.scene.paint.Color.web("#ef4444")
                        : javafx.scene.paint.Color.web("#22c55e"));
        Label userName = new Label(currentUser != null ? currentUser.getUsername().toUpperCase() : "GUEST");
        userName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        Label userRole = new Label(currentUser != null ? "(" + currentUser.getRole() + ")" : "");
        userRole.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 10px;");

        userBadge.getChildren().addAll(userDot, userName, userRole);

        // Logout Button
        Button btnLogout = createNavButton("🚪 Logout", false);
        btnLogout.setStyle(btnLogout.getStyle() + "-fx-text-fill: #fca5a5;"); // Light red for logout
        btnLogout.setOnAction(e -> handleLogout());

        footer.getChildren().addAll(userBadge, btnLogout);

        // Quick Lock Button
        Button btnLock = createNavButton("🔒 Quick Lock", false);
        btnLock.setStyle(btnLock.getStyle() + "-fx-text-fill: #94a3b8;");
        btnLock.setOnAction(e -> handleQuickLock());

        footer.getChildren().add(0, btnLock); // Add at top of footer

        // Add a spacer to push footer to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);
        sidebar.getChildren().add(footer);

        return sidebar;
    }

    private void handleQuickLock() {
        if (currentUser == null)
            return;

        com.lax.sme_manager.domain.User previousUser = this.currentUser;
        stage.hide();

        // 1. Trigger Silent Backup
        runSilentBackup();

        // 2. Show login again
        showLoginDialog();

        if (currentUser != null) {
            if (previousUser != null && currentUser.getId().equals(previousUser.getId())) {
                // Same user returned, just show the stage
                stage.show();
            } else {
                // Different user, must clear all views for privacy
                dashboardView = null;
                purchaseEntryView = null;
                purchaseHistoryView = null;
                settingsView = null;
                vendorManagementView = null;
                chequeWriterView = null;
                chequeSettingsView = null;
                recycleBinView = null;

                initialize();
                stage.show();
            }
        } else {
            // Cancelled login = exit app for safety
            System.exit(0);
        }
    }

    private void runSilentBackup() {
        new Thread(() -> {
            try {
                new com.lax.sme_manager.util.BackupService().performBackup();
            } catch (Exception e) {
                System.err.println("Silent backup failed: " + e.getMessage());
            }
        }).start();
    }

    private void handleLogout() {
        if (AlertUtils.showConfirmation("Logout",
                "Are you sure you want to log out of: " + currentUser.getUsername() + "?")) {

            // 0. Trigger Silent Backup
            runSilentBackup();

            // 1. Reset Session
            currentUser = null;

            // 2. Clear Caches
            dashboardView = null;
            purchaseEntryView = null;
            purchaseHistoryView = null;
            settingsView = null;
            vendorManagementView = null;
            chequeWriterView = null;
            chequeSettingsView = null;
            recycleBinView = null;

            // 3. Clear UI
            stage.hide();

            // 4. Re-show Login
            showLoginDialog();

            // 5. If login successful, re-init and show
            if (currentUser != null) {
                initialize();
                stage.show();
            } else {
                System.exit(0);
            }
        }
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
            purchaseEntryView = new PurchaseEntryView(vendorCache, currentUser);
        } else {
            purchaseEntryView.refresh();
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(purchaseEntryView);
    }

    private void showPurchaseHistory() {
        if (purchaseHistoryView == null) {
            purchaseHistoryView = new PurchaseHistoryView(
                    new PurchaseHistoryViewModel(historyService), vendorRepository, currentUser);
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

        PurchaseEditView editView = new PurchaseEditView(purchase, vendorCache, currentUser);
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
            settingsView = new SettingsView(currentUser);
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

    private void showPrintQueueHub() {
        // We'll implement this dialog next
        com.lax.sme_manager.ui.view.PrintQueueHubDialog dialog = new com.lax.sme_manager.ui.view.PrintQueueHubDialog(
                stage, currentUser != null ? currentUser.getId() : null);
        dialog.setOnQueueChanged(this::refreshQueueBadge);
        dialog.showAndWait();
    }

    private void showPrintLedger() {
        if (printLedgerView == null) {
            printLedgerView = new PrintLedgerView();
        } else {
            printLedgerView.refresh();
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(printLedgerView);
    }

    private void showReports() {
        if (reportsView == null) {
            reportsView = new ReportsView(vendorCache);
        } else {
            reportsView.refresh();
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(reportsView);
    }

    private void refreshQueueBadge() {
        int count = new com.lax.sme_manager.repository.PrintQueueRepository().countItems();
        javafx.application.Platform.runLater(() -> {
            if (queueCountBadge != null) {
                queueCountBadge.setText(String.valueOf(count));
                queueCountBadge.setVisible(count > 0);
            }
        });
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
        printLedgerView = null;
        reportsView = null;
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