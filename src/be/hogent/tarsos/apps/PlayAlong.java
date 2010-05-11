package be.hogent.tarsos.apps;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import javax.sound.midi.MidiDevice.Info;
import javax.swing.JFrame;

import be.hogent.tarsos.midi.DumpReceiver;
import be.hogent.tarsos.midi.MidiCommon;
import be.hogent.tarsos.midi.MidiUtils;
import be.hogent.tarsos.midi.ReceiverSink;
import be.hogent.tarsos.pitch.AubioPitchDetection;
import be.hogent.tarsos.pitch.IPEMPitchDetection;
import be.hogent.tarsos.pitch.PitchConverter;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.PitchUnit;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.YinPitchDetection;
import be.hogent.tarsos.pitch.AubioPitchDetection.AubioPitchDetectionMode;
import be.hogent.tarsos.ui.PianoTestFrame;
import be.hogent.tarsos.ui.VirtualKeyboard;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

public class PlayAlong {

    /**
     * Choose a MIDI device using a CLI. If an invalid device number is given
     * the user is requested to choose another one.
     * 
     * @param inputDevice
     *            is the MIDI device needed for input of events? E.G. a keyboard
     * @param outputDevice
     *            is the MIDI device needed to send events to? E.g. a (software)
     *            synthesizer.
     * @return the chosen MIDI device
     */
    public static MidiDevice chooseDevice(boolean inputDevice, boolean outputDevice) {
        try {
            // choose MIDI input device
            MidiCommon.listDevices(inputDevice, outputDevice);
            String deviceType = (inputDevice ? " IN " : "") + (outputDevice ? " OUT " : "");
            System.out.print("Choose the MIDI" + deviceType + "device: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            int deviceIndex = Integer.parseInt(br.readLine());
            System.out.println();
            Info midiDeviceInfo = MidiSystem.getMidiDeviceInfo()[deviceIndex];

            MidiDevice device = MidiSystem.getMidiDevice(midiDeviceInfo);
            if ((device.getMaxTransmitters() == 0 == inputDevice)
                    && (device.getMaxReceivers() == 0 == outputDevice)) {
                System.out.println("Invalid choise, please try again");
                return chooseDevice(inputDevice, outputDevice);
            } else {
                return device;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number, please try again");
            return chooseDevice(inputDevice, outputDevice);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MidiUnavailableException e) {
            System.out.println("The device is not available ( " + e.getMessage()
                    + " ), please choose another device.");
            return chooseDevice(inputDevice, outputDevice);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Number out of bounds, please try again");
            return chooseDevice(inputDevice, outputDevice);
        }
        return null;
    }

    public static void main(final String[] args) throws MidiUnavailableException, InterruptedException,
    IOException {
        LongOpt[] longopts = new LongOpt[4];
        longopts[0] = new LongOpt("in", LongOpt.REQUIRED_ARGUMENT, null, 'i');
        longopts[1] = new LongOpt("detector", LongOpt.REQUIRED_ARGUMENT, null, 'd');
        longopts[2] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
        longopts[3] = new LongOpt("midi_in", LongOpt.NO_ARGUMENT, null, 'm');

        Getopt g = new Getopt("playalong", args, "-i:d:h:m", longopts);
        int device = -1;
        String detectorString = "TARSOS";
        String fileName = null;

        int c;
        while ((c = g.getopt()) != -1) {
            String arg = g.getOptarg();
            switch (c) {
            case 'i':
                fileName = arg;
                break;
            case 'd':
                detectorString = arg.toUpperCase();
                break;
            case 'm':
                device = Integer.parseInt(arg);
                break;
            case 'h':
                printHelp();
                System.exit(0);
                return;
            }
        }

        if (fileName == null || !FileUtils.exists(fileName)) {
            printHelp();
            System.exit(-1);
        }

        AudioFile fileToPlayAlongWith = new AudioFile(fileName);
        PitchDetector detector = new YinPitchDetection(fileToPlayAlongWith);
        if (detectorString.equals("AUBIO")) {
            detector = new AubioPitchDetection(fileToPlayAlongWith, AubioPitchDetectionMode.YIN);
        } else if (detectorString.equals("IPEM")) {
            detector = new IPEMPitchDetection(fileToPlayAlongWith);
        }

        detector.executePitchDetection();
        final List<Sample> samples = detector.getSamples();
        String baseName = FileUtils.basename(fileName);

        FileUtils.mkdirs("data/octave/" + baseName);
        FileUtils.mkdirs("data/range/" + baseName);

        // String toneScalefileName = baseName + '/' + baseName + "_" +
        // detector.getName() + "_octave.txt";
        Histogram octaveHistogram = Sample.ambitusHistogram(samples).toneScaleHistogram();
        List<Peak> peaks = PeakDetector.detect(octaveHistogram, 15, 0.5);

        System.out.println(peaks.size() + " peaks found in: " + FileUtils.basename(fileName));
        System.out.println("");
        double[] peakPositions = new double[peaks.size()];
        int peakIndex = 0;
        for (Peak p : peaks) {
            peakPositions[peakIndex] = p.getPosition();
            System.out.println(p.getPosition());
            peakIndex++;
        }
        System.out.println("");

        final VirtualKeyboard keyboard = VirtualKeyboard.createVirtualKeyboard(peaks.size());
        double[] rebasedTuning = tuningFromPeaks(peakPositions);

        try {
            MidiDevice.Info synthInfo = MidiCommon.getMidiDeviceInfo("Gervill", true);
            MidiDevice synthDevice = null;
            synthDevice = MidiSystem.getMidiDevice(synthInfo);
            synthDevice.open();

            Receiver recv;
            recv = new ReceiverSink(true, synthDevice.getReceiver(), new DumpReceiver(System.out));
            keyboard.setReceiver(recv);

            MidiDevice virtualMidiInputDevice;
            if (device == -1) {
                virtualMidiInputDevice = chooseDevice(true, false);
            } else {
                Info midiDeviceInfo = MidiSystem.getMidiDeviceInfo()[device];
                virtualMidiInputDevice = MidiSystem.getMidiDevice(midiDeviceInfo);
            }
            virtualMidiInputDevice.open();
            Transmitter midiInputTransmitter = virtualMidiInputDevice.getTransmitter();
            midiInputTransmitter.setReceiver(keyboard);

            MidiUtils.sendTunings(recv, 0, 2, "african", rebasedTuning);
            MidiUtils.sendTuningChange(recv, VirtualKeyboard.CHANNEL, 2);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }

        JFrame f = new PianoTestFrame(keyboard, rebasedTuning);
        f.setVisible(true);

        final List<Double> midiKeysUnfiltered = new ArrayList<Double>();
        final List<Long> time = new ArrayList<Long>();

        Iterator<Sample> sampleIterator = samples.iterator();
        Sample currentSample = sampleIterator.next();
        int currentMidiKey = 0;
        while (sampleIterator.hasNext()) {
            List<Double> currentPitches = currentSample.getPitchesIn(PitchUnit.ABSOLUTE_CENTS);
            if (currentPitches.size() == 1) {
                // double pitch = currentPitches.get(0);
                /*
                 * for (int midiKey = 0; midiKey < 128; midiKey++) { if(pitch >
                 * tuningMin[midiKey] && pitch < tuningMax[midiKey]){
                 * currentMidiKey = midiKey; } }
                 */
                midiKeysUnfiltered.add(currentMidiKey + 0.0);
                time.add(currentSample.getStart());
            }
            currentSample = sampleIterator.next();
        }
    }

    /**
     * @param peaks
     * @return
     */
    public static double[] tuningFromPeaks(final double[] peaks) {
        final double[] tuning = new double[128];

        // align tuning to MIDI note 57, A3 or 220Hz.
        Double referenceNote = PitchConverter.hertzToAbsoluteCent(220.0);
        int referenceNoteMidiNumber = 57;

        int midiNoteClosestToReference = -1;
        double closestDistance = Double.MAX_VALUE;
        for (int i = 0; i < tuning.length; i++) {
            int octave = i / peaks.length;
            double centOffset = peaks[i % peaks.length];
            tuning[i] = octave * 1200 + centOffset;
            double distanceToReferenceNote = Math.abs(tuning[i] - referenceNote); // cents
            if (distanceToReferenceNote < closestDistance) {
                closestDistance = distanceToReferenceNote;
                midiNoteClosestToReference = i;
            }
        }

        System.out.println("Closest to midi key 57 (220Hz," + referenceNote
                + " cents) is the tuned midi key " + midiNoteClosestToReference + " at "
                + tuning[midiNoteClosestToReference] + " cents");

        double[] rebasedTuning = new double[128];
        int diff = referenceNoteMidiNumber - midiNoteClosestToReference;
        for (int i = 0; i < tuning.length; i++) {
            rebasedTuning[i] = tuning[(i + diff) % 128];
        }
        return rebasedTuning;
    }

    private static void printHelp() {
        System.out.println("");
        System.out.println("Play along with a file using a MIDI keyboard");
        System.out.println("");
        System.out.println("-----------------------");
        System.out.println("");
        System.out
        .println("java -jar playalong.jar --in file.wav [--detector TARSOS|AUBIO|IPEM] [--midi_in 1]");
        System.out.println("");
        System.out.println("-----------------------");
        System.out.println("");
        System.out.println("--in file.wav\t\tThe file to process");
        System.out.println("--detector DETECTOR\tThe pitch detector used.");
        System.out.println("--midi_in integer\t\tThe input midi device number.");
        System.out.println("");
    }
}
