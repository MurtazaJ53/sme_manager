package com.lax.sme_manager.ui.component;

import com.lax.sme_manager.ui.LaxSmeManagerApp;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.StageStyle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.util.Optional;

/**
 * Modern, premium dialog utility for SME Manager.
 * Replaces all system-generated dialogs with styled CSS popups.
 */
public class AlertUtils {

    private static final String CSS_PATH = "/css/modern_alerts.css";

    /**
     * Apply premium CSS styling to any dialog.
     */
    public static void styleDialog(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        try {
            String cssUrl = AlertUtils.class.getResource(CSS_PATH).toExternalForm();
            dialogPane.getStylesheets().add(cssUrl);
        } catch (Exception e) {
            System.err.println("Could not load modern_alerts.css");
        }

        // Style specific buttons
        dialogPane.getButtonTypes().forEach(bt -> {
            Button btn = (Button) dialogPane.lookupButton(bt);
            if (btn != null) {
                if (bt == ButtonType.CANCEL || bt == ButtonType.NO || bt == ButtonType.CLOSE) {
                    btn.setStyle(
                            "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                                    "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 8; " +
                                    "-fx-background-radius: 8; -fx-padding: 9 22; -fx-font-size: 13px; " +
                                    "-fx-font-weight: bold; -fx-cursor: hand;");
                    btn.setOnMouseEntered(e -> btn.setStyle(
                            "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; " +
                                    "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-border-radius: 8; " +
                                    "-fx-background-radius: 8; -fx-padding: 9 22; -fx-font-size: 13px; " +
                                    "-fx-font-weight: bold; -fx-cursor: hand;"));
                    btn.setOnMouseExited(e -> btn.setStyle(
                            "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                                    "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 8; " +
                                    "-fx-background-radius: 8; -fx-padding: 9 22; -fx-font-size: 13px; " +
                                    "-fx-font-weight: bold; -fx-cursor: hand;"));
                } else {
                    btn.setStyle(
                            "-fx-background-color: #0d9488; -fx-text-fill: white; " +
                                    "-fx-background-radius: 8; -fx-padding: 9 22; -fx-font-size: 13px; " +
                                    "-fx-font-weight: bold; -fx-cursor: hand; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.25), 6, 0, 0, 2);");
                    btn.setOnMouseEntered(e -> btn.setStyle(
                            "-fx-background-color: #0f766e; -fx-text-fill: white; " +
                                    "-fx-background-radius: 8; -fx-padding: 9 22; -fx-font-size: 13px; " +
                                    "-fx-font-weight: bold; -fx-cursor: hand; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.4), 10, 0, 0, 3);"));
                    btn.setOnMouseExited(e -> btn.setStyle(
                            "-fx-background-color: #0d9488; -fx-text-fill: white; " +
                                    "-fx-background-radius: 8; -fx-padding: 9 22; -fx-font-size: 13px; " +
                                    "-fx-font-weight: bold; -fx-cursor: hand; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.25), 6, 0, 0, 2);"));
                }
            }
        });

        // Set minimum width for polish
        dialogPane.setMinWidth(400);

        // Attempt to set app icon on the stage
        javafx.stage.Window window = dialogPane.getScene().getWindow();
        if (window instanceof javafx.stage.Stage) {
            LaxSmeManagerApp.setAppIcon((javafx.stage.Stage) window);
        } else {
            // If window is not ready, attach a listener or just let it be.
            // Usually for Dialogs it's ready after show() or during init.
            dialog.showingProperty().addListener((obs, old, showing) -> {
                if (showing) {
                    javafx.stage.Window w = dialogPane.getScene().getWindow();
                    if (w instanceof javafx.stage.Stage) {
                        LaxSmeManagerApp.setAppIcon((javafx.stage.Stage) w);
                    }
                }
            });
        }
    }

    /**
     * Show a premium INFORMATION dialog.
     */
    public static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        styleDialog(alert);
        alert.showAndWait();
    }

    /**
     * Show a premium ERROR dialog.
     */
    public static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        styleDialog(alert);
        alert.showAndWait();
    }

    /**
     * Show a premium WARNING dialog.
     */
    public static void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        styleDialog(alert);
        alert.showAndWait();
    }

    /**
     * Show a premium CONFIRMATION dialog. Returns true if YES.
     */
    public static boolean showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(title);
        styleDialog(alert);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    /**
     * Create a styled Alert for advanced use (showAndWait with callback).
     */
    public static Alert createStyledAlert(Alert.AlertType type, String title, String content, ButtonType... buttons) {
        Alert alert = new Alert(type, content, buttons);
        alert.setTitle(title);
        alert.setHeaderText(title);
        styleDialog(alert);
        return alert;
    }
}
