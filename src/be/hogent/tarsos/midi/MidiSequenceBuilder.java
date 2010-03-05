package be.hogent.tarsos.midi;
import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;


public class MidiSequenceBuilder {
	private Sequence sequence;
	private Track track;
	private int  currentTicks;
	
	private final int VELOCITY = 64;

	public MidiSequenceBuilder(){		
		try
		{
			sequence = new Sequence(Sequence.PPQ, 1);
			track = sequence.createTrack();
			currentTicks = 0;
		}
		catch (InvalidMidiDataException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void addNote(int midiKey,int numberOfTicks){
		track.add(createNoteEvent(ShortMessage.NOTE_ON,midiKey,VELOCITY,currentTicks));
		currentTicks = numberOfTicks + currentTicks;
		track.add(createNoteEvent(ShortMessage.NOTE_OFF,midiKey,0,currentTicks));
	}
	
	public void addNoteByFrequency(double frequency,int numberOfTicks){
		int closestMidiNumber = (int) Math.round((69 + 12 * Math.log(frequency/440.0)/Math.log(2.0)));
		double frequencyClosestMidiNumber = (Math.pow(2.0,( closestMidiNumber - 69.0)/12.0)*440.0);		
		double deviationInCents = 1200 * Math.log(frequency/frequencyClosestMidiNumber)/Math.log(2.0);		
		assert deviationInCents <= 50;		
		//System.out.println("Requested: " + frequency + "Hz; Midi key " + closestMidiNumber + " frequency: " + frequencyClosestMidiNumber + "Hz; Deviation " + deviationInCents + " cents");
		this.addNoteByDeviationInCents(closestMidiNumber, numberOfTicks, deviationInCents);
	}
	
	public void addNoteByAbsoluteCents(double absoluteCents,int numberOfTicks){
		//reference frequency of 32.7032... Hz
		//27.5 Hz is A0 (440, 220, 110, 55, 27.5) 
		double reference_frequency = 27.5 * Math.pow(2.0,0.25);
		double frequency = reference_frequency * Math.pow(2.0,absoluteCents/1200);		
		this.addNoteByFrequency(frequency, numberOfTicks);
	}
	
	public void addNoteByDeviationInCents(int midiKey,int numberOfTicks,double deviationInCents){
		midiKey = midiKey + (int) (deviationInCents / 100);
		deviationInCents = deviationInCents % 100;
		track.add(createPitchBendEvent(deviationInCents,currentTicks));		
		addNote(midiKey,numberOfTicks);
		track.add(createPitchBendEvent(0.0,currentTicks));
	}
	
	private MidiEvent createPitchBendEvent(double deviationInCents,int startTick){
		int bendFactorInMidi=0;
		//16384 values for 400 cents
		bendFactorInMidi = new Float((deviationInCents * (16384.0 / 400.0))).intValue();
		if(bendFactorInMidi < -8191)
			bendFactorInMidi = -8191;
		return createPitchBendEvent(bendFactorInMidi,(long) startTick);		
	}
	
	public void export(String fileName){
		try {
			MidiSystem.write(sequence, 0, new File(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void play() throws MidiUnavailableException, InvalidMidiDataException{
		final Sequencer sequencer;
		final Synthesizer synthesizer;
		/* Now, we need a Sequencer to play the sequence.  Here, we
		 * simply request the default sequencer without an implicitly
		 * connected synthesizer
		 */
		sequencer = MidiSystem.getSequencer(false);

		/* The Sequencer is still a dead object.  We have to open() it
		 * to become live.  This is necessary to allocate some
		 * ressources in the native part.
		 */
		sequencer.open();

		/* Next step is to tell the Sequencer which Sequence it has to
		 * play. In this case, we set it as the Sequence object
		 * created above.
		 */
		sequencer.setSequence(sequence);

		/* We try to get the default synthesizer, open() it and chain
		 * it to the sequencer with a Transmitter-Receiver pair.
		 */
		synthesizer = MidiSystem.getSynthesizer();
		synthesizer.open();
		Receiver	synthReceiver = synthesizer.getReceiver();
		Transmitter	seqTransmitter = sequencer.getTransmitter();
		seqTransmitter.setReceiver(synthReceiver);

		/* To free system resources, it is recommended to close the
		 * synthesizer and sequencer properly.
		 *
		 * To accomplish this, we register a Listener to the
		 * Sequencer. It is called when there are "meta" events. Meta
		 * event 47 is end of track.
		 *
		 * Thanks to Espen Riskedal for finding this trick.
		 */
		sequencer.addMetaEventListener(new MetaEventListener()
			{
				public void meta(MetaMessage event)
				{
					if (event.getType() == 47)
					{
						sequencer.close();
						if (synthesizer != null)
						{
							synthesizer.close();
						}
						//System.exit(0);
					}
				}
			});

		/* Now, we can start over.
		 */
		sequencer.start();
	}
	
	/*
	 * 
	 * While almost all channel voice messages assign a single data byte to a single parameter such as key # 
	 * or velocity (128 values because they start with '0,' so = 2^7=128), 
	 * the exception is pitch bend. If pitch bend used only 128 values, 
	 * discreet steps might be heard if the bend range were large
	 * (this range is set on the instrument, not by MIDI). 
	 * So the 7 non-zero bits of the first data byte (called the most significant byte or MSB)
	 * are combined with the 7 non-zero bits from the second data byte (called the least significant byte or LSB) 
	 * to create a 14-bit data value, giving pitch bend data a range of 16,384 values.
	 *   
	 * Pitch Bend Range: 
	 * <pre>   
	 * RPN LSB = 0: Bn 64 00 
	 * RPN MSB = 0: Bn 65 00 
	 * Data MSB: Bn 06 mm (mm sets bend range in semitones. mm can be from 00 to 18 for 0 to 24 (+/- 12) semitones both up and down) 
	 * Data LSB=0 (usually not required): Bn 26 00
	 * 
	 * So to set Pitch Bend Range to +/- 12 semitones:
	 * 
	 * HEX        ;  DECIMAL
	 * 
	 * Bn 65 00 ; 101  00 MSB
	 * 
	 * Bn 64 00 ; 100 00 LSB
	 * 
	 * Bn 06 18 ; 06 24 MSB
	 * 
	 * Bn 26 00 ; 38 00 LSB 
	 * </pre>
	 * */	
	private MidiEvent createPitchBendEvent(int bendFactor,long startTick){
		
		ShortMessage	message = new ShortMessage();		

		//-8191 <= bendfactor <= +8192
		bendFactor += 8191;
		if( 0 >  bendFactor || bendFactor >= 16384)
			throw new IllegalArgumentException("bendFactor invalid:  -8191 <= bendFactor <= +8192");
			
		String binary = toBinaryString(bendFactor);
		int msb = Integer.parseInt(binary.substring(0, 7),2);
		int lsb = Integer.parseInt(binary.substring(7,14),2);
		
		try{
			message.setMessage(ShortMessage.PITCH_BEND,0,lsb,msb);
		}catch (InvalidMidiDataException e){			
			e.printStackTrace();
			throw new Error(e);
		}
		return new MidiEvent(message,startTick);
	}
	
	private static String toBinaryString(int i){
		String binary = Integer.toBinaryString(i);
		while (binary.length() < 14)
			binary = "0" + binary;
		return binary;
	}
	
	private MidiEvent createNoteEvent(int nCommand, int nKey,int nVelocity,long lTick)
	{
		ShortMessage	message = new ShortMessage();
		try
		{
			message.setMessage(nCommand,
					0,	// always on channel 1
					nKey,
					nVelocity);
		}
		catch (InvalidMidiDataException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		MidiEvent	event = new MidiEvent(message,lTick);
		return event;
	}
	
	public static void main(String[] args)
	{	
		MidiSequenceBuilder builder = new MidiSequenceBuilder();
		
		builder.addNote(69,3);
		builder.addNoteByFrequency(440,3);
		builder.addNoteByDeviationInCents(68, 3,  100);		
		builder.addNoteByDeviationInCents(72, 3, -300);
		builder.addNoteByDeviationInCents(73, 3, -400);
		builder.addNoteByDeviationInCents(62, 3,  700);
		
		try {
			builder.play();
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
}
