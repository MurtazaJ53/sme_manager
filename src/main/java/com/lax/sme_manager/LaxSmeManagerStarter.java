package com.lax.sme_manager;

import com.lax.sme_manager.util.AppLogger;
import javafx.application.Application;
import javafx.stage.Stage;
import com.lax.sme_manager.ui.LaxSmeManagerApp;
import org.slf4j.Logger;

public class LaxSmeManagerStarter extends Application {

    private static final Logger LOGGER = AppLogger.getLogger(LaxSmeManagerStarter.class);

    @Override
    public void init() throws Exception {
        // Run before start()
        LOGGER.info("Initializing Data Services...");

        // 1. Initialize Migrator
        com.lax.sme_manager.util.DatabaseMigrator migrator = new com.lax.sme_manager.util.DatabaseMigrator();
        migrator.migrate();

        // 2. Load Config
        com.lax.sme_manager.util.ConfigManager.getInstance();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // ✅ Initialize logging first
        AppLogger.logApplicationStartup("Lax SME Manager", "2.2");

        LOGGER.info("=== LAX SME MANAGER STARTED ===");
        LOGGER.info("Java Version: {}", System.getProperty("java.version"));

        new LaxSmeManagerApp(primaryStage);
        LOGGER.info("Application UI initialized successfully");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
