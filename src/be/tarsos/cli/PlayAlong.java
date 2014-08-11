/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.cli;

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

import be.tarsos.Tarsos;
import be.tarsos.midi.LogReceiver;
import be.tarsos.midi.MidiCommon;
import be.tarsos.midi.MidiUtils;
import be.tarsos.midi.ReceiverSink;
import be.tarsos.midi.TarsosSynth;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.IPEMPitchDetection;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.sampled.pitch.TarsosPitchDetection;
import be.tarsos.sampled.pitch.VampPitchDetection;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.ui.virtualkeyboard.PianoTestFrame;
import be.tarsos.ui.virtualkeyboard.VirtualKeyboard;
import be.tarsos.util.AudioFile;
import be.tarsos.util.FileUtils;
import be.tarsos.util.histogram.Histogram;
import be.tarsos.util.histogram.HistogramFactory;
import be.tarsos.util.histogram.peaks.Peak;
import be.tarsos.util.histogram.peaks.PeakDetector;

public final class PlayAlong {

	/**
	 * Log messages.
	 */
	public static final Logger LOG = Logger.getLogger(PlayAlong.class.getName());

	private PlayAlong() {
	}

	public static void main(final String[] args) throws MidiUnavailableException, InterruptedException,
			IOException {
		
		String detectorString = "TARSOS";
		String fileName = null;
		if (fileName == null || !FileUtils.exists(fileName)) {
			printHelp();
			System.exit(-1);
		}

		AudioFile fileToPlayAlongWith;
		try {
			fileToPlayAlongWith = new AudioFile(fileName);
			PitchDetector detector = new TarsosPitchDetection(fileToPlayAlongWith,
					PitchDetectionMode.TARSOS_YIN);
			if (detectorString.equals("AUBIO")) {
				detector = new VampPitchDetection(fileToPlayAlongWith, PitchDetectionMode.VAMP_YIN);
			} else if (detectorString.equals("IPEM_SIX")) {
				detector = new IPEMPitchDetection(fileToPlayAlongWith, PitchDetectionMode.IPEM_SIX);
			}

			detector.executePitchDetection();
			final List<Annotation> samples = detector.getAnnotations();
			final String baseName = FileUtils.basename(fileName);

			FileUtils.mkdirs("data/octave/" + baseName);
			FileUtils.mkdirs("data/range/" + baseName);

			// String toneScalefileName = baseName + '/' + baseName + "_" +
			// detector.getName() + "_octave.txt";
			final Histogram octaveHistogram = HistogramFactory.createPitchHistogram(samples).pitchClassHistogram();
			final List<Peak> peaks = PeakDetector.detect(octaveHistogram, 15,15);

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
				int device = -1;
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
				MidiUtils.sendTuningChange(recv, TarsosSynth.TUNED_MIDI_CHANNEL, 2);
			} catch (final MidiUnavailableException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			} catch (final InvalidMidiDataException e) {
				e.printStackTrace();
			}

			final JFrame f = new PianoTestFrame(keyboard, rebasedTuning);
			f.setVisible(true);

			final List<Integer> midiKeysUnfiltered = new ArrayList<Integer>();
			final List<Double> time = new ArrayList<Double>();

			final Iterator<Annotation> sampleIterator = samples.iterator();
			Annotation currentSample = sampleIterator.next();
			int currentMidiKey = 0;
			while (sampleIterator.hasNext()) {
				currentMidiKey = (int) currentSample.getPitch(PitchUnit.MIDI_KEY);

				// double pitch = currentPitches.get(0);
				/*
				 * for (int midiKey = 0; midiKey < 128; midiKey++) { if(pitch >
				 * tuningMin[midiKey] && pitch < tuningMax[midiKey]){
				 * currentMidiKey = midiKey; } }
				 */
				midiKeysUnfiltered.add(currentMidiKey);
				time.add(currentSample.getStart());

				currentSample = sampleIterator.next();
			}
		} catch (EncoderException e) {
			e.printStackTrace();
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
