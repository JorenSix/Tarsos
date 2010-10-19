package be.hogent.tarsos.cli;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import javax.swing.JFrame;

import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.midi.LogReceiver;
import be.hogent.tarsos.midi.MidiCommon;
import be.hogent.tarsos.midi.MidiUtils;
import be.hogent.tarsos.midi.ReceiverSink;
import be.hogent.tarsos.sampled.pitch.IPEMPitchDetection;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.sampled.pitch.Sample;
import be.hogent.tarsos.sampled.pitch.TarsosPitchDetection;
import be.hogent.tarsos.sampled.pitch.VampPitchDetection;
import be.hogent.tarsos.ui.virtualkeyboard.PianoTestFrame;
import be.hogent.tarsos.ui.virtualkeyboard.VirtualKeyboard;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

public final class PlayAlong {

	/**
	 * Log messages.
	 */
	public static final Logger LOG = Logger.getLogger(PlayAlong.class.getName());

	private PlayAlong() {
	}

	/**
	 * Log messages.
	 */
	// private static final Logger LOG =
	// Logger.getLogger(PlayAlong.class.getName());

	public static void main(final String[] args) throws MidiUnavailableException, InterruptedException,
			IOException {
		final LongOpt[] longopts = new LongOpt[4];
		longopts[0] = new LongOpt("in", LongOpt.REQUIRED_ARGUMENT, null, 'i');
		longopts[1] = new LongOpt("detector", LongOpt.REQUIRED_ARGUMENT, null, 'd');
		longopts[2] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
		longopts[3] = new LongOpt("midi_in", LongOpt.NO_ARGUMENT, null, 'm');

		final Getopt g = new Getopt("playalong", args, "-bufferCount:d:h:m", longopts);
		int device = -1;
		String detectorString = "TARSOS";
		String fileName = null;

		int c;
		while ((c = g.getopt()) != -1) {
			final String arg = g.getOptarg();
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
			default:
				printHelp();
				System.exit(0);
				return;

			}
		}

		if (fileName == null || !FileUtils.exists(fileName)) {
			printHelp();
			System.exit(-1);
		}

		final AudioFile fileToPlayAlongWith = new AudioFile(fileName);
		PitchDetector detector = new TarsosPitchDetection(fileToPlayAlongWith, PitchDetectionMode.TARSOS_YIN);
		if (detectorString.equals("AUBIO")) {
			detector = new VampPitchDetection(fileToPlayAlongWith, PitchDetectionMode.VAMP_YIN);
		} else if (detectorString.equals("IPEM_SIX")) {
			detector = new IPEMPitchDetection(fileToPlayAlongWith, PitchDetectionMode.IPEM_SIX);
		}

		detector.executePitchDetection();
		final List<Sample> samples = detector.getSamples();
		final String baseName = FileUtils.basename(fileName);

		FileUtils.mkdirs("data/octave/" + baseName);
		FileUtils.mkdirs("data/range/" + baseName);

		// String toneScalefileName = baseName + '/' + baseName + "_" +
		// detector.getName() + "_octave.txt";
		final Histogram octaveHistogram = Sample.ambitusHistogram(samples).toneScaleHistogram();
		final List<Peak> peaks = PeakDetector.detect(octaveHistogram, 15, 0.5);

		Tarsos.println(peaks.size() + " peaks found in: " + FileUtils.basename(fileName));
		Tarsos.println("");
		final double[] peakPositions = new double[peaks.size()];
		int peakIndex = 0;
		for (final Peak p : peaks) {
			peakPositions[peakIndex] = p.getPosition();
			Tarsos.println(p.getPosition() + "");
			peakIndex++;
		}
		Tarsos.println("");

		final VirtualKeyboard keyboard = VirtualKeyboard.createVirtualKeyboard(peaks.size());
		final double[] rebasedTuning = MidiCommon.tuningFromPeaks(peakPositions);

		try {
			final MidiDevice.Info synthInfo = MidiCommon.getMidiDeviceInfo("Gervill", true);
			MidiDevice synthDevice = null;
			synthDevice = MidiSystem.getMidiDevice(synthInfo);
			synthDevice.open();

			Receiver recv;
			recv = new ReceiverSink(true, synthDevice.getReceiver(), new LogReceiver());
			keyboard.setReceiver(recv);

			MidiDevice virtualMidiInputDevice;
			if (device == -1) {
				virtualMidiInputDevice = MidiCommon.chooseMidiDevice(true, false);
			} else {
				final Info midiDeviceInfo = MidiSystem.getMidiDeviceInfo()[device];
				virtualMidiInputDevice = MidiSystem.getMidiDevice(midiDeviceInfo);
			}
			virtualMidiInputDevice.open();
			final Transmitter midiInputTransmitter = virtualMidiInputDevice.getTransmitter();
			midiInputTransmitter.setReceiver(keyboard);

			MidiUtils.sendTunings(recv, 0, 2, "african", rebasedTuning);
			MidiUtils.sendTuningChange(recv, VirtualKeyboard.CHANNEL, 2);
		} catch (final MidiUnavailableException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InvalidMidiDataException e) {
			e.printStackTrace();
		}

		final JFrame f = new PianoTestFrame(keyboard, rebasedTuning);
		f.setVisible(true);

		final List<Double> midiKeysUnfiltered = new ArrayList<Double>();
		final List<Long> time = new ArrayList<Long>();

		final Iterator<Sample> sampleIterator = samples.iterator();
		Sample currentSample = sampleIterator.next();
		final int currentMidiKey = 0;
		while (sampleIterator.hasNext()) {
			final List<Double> currentPitches = currentSample.getPitchesIn(PitchUnit.ABSOLUTE_CENTS);
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

	private static void printHelp() {
		Tarsos.println("");
		Tarsos.println("Play along with a file using a MIDI keyboard");
		Tarsos.println("");
		Tarsos.println("-----------------------");
		Tarsos.println("");
		Tarsos.println("java -jar playalong.jar --in file.wav "
				+ "[--detector TARSOS|AUBIO|IPEM_SIX] [--midi_in 1]");
		Tarsos.println("");
		Tarsos.println("-----------------------");
		Tarsos.println("");
		Tarsos.println("--in file.wav\t\tThe file to process");
		Tarsos.println("--detector DETECTOR\tThe pitch detector used.");
		Tarsos.println("--midi_in integer\t\tThe input midi device number.");
		Tarsos.println("");
	}
}
