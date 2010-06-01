package be.hogent.tarsos.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Utility class to access (read and write) configuration settings. There are
 * utility methods for booleans, doubles and integers. It automatically converts
 * directory separators with the correct file separator for the current
 * operating system.
 * @author Joren Six
 */
public final class Configuration {
    /**
     * Log messages.
     */
    private static final Logger LOG = Logger.getLogger(Configuration.class.getName());

    /**
     * The default configuration, defined in a properties file.
     */
    private static Properties defaultConfProps;

    /**
     * The preferences of the user. Defined in an XML-file (UNIX) or registry
     * (Windows).
     */
    private static Preferences userPreferences = null;

    // hides default constuctor
    private Configuration() {
    }

    /************************************************
     * Get configuration methods.
     ************************************************/

    /**
     * Read the configuration for a certain key. If the key can not be found in
     * the UserPreferences the default is loaded from a properties file, written
     * in the UserPreferences, and returned. Otherwise the user configured value
     * is returned.
     * @param key
     *            the configuration key
     * @return if the key is configured, the configured value. Otherwise the
     *         default value is returned.
     */
    public static String get(final ConfKey key) {
        return get(key.name());
    }

    /**
     * Read the configured Integer for a certain key. If the key can not be
     * found in the UserPreferences the default is loaded from a properties
     * file, written in the UserPreferences, and returned. Otherwise the
     * configured value is returned.
     * @param key
     *            The configuration key.
     * @return if the key is configured, the configured value. Otherwise the
     *         default value is returned.
     * @exception NumberFormatException
     *                If the configured value can not be parsed to an integer a
     *                is thrown.
     */
    public static int getInt(final ConfKey key) {
        return Integer.parseInt(get(key.name()));
    }

    /**
     * Read the configuration for a certain key. If the key can not be found in
     * the UserPreferences the default is loaded from a properties file, written
     * in the UserPreferences, and returned. Otherwise the configured value is
     * returned.
     * @param key
     *            The configuration key.
     * @return If the key is configured, the configured value. Otherwise the
     *         default value is returned.
     */
    public static double getDouble(final ConfKey key) {
        return Double.parseDouble(get(key.name()));
    }

    /**
     * Parses a configured string as a boolean. The boolean returned represents
     * the value true if the configured value is not null and is equal, ignoring
     * case, to the string "true".
     * <p>
     * Example: <code>Boolean.parseBoolean("True")</code> returns
     * <code>true</code>.<br>
     * Example: <code>Boolean.parseBoolean("yes")</code> returns
     * <code>false</code>.
     * </p>
     * @param key
     *            the name of the configuration parameter
     * @return <code>true</code> if the configured value is not null and is
     *         equal, ignoring case, to the string "true", <code>false</code>
     *         otherwise.
     */
    public static boolean getBoolean(final ConfKey key) {
        return Boolean.parseBoolean(get(key));
    }

    /**
     * Fetches configured values. If no values are configured a default
     * configuration is written (based on configuration.properties).
     * @param key
     *            the name of the configuration parameter
     * @return a configured or default value
     */
    private static String get(final String key) {
        synchronized (Configuration.class) {
            if (defaultConfProps == null) {
                defaultConfProps = new Properties();
                final InputStream propertiesStream = Configuration.class
                .getResourceAsStream("configuration.properties");
                try {
                    defaultConfProps.load(propertiesStream);
                } catch (final IOException e) {
                    LOG.severe("Could not find default configurations");
                }
            }
        }
        final String defaultValue = defaultConfProps.getProperty(key);

        synchronized (Configuration.class) {
            if (userPreferences == null) {
                userPreferences = Preferences.userNodeForPackage(Configuration.class);
                for (final ConfKey configKey : ConfKey.values()) {
                    // If there is no configuration for this user, write the default
                    // configuration
                    if (userPreferences.get(configKey.name(), null) == null) {
                        final String defaultConfigVal = defaultConfProps.getProperty(configKey
                                .name());
                        set(configKey, defaultConfigVal);
                    }
                }
            }
        }
        String configuredValue = userPreferences.get(key, defaultValue);
        configuredValue = sanitizeConfiguredValue(key, configuredValue);
        return configuredValue;
    }

    /**
     * Set a configuration parameter. Sanitizes the value automatically: removes
     * whitespace and correct file separators if the configured value is a
     * directory.
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public static void set(final ConfKey key, final String value) {
        if (value == null) {
            return;
        }
        try {
            final String actualValue = sanitizeConfiguredValue(key.name(), value);
            userPreferences.put(key.name(), actualValue);
            userPreferences.flush();
            LOG.info(key.name() + " = " + actualValue);
        } catch (final BackingStoreException e) {
            LOG.severe("Could not save preference for " + key.name());
        }
    }

    /**
     * See the other set method.
     * @param key
     * @param value
     */
    public static void set(final ConfKey key, final Object value) {
        set(key, value.toString());
    }

    /**
     * Sanitizes each configured value. Is called every time when a
     * configuration is written or read.
     * @param key
     *            the configuration key
     * @param configuredValue
     *            the value to sanitize
     * @return a sanitized value: remove whitespace and correct directory
     *         separator for the current operating system.
     */
    private static String sanitizeConfiguredValue(final String key, final String configuredValue) {
        String sanitizedValue = null;
        if (configuredValue != null) {
            // removes trailing and leading whitespace
            // limitation: whitespace can not be configured!
            // depending on the operating system: use the correct path
            // separators!
            sanitizedValue = handleConfiguredDirectory(key, configuredValue.trim());
        }
        return sanitizedValue;
    }

    /**
     * Makes sure the correct path separator is used in configured directory
     * names.
     * @param key
     *            the name of the configuration parameter.
     * @param configuredValue
     *            the configured value.
     * @return a path with correct path separator for the current operating
     *         system.
     */
    private static String handleConfiguredDirectory(final String key, final String configuredValue) {
        final ConfKey configurationKey = ConfKey.valueOf(key);
        String configuredDir = configuredValue;
        if (configurationKey.isRequiredDirectory()) {
            // split on / or on \
            final String[] path = configuredDir.split("(/|\\\\)");
            // combine using the correct path separator
            configuredDir = FileUtils.combine(path);
        }
        return configuredDir;
    }

    /**
     * Creates all the required directories if needed. Iterates over all
     * configuration values and creates the ones marked as required directory.
     */
    public static void createRequiredDirectories() {
        for (final ConfKey confKey : ConfKey.values()) {
            if (confKey.isRequiredDirectory() && FileUtils.mkdirs(get(confKey))) {
                LOG.info("Created directory: " + get(confKey));
            }
        }
    }
}

