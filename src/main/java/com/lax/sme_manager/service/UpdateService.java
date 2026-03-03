package com.lax.sme_manager.service;

import com.lax.sme_manager.util.AppLogger;
import javafx.application.Platform;
import org.slf4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UpdateService {
    private static final Logger LOGGER = AppLogger.getLogger(UpdateService.class);
    private static final String CURRENT_VERSION = "2.2";

    // Raw URL to the version.json file on your GitHub repository
    private static final String VERSION_JSON_URL = "https://raw.githubusercontent.com/murtuzalaxmidhar/sme_manager/main/version.json";

    public static class UpdateInfo {
        public String latestVersion;
        public String downloadUrl;
        public String releaseNotes;
        public boolean isUpdateAvailable;

        public UpdateInfo(String latestVersion, String downloadUrl, String releaseNotes, boolean isUpdateAvailable) {
            this.latestVersion = latestVersion;
            this.downloadUrl = downloadUrl;
            this.releaseNotes = releaseNotes;
            this.isUpdateAvailable = isUpdateAvailable;
        }
    }

    public CompletableFuture<UpdateInfo> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Real network call to fetch version.json
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(VERSION_JSON_URL))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    String version = extractJsonValue(body, "latestVersion");
                    String url = extractJsonValue(body, "downloadUrl");
                    String notes = extractJsonValue(body, "releaseNotes"); // Notes can be empty

                    // Compare versions
                    boolean available = isNewerVersion(version, CURRENT_VERSION);
                    return new UpdateInfo(version, url, notes, available);
                } else {
                    LOGGER.warn("Update check failed with status: {}", response.statusCode());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to check for updates: {}", e.getMessage());
            }

            // Return "No Update" if check fails
            return new UpdateInfo(CURRENT_VERSION, "", "", false);
        });
    }

    private boolean isNewerVersion(String latest, String current) {
        try {
            String[] v1 = latest.split("\\.");
            String[] v2 = current.split("\\.");
            int length = Math.max(v1.length, v2.length);
            for (int i = 0; i < length; i++) {
                int num1 = i < v1.length ? Integer.parseInt(v1[i]) : 0;
                int num2 = i < v2.length ? Integer.parseInt(v2[i]) : 0;
                if (num1 > num2)
                    return true;
                if (num1 < num2)
                    return false;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private String extractJsonValue(String json, String key) {
        try {
            int keyIndex = json.indexOf("\"" + key + "\"");
            if (keyIndex != -1) {
                int valueStart = json.indexOf("\"", keyIndex + key.length() + 2) + 1;
                int valueEnd = json.indexOf("\"", valueStart);
                return json.substring(valueStart, valueEnd);
            }
        } catch (Exception e) {
        }
        return "";
    }

    public void downloadUpdate(String downloadUrl, Consumer<Double> progressCallback) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(downloadUrl);
                Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "sme_manager_update.jar");

                try (InputStream in = new BufferedInputStream(url.openStream());
                        FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {

                    byte[] dataBuffer = new byte[1024];
                    int bytesRead;
                    long totalBytesRead = 0;
                    long fileSize = 5 * 1024 * 1024; // Mock size or get from content-length if possible

                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        if (progressCallback != null) {
                            double progress = (double) totalBytesRead / fileSize;
                            Platform.runLater(() -> progressCallback.accept(Math.min(0.99, progress)));
                        }
                    }
                }

                Platform.runLater(() -> progressCallback.accept(1.0));
                LOGGER.info("Update downloaded to {}", tempFile);

                createAndExecuteUpdaterScript(tempFile);

            } catch (Exception e) {
                LOGGER.error("Download failed: {}", e.getMessage());
            }
        });
    }

    private void createAndExecuteUpdaterScript(Path updateJar) throws IOException {
        String currentJarPath = ProcessHandle.current().info().command().orElse("");
        if (currentJarPath.isEmpty() || !currentJarPath.endsWith(".jar")) {
            // Fallback for development (running via gradle)
            LOGGER.warn("Not running from a JAR. Update swap skipped in dev mode.");
            return;
        }

        Path currentPath = Paths.get(currentJarPath);
        Path scriptPath = Paths.get("updater.bat");

        StringBuilder scriptContent = new StringBuilder();
        scriptContent.append("@echo off\n");
        scriptContent.append("timeout /t 2 /nobreak > nul\n"); // Wait for app to close
        scriptContent.append("move /y \"").append(updateJar.toString()).append("\" \"").append(currentPath.toString())
                .append("\"\n");
        scriptContent.append("start \"\" \"javaw\" -jar \"").append(currentPath.toString()).append("\"\n");
        scriptContent.append("del \"%~f0\"\n"); // Delete itself

        Files.writeString(scriptPath, scriptContent.toString());

        // Execute and exit
        Runtime.getRuntime().exec("cmd /c start " + scriptPath.toString());
        System.exit(0);
    }
}
