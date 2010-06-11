package be.hogent.tarsos.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.midi.ToneSequenceBuilder;
import be.hogent.tarsos.midi.ToneSequenceBuilder.AnnotationCVSFileHandlers;
import be.hogent.tarsos.midi.ToneSequenceBuilder.CSVFileHandler;
import be.hogent.tarsos.util.FileUtils;

/**
 * Generates audio from a set of annotations. AnnotationSynth is used to
 * sonificate pitch annotation files. For the moment it uderstands the pitch
 * files used by BOZKURT, AUBIO and IPEM. It reads the data from a file or from
 * STDIN.
 * @author Joren Six
 */
public final class AnnotationSynth extends AbstractTarsosApp {
    /**
     * Log messages.
     */
    private static final Logger LOG = Logger.getLogger(AnnotationSynth.class.getName());

    @Override
    public String description() {
        return "Generates audio from a set of annotations. AnnotationSynth is used to "
        + "sonificate pitch annotation files. For the moment it uderstands the pitch "
        + "files used by BOZKURT, AUBIO and IPEM. It reads the data from a file or from STDIN.";
    }

    @Override
    public String name() {
        return "annotation_synth";
    }

    @Override
    public void run(final String... args) {
        final OptionParser parser = new OptionParser();


        final OptionSpec<File> inputSpec = parser.accepts("in",
        "Input annotations. If no file is given it reads standard input.").withRequiredArg().ofType(
                File.class);

        final OptionSpec<File> outputSpec = parser.accepts("out", "Output WAV-file.").withRequiredArg()
        .ofType(
                File.class).defaultsTo(new File("out.wav"));

        final OptionSpec<AnnotationCVSFileHandlers> annoFormatSpec = parser.accepts("format",
        "Annotation format of the input file: AUBIO|IPEM|BOZKURT").withRequiredArg().ofType(
                AnnotationCVSFileHandlers.class).defaultsTo(AnnotationCVSFileHandlers.AUBIO);

        parser.accepts("listen", "Do not write a "
                + "wav file but listen to the generated tones.");


        final OptionSpec<Integer> filterSpec = parser
        .accepts(
                "filter",
                "Defines the number of samples are used in a median filter. "
                + "With samples every 10ms and a median filter of 5 there can be a 50/2 ms delay")
                .withRequiredArg().ofType(Integer.class).defaultsTo(0);

        final OptionSet options = parse(args, parser, this);

        if (isHelpOptionSet(options)) {
            printHelp(parser);
        } else {
            final CSVFileHandler handler = options.valueOf(annoFormatSpec).getCvsFileHandler();
            final File outputFile = options.valueOf(outputSpec);
            final File inputFile = options.valueOf(inputSpec);
            final int filterSize = options.valueOf(filterSpec);
            final boolean listen = options.has("listen");

            final ToneSequenceBuilder builder = new ToneSequenceBuilder();
            final String separator = handler.getSeparator();
            if (inputFile == null) {
                final BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
                String line;
                try {
                    line = stdIn.readLine();
                    while (line != null && line.length() != 0) {
                        final String[] row = line.split(separator);
                        handler.handleRow(builder, row);
                        line = stdIn.readLine();
                    }
                } catch (final IOException e1) {
                    LOG.log(Level.SEVERE, "Could not read from standard input.", e1);
                }
            } else {
                final List<String[]> rows = FileUtils.readCSVFile(inputFile.getAbsolutePath(), separator, -1);
                for (final String[] row : rows) {
                    handler.handleRow(builder, row);
                }
            }

            if (listen) {
                builder.playAnnotations(filterSize);
            } else {
                try {
                    builder.writeFile(outputFile.getAbsolutePath(), filterSize);
                } catch (final Exception e) {
                    LOG.log(Level.SEVERE, "Could not write: " + outputFile + "\n", e);
                }
            }
        }
    }
}
