package be.hogent.tarsos.apps;

import java.io.File;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.SignalPowerExtractor;
import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

/**
 * Annotates an audio file with everything Tarsos is capable off Pitch
 * information, ambitus, tone scale peaks, waveform, scala files...
 * @author Joren Six
 */
public final class Annotate implements TarsosApplication {


    /**
     * Annotates an input file.
     * @param inputFile
     *            The file to annotate.
     * @param detector
     *            The detector to use.
     */
    private void annotateInputFile(final String inputFile, final PitchDetectionMode detectionMode) {

        AudioFile audioFile = new AudioFile(inputFile);

        PitchDetector pitchDetector = detectionMode.getPitchDetector(audioFile);

        pitchDetector.executePitchDetection();
        String baseName = audioFile.basename();
        String directory = FileUtils.combine("annotations", baseName);
        FileUtils.mkdirs(directory);

        String prefix = baseName + "_" + pitchDetector.getName();

        List<Sample> samples = pitchDetector.getSamples();
        AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
        String ambitusTextFileName = FileUtils.combine(directory, prefix + "_ambitus.txt");
        String ambitusPNGFileName = FileUtils.combine(directory, prefix + "_ambitus.png");
        String coloredToneScalePNGFileName = prefix + "_tone_scale_colored.png";
        ambitusHistogram.plotToneScaleHistogram(FileUtils.combine(directory, coloredToneScalePNGFileName),
                true);
        ambitusHistogram.export(ambitusTextFileName);
        ambitusHistogram.plot(ambitusPNGFileName, "Ambitus " + baseName + " " + pitchDetector.getName());
        ToneScaleHistogram toneScaleHistogram = ambitusHistogram.toneScaleHistogram();
        String toneScaleTextFileName = FileUtils.combine(directory, prefix + "_tone_scale.txt");
        String toneScalePNGFileName = FileUtils.combine(directory, prefix + "_tone_scale.png");
        toneScaleHistogram.export(toneScaleTextFileName);
        toneScaleHistogram.plot(toneScalePNGFileName, "Tone scale " + baseName + " "
                + pitchDetector.getName());

        toneScaleHistogram.gaussianSmooth(1.0);
        List<Peak> peaks = PeakDetector.detect(toneScaleHistogram, 15, 0.8);
        Histogram peakHistogram = PeakDetector.newPeakDetection(peaks);
        String peaksTitle = prefix + "_peaks_" + 1.0 + "_" + 15 + "_" + 0.8;
        SimplePlot p = new SimplePlot(peaksTitle);
        p.addData(0, toneScaleHistogram);
        p.addData(1, peakHistogram);
        p.save(FileUtils.combine(directory, peaksTitle + ".png"));
        ToneScaleHistogram.exportPeaksToScalaFileFormat(FileUtils.combine(directory, peaksTitle + ".scl"),
                peaksTitle, peaks);

        SignalPowerExtractor powerExtractor = new SignalPowerExtractor(audioFile);
        powerExtractor.saveTextFile(FileUtils.combine(directory, prefix + "_power.txt"));
        powerExtractor.saveWaveFormPlot(FileUtils.combine(directory, prefix + "_wave.png"));
    }

    @Override
    public void run(final String... args) {

        OptionParser parser = new OptionParser();
        OptionSpec<File> fileSpec = parser.accepts("in", "The file to annotate").withRequiredArg().ofType(
                File.class)
                .withValuesSeparatedBy(' ').defaultsTo(new File("in.wav"));

        OptionSpec<PitchDetectionMode> detectionModeSpec = parser.accepts("detector", "The detector to use")
        .withRequiredArg().ofType(PitchDetectionMode.class)
        .defaultsTo(PitchDetectionMode.TARSOS_YIN);



        OptionSet options = parser.parse(args);
        String inputFile = options.valueOf(fileSpec).getAbsolutePath();
        PitchDetectionMode detectionMode = options.valueOf(detectionModeSpec);

        if (inputFile != null && !FileUtils.exists(inputFile)) {
            // help
        } else if (inputFile == null) {
            String pattern = Configuration.get(ConfKey.audio_file_name_pattern);
            String globDirectory = FileUtils.combine(FileUtils.getRuntimePath(), "audio");
            List<String> inputFiles = FileUtils.glob(globDirectory, pattern);
            inputFiles.addAll(FileUtils.glob(globDirectory, pattern.toLowerCase()));
            for (String file : inputFiles) {
                annotateInputFile(file, detectionMode);
            }
        } else {
            annotateInputFile(inputFile, detectionMode);
        }
    }

    @Override
    public String description() {
        return "Annotate can be used to annotate audio files. It transcodes "
        + "audio to an understandable format, detects pitch and stores information about the files. "
        + "It uses the defined files with the in "
        + "option or all the audiofiles in the audio directory.";
    }

    @Override
    public String name() {
        return "annotate";
    }

}
