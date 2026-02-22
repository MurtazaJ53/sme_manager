package com.lax.sme_manager.util;

/**
 * Centralized password management for the application.
 * Passwords are stored in config.properties via ConfigManager.
 * Default password for both login and recycle bin: admin123
 */
public class PasswordManager {

    private static final String DEFAULT_PASSWORD = "admin123";

    private PasswordManager() {
    }

    /**
     * Get the current login password.
     */
    public static String getLoginPassword() {
        return ConfigManager.getInstance()
                .getProperty(ConfigManager.KEY_LOGIN_PASSWORD, DEFAULT_PASSWORD);
    }

    /**
     * Get the current recycle bin password.
     */
    public static String getRecyclePassword() {
        return ConfigManager.getInstance()
                .getProperty(ConfigManager.KEY_RECYCLE_PASSWORD, DEFAULT_PASSWORD);
    }

    /**
     * Get the configured first security question.
     * Returns "" if not configured.
     */
    public static String getSecurityQuestion1() {
        return ConfigManager.getInstance().getProperty(ConfigManager.KEY_SECURITY_QUESTION, "");
    }

    /**
     * Get the configured second security question.
     * Returns "" if not configured.
     */
    public static String getSecurityQuestion2() {
        return ConfigManager.getInstance().getProperty(ConfigManager.KEY_SECURITY_QUESTION_2, "");
    }

    /**
     * Set both security questions and their answers.
     * Answers are stored in lowercase to make them case-insensitive.
     */
    public static void setSecurityQuestions(String q1, String a1, String q2, String a2) {
        ConfigManager manager = ConfigManager.getInstance();
        manager.setProperty(ConfigManager.KEY_SECURITY_QUESTION, q1);
        manager.setProperty(ConfigManager.KEY_SECURITY_ANSWER, a1.toLowerCase().trim());
        manager.setProperty(ConfigManager.KEY_SECURITY_QUESTION_2, q2);
        manager.setProperty(ConfigManager.KEY_SECURITY_ANSWER_2, a2.toLowerCase().trim());
    }

    /**
     * Check if security questions are configured.
     */
    public static boolean hasSecurityQuestions() {
        String q1 = getSecurityQuestion1();
        String q2 = getSecurityQuestion2();
        return q1 != null && !q1.isEmpty() && q2 != null && !q2.isEmpty();
    }

    /**
     * Validate both security answers.
     */
    public static boolean validateSecurityAnswers(String a1, String a2) {
        ConfigManager manager = ConfigManager.getInstance();
        String stored1 = manager.getProperty(ConfigManager.KEY_SECURITY_ANSWER, "");
        String stored2 = manager.getProperty(ConfigManager.KEY_SECURITY_ANSWER_2, "");

        boolean match1 = stored1.equalsIgnoreCase(a1.toLowerCase().trim());
        boolean match2 = stored2.equalsIgnoreCase(a2.toLowerCase().trim());

        return match1 && match2;
    }

    /**
     * Reset login password using security answers.
     */
    public static boolean resetLoginPasswordWithAnswers(String a1, String a2, String newPassword) {
        if (validateSecurityAnswers(a1, a2)) {
            ConfigManager.getInstance().setProperty(ConfigManager.KEY_LOGIN_PASSWORD, newPassword);
            return true;
        }
        return false;
    }

    /**
     * Reset recycle bin password using security answers.
     */
    public static boolean resetRecyclePasswordWithAnswers(String a1, String a2, String newPassword) {
        if (validateSecurityAnswers(a1, a2)) {
            ConfigManager.getInstance().setProperty(ConfigManager.KEY_RECYCLE_PASSWORD, newPassword);
            return true;
        }
        return false;
    }

    /**
     * Validate the old password and change the login password.
     * 
     * @return true if password was changed successfully
     */
    public static boolean changeLoginPassword(String oldPassword, String newPassword) {
        if (!getLoginPassword().equals(oldPassword)) {
            return false;
        }
        ConfigManager.getInstance().setProperty(ConfigManager.KEY_LOGIN_PASSWORD, newPassword);
        return true;
    }

    /**
     * Validate the old password and change the recycle bin password.
     * 
     * @return true if password was changed successfully
     */
    public static boolean changeRecyclePassword(String oldPassword, String newPassword) {
        if (!getRecyclePassword().equals(oldPassword)) {
            return false;
        }
        ConfigManager.getInstance().setProperty(ConfigManager.KEY_RECYCLE_PASSWORD, newPassword);
        return true;
    }

    /**
     * Validate login credentials.
     */
    public static boolean validateLogin(String password) {
        return getLoginPassword().equals(password);
    }

    /**
     * Validate recycle bin access.
     */
    public static boolean validateRecycleAccess(String password) {
        return getRecyclePassword().equals(password);
    }
}
