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

/**
 */
package be.tarsos.cli;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
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
import be.tarsos.Tarsos;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.Yin;
import be.tarsos.midi.MidiCommon;
import be.tarsos.sampled.pitch.Pitch;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.AudioFile;
import be.tarsos.util.FFT;
import be.tarsos.util.FileUtils;
import be.tarsos.util.SignalPowerExtractor;

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

	private final Note[] notes;
	private final List<Note> noteList;
	
	//private int bufferCount = 0;

	private boolean toFile;

	private Track track;

	String outputMidi;

	public PitchToMidi() {
		track = null;
		notes = new Note[128];
		for(int i = 0; i < MIDI_NUMBERS ; i++){
			notes[i] = new Note(i);
		}
		noteList = Arrays.asList(notes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.tarsos.exp.cli.AbstractTarsosApp#description()
	 */

	@Override
	public String description() {
		return "Listens to incoming audio and fires midi events if the detected "
				+ "pitch is close to a predefined pitch class.";
	}
	
	private class MidiKey{
		private final int noteNumber;
		private boolean isOn;
		
		public MidiKey(final int midiNoteNumber){
			noteNumber = midiNoteNumber;
			isOn = false;
		}
		
		public void push(Receiver receiver,int velocity){
			sendNoteMessage(receiver, velocity);
			isOn = true;
		}
		
		public void release(Receiver receiver){
			sendNoteMessage(receiver,0);
			isOn = false;
		}
		
		private void sendNoteMessage(Receiver receiver,int velocity){
			final ShortMessage sm = new ShortMessage();
			final int command = isOn ? ShortMessage.NOTE_OFF : ShortMessage.NOTE_ON;
			try {
				sm.setMessage(command, 1, noteNumber, velocity);
			} catch (final InvalidMidiDataException e) {
				//this should not happen
			}
			try{
				receiver.send(sm, -1);
			} catch(final IndexOutOfBoundsException e){
				//when sending multiple midi events to a java synth it can throw
				//index out of bounds exceptions. Ignore those
				Tarsos.println("IndexOutOfBoundsException ignored");
			}
		}
		
		public boolean isOn(){
			return isOn;
		}
	}
	
	private class Note{
		private final int MIN_MIDI_VELOCITY = 15;
		
		private final MidiKey key;
		private int velocity;
		private boolean bigVelocityChange;
		private long start;
		private int maxNoteLength;//milliseconds
		
		
		public Note(final int midiNoteNumber){
			key = new MidiKey(midiNoteNumber);
			velocity = -1;
			start = -1;
			//a random maximum note length of minimum
			//100 ms and maximum 350ms.
			maxNoteLength = (int) (100 + Math.random() * 250);
		}
		
		public void setVelocity(final int newVelocity){
			if(newVelocity < MIN_MIDI_VELOCITY){
				if(velocity != -1){
					bigVelocityChange = true;
					velocity = -1;
				} else {
					bigVelocityChange = false;
				}
			} else {
				if(newVelocity > 2 * velocity || newVelocity < 2 * velocity){
					bigVelocityChange = true;
				} else {
					bigVelocityChange = true;
				}
				velocity = newVelocity;
			}
		}
		
		public void sound(final Receiver receiver){			
			//send not off to long notes or notes with big change in velocity.			
			boolean isLongNote = start != -1 && System.currentTimeMillis() - start > maxNoteLength;
			boolean release = isLongNote || bigVelocityChange;
			if(key.isOn() && release){
				key.release(receiver);
				start = -1;
			}
			//send not on to notes with big velocity change and a velocity
			if(!key.isOn() && bigVelocityChange && velocity!=-1){
				key.push(receiver, velocity);
				start = System.currentTimeMillis();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.tarsos.exp.cli.AbstractTarsosApp#run(java.lang.String[])
	 */

	@Override
	public void run(final String... args) {

		final OptionParser parser = new OptionParser();
		toFile = false;
		boolean fromFile = false;

		String inputAudio = null;

		@SuppressWarnings("rawtypes")
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
					final MidiDevice synth = MidiCommon.chooseMidiDevice(false, true);
					synth.open();
					receiver = synth.getReceiver();
					notes[69].setVelocity(100);
					notes[69].sound(receiver);					
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

				final AudioDispatcher proc;
				if (!fromFile) {
					final Mixer mixer = MidiCommon.chooseMixerDevice();
					final AudioFormat format = new AudioFormat(samplingRate, 16, 1, true, false);
					final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
					final TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
					final int numberOfSamples = (int) (0.1 * samplingRate);
					line.open(format, numberOfSamples);
					line.start();
					final AudioInputStream stream = new AudioInputStream(line);
					JVMAudioInputStream inputStream = new JVMAudioInputStream(stream); 
					proc = new AudioDispatcher(inputStream, samplesPerBuffer, 0);
				} else {
					final String path = new AudioFile(inputAudio).transcodedPath();
					proc = AudioDispatcherFactory.fromFile(new File(path), samplesPerBuffer,1024);
				}

				proc.addAudioProcessor(processor);
				final Thread thread = new Thread(proc,"Audio processor thread");
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
			} catch (EncoderException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void sendTestMessage(Receiver receiver){
		final ShortMessage sm = new ShortMessage();
		final int command = ShortMessage.NOTE_ON;
		final int velocity = 100;
		try {
			sm.setMessage(command, 1, 69, velocity);
		} catch (final InvalidMidiDataException e) {
			e.printStackTrace();
		}
		receiver.send(sm,-1);	
	}

	private class FFTAudioProcessor implements AudioProcessor {

		// piano keys go from MIDI number 21 to 108
		private final int MAX_MIDI_VELOCITY = 108;
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
		 * 
		 * @seebe.tarsos.util.RealTimeAudioProcessor.AudioProcessor#
		 * processingFinished()
		 */
		public void processingFinished() {
			cleanup();
		}


		public boolean process(AudioEvent audioEvent) {
			float[] audioBuffer = audioEvent.getFloatBuffer();
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
			float currentMax = 0;
			for (int i = 0; i < amplitudes.length; i++) {
				amplitudes[i] = MAX_MIDI_VELOCITY *  ((amplitudes[i]) - minAmpl) / maxAmpl;
				currentMax = Math.max(currentMax,amplitudes[i]);
			}
			
			System.out.println(currentMax);

			// piano keys go from MIDI number 21 to 108
			for (int i = START_MIDI_KEY; i < STOP_MIDI_KEY; i++) {
				final Pitch pitchObj = Pitch.getInstance(PitchUnit.MIDI_KEY, i);
				final double pitchInHz = pitchObj.getPitch(PitchUnit.HERTZ);
				final int bin = (int) (pitchInHz * fftSize / sampleRate);
				notes[i].setVelocity((int) amplitudes[bin]);
			}

			sendNoteMessages();

			//bufferCount++;
			return true;
		}

	}

	private class PitchAudioProcessor implements AudioProcessor {
		private final PitchDetector pure;

		public PitchAudioProcessor(final double sampleRate) {
			pure = new Yin((float) sampleRate, 1024, 512);
		}

	
		/*
		 * (non-Javadoc)
		 * 
		 * @seebe.tarsos.util.RealTimeAudioProcessor.AudioProcessor#
		 * processingFinished()
		 */
		public void processingFinished() {
			cleanup();
		}


		public boolean process(AudioEvent audioEvent) {
			float[] audioBuffer = audioEvent.getFloatBuffer();
			final PitchDetectionResult pitch = pure.getPitch(audioBuffer);
			final double midiCentValue = PitchUnit.hertzToMidiCent(pitch.getPitch());
			final int midiKey = (int) midiCentValue;
			// 'musical' pitch detected ?
			if (Math.abs(midiCentValue - midiKey) < 0.3 && midiCentValue < 128 && midiCentValue >= 0) {
				final String lastDetectedNote = "Name: "
						+ Pitch.getInstance(PitchUnit.HERTZ, pitch.getPitch()).noteName() + "\t Frequency: "
						+ (int) pitch.getPitch() + "Hz \t" + " MIDI note:" + PitchUnit.hertzToMidiCent(pitch.getPitch());
				Tarsos.println(lastDetectedNote);
				// SPL is defined in db: 0 db = max => 128-SPL gives a MIDI
				// velocity
				notes[midiKey].setVelocity(128 + (int) SignalPowerExtractor.soundPressureLevel(audioBuffer));
				sendNoteMessages();
			}
			return true;
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
		//shuffle order of note on messages
		Collections.shuffle(noteList);
		for(Note note : noteList) {
			note.sound(receiver);
		}
	}

	public static void main(final String... args) throws IOException {
		new PitchToMidi().run("--pitch");
	}

}
