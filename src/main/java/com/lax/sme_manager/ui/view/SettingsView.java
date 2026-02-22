package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.repository.SignatureRepository;
import com.lax.sme_manager.repository.ChequeConfigRepository;
import com.lax.sme_manager.repository.model.SignatureConfig;
import com.lax.sme_manager.ui.component.AlertUtils;
import com.lax.sme_manager.repository.model.ChequeConfig;
import com.lax.sme_manager.ui.component.UIStyles;
import com.lax.sme_manager.ui.theme.LaxTheme;
import com.lax.sme_manager.util.ImageUtils;
import com.lax.sme_manager.util.BackupService;
import com.lax.sme_manager.util.PasswordManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class SettingsView extends VBox {

    private final SignatureRepository sigRepo = new SignatureRepository();
    private final ChequeConfigRepository configRepo = new ChequeConfigRepository();
    private final VBox signatureList = new VBox(10);
    private final ImageView previewView = new ImageView();
    private SignatureConfig activeConfig;
    private ChequeConfig chequeConfig;

    public SettingsView() {
        this.chequeConfig = configRepo.getConfig();
        if (this.chequeConfig == null)
            this.chequeConfig = new ChequeConfig();
        initializeUI();
        loadSignatures();
    }

    private void initializeUI() {
        setPadding(new Insets(25));
        setSpacing(32);
        setStyle("-fx-background-color: transparent;");

        Label titleLabel = new Label("⚙️ Settings");
        titleLabel.setStyle(UIStyles.getTitleStyle());

        // --- 1. GENERAL SETTINGS (Restored Language) ---
        VBox generalSection = createSection("🔧 General & Localization");
        GridPane generalGrid = new GridPane();
        generalGrid.setHgap(20);
        generalGrid.setVgap(15);

        generalGrid.add(new Label("Application Language:"), 0, 0);
        ComboBox<com.lax.sme_manager.util.i18n.LanguageMode> langCombo = new ComboBox<>();
        langCombo.getItems().addAll(com.lax.sme_manager.util.i18n.LanguageMode.values());
        langCombo.setValue(com.lax.sme_manager.util.i18n.LanguageManager.getInstance().getMode());
        langCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, n) -> {
            if (n != null)
                com.lax.sme_manager.util.i18n.LanguageManager.getInstance().setMode(n);
        });
        generalGrid.add(langCombo, 1, 0);

        generalGrid.add(new Label("Database Path:"), 0, 1);
        TextField dbPath = new TextField("data/lax_manager.db");
        dbPath.setEditable(false);
        dbPath.setPrefWidth(300);
        generalGrid.add(dbPath, 1, 1);

        Button btnBackup = new Button("📦 Create Backup Now");
        btnBackup.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY));
        btnBackup.setOnAction(e -> {
            new BackupService().performBackup();
            AlertUtils.showInfo("Information", "Manual Backup Successful");
        });
        generalGrid.add(btnBackup, 1, 2);
        generalSection.getChildren().add(generalGrid);

        // --- 2. CHEQUE CONFIGURATION ---
        VBox chequeSection = createSection("🏦 Cheque Print Defaults");
        GridPane chequeGrid = new GridPane();
        chequeGrid.setHgap(20);
        chequeGrid.setVgap(15);

        chequeGrid.add(new Label("Default Bank Name:"), 0, 0);
        TextField bankName = new TextField(chequeConfig.getBankName());
        bankName.setPrefWidth(250);
        chequeGrid.add(bankName, 1, 0);

        chequeGrid.add(new Label("Default Font Size:"), 0, 1);
        Slider fontSlider = new Slider(8, 24, chequeConfig.getFontSize() > 0 ? chequeConfig.getFontSize() : 12);
        fontSlider.setShowTickLabels(true);
        fontSlider.setShowTickMarks(true);
        chequeGrid.add(fontSlider, 1, 1);

        Button btnSaveCheque = new Button("Save Cheque Defaults");
        btnSaveCheque.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.PRIMARY));
        btnSaveCheque.setOnAction(e -> {
            chequeConfig.setBankName(bankName.getText());
            chequeConfig.setFontSize((int) fontSlider.getValue());
            configRepo.saveConfig(chequeConfig);
            AlertUtils.showInfo("Settings Saved", "Cheque defaults saved.");
        });
        chequeGrid.add(btnSaveCheque, 1, 2);
        chequeSection.getChildren().add(chequeGrid);

        // --- 3. SIGNATURE SECTION ---
        VBox sigSection = createSection("🖋️ Digital Signature Management");
        Label rbiNote = new Label(
                "RBI Compliance: Ensure scanned signatures look 'pen-authentic'. Check opacity and transparency.");
        rbiNote.setStyle(
                "-fx-text-fill: #b45309; -fx-background-color: #fffbeb; -fx-padding: 10; -fx-border-color: #f59e0b; -fx-font-size: 11px;");

        HBox sigLayout = new HBox(30);
        VBox left = new VBox(15);
        Button btnAdd = new Button("+ Add New Signature");
        btnAdd.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.PRIMARY));
        btnAdd.setOnAction(e -> handleAdd());

        signatureList.setPrefWidth(380);
        left.getChildren().addAll(btnAdd, new Label("Saved Signatures:"), signatureList);

        VBox right = new VBox(15);
        right.setStyle(
                "-fx-background-color: #f8fafc; -fx-padding: 15; -fx-border-color: #e2e8f0; -fx-background-radius: 8;");
        Label tuningLabel = new Label("Signature Tuning");
        tuningLabel.setStyle("-fx-font-weight: bold;");

        Slider opacitySlider = new Slider(0.1, 1.0, 1.0);
        CheckBox cbTransparent = new CheckBox("Remove Background (Make Transparent)");
        cbTransparent.setSelected(true);

        opacitySlider.valueProperty().addListener((obs, old, n) -> {
            if (activeConfig != null) {
                activeConfig.setOpacity(n.doubleValue());
                updatePreview();
            }
        });
        cbTransparent.selectedProperty().addListener((obs, old, n) -> {
            if (activeConfig != null) {
                activeConfig.setTransparent(n);
                updatePreview();
            }
        });

        StackPane previewBox = new StackPane(previewView);
        previewBox.setPrefSize(200, 70);
        previewBox.setStyle(
                "-fx-border-color: #64748b; -fx-border-style: dashed; -fx-background-color: white; -fx-padding: 5;");

        Button btnSaveSig = new Button("Apply Tuning & Save");
        btnSaveSig.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY));
        btnSaveSig.setOnAction(e -> {
            if (activeConfig != null) {
                sigRepo.saveSignature(activeConfig);
                AlertUtils.showInfo("Signature Saved", "Signature tuning saved.");
            }
        });

        right.getChildren().addAll(tuningLabel, new Label("Opacity:"), opacitySlider, cbTransparent,
                new Label("Actual Size Visualizer (Scale 1:1):"), previewBox, btnSaveSig);

        sigLayout.getChildren().addAll(left, right);
        sigSection.getChildren().addAll(rbiNote, sigLayout);

        // --- 4. SECURITY (Password Management) ---
        VBox securitySection = createSection("🔒 Security Settings");

        // Container for side-by-side password change forms
        HBox passwordsContainer = new HBox(30);
        passwordsContainer.setAlignment(Pos.TOP_LEFT);

        // -- Left: Login Password --
        VBox loginPwBox = new VBox(10);
        Label loginPwTitle = new Label("Change Login Password");
        loginPwTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");

        GridPane loginPwGrid = new GridPane();
        loginPwGrid.setHgap(12);
        loginPwGrid.setVgap(10);
        String labelStyle = "-fx-font-size: 12px; -fx-text-fill: #475569;";

        Label lOld = new Label("Current Password:");
        lOld.setStyle(labelStyle);
        PasswordField loginOldPw = new PasswordField();
        loginOldPw.setPromptText("Current");
        loginOldPw.setStyle(LaxTheme.getInputStyle());
        Label lNew = new Label("New Password:");
        lNew.setStyle(labelStyle);
        PasswordField loginNewPw = new PasswordField();
        loginNewPw.setPromptText("New");
        loginNewPw.setStyle(LaxTheme.getInputStyle());
        Label lConf = new Label("Confirm Password:");
        lConf.setStyle(labelStyle);
        PasswordField loginConfPw = new PasswordField();
        loginConfPw.setPromptText("Confirm");
        loginConfPw.setStyle(LaxTheme.getInputStyle());

        loginPwGrid.add(lOld, 0, 0);
        loginPwGrid.add(loginOldPw, 1, 0);
        loginPwGrid.add(lNew, 0, 1);
        loginPwGrid.add(loginNewPw, 1, 1);
        loginPwGrid.add(lConf, 0, 2);
        loginPwGrid.add(loginConfPw, 1, 2);

        Button btnChangeLogin = new Button("🔑 Change Login Password");
        btnChangeLogin.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.PRIMARY));
        btnChangeLogin.setOnAction(e -> {
            String oldPw = loginOldPw.getText();
            String newPw = loginNewPw.getText();
            String confPw = loginConfPw.getText();
            if (oldPw.isEmpty() || newPw.isEmpty() || confPw.isEmpty()) {
                AlertUtils.showWarning("Missing Fields", "Please fill all fields.");
                return;
            }
            if (!newPw.equals(confPw)) {
                AlertUtils.showError("Mismatch", "New password and confirmation do not match.");
                return;
            }
            if (newPw.length() < 4) {
                AlertUtils.showError("Too Short", "Password must be at least 4 characters.");
                return;
            }
            if (PasswordManager.changeLoginPassword(oldPw, newPw)) {
                AlertUtils.showInfo("Password Changed", "Login password updated successfully!");
                loginOldPw.clear();
                loginNewPw.clear();
                loginConfPw.clear();
            } else {
                AlertUtils.showError("Wrong Password", "Current password is incorrect.");
            }
        });
        loginPwBox.getChildren().addAll(loginPwTitle, loginPwGrid, btnChangeLogin);

        // -- Right: Recycle Bin Password --
        VBox recyclePwBox = new VBox(10);
        Label recyclePwTitle = new Label("Change Recycle Bin Password");
        recyclePwTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");

        GridPane recyclePwGrid = new GridPane();
        recyclePwGrid.setHgap(12);
        recyclePwGrid.setVgap(10);

        Label rOld = new Label("Current Password:");
        rOld.setStyle(labelStyle);
        PasswordField recycleOldPw = new PasswordField();
        recycleOldPw.setPromptText("Current");
        recycleOldPw.setStyle(LaxTheme.getInputStyle());
        Label rNew = new Label("New Password:");
        rNew.setStyle(labelStyle);
        PasswordField recycleNewPw = new PasswordField();
        recycleNewPw.setPromptText("New");
        recycleNewPw.setStyle(LaxTheme.getInputStyle());
        Label rConf = new Label("Confirm Password:");
        rConf.setStyle(labelStyle);
        PasswordField recycleConfPw = new PasswordField();
        recycleConfPw.setPromptText("Confirm");
        recycleConfPw.setStyle(LaxTheme.getInputStyle());

        recyclePwGrid.add(rOld, 0, 0);
        recyclePwGrid.add(recycleOldPw, 1, 0);
        recyclePwGrid.add(rNew, 0, 1);
        recyclePwGrid.add(recycleNewPw, 1, 1);
        recyclePwGrid.add(rConf, 0, 2);
        recyclePwGrid.add(recycleConfPw, 1, 2);

        Button btnChangeRecycle = new Button("🔑 Change Recycle Bin Password");
        btnChangeRecycle.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.PRIMARY));
        btnChangeRecycle.setOnAction(e -> {
            String oldPw = recycleOldPw.getText();
            String newPw = recycleNewPw.getText();
            String confPw = recycleConfPw.getText();
            if (oldPw.isEmpty() || newPw.isEmpty() || confPw.isEmpty()) {
                AlertUtils.showWarning("Missing Fields", "Please fill all fields.");
                return;
            }
            if (!newPw.equals(confPw)) {
                AlertUtils.showError("Mismatch", "New password and confirmation do not match.");
                return;
            }
            if (newPw.length() < 4) {
                AlertUtils.showError("Too Short", "Password must be at least 4 characters.");
                return;
            }
            if (PasswordManager.changeRecyclePassword(oldPw, newPw)) {
                AlertUtils.showInfo("Password Changed", "Recycle Bin password updated successfully!");
                recycleOldPw.clear();
                recycleNewPw.clear();
                recycleConfPw.clear();
            } else {
                AlertUtils.showError("Wrong Password", "Current password is incorrect.");
            }
        });
        recyclePwBox.getChildren().addAll(recyclePwTitle, recyclePwGrid, btnChangeRecycle);

        passwordsContainer.getChildren().addAll(loginPwBox, recyclePwBox);

        // Separator
        Separator sep = new Separator();
        sep.setPadding(new Insets(15, 0, 10, 0));

        // -- Security Question Setup --
        VBox secQuestionBox = new VBox(10);
        Label secQuestionTitle = new Label("Password Recovery (Security Question)");
        secQuestionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");
        Label secQuestionSub = new Label("Set up a security question to recover forgotten passwords.");
        secQuestionSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        GridPane sqGrid = new GridPane();
        sqGrid.setHgap(12);
        sqGrid.setVgap(10);

        Label q1Label = new Label("Security Question 1:");
        q1Label.setStyle(labelStyle);
        TextField sq1Field = new TextField();
        sq1Field.setPromptText("e.g. What is your mother's maiden name?");
        sq1Field.setPrefWidth(300);
        sq1Field.setStyle(LaxTheme.getInputStyle());
        sq1Field.setText(PasswordManager.getSecurityQuestion1());

        Label a1Label = new Label("Answer 1:");
        a1Label.setStyle(labelStyle);
        PasswordField ans1Field = new PasswordField();
        ans1Field.setPromptText("Answer to question 1");
        ans1Field.setPrefWidth(300);
        ans1Field.setStyle(LaxTheme.getInputStyle());

        Label q2Label = new Label("Security Question 2:");
        q2Label.setStyle(labelStyle);
        TextField sq2Field = new TextField();
        sq2Field.setPromptText("e.g. What was your first pet's name?");
        sq2Field.setPrefWidth(300);
        sq2Field.setStyle(LaxTheme.getInputStyle());
        sq2Field.setText(PasswordManager.getSecurityQuestion2());

        Label a2Label = new Label("Answer 2:");
        a2Label.setStyle(labelStyle);
        PasswordField ans2Field = new PasswordField();
        ans2Field.setPromptText("Answer to question 2");
        ans2Field.setPrefWidth(300);
        ans2Field.setStyle(LaxTheme.getInputStyle());

        sqGrid.add(q1Label, 0, 0);
        sqGrid.add(sq1Field, 1, 0);
        sqGrid.add(a1Label, 0, 1);
        sqGrid.add(ans1Field, 1, 1);

        sqGrid.add(q2Label, 0, 2);
        sqGrid.add(sq2Field, 1, 2);
        sqGrid.add(a2Label, 0, 3);
        sqGrid.add(ans2Field, 1, 3);

        boolean alreadySet = PasswordManager.hasSecurityQuestions();
        sqGrid.setDisable(alreadySet);

        // Verification field if questions already exist
        VBox actionBox = new VBox(10);
        Button btnSaveSq = new Button("🛡️ Save Recovery Setup");
        btnSaveSq.setStyle(LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY));
        btnSaveSq.setDisable(alreadySet);

        if (alreadySet) {
            Button btnUnlock = new Button("🔓 Change Recovery Questions");
            btnUnlock.setStyle(
                    "-fx-background-color: #f8fafc; -fx-text-fill: #475569; -fx-border-color: #e2e8f0; -fx-border-radius: 6;");

            btnUnlock.setOnAction(e -> {
                Dialog<String> authDialog = new Dialog<>();
                authDialog.setTitle("Authorize Change");
                authDialog.setHeaderText("Enter Admin Password to modify recovery settings.");
                AlertUtils.styleDialog(authDialog);

                ButtonType okBtn = new ButtonType("Authorize", ButtonBar.ButtonData.OK_DONE);
                authDialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

                PasswordField pf = new PasswordField();
                pf.setPromptText("Current Login Password");
                pf.setStyle(LaxTheme.getInputStyle());
                authDialog.getDialogPane().setContent(new VBox(10, new Label("Admin Password:"), pf));

                authDialog.setResultConverter(b -> b == okBtn ? pf.getText() : null);

                java.util.Optional<String> result = authDialog.showAndWait();
                if (result.isPresent() && PasswordManager.validateLogin(result.get())) {
                    sqGrid.setDisable(false);
                    btnSaveSq.setDisable(false);
                    btnUnlock.setDisable(true);
                    btnUnlock.setText("🔓 Unlocked for Editing");
                } else if (result.isPresent()) {
                    AlertUtils.showError("Unauthorized", "Incorrect password.");
                }
            });
            actionBox.getChildren().add(btnUnlock);
        }

        btnSaveSq.setOnAction(e -> {
            String q1 = sq1Field.getText().trim();
            String a1 = ans1Field.getText().trim();
            String q2 = sq2Field.getText().trim();
            String a2 = ans2Field.getText().trim();

            if (q1.isEmpty() || a1.isEmpty() || q2.isEmpty() || a2.isEmpty()) {
                AlertUtils.showWarning("Missing Fields", "Please enter both questions and answers.");
                return;
            }

            PasswordManager.setSecurityQuestions(q1, a1, q2, a2);
            AlertUtils.showInfo("Protected",
                    "Security questions updated successfully!\nThey are now locked again for safety.");

            // Clean up and Lock Again
            ans1Field.clear();
            ans2Field.clear();
            sqGrid.setDisable(true);
            btnSaveSq.setDisable(true);

            // Re-show the settings to refresh the "Unlock" button logic
            // In a real app we'd trigger a reload, but for now just locking UI is enough.
        });

        secQuestionBox.getChildren().addAll(secQuestionTitle, secQuestionSub, sqGrid, actionBox, btnSaveSq);

        securitySection.getChildren().addAll(passwordsContainer, sep, secQuestionBox);

        getChildren().addAll(titleLabel, generalSection, chequeSection, sigSection, securitySection);
    }

    private void loadSignatures() {
        signatureList.getChildren().clear();
        List<SignatureConfig> list = sigRepo.getAllSignatures();
        for (SignatureConfig sig : list) {
            HBox item = new HBox(10);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setStyle(
                    "-fx-padding: 8; -fx-border-color: #e2e8f0; -fx-background-color: white; -fx-background-radius: 4;");

            Label name = new Label(sig.getName());
            name.setPrefWidth(140);
            name.setWrapText(true);
            name.setStyle("-fx-font-size: 12px;");

            Button btnActive = new Button(
                    chequeConfig.getActiveSignatureId() == sig.getId() ? "✅ Active" : "Set Active");
            btnActive.setStyle(chequeConfig.getActiveSignatureId() == sig.getId()
                    ? "-fx-background-color: #0d9488; -fx-text-fill: white;"
                    : LaxTheme.getButtonStyle(LaxTheme.ButtonType.SECONDARY));
            btnActive.setOnAction(e -> {
                chequeConfig.setActiveSignatureId(sig.getId());
                configRepo.saveConfig(chequeConfig);
                loadSignatures();
                AlertUtils.showInfo("Signature Updated", "Signature set as active for printing.");
            });

            Button btnSelect = new Button("⚙️");
            btnSelect.setTooltip(new Tooltip("Edit Tuning"));
            btnSelect.setOnAction(e -> selectSignature(sig));

            Button btnDel = new Button("🗑️");
            btnDel.setStyle("-fx-text-fill: #ef4444; -fx-background-color: transparent;");
            btnDel.setOnAction(e -> {
                sigRepo.deleteSignature(sig.getId());
                if (chequeConfig.getActiveSignatureId() == sig.getId()) {
                    chequeConfig.setActiveSignatureId(0);
                    configRepo.saveConfig(chequeConfig);
                }
                loadSignatures();
            });

            item.getChildren().addAll(name, btnActive, btnSelect, btnDel);
            signatureList.getChildren().add(item);
        }
    }

    private void selectSignature(SignatureConfig sig) {
        this.activeConfig = sig;
        updatePreview();
    }

    private void handleAdd() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Signature Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try {
                SignatureConfig sig = SignatureConfig.builder()
                        .name(file.getName())
                        .path(file.getAbsolutePath())
                        .opacity(1.0)
                        .isTransparent(true)
                        .scale(1.0)
                        .thickness(1.0)
                        .build();
                sigRepo.saveSignature(sig);

                // Immediately reload and select the new one
                loadSignatures();
                List<SignatureConfig> updatedList = sigRepo.getAllSignatures();
                SignatureConfig added = updatedList.stream()
                        .filter(s -> s.getPath().equals(file.getAbsolutePath()))
                        .findFirst().orElse(null);
                if (added != null)
                    selectSignature(added);

                AlertUtils.showInfo("Information", "Signature uploaded and selected for tuning.");
            } catch (Exception e) {
                AlertUtils.showError("Error", "Failed to upload: " + e.getMessage());
            }
        }
    }

    private void updatePreview() {
        if (activeConfig == null)
            return;
        var img = ImageUtils.processSignature(activeConfig.getPath(), activeConfig.getOpacity(),
                activeConfig.isTransparent());
        if (img != null) {
            previewView.setImage(img);
            previewView.setFitWidth(180);
            previewView.setFitHeight(60);
            previewView.setPreserveRatio(true);
        } else {
            previewView.setImage(null);
            Label error = new Label("Image load fail");
            error.setStyle("-fx-text-fill: red;");
            // stackpane will show it
        }
    }

    private VBox createSection(String title) {
        VBox box = new VBox(15);
        box.setStyle(UIStyles.getCardStyle());
        box.setPadding(new Insets(20));
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #334155;");
        box.getChildren().add(lbl);
        return box;
    }
}
