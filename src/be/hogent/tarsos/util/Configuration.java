package be.hogent.tarsos.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * @author Joren Six
 * Utility class to access (read and write) configuration settings.
 *
 */
public class Configuration {
	private static final Logger log = Logger.getLogger(Configuration.class.getName());

	private static Properties defaultConfigurationProperties;
	private static Preferences userPreferences = null;

	/**
	 * Defines a configuration key.
	 * @author Joren Six
	 *
	 */
	public enum Config{

		/**
		 * The shell executable used to execute
		 * external commands on UNIX;
		 * The default is <code>/bin/bash</code>
		 */
		unix_shell_executable,
		/**
		 * The shell executable option used to execute
		 * external commands on UNIX;
		 * The default is <code>-c</code>
		 */
		unix_shell_executable_option,
		/**
		 * The exit code of a "command not found"
		 * operation on UNIX
		 * The default is <code>9009</code>
		 */
		unix_shell_executable_not_found_exit_code,
		/**
		 * The shell executable used to execute
		 * external commands on windows;
		 * The default is <code>cmd.exe</code>
		 */
		win_shell_executable,
		/**
		 * The shell executable option used to execute
		 * external commands on windows;
		 * The default is <code>\c</code>
		 */
		win_shell_executable_option,
		/**
		 * The exit code of a "command not found"
		 * operation on windows
		 * The default is <code>9009</code>
		 */
		win_shell_executable_not_found_exit_code,
		/**
		 * The histogram bin width in cents
		 */
		histogram_bin_width,
		/**
		 * The (relative) directory with audio files
		 */
		audio_directory(true),
		/**
		 * The (relative) directory to save (text) data files
		 */
		data_directory(true),

		/**
		 * The (relative) directory to save generated audio
		 */
		generated_audio_directory(true),

		/**
		 * The (relative) directory to save
		 * IPEM annotations
		 */
		raw_ipem_annotations_directory(true),

		/**
		 * The (relative) directory to save
		 * AUBIO annotations
		 */
		raw_aubio_annotations_directory(true),

		/**
		 * The reference frequency to base all absolute cent calculations on.
		 * <br>
		 * The default value is the frequency of C0 with A4 tuned to 440Hz:
		 * <br>
		 * <code>440/32 * Math.pow(2.0,0.25) = 16.35 Hz</code>
		 */
		absolute_cents_reference_frequency,

		/**
		 * The Ambitus (range) start value in cents:
		 * <br>
		 * The default value is 0 (from 16Hz)
		 */
		ambitus_start,

		/**
		 The Ambitus (range) stop value in cents:
		 * <br>
		 * The default value is <br>
		 * <code>1200 * 8 = 9600 cents = C8 = 4186.01 Hz<code>
		 */
		ambitus_stop,

		/**
		 * Transcode the audio or just copy it.
		 * <br>
		 * The default is <code>true</code>.
		 */
		transcode_audio,

		/**
		 * If the transcoded audio format check is skipped
		 * the program only checks if the file exists, not its format.
		 * Checking each file format takes a while on large data sets and is only
		 * needed if the transcoded audio file format is changed.
		 * <br>
		 * The default is <code>true</code>.
		 */
		skip_transcoded_audio_format_check,

		/**
		 * The directory where the transcoded audio is saved.
		 * <br>
		 * The default is <code>data/transcoded_audio</code>.
		 */
		transcoded_audio_directory(true),

		/**
		 * The sampling rate for the transcoded audio.
		 * <br>
		 * The default rate is 44.1 kHz.
		 */
		transcoded_audio_sampling_rate,

		/**
		 * The audio codec used for the transcoded files.
		 * <br>
		 * The default is <code>pcm_s16le</code>: signed 16 bit little endian PCM.
		 * <br>
		 * See <a href="http://www.sauronsoftware.it/projects/jave/manual.php#8">
		 * The JAVE (Java Audio Video Encoder) library web site.
		 * </a>
		 */
		transcoded_audio_codec,

		/**
		 * The audio format (container) used for the transcoded files.
		 * <br>
		 * The default is <code>wav</code>.
		 * <br>
		 * See <a href="http://www.sauronsoftware.it/projects/jave/manual.php#8">
		 * The JAVE (Java Audio Video Encoder) library web site.
		 * </a>
		 */
		transcoded_audio_format,

		/**
		 * The number of channels in the transcoded audio files.
		 * If the source is mono and the number of channels is 2 a file with
		 * two identical channels is created. If the source is stereo and the
		 * number of channels is 1 the two channels are down mixed.
		 * <br>
		 * The default is mono so 1 channel.
		 */
		transcoded_audio_number_of_channels,

		/**
		 * If a filename matches this regular
		 * expression pattern it is an audio
		 * file.
		 * <br>
		 * The default is <code>.*\.(mp3|...|mpc|MPC)</code>.
		 */
		audio_file_name_pattern;

		boolean isRequiredDirectory;
		Config(boolean isRequiredDirectory){
			this.isRequiredDirectory = isRequiredDirectory;
		}

		Config(){
			this(false);
		}
	}

	/**
	 * Creates all the required directories if needed. Iterates over all
	 * configuration values and creates the ones marked as required directory.
	 */
	public static void createRequiredDirectories(){
		for(Config config : Config.values())
			if(config.isRequiredDirectory && FileUtils.mkdirs(get(config)))
				log.info("Created directory: " + get(config));
	}


	/**
	 * Read the configuration for a certain key.
	 * If the key can not be found in the UserPreferences the default is loaded from
	 * a properties file, written in the UserPreferences, and returned. Otherwise the
	 * configured value is returned.
	 * @param key the configuration key
	 * @return if the key is configured, the configured value. Otherwise the default value is returned.
	 */
	public static String get(Config key){
		return get(key.name());
	}


	/**
	 * Read the configuration for a certain key.
	 * If the key can not be found in the UserPreferences the default is loaded from
	 * a properties file, written in the UserPreferences, and returned. Otherwise the
	 * configured value is returned.
	 * @param key the configuration key
	 * @return if the key is configured, the configured value. Otherwise the default value is returned.
	 * @exception if the configured value can not be parsed to a double.
	 */
	public static int getInt(Config key) throws NumberFormatException{
		return Integer.parseInt(get(key.name()));
	}

	/**
	 * Read the configuration for a certain key.
	 * If the key can not be found in the UserPreferences the default is loaded from
	 * a properties file, written in the UserPreferences, and returned. Otherwise the
	 * configured value is returned.
	 * @param key the configuration key
	 * @return if the key is configured, the configured value. Otherwise the default value is returned.
	 * @exception if the configured value can not be parsed to a double.
	 */
	public static double getDouble(Config key) throws NumberFormatException{
		return Double.parseDouble(get(key.name()));
	}

	/**
	 * Parses a configured string as a boolean.
	 * The boolean returned represents the value true if the configured value is not null and is
	 * equal, ignoring case, to the string "true".
	 * <p>
	 * Example: <code>Boolean.parseBoolean("True")</code> returns <code>true</code>.<br>
	 * Example: <code>Boolean.parseBoolean("yes")</code> returns <code>false</code>.
	 * </p>
	 * @param key the name of the configuration parameter
	 * @return <code>true</code> if the configured value is not null and is
	 * equal, ignoring case, to the string "true", <code>false</code> otherwise.
	 */
	public static boolean getBoolean(Config key) {
		return Boolean.parseBoolean(get(key));
	}


	/**
	 * Set a configuration parameter
	 * @param key the key
	 * @param value the value
	 */
	public static void set(Config key,String value){
		try {
			userPreferences.put(key.name(), value);
			userPreferences.flush();
			log.info(key.name() + " = " + value );
		} catch (BackingStoreException e) {
			log.severe("Could not save preference for " + key.name());
			e.printStackTrace();
		}
	}

	/**
	 * Fetches configured values.
	 * If no values are configured a default configuration is written (based on configuration.properties).
	 * @param key the name of the configuration parameter
	 * @return a configured or default value
	 */
	private static String get(String key) {
		if(defaultConfigurationProperties==null){
			defaultConfigurationProperties = new Properties();
			InputStream propertiesStream = Configuration.class.getResourceAsStream("configuration.properties");
			try {
				defaultConfigurationProperties.load(propertiesStream);
			} catch (IOException e) {
				log.severe("Could not find default database configuration properties file");
				throw new Error(e);
			}
		}
		String defaultValue=defaultConfigurationProperties.getProperty(key);

		if(userPreferences==null){
			userPreferences=Preferences.userRoot();
			for(Config configKey : Config.values()){
				String defaultConfigValue=defaultConfigurationProperties.getProperty(configKey.name());
				//If there is no configuration for this user, write the default configuration
				if(userPreferences.get(configKey.name(), defaultConfigValue).equals(defaultConfigValue)){
					userPreferences.put(configKey.name(),defaultConfigValue);
				}
				try{
					userPreferences.flush();
				} catch (BackingStoreException e) {
					log.severe("Could not write config values: " + e.getMessage()) ;
					e.printStackTrace();
				}
			}
		}
		String configuredValue = userPreferences.get(key, defaultValue).trim();
		configuredValue = handleConfiguredDirectory(key,configuredValue);
		return configuredValue;
	}

	/**
	 * Makes sure the correct path separator is used in configured directory names.
	 * @param key the name of the configuration parameter.
	 * @param configuredValue the configured value.
	 * @return a path with correct path separator for the current operating system.
	 */
	private static String handleConfiguredDirectory(String key, String configuredValue){
		Config configurationKey = Config.valueOf(key);
		if(configurationKey.isRequiredDirectory){
			//split on / or on \
			String [] path = configuredValue.split("(/|\\\\)");
			//combine using the correct path separator
			configuredValue = FileUtils.combine(path);
		}
		return configuredValue;
	}
}
