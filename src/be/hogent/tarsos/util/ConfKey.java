package be.hogent.tarsos.util;

/**
 * Defines a configuration key.
 * 
 * @author Joren Six
 * 
 */
public enum ConfKey {

    /**
     * The shell executable used to execute external commands on UNIX; The
     * default is <code>/bin/bash</code>
     */
    unix_shell_executable,
    /**
     * The shell executable option used to execute external commands on UNIX;
     * The default is <code>-c</code>
     */
    unix_shell_executable_option,
    /**
     * The exit code of a "command not found" operation on UNIX The default is
     * <code>9009</code>
     */
    unix_shell_executable_not_found_exit_code,
    /**
     * The shell executable used to execute external commands on windows; The
     * default is <code>cmd.exe</code>
     */
    win_shell_executable,
    /**
     * The shell executable option used to execute external commands on windows;
     * The default is <code>\c</code>
     */
    win_shell_executable_option,
    /**
     * The exit code of a "command not found" operation on windows The default
     * is <code>9009</code>
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
     * The (relative) directory to save IPEM annotations
     */
    raw_ipem_annotations_directory(true),

    /**
     * The (relative) directory to save AUBIO annotations
     */
    raw_aubio_annotations_directory(true),

    /**
     * The reference frequency to base all absolute cent calculations on. <br>
     * The default value is the frequency of C0 with A4 tuned to 440Hz: <br>
     * <code>440/32 * Math.pow(2.0,0.25) = 16.35 Hz</code>
     */
    absolute_cents_reference_frequency,

    /**
     * The Ambitus (range) start value in cents: <br>
     * The default value is 0 (from 16Hz)
     */
    ambitus_start,

    /**
     * The Ambitus (range) stop value in cents: <br>
     * The default value is <br>
     * <code>1200 * 8 = 9600 cents = C8 = 4186.01 Hz<code>
     */
    ambitus_stop,

    /**
     * Transcode the audio or just copy it. <br>
     * The default is <code>true</code>.
     */
    transcode_audio,

    /**
     * If the transcoded audio format check is skipped the program only checks
     * if the file exists, not its format. Checking each file format takes a
     * while on large data sets and is only needed if the transcoded audio file
     * format is changed. <br>
     * The default is <code>true</code>.
     */
    skip_transcoded_audio_format_check,

    /**
     * The directory where the transcoded audio is saved. <br>
     * The default is <code>data/transcoded_audio</code>.
     */
    transcoded_audio_directory(true),

    /**
     * The sampling rate for the transcoded audio. <br>
     * The default rate is 44.1 kHz.
     */
    transcoded_audio_sampling_rate,

    /**
     * The audio codec used for the transcoded files. <br>
     * The default is <code>pcm_s16le</code>: signed 16 bit little endian PCM. <br>
     * See <a href="http://www.sauronsoftware.it/projects/jave/manual.php#8">
     * The JAVE (Java Audio Video Encoder) library web site. </a>
     */
    transcoded_audio_codec,

    /**
     * The audio format (container) used for the transcoded files. <br>
     * The default is <code>wav</code>. <br>
     * See <a href="http://www.sauronsoftware.it/projects/jave/manual.php#8">
     * The JAVE (Java Audio Video Encoder) library web site. </a>
     */
    transcoded_audio_format,

    /**
     * The number of channels in the transcoded audio files. If the source is
     * mono and the number of channels is 2 a file with two identical channels
     * is created. If the source is stereo and the number of channels is 1 the
     * two channels are down mixed. <br>
     * The default is mono so 1 channel.
     */
    transcoded_audio_number_of_channels,

    /**
     * If a filename matches this regular expression pattern it is an audio
     * file. <br>
     * The default is <code>.*\.(mp3|...|mpc|MPC)</code>.
     */
    audio_file_name_pattern;

    boolean isRequiredDirectory;

    ConfKey(boolean isRequiredDirectory) {
        this.isRequiredDirectory = isRequiredDirectory;
    }

    ConfKey() {
        this(false);
    }

    boolean isRequiredDirectory() {
        return isRequiredDirectory;
    }
}