package be.hogent.tarsos.cli;

import java.io.File;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.Sample;
import be.hogent.tarsos.util.AudioFile;

/**
 * Detects pitch for an input file using a pitch detector. Outputs two columns,
 * one in ms and the oters in Hz.
 * @author Joren Six
 */
public final class DetectPitch extends AbstractTarsosApp {

    @Override
    public String description() {
        return "Detects pitch for an input file"
        + " using a pitch detector. Outputs two columns, one in ms and the oters in Hz.";
    }

    @Override
    public String name() {
        return "detect_pitch";
    }

    @Override
    public void run(final String... args) {
        final OptionParser parser = new OptionParser();
        final OptionSpec<File> fileSpec = parser.accepts("in", "The file to annotate").withRequiredArg()
        .ofType(File.class).withValuesSeparatedBy(' ').defaultsTo(new File("in.wav"));
        final OptionSpec<PitchDetectionMode> detectionModeSpec = createDetectionModeSpec(parser);

        final OptionSet options = parse(args, parser, this);

        if (isHelpOptionSet(options)) {
            printHelp(parser);
        } else {
            final File inputFile = options.valueOf(fileSpec);
            final PitchDetectionMode detectionMode = options.valueOf(detectionModeSpec);
            final AudioFile audioFile = new AudioFile(inputFile.getAbsolutePath());
            final PitchDetector detector = detectionMode.getPitchDetector(audioFile);
            detector.executePitchDetection();
            for (final Sample sample : detector.getSamples()) {
                Tarsos.println(sample.toString());
            }
        }
    }

}
