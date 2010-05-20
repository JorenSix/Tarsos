package be.hogent.tarsos.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

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
public class AnnotationSynth implements TarsosApplication {


    @Override
    public String description() {
        return "Generates audio from a set of annotations. AnnotationSynth is used to "
        + "sonificate pitch annotation files. For the moment it uderstands the pitch "
        + "files used by BOZKURT, AUBIO and IPEM. It reads the data from a file or from " + "STDIN.";
    }

    @Override
    public String name() {
        return "annotation_synth";
    }

    @Override
    public void run(final String... args) {
        OptionParser parser = new OptionParser();

        OptionSpec<File> inputSpec = parser.accepts("in",
        "Input annotations. If no file is given it reads standard input.").withRequiredArg().ofType(
                File.class);

        OptionSpec<File> outputSpec = parser.accepts("out", "Output WAV-file.").withRequiredArg().ofType(
                File.class).defaultsTo(new File("out.wav"));

        OptionSpec<AnnotationCVSFileHandlers> annotationFormatSpec = parser.accepts("format",
        "Annotation format of the input file: AUBIO|IPEM|BOZKURT").withRequiredArg().ofType(
                AnnotationCVSFileHandlers.class).defaultsTo(AnnotationCVSFileHandlers.AUBIO);

        String listenOption = "listen";
        parser.accepts(listenOption, "Do not write a "
                + "wav file but listen to the generated tones.");


        OptionSpec<Integer> filterSpec = parser
        .accepts(
                "filter",
                "Defines the number of samples are used in a median filter. "
                + "With samples every 10ms and a median filter of 5 there can be a 50/2 ms delay")
                .withRequiredArg().ofType(Integer.class).defaultsTo(0);

        OptionSet options = Tarsos.parse(args, parser, this);

        CSVFileHandler handler = options.valueOf(annotationFormatSpec).getCvsFileHandler();
        File outputFile = options.valueOf(outputSpec);
        File inputFile = options.valueOf(inputSpec);
        int filterSize = options.valueOf(filterSpec);
        boolean listen = options.has(listenOption);


        // System.out.println("Bugs: currently gain information is only
        // used while listening, not while writing a file.");

        ToneSequenceBuilder builder = new ToneSequenceBuilder();
        String separator = handler.getSeparator();
        if (inputFile == null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line;
            try {
                line = in.readLine();
                while (line != null && line.length() != 0) {
                    String[] row = line.split(separator);
                    handler.handleRow(builder, row);
                    line = in.readLine();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            List<String[]> rows = FileUtils.readCSVFile(inputFile.getAbsolutePath(), separator, -1);
            for (String[] row : rows) {
                handler.handleRow(builder, row);
            }
        }

        if (listen) {
            builder.playAnnotations(filterSize);
        } else {
            try {
                builder.writeFile(outputFile.getAbsolutePath(), filterSize);
            } catch (Exception e) {
                System.out.println("Could not write: " + outputFile + "\n");
            }
        }
    }
}
