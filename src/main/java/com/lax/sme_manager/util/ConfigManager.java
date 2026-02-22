package com.lax.sme_manager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Manages application configuration persistence (e.g., language, backup path).
 * Stores settings in a .properties file in the AppData directory.
 */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "config.properties";
    private static ConfigManager instance;

    private final Properties properties = new Properties();
    private final Path configPath;

    private ConfigManager() {
        this.configPath = DatabaseManager.getAppDataDir().resolve(CONFIG_FILE);
        load();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void load() {
        File file = configPath.toFile();
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                properties.load(is);
                LOGGER.info("Config loaded from {}", configPath);
            } catch (IOException e) {
                LOGGER.error("Failed to load config", e);
            }
        }
    }

    public void save() {
        try (OutputStream os = new FileOutputStream(configPath.toFile())) {
            properties.store(os, "Lax SME Manager Settings");
            LOGGER.info("Config saved to {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        save();
    }

    // Convenience Keys
    public static final String KEY_LANGUAGE = "app.language";
    public static final String KEY_BACKUP_PATH = "app.backup.path";
    public static final String KEY_LOGIN_PASSWORD = "app.security.login_password";
    public static final String KEY_RECYCLE_PASSWORD = "app.security.recycle_password";
    public static final String KEY_SECURITY_QUESTION = "app.security.question";
    public static final String KEY_SECURITY_ANSWER = "app.security.answer";
    public static final String KEY_SECURITY_QUESTION_2 = "app.security.question2";
    public static final String KEY_SECURITY_ANSWER_2 = "app.security.answer2";
}
