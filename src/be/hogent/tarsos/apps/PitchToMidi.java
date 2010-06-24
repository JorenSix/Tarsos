/**
 */
package be.hogent.tarsos.apps;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.pitch.Pitch;
import be.hogent.tarsos.pitch.PitchUnit;
import be.hogent.tarsos.pitch.pure.MetaPitchDetector;
import be.hogent.tarsos.pitch.pure.PurePitchDetector;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FFT;
import be.hogent.tarsos.util.RealTimeAudioProcessor;
import be.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor;

/**
 * @author Joren Six
 */
public class PitchToMidi extends AbstractTarsosApp implements AudioProcessor {

    PurePitchDetector pure;
    Receiver receiver;
    FFT fft;

    /*
     * (non-Javadoc)
     * 
     * @see be.hogent.tarsos.apps.AbstractTarsosApp#description()
     */
    @Override
    public String description() {
        return "Listens to incoming audio and fires midi events if the detected "
        + "pitch is close to a predefined pitch class.";
    }

    /*
     * (non-Javadoc)
     * 
     * @see be.hogent.tarsos.apps.AbstractTarsosApp#name()
     */
    @Override
    public String name() {
        return "pitch_to_midi";
    }

    Sequence sequence;
    Sequencer sequencer;

    /*
     * (non-Javadoc)
     * 
     * @see be.hogent.tarsos.apps.AbstractTarsosApp#run(java.lang.String[])
     */
    @Override
    public void run(final String... args) {

        final float samplingRate = 44100.0f;

        pure = new MetaPitchDetector(samplingRate);



        try {

            final boolean toFile = true;
            if (toFile) {
                sequencer = MidiSystem.getSequencer();
                sequencer.open();
                receiver = sequencer.getReceiver();
                sequence = new Sequence(Sequence.PPQ, 20, 1);
                sequencer.setSequence(sequence);
                sequencer.recordDisable(sequence.getTracks()[0]);

                sequencer.startRecording();
            } else {
                final MidiDevice synth = Tarsos.chooseMidiDevice(false,true);
                synth.open();
                receiver = synth.getReceiver();
            }

            final boolean fromFile = true;
            final RealTimeAudioProcessor proc;
            if (!fromFile) {
                final Mixer mixer = Tarsos.chooseMixerDevice();
                final AudioFormat format = new AudioFormat(samplingRate, 16, 1,true, false);
                final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
                final TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
                final int numberOfSamples = (int) (0.1 * samplingRate);
                line.open(format, numberOfSamples);
                line.start();
                final AudioInputStream stream = new AudioInputStream(line);
                proc = new RealTimeAudioProcessor(stream, 2048);
            }else{
                final String path = new AudioFile("spraak_kort.wav").transcodedPath();
                proc = new RealTimeAudioProcessor(path, 2048);
            }
            proc.addAudioProcessor(this);

            fft = new FFT(1024);
            keyOn = new boolean[128];
            velocities = new int[128];
            previousKeys = new boolean[128];
            new Thread(proc).start();



        } catch (final LineUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final MidiUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvalidMidiDataException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    float maxAmpl = 1;
    float minAmpl = 0;
    int i = 0;

    /*
     * (non-Javadoc)
     * 
     * @see
     * be.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor#proccess(
     * float[])
     */
    @Override
    public void proccess(final float[] audioBuffer) {
        /*
         * final float pitch = pure.getPitch(audioBuffer); final double
         * midiCentValue = PitchConverter.hertzToMidiCent(pitch); // 'musical'
         * pitch detected ? if (Math.abs(midiCentValue - (int) midiCentValue) <
         * 0.3 && midiCentValue < 128 && midiCentValue >= 0) { final String
         * lastDetectedNote = "Name: " + Pitch.getInstance(PitchUnit.HERTZ,
         * pitch).noteName() + "\t Frequency: " + ((int) pitch) + "Hz \t" +
         * " MIDI note:" + PitchConverter.hertzToMidiCent(pitch);
         * Tarsos.println(lastDetectedNote); }
         */
        fft.forwardTransform(audioBuffer);
        final float[] amplitudes = new float[1024];
        fft.modulus(audioBuffer, amplitudes);
        for (int i = 0; i < amplitudes.length; i++) {
            final float ampl = (float) Math.log1p(amplitudes[i]);
            amplitudes[i] = ampl;
            maxAmpl = Math.max(maxAmpl, ampl);
            minAmpl = Math.min(minAmpl, ampl);
        }
        maxAmpl = maxAmpl - minAmpl;
        for (int i = 0; i < amplitudes.length; i++) {
            amplitudes[i] = 127 * (amplitudes[i] - minAmpl) / maxAmpl;
        }

        // piano keys go from MIDI number 21 to 108
        for (int i = 25; i < 108; i++) {
            final Pitch pitchObj = Pitch.getInstance(PitchUnit.MIDI_KEY, i);
            final double pitchInHz = pitchObj.getPitch(PitchUnit.HERTZ);
            final int bin = (int) (pitchInHz * 1024.0 / 44100.0);
            keyOn[i] = amplitudes[bin] > 15;
            velocities[i] = (int) amplitudes[bin];
        }

        sendNoteMessages();

        i++;

        if (i % 1000 == 0) {
            Tarsos.println(i * 1024.0 / 44100.0 + "s");
        }
    }

    boolean[] keyOn;
    boolean[] previousKeys;
    int[] velocities;


    public void sendNoteMessages() {
        for (int i = 0; i < keyOn.length; i++) {
            if (previousKeys[i] != keyOn[i]) {
                final ShortMessage sm = new ShortMessage();

                final int command = keyOn[i] ? ShortMessage.NOTE_ON : ShortMessage.NOTE_OFF;
                final int velocity = keyOn[i] ? velocities[i] : 0;
                try {
                    sm.setMessage(command, 2, i, velocity);
                } catch (final InvalidMidiDataException e) {
                    e.printStackTrace();
                }
                receiver.send(sm, -1);
            }
            previousKeys[i] = keyOn[i];
        }
    }

    public static void main(final String... args) throws IOException {
        new PitchToMidi().run(args);
    }

    /*
     * (non-Javadoc)
     * 
     * @seebe.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor#
     * processingFinished()
     */
    @Override
    public void processingFinished() {
        try {
            sequencer.stopRecording();
            MidiSystem.write(sequence, 0, new File("ttm.midi"));
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
