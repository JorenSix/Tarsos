/**
 */
package be.hogent.tarsos.apps;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.pitch.Pitch;
import be.hogent.tarsos.pitch.PitchConverter;
import be.hogent.tarsos.pitch.PitchUnit;
import be.hogent.tarsos.pitch.pure.PurePitchDetector;
import be.hogent.tarsos.pitch.pure.Yin;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FFT;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.RealTimeAudioProcessor;
import be.hogent.tarsos.util.SignalPowerExtractor;
import be.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor;

/**
 * @author Joren Six
 */
public class PitchToMidi extends AbstractTarsosApp {

    /**
     * The number of MIDI keys.
     */
    private static final int MIDI_NUMBERS = 128;

    private Receiver receiver;
    private Sequence sequence;
    private Sequencer sequencer;

    private final boolean[] keyOn;
    private final boolean[] previousKeys;
    private final int[] velocities;

    int bufferCount = 0;

    private boolean toFile;

    private Track track;

    String outputMidi;


    public PitchToMidi() {
        track = null;
        keyOn = new boolean[MIDI_NUMBERS];
        velocities = new int[MIDI_NUMBERS];
        previousKeys = new boolean[MIDI_NUMBERS];
    }

    /*
     * (non-Javadoc)
     * @see be.hogent.tarsos.apps.AbstractTarsosApp#description()
     */
    @Override
    public String description() {
        return "Listens to incoming audio and fires midi events if the detected "
        + "pitch is close to a predefined pitch class.";
    }

    /*
     * (non-Javadoc)
     * @see be.hogent.tarsos.apps.AbstractTarsosApp#name()
     */
    @Override
    public String name() {
        return "pitch_to_midi";
    }


    /*
     * (non-Javadoc)
     * @see be.hogent.tarsos.apps.AbstractTarsosApp#run(java.lang.String[])
     */
    @Override
    public void run(final String... args) {

        final OptionParser parser = new OptionParser();
        toFile = false;
        boolean fromFile = false;

        String inputAudio = null;

        final OptionSpec spec = parser.accepts("pitch");

        final OptionSet options = parse(args, parser, this);

        if (options.nonOptionArguments().size() == 1) {
            final String fileName = options.nonOptionArguments().get(0);
            if (FileUtils.extension(fileName).toUpperCase().contains("MID")) {
                outputMidi = fileName;
                toFile = true;
            } else {
                inputAudio = fileName;
                fromFile = true;
            }
        } else if (options.nonOptionArguments().size() == 2) {
            String fileName = options.nonOptionArguments().get(0);
            if (FileUtils.extension(fileName).toUpperCase().contains("MID")) {
                outputMidi = fileName;
                toFile = true;
            } else {
                inputAudio = fileName;
                fromFile = true;
            }
            fileName = options.nonOptionArguments().get(1);
            if (FileUtils.extension(fileName).toUpperCase().contains("MID")) {
                outputMidi = fileName;
                toFile = true;
            } else {
                inputAudio = fileName;
                fromFile = true;
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                PitchToMidi.this.cleanup();
            }
        });

        if (isHelpOptionSet(options)) {
            printHelp(parser);
        } else {
            final boolean doCompleteFFT = !options.has(spec);
            try {
                if (toFile) {
                    sequencer = MidiSystem.getSequencer();
                    sequence = new Sequence(Sequence.PPQ, 40, 1);

                    sequencer.setSequence(sequence);
                    sequencer.open();
                    track = sequence.getTracks()[0];
                    sequencer.recordEnable(track, 1);
                    sequencer.startRecording();

                    receiver = sequencer.getReceiver();
                } else {
                    final MidiDevice synth = Tarsos.chooseMidiDevice(false, true);
                    synth.open();
                    receiver = synth.getReceiver();
                }

                final int samplesPerBuffer;

                final AudioProcessor processor;
                final float samplingRate = 44100.0f;
                if (doCompleteFFT) {
                    samplesPerBuffer = 2048;
                    processor = new FFTAudioProcessor(samplingRate, 1024);
                } else {
                    samplesPerBuffer = 1024;
                    processor = new PitchAudioProcessor(samplingRate);
                }


                final RealTimeAudioProcessor proc;
                if (!fromFile) {
                    final Mixer mixer = Tarsos.chooseMixerDevice();
                    final AudioFormat format = new AudioFormat(samplingRate, 16, 1, true, false);
                    final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
                    final TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
                    final int numberOfSamples = (int) (0.1 * samplingRate);
                    line.open(format, numberOfSamples);
                    line.start();
                    final AudioInputStream stream = new AudioInputStream(line);
                    proc = new RealTimeAudioProcessor(stream, samplesPerBuffer, 0, true);
                } else {
                    final String path = new AudioFile(inputAudio).transcodedPath();
                    proc = new RealTimeAudioProcessor(path, samplesPerBuffer);
                }

                proc.addAudioProcessor(processor);

                final Thread thread = new Thread(proc);
                thread.start();
                thread.join();
            } catch (final LineUnavailableException e) {
                e.printStackTrace();
            } catch (final UnsupportedAudioFileException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            } catch (final MidiUnavailableException e) {
                e.printStackTrace();
            } catch (final InvalidMidiDataException e) {
                e.printStackTrace();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class FFTAudioProcessor implements AudioProcessor {


        // piano keys go from MIDI number 21 to 108
        private final int MAX_MIDI_VELOCITY = 108;
        private final int MIN_MIDI_VELOCITY = 15;
        private final int START_MIDI_KEY = 25;
        private final int STOP_MIDI_KEY = 108;

        float maxAmpl = 1;
        float minAmpl = 0;
        private final FFT fft;
        private final float sampleRate;
        private final int fftSize;

        public FFTAudioProcessor(final float samplingRate, final int size) {
            fft = new FFT(size);
            this.sampleRate = samplingRate;
            this.fftSize = size;
        }

        /*
         * (non-Javadoc)
         * @see
         * be.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor#proccess
         * (float[])
         */
        @Override
        public void proccess(final float[] audioBuffer) {
            fft.forwardTransform(audioBuffer);
            final float[] amplitudes = new float[fftSize];
            fft.modulus(audioBuffer, amplitudes);
            for (int i = 0; i < amplitudes.length; i++) {
                final float ampl = (float) Math.log1p(amplitudes[i]);
                amplitudes[i] = ampl;
                maxAmpl = Math.max(maxAmpl, ampl);
                minAmpl = Math.min(minAmpl, ampl);
            }
            maxAmpl = maxAmpl - minAmpl;
            for (int i = 0; i < amplitudes.length; i++) {
                amplitudes[i] = MAX_MIDI_VELOCITY * (amplitudes[i] - minAmpl) / maxAmpl;
            }

            // piano keys go from MIDI number 21 to 108
            for (int i = START_MIDI_KEY; i < STOP_MIDI_KEY; i++) {
                final Pitch pitchObj = Pitch.getInstance(PitchUnit.MIDI_KEY, i);
                final double pitchInHz = pitchObj.getPitch(PitchUnit.HERTZ);
                final int bin = (int) (pitchInHz * fftSize / sampleRate);
                keyOn[i] = amplitudes[bin] > MIN_MIDI_VELOCITY;
                velocities[i] = (int) amplitudes[bin];
            }

            sendNoteMessages();

            bufferCount++;
        }

        /*
         * (non-Javadoc)
         * 
         * @seebe.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor#
         * processingFinished()
         */
        @Override
        public void processingFinished() {
            cleanup();
        }
    }

    private class PitchAudioProcessor implements AudioProcessor {
        private final PurePitchDetector pure;

        public PitchAudioProcessor(final double sampleRate) {
            pure = new Yin((float) sampleRate, 1024, 512);
        }

        /*
         * (non-Javadoc)
         * @see
         * be.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor#proccess
         * (float[])
         */
        @Override
        public void proccess(final float[] audioBuffer) {
            final float pitch = pure.getPitch(audioBuffer);
            final double midiCentValue = PitchConverter.hertzToMidiCent(pitch);
            final int midiKey = (int) midiCentValue;
            // 'musical' pitch detected ?
            if (Math.abs(midiCentValue - midiKey) < 0.3 && midiCentValue < 128 && midiCentValue >= 0) {
                final String lastDetectedNote = "Name: "
                    + Pitch.getInstance(PitchUnit.HERTZ, pitch).noteName() + "\t Frequency: "
                    + ((int) pitch) + "Hz \t" + " MIDI note:" + PitchConverter.hertzToMidiCent(pitch);
                Tarsos.println(lastDetectedNote);
                for (int i = 0; i < 128; i++) {
                    keyOn[i] = false;
                    velocities[i] = 0;
                }
                keyOn[midiKey] = true;

                // SPL is defined in db: 0 db = max => 128-SPL gives a MIDI
                // velocity
                velocities[midiKey] = 128 + (int) SignalPowerExtractor.soundPressureLevel(audioBuffer);
                sendNoteMessages();
            }
        }

        /*
         * (non-Javadoc)
         * @seebe.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor#
         * processingFinished()
         */
        @Override
        public void processingFinished() {
            cleanup();
        }
    }

    public void cleanup() {
        try {
            if (toFile) {
                sequencer.stopRecording();
                MidiSystem.write(sequence, 0, new File(outputMidi));
                sequencer.stop();
            }
            receiver.close();
        } catch (final IOException e) {
            // ignore
        } catch (final IllegalStateException e) {
            // ignore
        } catch (final NullPointerException e) {
            // ignore
        }
    }

    public void sendNoteMessages() {
        for (int i = 0; i < keyOn.length; i++) {
            if (previousKeys[i] != keyOn[i]) {
                final ShortMessage sm = new ShortMessage();

                final int command = keyOn[i] ? ShortMessage.NOTE_ON : ShortMessage.NOTE_OFF;
                final int velocity = keyOn[i] ? velocities[i] : 0;
                try {
                    sm.setMessage(command, 1, i, velocity);
                } catch (final InvalidMidiDataException e) {
                    e.printStackTrace();
                }
                final double seconds = bufferCount * 1024.0 / 44100.0;
                // 40 = the number of ticks per quarter note if the division
                // type is PPQ
                // 120 BPM => 2 BPS (second)

                final double ticksPerSecond = 40 * (120 / 60.0);
                final long ticks = (long) (seconds * ticksPerSecond);

                if (toFile) {
                    track.add(new MidiEvent(sm, ticks * 2));
                } else {
                    receiver.send(sm, -1);
                }
            }
            previousKeys[i] = keyOn[i];
        }
    }

    public static void main(final String... args) throws IOException {
        new PitchToMidi().run("--pitch");
    }

}
