package be.hogent.tarsos.midi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JFrame;

import be.hogent.tarsos.peak.Peak;
import be.hogent.tarsos.peak.PeakDetector;
import be.hogent.tarsos.pitch.AubioPitchDetection;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.AubioPitchDetection.AubioPitchDetectionMode;
import be.hogent.tarsos.pitch.Sample.PitchUnit;
import be.hogent.tarsos.ui.PianoTestFrame;
import be.hogent.tarsos.ui.UniversalVirtualKeyboard;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.Histogram;


public class PlayAlong {
	
	public static MidiDevice getMidiDeviceInfo(String strDeviceName, boolean bForOutput)
	{
		MidiDevice.Info[]	aInfos = MidiSystem.getMidiDeviceInfo();
		for (int i = 0; i < aInfos.length; i++)
		{
			if (aInfos[i].getName().equals(strDeviceName))
			{
				try
				{
					MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
					boolean	bAllowsInput = (device.getMaxTransmitters() != 0);
					boolean	bAllowsOutput = (device.getMaxReceivers() != 0);
					if ((bAllowsOutput && bForOutput) || (bAllowsInput && !bForOutput))
					{
						return device;
					}
				}
				catch (MidiUnavailableException e)
				{
				}
			}
		}
		return null;
	}

	
	public static void main(String args[]) throws MidiUnavailableException, InterruptedException{
		
		
		List<String> files = FileUtils.glob("audio/midi",".*.wav");
		String fileName = files.get(0);
		PitchDetector detector = new AubioPitchDetection(new AudioFile(fileName), AubioPitchDetectionMode.YIN);
		detector.executePitchDetection();
		final List<Sample> samples = detector.getSamples();
		String baseName = FileUtils.basename(fileName);
		
		FileUtils.mkdirs("data/octave/" + baseName);
		FileUtils.mkdirs("data/range/" + baseName);
		
		Histogram octaveHistogram = Sample.printOctaveInformation(baseName + '/' + baseName + "_" + detector.getName() +  "_octave.txt", samples);
		List<Peak> peaks = PeakDetector.detect(octaveHistogram, 15, 0.5);
		System.out.println(peaks.size());
		System.out.println("");
		for(Peak p:peaks){
			System.out.println(p.getPosition());
		}
		System.out.println("");
		
		final double tuning[] = new double [128];
		double margin = 0.1;

		final double[] tuningMax = new double [128];
		final double[] tuningMin = new double [128];
		
		for(int i = 0 ; i < tuning.length ;i++){
			int octave = i / 5 ;
			double centOffset = peaks.get(i % peaks.size()).getPosition();
			tuning[i] = octave * 1200 + centOffset; 
			//System.out.println(tuning[i]);
			tuningMin[i] = tuning[i] - tuning[i] * margin; 
			tuningMax[i] = tuning[i] + tuning[i] * margin;
		}
		
		final UniversalVirtualKeyboard keyboard = new UniversalVirtualKeyboard(peaks.size());
		JFrame f = new PianoTestFrame(keyboard, tuning);
		f.setVisible(true);
		
		final List<Double> midiKeysUnfiltered = new ArrayList<Double>();
		final List<Long> time = new ArrayList<Long>();
		
		Iterator<Sample> sampleIterator = samples.iterator();
		Sample currentSample = sampleIterator.next();
		int currentMidiKey = 0;
		while(sampleIterator.hasNext()){
			List<Double> currentPitches = currentSample.getPitchesIn(PitchUnit.ABSOLUTE_CENTS);
			if(currentPitches.size()==1){
				double pitch = currentPitches.get(0);
				for(int midiKey = 0 ; midiKey < 128 ; midiKey ++){
					if(pitch > tuningMin[midiKey] && pitch < tuningMax[midiKey]){
						currentMidiKey = midiKey;
					}
				}
				midiKeysUnfiltered.add(currentMidiKey + 0.0);
				time.add(currentSample.getStart());
			}
			currentSample = sampleIterator.next();
		}
		/*
		final List<Double> midiKeys = PitchFunctions.medianFilter(midiKeysUnfiltered,7);
		Thread t = new Thread(){

			@Override
			public void run() {
				StopWatch watch = new StopWatch();	
				int previousMidiKey = 0;
				for(int i = 0; i < midiKeys.size() ; i++){
					int currentMidiKey = midiKeys.get(i).intValue();
					if(currentMidiKey != previousMidiKey){
						//keyboard.releaseKey(previousMidiKey);
						//keyboard.pressKey(currentMidiKey);
					}
					long currentTick = time.get(i);
					int numberOfTicksToSleep = (int) (currentTick - watch.ticksPassed());
					if(numberOfTicksToSleep>0)
						try {
							Thread.sleep(numberOfTicksToSleep);
						} catch (InterruptedException e) {
					}
					previousMidiKey = currentMidiKey;
				}
			}
		};*/
		
	
			//MediaPlayer m = new MediaPlayer(fileName);
			//m.start();
			//t.start();

		
		

		
	}
}
