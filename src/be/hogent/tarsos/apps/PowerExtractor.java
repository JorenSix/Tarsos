package be.hogent.tarsos.apps;

import java.util.List;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.SignalPowerExtractor;

/**
 * Extracts power from a file.
 * @author Joren Six
 */
public final class PowerExtractor {

    /**
     *
     */
    private PowerExtractor() {
    }

    /**
     * Extracts power features from some files.
     * @param args
     *            Nothing.
     */
    public static void main(final String... args) {
        String[] globDirectories = { "makam", "maghreb" };
        List<AudioFile> files = AudioFile.audioFiles(globDirectories);
        for (AudioFile file : files) {
            final SignalPowerExtractor spex = new SignalPowerExtractor(file);
            spex.savePowerPlot("data/tests/power_" + file.basename() + ".png", -40);
            spex.saveTextFile("data/tests/power_" + file.basename() + ".txt");
            spex.saveWaveFormPlot("data/tests/waveform_" + file.basename() + ".png");
        }
    }
}
