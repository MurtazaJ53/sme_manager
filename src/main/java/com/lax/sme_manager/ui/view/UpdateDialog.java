package com.lax.sme_manager.ui.view;

import com.lax.sme_manager.ui.LaxSmeManagerApp;

import com.lax.sme_manager.service.UpdateService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class UpdateDialog {
    private final Stage stage;
    private final UpdateService.UpdateInfo updateInfo;
    private final UpdateService updateService;

    private ProgressBar progressBar;
    private Label statusLabel;
    private Button btnAction;
    private VBox downloadBox;

    public UpdateDialog(UpdateService.UpdateInfo info) {
        this.updateInfo = info;
        this.updateService = new UpdateService();
        this.stage = new Stage();
        LaxSmeManagerApp.setAppIcon(this.stage);
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #E2E8F0; -fx-border-radius: 16; -fx-border-width: 1;");
        root.setEffect(new javafx.scene.effect.DropShadow(20, Color.rgb(0, 0, 0, 0.15)));
        root.setPrefWidth(450);

        // Header
        VBox header = new VBox(10);
        header.setPadding(new Insets(30, 30, 20, 30));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 16 16 0 0;");

        Label badge = new Label("NEW UPDATE");
        badge.setStyle(
                "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 4 8 4 8; -fx-background-radius: 20;");

        Label title = new Label("SME Manager v" + info.latestVersion);
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        header.getChildren().addAll(badge, title);

        // Content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20, 30, 30, 30));

        Label notesTitle = new Label("WHAT'S NEW IN THIS VERSION:");
        notesTitle.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #94A3B8; -fx-letter-spacing: 0.1em;");

        TextArea releaseNotes = new TextArea(info.releaseNotes);
        releaseNotes.setEditable(false);
        releaseNotes.setWrapText(true);
        releaseNotes.setPrefHeight(120);
        releaseNotes.setStyle(
                "-fx-background-color: transparent; -fx-control-inner-background: #F8FAFC; -fx-background-insets: 0; -fx-padding: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-font-size: 13px; -fx-text-fill: #334155;");

        // Download Progress Box (Hidden initially)
        downloadBox = new VBox(10);
        downloadBox.setManaged(false);
        downloadBox.setVisible(false);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setStyle("-fx-accent: #0D9488;");

        statusLabel = new Label("Downloading update...");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        downloadBox.getChildren().addAll(statusLabel, progressBar);

        // Footer Actions
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button btnLater = new Button("Remind Me Later");
        btnLater.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: 600; -fx-cursor: hand;");
        btnLater.setOnAction(e -> stage.close());

        btnAction = new Button("Update Now");
        btnAction.setPrefWidth(140);
        btnAction.setPrefHeight(40);
        btnAction.setStyle(
                "-fx-background-color: #0F172A; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8; -fx-cursor: hand;");
        btnAction.setOnAction(e -> startDownload());

        footer.getChildren().addAll(btnLater, btnAction);

        content.getChildren().addAll(notesTitle, releaseNotes, downloadBox, footer);
        root.getChildren().addAll(header, content);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
    }

    private void startDownload() {
        btnAction.setDisable(true);
        downloadBox.setManaged(true);
        downloadBox.setVisible(true);

        updateService.downloadUpdate(updateInfo.downloadUrl, progress -> {
            progressBar.setProgress(progress);
            if (progress >= 1.0) {
                statusLabel.setText("Optimizing installation...");
                statusLabel.setStyle("-fx-text-fill: #0D9488; -fx-font-weight: 700;");
            } else {
                int percent = (int) (progress * 100);
                statusLabel.setText("Downloading: " + percent + "%");
            }
        });
    }

    public void show() {
        stage.show();
    }
}
