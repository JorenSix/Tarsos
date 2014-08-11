/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import be.tarsos.sampled.pitch.PitchDetectionMode;

/**
 * Utility class to access (read and write) configuration settings. There are
 * utility methods for booleans, doubles and integers. It automatically converts
 * directory separators with the correct file separator for the current
 * operating system.
 * 
 * @author Joren Six
 */
public final class Configuration {
	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(Configuration.class.getName());
	/**
	 * The suffix used for the human name of a configuration setting.
	 */
	private static final String HUMAN_NAME_SUFFIX = "_human";
	/**
	 * The suffix used for a discription of a configuration setting.
	 */
	private static final String DESCIPTION_SUFFIX = "_descr";
	
	
	/**
	 * A separator to split string lists.
	 */
	private static final String SEPARATOR = " __,__ ";

	/**
	 * A list of listeners that are notified when a configuration setting
	 * changes.
	 */
	private static final List<ConfigChangeListener> LISTENERS = new ArrayList<ConfigChangeListener>();

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
	 * 
	 * @param key
	 *            the configuration key
	 * @return if the key is configured, the configured value. Otherwise the
	 *         default value is returned.
	 */
	public static String get(final ConfKey key) {
		return get(key.name());
	}

	/**
	 * Read the human name for a certain key.
	 * 
	 * @param key
	 *            the configuration key
	 * @return The human name for the configuration or null.
	 */
	public static String getHumanName(final ConfKey key) {
		return get(key.name() + HUMAN_NAME_SUFFIX);
	}

	/**
	 * Read the description name for a certain key.
	 * 
	 * @param key
	 *            the configuration key
	 * @return The human name for the configuration or null.
	 */
	public static String getDescription(final ConfKey key) {
		return get(key.name() + DESCIPTION_SUFFIX);
	}

	/**
	 * Read the configured Integer for a certain key. If the key can not be
	 * found in the UserPreferences the default is loaded from a properties
	 * file, written in the UserPreferences, and returned. Otherwise the
	 * configured value is returned.
	 * 
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
	 * 
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
	 * 
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
	 * Return a configured PitchDetectionMode
	 * 
	 * @param key
	 *            the key of the configured value
	 * @return a PitchDetectionMode if the configured string represents a
	 *         correct value.
	 */
	public static PitchDetectionMode getPitchDetectionMode(final ConfKey key) {
		return PitchDetectionMode.valueOf(get(key));
	}

	/**
	 * Fetches configured values. If no values are configured a default
	 * configuration is written (based on configuration.properties).
	 * 
	 * @param key
	 *            the name of the configuration parameter
	 * @return a configured or default value
	 */
	private static String get(final String key) {
		checkForConfigurationAndWriteDefaults();
		final String defaultValue = defaultConfProps.getProperty(key);		
		String configuredValue = userPreferences.get(key, defaultValue);
		configuredValue = sanitizeConfiguredValue(key, configuredValue);
		return configuredValue;
	}
	
	/**
	 * Checks if there is a configuration file there and, if not, writes the
	 * default configuration parameters to the configuration file.
	 */
	public static void checkForConfigurationAndWriteDefaults() {
		if( userPreferences == null || defaultConfProps == null){
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
				if (userPreferences == null) {
					userPreferences = Preferences.userNodeForPackage(Configuration.class);
					for (final ConfKey configKey : ConfKey.values()) {
						// If there is no configuration for this user, write the
						// default
						// configuration
						if (userPreferences.get(configKey.name(), null) == null) {
							final String defaultConfigVal = defaultConfProps.getProperty(configKey.name());
							set(configKey, defaultConfigVal);
						}
					}
				}
			}
		}
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
	public static void set(final ConfKey key, final String value) {
		if (value == null) {
			return;
		}
		checkForConfigurationAndWriteDefaults();
		try {
			final String actualValue = sanitizeConfiguredValue(key.name(), value);
			userPreferences.put(key.name(), actualValue);
			userPreferences.flush();
			LOG.finer(key.name() + " = " + actualValue);
		} catch (final BackingStoreException e) {
			LOG.severe("Could not save preference for " + key.name());
		}
		for (ConfigChangeListener listener : LISTENERS) {
			listener.configurationChanged(key);
		}
	}
	
	/**
	 * Store a list of strings.
	 * @param key the configuration key.
	 * @param list The list of strings.
	 */
	private static void setList(final ConfKey key,final List<?> list){
		List<String> strings = new ArrayList<String>();
		for(Object o:list){
			strings.add(o.toString());
		}
		String values = StringUtils.join(strings, SEPARATOR);
		set(key,values);
	}
	
	/**
	 * Return a list of values stored with a key
	 * @param key The key.
	 * @return A list of configured values.
	 */
	public static List<String> getList(final ConfKey key){
		String[] values = get(key).split(SEPARATOR);
		return new ArrayList<String>(Arrays.asList(values));
	}

	/**
	 * An interface used to send notifications of a changed configurations
	 * setting.
	 * 
	 */
	public static interface ConfigChangeListener {
		/**
		 * Fires when a configuration setting changes. The value of the
		 * configuration setting can be found using the Configuration.get
		 * method.
		 * 
		 * @param key
		 *            The changed configuration setting.
		 */
		void configurationChanged(final ConfKey key);
	}

	/**
	 * 
	 * @param listener
	 */
	public static void addListener(final ConfigChangeListener listener) {
		LISTENERS.add(listener);
	}

	/**
	 * Delegates to either the set(String) method or setList method to store
	 * strings or lists respectively.
	 * 
	 * @param key
	 * @param value
	 */
	public static void set(final ConfKey key, final Object value) {
		if(value instanceof List<?>)
			setList(key, (List<?>) value);
		else
			set(key, value.toString());
		
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
	 * 
	 * @param key
	 *            the name of the configuration parameter.
	 * @param configuredValue
	 *            the configured value.
	 * @return a path with correct path separator for the current operating
	 *         system.
	 */
	private static String handleConfiguredDirectory(final String key, final String configuredValue) {
		String configuredDir = configuredValue;
		if (isActualConfiguredValue(key)) {
			final ConfKey configurationKey = ConfKey.valueOf(key);
			if (configurationKey.isRequiredDirectory()) {
				// split on / or on \
				final String[] path = configuredDir.split("(/|\\\\)");
				// combine using the correct path separator
				configuredDir = FileUtils.combine(path);
			}
		}
		return configuredDir;
	}

	private static boolean isActualConfiguredValue(final String key) {
		return !key.endsWith(DESCIPTION_SUFFIX) && !key.endsWith(HUMAN_NAME_SUFFIX);
	}

	public static File getFile(ConfKey key) {
		return new File(get(key));
	}
}
