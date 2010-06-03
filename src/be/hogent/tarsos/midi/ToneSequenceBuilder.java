package be.hogent.tarsos.midi;

import jass.engine.BufferNotAvailableException;
import jass.engine.SinkIsFullException;
import jass.generators.LoopBuffer;
import jass.generators.Mixer;
import jass.render.SourcePlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
 * @author Joren Six
 */
public final class ToneSequenceBuilder {

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
    public void addTone(final double frequency, final double realTime) {
        frequencies.add(frequency);
        realTimes.add(realTime);
        powers.add(0.75);
    }

    public void addTone(final double frequency, final double realTime, final double power) {
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
        final File sineWaveFile = new File(FileUtils.combine(FileUtils.temporaryDirectory(), "sin20ms.wav"));
        if (!sineWaveFile.exists()) {
            FileUtils.copyFileFromJar("be.hogent.tarsos.midi.sin20ms.wav", sineWaveFile.getAbsolutePath());
        }
        return sineWaveFile.toURI().toURL();
    }

    public void playAnnotations(final int smootFilterWindowSize) {
        try {
            writeFile(null, smootFilterWindowSize);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final SinkIsFullException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final BufferNotAvailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
     * @throws IOException
     *             When something goes awry.
     * @throws SinkIsFullException
     * @throws InterruptedException
     * @throws BufferNotAvailableException
     */
    public void writeFile(final String fileName, final int smootFilterWindowSize) throws IOException,
    SinkIsFullException, InterruptedException, BufferNotAvailableException {
        // invariant: at any time the lists are equal in length
        assert frequencies.size() == realTimes.size();

        if (smootFilterWindowSize > 0) {
            frequencies = PitchFunctions.medianFilter(frequencies, smootFilterWindowSize);
            powers = PitchFunctions.medianFilter(powers, smootFilterWindowSize);
        }

        final URL sine50Hz44100 = sineWaveURL();

        final float baseFreqWavFile = 50;
        final float srate = 44100.f;
        final int bufferSize = 1024;
        final LoopBuffer tone = new LoopBuffer(srate, bufferSize, sine50Hz44100);
        SourcePlayer player;
        if (fileName != null) {
            final String rawFileName = fileName + ".raw";
            player = new SourcePlayer(bufferSize, srate, rawFileName);
        } else {
            player = new SourcePlayer(bufferSize, srate);
        }

        final Mixer mixer = new Mixer(bufferSize, 1);
        player.addSource(mixer);
        mixer.addSource(tone);
        tone.setSpeed(0f / baseFreqWavFile);
        if (fileName == null) {
            player.start();
        }

        for (int i = 0; i < frequencies.size(); i++) {
            final double freq = frequencies.get(i) == -1.0 ? 0.0 : frequencies.get(i);
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
            final String rawFileName = fileName + ".raw";
            convertRawToWav(srate, rawFileName, fileName);
            new File(rawFileName).deleteOnExit();
        }
    }

    /**
     * Adds a correct header to a raw file.
     * 
     * @throws IOException
     */
    private void convertRawToWav(final double srate, final String rawFileName, final String wavFileName) throws IOException {
        final FileInputStream inStream = new FileInputStream(new File(rawFileName));
        final File out = new File(wavFileName);
        final int bytesAvailable = inStream.available();
        final int sampleSizeInBits = 16;
        final int channels = 1;
        final boolean signed = false;
        final boolean bigEndian = false;
        final AudioFormat audioFormat = new AudioFormat((float) srate, sampleSizeInBits, channels, signed,
                bigEndian);
        final AudioInputStream audioInputStream = new AudioInputStream(inStream, audioFormat, bytesAvailable / 2);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
        audioInputStream.close();
        inStream.close();
    }

    /**
     * Read data from a CSV-File, handle it with the handler, smooth it and save
     * it to the generated audio folder.
     * @param csvFileName
     *            the CSV-file to process
     * @param handler
     *            the handler
     * @param smootFilterWindowSize
     *            the window size for the smoothing function (Median filter).
     */
    public static void saveAsWav(final String csvFileName, final CSVFileHandler handler,
            final int smootFilterWindowSize) {
        final String correctedFileName = new File(csvFileName).getAbsolutePath();
        try {
            final ToneSequenceBuilder builder = new ToneSequenceBuilder();
            final List<String[]> rows = FileUtils.readCSVFile(correctedFileName, handler.getSeparator(), handler
                    .getNumberOfExpectedColumn());
            for (final String[] row : rows) {
                handler.handleRow(builder, row);
            }
            builder.writeFile("data/generated_audio/" + FileUtils.basename(correctedFileName) + ".wav",
                    smootFilterWindowSize);
        } catch (final Exception e) {
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

        void setExtractor(SignalPowerExtractor extractor);
    }

    /**
     * @author Joren Six
     */
    public enum AnnotationCVSFileHandlers {
        /**
         * Handles files generated by BOZKURT.
         */
        BOZKURT(new BozkurtCSVFileHandler()),
        /**
         * Handles files generated by IPEM.
         */
        IPEM(new IpemCSVFileHandler()),
        /**
         * Handles files generated by AUBIO.
         */
        AUBIO(new AubioCSVHandler());

        /**
         * The underlying handler.
         */
        private final CSVFileHandler cvsFileHandler;

        /**
         * Create a new annotation Handler.
         * @param handler
         *            The underlying handler.
         */
        private AnnotationCVSFileHandlers(final CSVFileHandler handler) {
            cvsFileHandler = handler;
        }

        /**
         * @return the cvsFileHandler
         */
        public CSVFileHandler getCvsFileHandler() {
            return cvsFileHandler;
        }

    }

    private static class BozkurtCSVFileHandler implements CSVFileHandler {
        private static final double REFFREQUENCY = 8.17579891564371; // Hz
        private static final int SAMPLERATE = 100; // Hz
        private static final int CENTSINOCTAVE = 1200;
        private SignalPowerExtractor extr;

        @Override
        public void handleRow(final ToneSequenceBuilder builder, final String[] row) {
            final double realTime = Double.parseDouble(row[0]) / SAMPLERATE;
            final double frequency = REFFREQUENCY * Math.pow(2.0, Double.parseDouble(row[1]) / CENTSINOCTAVE);
            if (extr == null) {
                builder.addTone(frequency, realTime);
            } else {
                builder.addTone(frequency, realTime, extr.powerAt(realTime, true));
            }
        }

        @Override
        public int getNumberOfExpectedColumn() {
            return 2;
        }

        @Override
        public String getSeparator() {
            return "[\\s]+";
        }

        @Override
        public void setExtractor(final SignalPowerExtractor extractor) {
            extr = extractor;
        }
    }

    private static class AubioCSVHandler implements CSVFileHandler {
        private SignalPowerExtractor extr;

        @Override
        public void handleRow(final ToneSequenceBuilder builder, final String[] row) {
            final double realTime = Double.parseDouble(row[0]);
            final double frequency = Double.parseDouble(row[1]);
            if (extr == null) {
                builder.addTone(frequency, realTime);
            } else {
                builder.addTone(frequency, realTime, extr.powerAt(realTime, true));
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

        @Override
        public void setExtractor(final SignalPowerExtractor extractor) {
            extr = extractor;
        }
    };

    private static class IpemCSVFileHandler implements CSVFileHandler {
        /**
         * Log messages.
         */
        private static final Logger LOG = Logger.getLogger(IpemCSVFileHandler.class.getName());
        private int sampleNumber = 0;
        private SignalPowerExtractor extr;

        @Override
        public void handleRow(final ToneSequenceBuilder builder, final String[] row) {
            sampleNumber++;
            final double realTime = sampleNumber / 100.0; // 100 Hz sample frequency
            // (every 10 ms)
            double frequency = 0.0;
            try {
                frequency = Double.parseDouble(row[0]);
                if (extr == null) {
                    builder.addTone(frequency, realTime);
                } else {
                    builder.addTone(frequency, realTime, extr.powerAt(realTime, true));
                }
            } catch (final NumberFormatException e) {
                LOG.warning("Ignored invalid formatted number: " + row[0]);
            }
        }

        @Override
        public int getNumberOfExpectedColumn() {
            return 0;
        }

        @Override
        public String getSeparator() {
            return " ";
        }

        @Override
        public void setExtractor(final SignalPowerExtractor extractor) {
            extr = extractor;
        }
    }

}
