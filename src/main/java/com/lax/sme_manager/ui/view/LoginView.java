package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.util.PasswordManager;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Compact login dialog matching the app's light theme.
 */
public class LoginView extends StackPane {

    private final Runnable onLoginSuccess;
    private PasswordField passwordField;
    private Label errorLabel;
    private VBox loginCard;

    public LoginView(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        buildUI();
    }

    private void buildUI() {
        setPrefSize(420, 480);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 12;");

        loginCard = createLoginCard();
        getChildren().add(loginCard);
        playEntranceAnimation();
    }

    private VBox createLoginCard() {
        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 36, 32, 36));

        // --- App Icon ---
        StackPane iconContainer = new StackPane();
        Circle iconBg = new Circle(30);
        iconBg.setFill(Color.web("#0d9488"));
        iconBg.setEffect(new DropShadow(10, Color.web("#0d9488", 0.3)));

        Label iconText = new Label("L");
        iconText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        iconText.setTextFill(Color.WHITE);
        iconContainer.getChildren().addAll(iconBg, iconText);

        // --- Title ---
        Label title = new Label("Lax SME Manager");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#0f172a"));

        Label subtitle = new Label("Admin Portal \u2022 Secure Access");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        subtitle.setTextFill(Color.web("#94a3b8"));

        // --- Password Field ---
        VBox fieldContainer = new VBox(8);
        fieldContainer.setAlignment(Pos.CENTER_LEFT);
        fieldContainer.setPadding(new Insets(10, 0, 0, 0));

        Label fieldLabel = new Label("ADMIN PASSWORD");
        fieldLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        fieldLabel.setTextFill(Color.web("#64748b"));

        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password...");
        passwordField.setPrefHeight(44);
        passwordField.setStyle(getFieldStyle(false));
        passwordField.focusedProperty()
                .addListener((obs, old, focused) -> passwordField.setStyle(getFieldStyle(focused)));

        fieldContainer.getChildren().addAll(fieldLabel, passwordField);

        // --- Error Label ---
        errorLabel = new Label();
        errorLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        errorLabel.setTextFill(Color.web("#ef4444"));
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // --- Login Button ---
        Button loginBtn = new Button("Sign In");
        loginBtn.setPrefWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(44);
        loginBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        loginBtn.setTextFill(Color.WHITE);
        loginBtn.setStyle(getBtnStyle(false));
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(getBtnStyle(true)));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(getBtnStyle(false)));
        loginBtn.setOnAction(e -> handleLogin());

        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER)
                handleLogin();
        });

        // --- Forgot Password Link ---
        Hyperlink forgotLink = new Hyperlink("Forgot Password?");
        forgotLink.setStyle("-fx-text-fill: #0d9488; -fx-font-size: 12px; -fx-underline: false;");
        forgotLink.setOnAction(e -> handleForgotPassword());

        // --- Footer ---
        Label footer = new Label("\u00A9 2026 Lax Yard \u2022 v2.0");
        footer.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        footer.setTextFill(Color.web("#94a3b8"));

        card.getChildren().addAll(
                iconContainer, title, subtitle,
                fieldContainer, errorLabel, loginBtn, forgotLink, footer);
        return card;
    }

    private String getFieldStyle(boolean focused) {
        if (focused) {
            return "-fx-background-color: #ffffff;" +
                    "-fx-border-color: #0d9488; -fx-border-width: 1.5;" +
                    "-fx-border-radius: 10; -fx-background-radius: 10;" +
                    "-fx-text-fill: #1e293b; -fx-prompt-text-fill: #94a3b8;" +
                    "-fx-font-size: 14px; -fx-padding: 10 14;" +
                    "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.15), 8, 0, 0, 0);";
        }
        return "-fx-background-color: #f8fafc;" +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-background-radius: 10;" +
                "-fx-text-fill: #1e293b; -fx-prompt-text-fill: #94a3b8;" +
                "-fx-font-size: 14px; -fx-padding: 10 14;";
    }

    private String getBtnStyle(boolean hover) {
        if (hover) {
            return "-fx-background-color: #0f766e; -fx-background-radius: 10; -fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.4), 12, 0, 0, 3);";
        }
        return "-fx-background-color: #0d9488; -fx-background-radius: 10; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.25), 8, 0, 0, 2);";
    }

    private void handleLogin() {
        String password = passwordField.getText();
        if (password.isEmpty()) {
            showError("Please enter a password");
            shakeCard();
            return;
        }
        if (PasswordManager.validateLogin(password)) {
            onLoginSuccess.run();
        } else {
            showError("Incorrect password. Please try again.");
            shakeCard();
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    private void handleForgotPassword() {
        if (!PasswordManager.hasSecurityQuestions()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Password Recovery");
            alert.setHeaderText("Recovery Not Fully Set Up");
            alert.setContentText("You need to set up BOTH security questions in Settings to use this feature.\n\n" +
                    "To manual reset, delete 'config.properties' in AppData and restart.");
            alert.showAndWait();
            return;
        }

        // Question 1
        TextInputDialog q1Dialog = new TextInputDialog();
        q1Dialog.setTitle("Recovery: Phase 1");
        q1Dialog.setHeaderText("Question 1: " + PasswordManager.getSecurityQuestion1());
        q1Dialog.setContentText("Answer 1:");
        java.util.Optional<String> ans1 = q1Dialog.showAndWait();

        if (ans1.isPresent()) {
            // Question 2
            TextInputDialog q2Dialog = new TextInputDialog();
            q2Dialog.setTitle("Recovery: Phase 2");
            q2Dialog.setHeaderText("Question 2: " + PasswordManager.getSecurityQuestion2());
            q2Dialog.setContentText("Answer 2:");
            java.util.Optional<String> ans2 = q2Dialog.showAndWait();

            if (ans2.isPresent()) {
                if (PasswordManager.validateSecurityAnswers(ans1.get(), ans2.get())) {
                    TextInputDialog newPwDialog = new TextInputDialog();
                    newPwDialog.setTitle("Reset Password");
                    newPwDialog.setHeaderText("Identity Verified! Reset your password.");
                    newPwDialog.setContentText("New Password:");
                    java.util.Optional<String> newPw = newPwDialog.showAndWait();
                    if (newPw.isPresent() && !newPw.get().isEmpty()) {
                        PasswordManager.resetLoginPasswordWithAnswers(ans1.get(), ans2.get(), newPw.get());
                        Alert info = new Alert(Alert.AlertType.INFORMATION, "Password reset successfully!");
                        info.showAndWait();
                    }
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Incorrect security answers. Verification failed.");
                    error.showAndWait();
                }
            }
        }
    }

    private void showError(String msg) {
        errorLabel.setText("\u26A0 " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        });
        pause.play();
    }

    private void shakeCard() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), loginCard);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setAutoReverse(true);
        shake.setCycleCount(6);
        shake.setOnFinished(e -> loginCard.setTranslateX(0));
        shake.play();
    }

    private void playEntranceAnimation() {
        loginCard.setOpacity(0);
        loginCard.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(400), loginCard);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(400), loginCard);
        slide.setFromY(20);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.SPLINE(0.16, 1, 0.3, 1));

        ParallelTransition entrance = new ParallelTransition(fade, slide);
        entrance.setDelay(Duration.millis(100));
        entrance.setOnFinished(e -> passwordField.requestFocus());
        entrance.play();
    }
}
