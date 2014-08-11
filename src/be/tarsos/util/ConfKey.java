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

/**
 * Defines a configuration key.
 * 
 * @author Joren Six
 */
public enum ConfKey {
	/**
	 * The histogram bin width in cents.
	 */
	histogram_bin_width,
	/**
	 * The (relative) directory to save transcoded files and data files.
	 */
	data_directory(true),

	/**
	 * The reference frequency to base all absolute cent calculations on. <br>
	 * The default value is the frequency of C0 with A4 tuned to 440Hz: <br>
	 * <code>440/32 * Math.pow(2.0,0.25) = 16.35 Hz</code>.
	 */
	absolute_cents_reference_frequency,

	/**
	 * The pitch histogram (range) start value in cents: <br>
	 * The default value is 0 (from 8Hz).
	 */
	pitch_histogram_start,

	/**
	 * The Ambitus (range) stop value in cents: <br>
	 * The default value is <br>
	 * <code>1200 * 8 = 9600 cents = C8 = 4186.01 Hz</code>.
	 */
	pitch_histogram_stop,

	/**
	 * Transcode the audio or just copy it. <br>
	 * The default is <code>true</code>.
	 */
	transcode_audio,

	/**
	 * The format to transcode the audio to. It is one of the enum values
	 * defined in DefaultAttributes.
	 */
	transcoded_audio_to,
	
	
	/**
	 * Checks if the transcoded audio is in the configured format, this can be
	 * skipped for performance reasons.
	 */
	transcode_check_format,
	
	/**
	 * The pitch trackers currently in use.
	 */
	pitch_tracker_list,

	/**
	 * A MIDI device used as INPUT.
	 */
	midi_input_device,

	/**
	 * A MIDI device used as OUTPUT. Should support the MIDI tuning standard.
	 */
	midi_output_device,
	
	/**
	 * A list of MIDI devices used as OUTPUT. Should support the MIDI tuning standard.
	 */
	midi_output_devices,
	
	/**
	 * An index for a General MIDI instrument
	 */
	midi_instrument_index,
	
	/**
	 * Defines the number of threads used to annotate files. Ideally this is the
	 * same as the number of cores on your CPU. Use one thread less if you want
	 * to keep your system responsive. My system has 4 so the default is 3.
	 */
	annotation_threads,

	/**
	 * If a filename matches this regular expression pattern it is an audio
	 * file. <br>
	 * The default is <code>.*\.(mp3|...|mpc|MPC)</code>.
	 */
	audio_file_name_pattern,

	/**
	 * When using the IPEM polyphonic pitch tracker a threshold can be used to
	 * accept only some pitches. Default value is 0.05.
	 */
	ipem_pitch_threshold,



	/**
	 * A threshold used in the silence detector.
	 */
	silence_threshold,

	/**
	 * The marks used in plots the value should be one of the following: none,
	 * points, dots, various, bigdots or pixels. The default value is points.
	 */
	pitch_contour_marks,

	/**
	 * Defines the unit used in the pitch contour diagram. One of the PitchUnit
	 * enumeration names
	 * (HERTZ|RELATIVE_CENTS|ABSOLUTE_CENTS|MIDI_KEY|MIDI_CENT).
	 */
	pitch_contour_unit,
	
	/**
	 * The location to import a file with a file chooser dialog.
	 */
	file_import_dir(true),
	
	/**
	 * The location to export a file with a file chooser dialog.
	 */
	file_export_dir(true),
	
	/**
	 * Files recently analysed.
	 */
	file_recent,
	
	/**
	 * 	A scala file with pitch intervals used to compare detected intervals.
	 */
	interval_scala_file,	
	/**
	 * Microphone mixer to use with Tarsos Live(tm).
	 */
	mixer_input_device,
	/**
	 * Sound card device index.
	 */
	mixer_output_device, 
	/**
	 * Reset the histograms when a new file is imported. Or not. 
	 */
	reset_on_import;

	/**
	 * True if the configured key is a required directory. False otherwise.
	 */
	private final boolean isRequiredDir;

	/**
	 * Create a configuration key.
	 * 
	 * @param isReqDir
	 *            True if the configured key is a required directory. False
	 *            otherwise.
	 */
	ConfKey(final boolean isReqDir) {
		this.isRequiredDir = isReqDir;
	}

	/**
	 * By default configured values are not required directories.
	 */
	ConfKey() {
		this(false);
	}

	/**
	 * Checks if this key is a required directory.
	 * 
	 * @return True if the configured value is a required directory, false
	 *         otherwise.
	 */
	public boolean isRequiredDirectory() {
		return isRequiredDir;
	}
}
