package be.hogent.tarsos.pitch;

import java.util.ArrayList;
import java.util.List;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.Execute;
import be.hogent.tarsos.util.FileUtils;

/**
 * Calls aubio (http://aubio.org/) to execute pitch detection. Aubio should be
 * installed correctly and available in PATH. This command is called when
 * executing pitch detection: <code>
 * aubiopitch  -u freq --mode yin -s -70 -i in.wav
 * </code> The output uses the frequency (Hz) unit.
 * The algorithm used is yin. The default silence threshold of -70 is used. The
 * file that is annoted is in.wav For more information see the <a
 * href="http//aubio.org">aubio</a> man pages or website. The pitch detection
 * algorithm is defined by {@link PitchDetectionMode}. See
 * http://www.elec.qmul.ac.uk/research/thesis/Brossier07-phdthesis.pdf for more
 * info.
 * 
 * @author Joren Six
 */
public final class AubioPitchDetection implements PitchDetector {

    private final AudioFile file;
    private final PitchDetectionMode pitchDetectionMode;
    private final List<Sample> samples;
    private final String name;

    public AubioPitchDetection(final AudioFile file, final PitchDetectionMode pitchDetectionMode) {
        this.file = file;
        this.pitchDetectionMode = pitchDetectionMode;
        this.samples = new ArrayList<Sample>();
        this.name = "aubio_" + pitchDetectionMode.getParametername();
    }

    @Override
    public void executePitchDetection() {
        final String annotationsDirectory = Configuration.get(ConfKey.raw_aubio_annotations_directory);
        final String csvFileName = FileUtils.combine(annotationsDirectory, this.name + "_" + file.basename()
                + ".txt");

        if (!FileUtils.exists(csvFileName)) {
            final String command = "aubiopitch  -u freq --mode " + this.pitchDetectionMode.getDetectionModeName()
            + "  -s -70  -i " + file.transcodedPath();
            Execute.command(command, csvFileName);
        }

        final List<String[]> csvData = FileUtils.readCSVFile(csvFileName, "\t", 2);
        for (final String[] row : csvData) {
            final long start = (long) (Double.parseDouble(row[0]) * 1000);
            final Double pitch = Double.parseDouble(row[1]);
            final Sample sample = pitch == -1 ? new Sample(start) : new Sample(start, pitch);
            switch (pitchDetectionMode) {
            case AUBIO_YIN:
                sample.source = PitchDetectionMode.AUBIO_YIN;
                break;
            case AUBIO_YINFFT:
                sample.source = PitchDetectionMode.AUBIO_YINFFT;
                break;
            case AUBIO_MCOMB:
                sample.source = PitchDetectionMode.AUBIO_MCOMB;
                break;
            case AUBIO_FCOMB:
                sample.source = PitchDetectionMode.AUBIO_FCOMB;
                break;
            case AUBIO_SCHMITT:
                sample.source = PitchDetectionMode.AUBIO_SCHMITT;
                break;
            default:
                break;
            }
            samples.add(sample);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<Sample> getSamples() {
        return this.samples;
    }

}
