package be.hogent.tarsos.midi;

import jass.generators.LoopBuffer;
import jass.generators.Mixer;
import jass.render.SourcePlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import be.hogent.tarsos.pitch.PitchFunctions;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.SignalPowerExtractor;

/**
 * Create a sequence of tones. Tones are in this case a sine wave of a certain
 * frequency (in Hertz) starting at a certain time (in seconds) the current tone
 * stops when another tone starts: this class generates only one tone at the
 * time (monophonic).
 * 
 * @author Joren Six
 */
public class ToneSequenceBuilder {

    /**
     * A list of frequencies.
     */
    private List<Double> frequencies;
    /**
     * A list of starting times, frequencies.size() == realTimes.size().
     */
    private final List<Double> realTimes;

    /**
     * Values between 0 and 1 that signify the strength of the signal
     * <code>frequencies.size() == realTimes.size() == powers.size();</code>.
     */
    private List<Double> powers;

    /**
     * Initializes the lists of frequencies and times.
     */
    public ToneSequenceBuilder() {
        frequencies = new ArrayList<Double>();
        realTimes = new ArrayList<Double>();
        powers = new ArrayList<Double>();
    }

    /**
     * Add a tone with a certain frequency (in Hertz) starting at a certain time
     * (seconds).
     * <p>
     * The tone stops when the next tone starts. The last tone has a duration of
     * 0 seconds. The entries should be added <b>chronologically</b>! Strange
     * things will happen if you ignore this rule.
     * </p>
     * 
     * @param frequency
     *            the frequency in Hertz of the tone to add
     * @param realTime
     *            the starttime in seconds of the tone. The tone stops when the
     *            next one starts. The last tone is never played.
     */
    public void addTone(double frequency, double realTime) {
        frequencies.add(frequency);
        realTimes.add(realTime);
        powers.add(0.75);
    }

    public void addTone(double frequency, double realTime, double power) {
        frequencies.add(frequency);
        realTimes.add(realTime);
        realTimes.add(realTime);
        powers.add(power);
    }

    /**
     * Clears the frequencies and times. When this method finishes the object is
     * in the same state as a new instance of {@link ToneSequenceBuilder}.
     */
    public void clear() {
        frequencies.clear();
        realTimes.clear();
    }

    /**
     * Returns a URL to a sine wave WAV file. If the file is not found it
     * unpacks a sine wave WAV file from the jar file to a temporary directory
     * (java.io.tmpdir).
     * 
     * @return the URL to the audio file.
     * @throws IOException
     *             when the file is not accessible or when the user has no
     *             rights to write in the temporary directory.
     */
    private URL sineWaveURL() throws IOException {
        File sineWaveFile = new File(FileUtils.combine(FileUtils.temporaryDirectory(), "sin20ms.wav"));
        if (!sineWaveFile.exists()) {
            FileUtils.copyFileFromJar("be.hogent.tarsos.midi.sin20ms.wav", sineWaveFile.getAbsolutePath());
        }
        return sineWaveFile.toURI().toURL();
    }

    /**
     * Write a WAV-file (sample rate 44.1 kHz) containing the tones and their
     * respective durations (start times). If the fileName the file played.
     * 
     * @param fileName
     *            the name of the file to render. e.g. "out.wav". Also temporary
     *            .raw file is generated: e.g. out.wav.raw. It can not be
     *            deleted using java because the library jass keeps the file
     *            open until garbage collection. A delete on exit is requested
     *            but can fail. Manually deleting the raw files is advised. So
     *            beware when generating a lot of files: temporarily you need
     *            twice the amount of hard disk space.
     * @param smootFilterWindowSize
     *            to prevent (very) sudden changes in the frequency of tones a
     *            smoothing function can be applied. The window size of the
     *            smoothing function defines what an unexpected value is and if
     *            it is smoothed. When no smoothing is required <strong>set it
     *            to zero</strong>. Otherwise a value between 5 and 50 is
     *            normal. ( 50 x 10 ms = 500 ms = 0.5 seconds). A
     *            <em>median filter</em> is used.
     * @throws Exception
     *             when something goes awry.
     */
    public void writeFile(String fileName, int smootFilterWindowSize) throws Exception {
        // invariant: at any time the lists are equal in length
        assert frequencies.size() == realTimes.size();

        if (smootFilterWindowSize > 0) {
            frequencies = PitchFunctions.medianFilter(frequencies, smootFilterWindowSize);
            powers = PitchFunctions.medianFilter(powers, smootFilterWindowSize);
        }

        URL sine50Hz44100 = sineWaveURL();

        float baseFreqWavFile = 50;
        float srate = 44100.f;
        int bufferSize = 1024;
        LoopBuffer tone = new LoopBuffer(srate, bufferSize, sine50Hz44100);
        SourcePlayer player;
        if (fileName != null) {
            String rawFileName = fileName + ".raw";
            player = new SourcePlayer(bufferSize, srate, rawFileName);
        } else {
            player = new SourcePlayer(bufferSize, srate);
        }

        Mixer mixer = new Mixer(bufferSize, 1);
        player.addSource(mixer);
        mixer.addSource(tone);
        tone.setSpeed(0f / baseFreqWavFile);
        if (fileName == null) {
            player.start();
        }

        for (int i = 0; i < frequencies.size(); i++) {
            double freq = frequencies.get(i) == -1.0 ? 0.0 : frequencies.get(i);
            tone.setSpeed((float) freq / baseFreqWavFile);
            mixer.setGain(0, (float) (1.41421 * Math.log(powers.get(i).floatValue()) / Math.log(1.6)));
            if (fileName == null) {
                if (i > 0) {
                    Thread.sleep(Math.round((realTimes.get(i) - realTimes.get(i - 1)) * 1000));
                }
            } else {
                player.advanceTime(realTimes.get(i));
            }
        }

        if (fileName != null) {
            String rawFileName = fileName + ".raw";
            convertRawToWav(srate, rawFileName, fileName);
            new File(rawFileName).deleteOnExit();
        }
    }

    /**
     * Adds a correct header to a raw file.
     */
    private void convertRawToWav(double srate, String rawFileName, String wavFileName) throws Exception {
        FileInputStream inStream = new FileInputStream(new File(rawFileName));
        File out = new File(wavFileName);
        int bytesAvailable = inStream.available();
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = false;
        boolean bigEndian = false;
        AudioFormat audioFormat = new AudioFormat((float) srate, sampleSizeInBits, channels, signed,
                bigEndian);
        AudioInputStream audioInputStream = new AudioInputStream(inStream, audioFormat, bytesAvailable / 2);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
        audioInputStream.close();
        inStream.close();
    }

    /**
     * Read data from a CSV-File, handle it with the handler, smooth it and save
     * it to the generated audio folder.
     * 
     * @param csvFileName
     *            the CSV-file to process
     * @param handler
     *            the handler
     * @param smootFilterWindowSize
     *            the window size for the smoothing function (Median filter).
     */
    public static void saveAsWav(String csvFileName, CSVFileHandler handler, int smootFilterWindowSize) {
        csvFileName = new File(csvFileName).getAbsolutePath();
        try {
            ToneSequenceBuilder builder = new ToneSequenceBuilder();
            List<String[]> rows = FileUtils.readCSVFile(csvFileName, handler.getSeparator(), handler
                    .getNumberOfExpectedColumn());
            for (String[] row : rows) {
                handler.handleRow(builder, row);
            }
            builder.writeFile("data/generated_audio/" + FileUtils.basename(csvFileName) + ".wav",
                    smootFilterWindowSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     *
     */
    public interface CSVFileHandler {
        void handleRow(ToneSequenceBuilder builder, String[] row);

        String getSeparator();

        int getNumberOfExpectedColumn();
    }

    public static CSVFileHandler AUBIO_CSVFILEHANDLER = new AubioCSVHandler();

    private static class AubioCSVHandler implements CSVFileHandler {
        private final SignalPowerExtractor extractor;

        public AubioCSVHandler() {
            extractor = null;
        }

        @Override
        public void handleRow(ToneSequenceBuilder builder, String[] row) {
            double realTime = Double.parseDouble(row[0]);
            double frequency = Double.parseDouble(row[1]);
            if (extractor == null) {
                builder.addTone(frequency, realTime);
            } else {
                builder.addTone(frequency, realTime, extractor.powerAt(realTime, true));
            }
        }

        @Override
        public int getNumberOfExpectedColumn() {
            return 2;
        }

        @Override
        public String getSeparator() {
            return "\t";
        }
    };

    public static CSVFileHandler BOZKURT_CSVFILEHANDLER = new BozkurtCSVFileHandler();

    public static CSVFileHandler IPEM_CSVFILEHANDLER = new IpemCSVFileHandler();

    private static class BozkurtCSVFileHandler implements CSVFileHandler {
        private static final double referenceFrequency = 8.17579891564371; // Hz

        @Override
        public void handleRow(ToneSequenceBuilder builder, String[] row) {
            double realTime = Double.parseDouble(row[0]) / 100; // 100 Hz sample
            // frequency
            // (every 10 ms)
            double frequency = referenceFrequency * Math.pow(2.0, Double.parseDouble(row[1]) / 1200);
            builder.addTone(frequency, realTime);
        }

        @Override
        public int getNumberOfExpectedColumn() {
            return 2;
        }

        @Override
        public String getSeparator() {
            return "[\\s]+";
        }
    }

    private static class IpemCSVFileHandler implements CSVFileHandler {
        private int sampleNumber = 0;

        @Override
        public void handleRow(ToneSequenceBuilder builder, String[] row) {
            sampleNumber++;
            double realTime = sampleNumber / 100.0; // 100 Hz sample frequency
            // (every 10 ms)
            double frequency = 0.0;
            try {
                frequency = Double.parseDouble(row[0]);
            } catch (NumberFormatException e) {
                // ignore

            }
            builder.addTone(frequency, realTime);
        }

        @Override
        public int getNumberOfExpectedColumn() {
            return 0;
        }

        @Override
        public String getSeparator() {
            return " ";
        }
    }

}
