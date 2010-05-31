package be.hogent.tarsos.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an audio file. Facilitates transcoding, handling of path names and
 * data sets.
 * 
 * @author Joren Six
 */
public final class AudioFile {

    /**
     * Where to save the transcoded files.
     */
    public static final String TRANSCODED_AUDIO_DIR = Configuration
    .get(ConfKey.transcoded_audio_directory);
    private static final String ORIGINAL_AUDIO_DIR = Configuration.get(ConfKey.audio_directory);

    private final String path;

    /**
     * Create and transcode an audio file.
     * 
     * @param path
     *            the path for the audio file
     */
    public AudioFile(final String path) {
        this.path = path;
        if (AudioTranscoder.transcodingRequired(transcodedPath())) {
            AudioTranscoder.transcode(path, transcodedPath());
        }
    }

    /**
     * @return the path of the transcoded audio file.
     */
    public String transcodedPath() {
        final String baseName = FileUtils.basename(FileUtils.sanitizedFileName(path));
        final String fileName = baseName + "." + Configuration.get(ConfKey.transcoded_audio_format);
        return FileUtils.combine(TRANSCODED_AUDIO_DIR, fileName);
    }

    /**
     * @return the path of the original file
     */
    public String path() {
        return this.path;
    }

    @Override
    public String toString() {
        return FileUtils.basename(path);
    }

    /**
     * @return the name of the file (without extension)
     */
    public String basename() {
        return this.toString();
    }

    /**
     * Returns a list of AudioFiles included in one or more datasets. Datasets
     * are sub folders of the audio directory.
     * 
     * @param datasets
     *            the datasets to find AudioFiles for
     * @return a list of AudioFiles
     */
    public static List<AudioFile> audioFiles(final String... datasets) {
        final List<AudioFile> files = new ArrayList<AudioFile>();
        for (final String dataset : datasets) {
            for (final String originalFile : FileUtils.glob(FileUtils.combine(ORIGINAL_AUDIO_DIR, dataset),
            ".*\\..*")) {
                files.add(new AudioFile(originalFile));
            }
        }
        return files;
    }
}
