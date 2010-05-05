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
 * 
 * @author Joren Six
 */
public class Configuration {
	private static final Logger log = Logger.getLogger(Configuration.class.getName());

	private static Properties defaultConfigurationProperties;
	private static Preferences userPreferences = null;

	// hides default constuctor
	private Configuration() {
	}

	/**
	 * Creates all the required directories if needed. Iterates over all
	 * configuration values and creates the ones marked as required directory.
	 */
	public static void createRequiredDirectories() {
		for (ConfKey confKey : ConfKey.values())
			if (confKey.isRequiredDirectory && FileUtils.mkdirs(get(confKey)))
				log.info("Created directory: " + get(confKey));
	}

	/**
	 * Read the configuration for a certain key. If the key can not be found in
	 * the UserPreferences the default is loaded from a properties file, written
	 * in the UserPreferences, and returned. Otherwise the configured value is
	 * returned.
	 * 
	 * @param key
	 *            the configuration key
	 * @return if the key is configured, the configured value. Otherwise the
	 *         default value is returned.
	 */
	public static String get(ConfKey key) {
		return get(key.name());
	}

	/**
	 * Read the configuration for a certain key. If the key can not be found in
	 * the UserPreferences the default is loaded from a properties file, written
	 * in the UserPreferences, and returned. Otherwise the configured value is
	 * returned.
	 * 
	 * @param key
	 *            the configuration key
	 * @return if the key is configured, the configured value. Otherwise the
	 *         default value is returned.
	 * @exception if the configured value can not be parsed to an integer a
	 *            NumberFormatException is thrown.
	 */
	public static int getInt(ConfKey key) throws NumberFormatException {
		return Integer.parseInt(get(key.name()));
	}

	/**
	 * Read the configuration for a certain key. If the key can not be found in
	 * the UserPreferences the default is loaded from a properties file, written
	 * in the UserPreferences, and returned. Otherwise the configured value is
	 * returned.
	 * 
	 * @param key
	 *            the configuration key
	 * @return if the key is configured, the configured value. Otherwise the
	 *         default value is returned.
	 * @exception if the configured value can not be parsed to a double a
	 *            NumberFormatException is thrown..
	 */
	public static double getDouble(ConfKey key) throws NumberFormatException {
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
	 * 
	 * @param key
	 *            the name of the configuration parameter
	 * @return <code>true</code> if the configured value is not null and is
	 *         equal, ignoring case, to the string "true", <code>false</code>
	 *         otherwise.
	 */
	public static boolean getBoolean(ConfKey key) {
		return Boolean.parseBoolean(get(key));
	}

	/**
	 * Set a configuration parameter. Sanitizes the value automatically: removes
	 * whitespace and correct file separators if the configured value is a
	 * directory.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 */
	public static void set(ConfKey key, String value) {
		if (value == null)
			return;
		try {
			value = sanitizeConfiguredValue(key.name(), value);
			userPreferences.put(key.name(), value);
			userPreferences.flush();
			log.info(key.name() + " = " + value);
		} catch (BackingStoreException e) {
			log.severe("Could not save preference for " + key.name());
			e.printStackTrace();
		}
	}

	/**
	 * Fetches configured values. If no values are configured a default
	 * configuration is written (based on configuration.properties).
	 * 
	 * @param key
	 *            the name of the configuration parameter
	 * @return a configured or default value
	 */
	private static String get(String key) {
		if (defaultConfigurationProperties == null) {
			defaultConfigurationProperties = new Properties();
			InputStream propertiesStream = Configuration.class.getResourceAsStream("configuration.properties");
			try {
				defaultConfigurationProperties.load(propertiesStream);
			} catch (IOException e) {
				log.severe("Could not find default configurations");
				throw new Error(e);
			}
		}
		String defaultValue = defaultConfigurationProperties.getProperty(key);

		if (userPreferences == null) {
			userPreferences = Preferences.userNodeForPackage(Configuration.class);
			for (ConfKey configKey : ConfKey.values()) {
				// If there is no configuration for this user, write the default
				// configuration
				if (userPreferences.get(configKey.name(), null) == null) {
					String defaultConfigValue = defaultConfigurationProperties.getProperty(configKey.name());
					set(configKey, defaultConfigValue);
				}
			}
		}
		String configuredValue = userPreferences.get(key, defaultValue);
		configuredValue = sanitizeConfiguredValue(key, configuredValue);
		return configuredValue;
	}

	/**
	 * Sanitizes each configured value. Is called every time when a
	 * configuration is written or read.
	 * 
	 * @param key
	 *            the configuration key
	 * @param configuredValue
	 *            the value to sanitize
	 * @return a sanitized value: remove whitespace and correct directory
	 *         separator for the current operating system.
	 */
	private static String sanitizeConfiguredValue(String key, String configuredValue) {
		if (configuredValue != null) {
			// removes trailing and leading whitespace
			// limitation: whitespace can not be configured!
			configuredValue = configuredValue.trim();
			// depending on the operating system: use the correct path
			// separators!
			configuredValue = handleConfiguredDirectory(key, configuredValue);
		}
		return configuredValue;
	}

	/**
	 * Makes sure the correct path separator is used in configured directory
	 * names.
	 * 
	 * @param key
	 *            the name of the configuration parameter.
	 * @param configuredValue
	 *            the configured value.
	 * @return a path with correct path separator for the current operating
	 *         system.
	 */
	private static String handleConfiguredDirectory(String key, String configuredValue) {
		ConfKey configurationKey = ConfKey.valueOf(key);
		if (configurationKey.isRequiredDirectory) {
			// split on / or on \
			String[] path = configuredValue.split("(/|\\\\)");
			// combine using the correct path separator
			configuredValue = FileUtils.combine(path);
		}
		return configuredValue;
	}
}
