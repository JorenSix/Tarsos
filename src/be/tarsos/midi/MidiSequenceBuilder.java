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

package be.tarsos.midi;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;

/**
 * Utility class to generate a sequence of MIDI events.
 * @author Joren Six
 */
public final class MidiSequenceBuilder {

    /**
     * Log messages.
     */
    private static final Logger LOG = Logger.getLogger(MidiSequenceBuilder.class.getName());

    private static final int VELOCITY = 96;
    private static final int RESOLUTION = 40;

    private final Sequence sequence;
    private final Track track;
    private int currentTicks;



    public MidiSequenceBuilder() throws InvalidMidiDataException {
        sequence = new Sequence(Sequence.SMPTE_25, RESOLUTION);
        track = sequence.createTrack();
        currentTicks = 0;
    }
    
    public int getCurrentTicks(){
    	return currentTicks;
    }
    
    public double getCurrentTime(){
    	return currentTicks / 100000.0;
    }

    public void addNote(final int midiKey, final int numberOfTicks) {
        track.add(createNoteEvent(ShortMessage.NOTE_ON, midiKey, VELOCITY, currentTicks));
        currentTicks = numberOfTicks + currentTicks;
        track.add(createNoteEvent(ShortMessage.NOTE_OFF, midiKey, 0, currentTicks));
    }
    
    public void addSilence(final int numberOfTicks) {
    	//I do not know how to add silence => velocity zero note events
        track.add(createNoteEvent(ShortMessage.NOTE_ON, 69, 0, currentTicks));
        currentTicks = numberOfTicks + currentTicks;
        track.add(createNoteEvent(ShortMessage.NOTE_OFF, 69, 0, currentTicks));
    }

    public void addNoteByFrequency(final double frequency, final int numberOfTicks) {
        final int closestMidiNumber = (int) Math.round(69 + 12 * Math.log(frequency / 440.0) / Math.log(2.0));
        final double frequencyClosestMidiNumber = Math.pow(2.0, (closestMidiNumber - 69.0) / 12.0) * 440.0;
        final double deviationInCents = 1200 * Math.log(frequency / frequencyClosestMidiNumber) / Math.log(2.0);
        assert deviationInCents <= 50;
        // System.out.println("Requested: " + frequency + "Hz; Midi key " +
        // closestMidiNumber + " frequency: " + frequencyClosestMidiNumber +
        // "Hz; Deviation " + deviationInCents + " cents");
        this.addNoteByDeviationInCents(closestMidiNumber, numberOfTicks, deviationInCents);
    }

    public void addNoteByAbsoluteCents(final double absoluteCents, final int numberOfTicks) {
        final double frequency = PitchUnit.absoluteCentToHertz(absoluteCents);
        this.addNoteByFrequency(frequency, numberOfTicks);
    }

    public void addNoteByDeviationInCents(final int midiKey, final int numberOfTicks, final double deviationInCents) {
        // E.G. midiKey = 69 ;deviation in cents is -105.0
        // actualMidiKey = 69 - 1 =68
        final int actualMidiKey = midiKey + (int) (deviationInCents / 100);
        final int channel = 0;
        // midiKeyDeviation = -5;
        final double midiKeyDeviation = deviationInCents % 100;
        track.add(createPitchBendEvent(midiKeyDeviation, channel , currentTicks));
        addNote(actualMidiKey, numberOfTicks);
        track.add(createPitchBendEvent(0.0,channel, currentTicks));
    }

    public static MidiEvent createPitchBendEvent(final double deviationInCents,final int channel, final int startTick) {
        int bendFactorInMidi = 0;
        // 16384 values for 400 cents
        bendFactorInMidi = (int) (deviationInCents * (16384.0 / 400.0));
        if (bendFactorInMidi < -8191) {
            bendFactorInMidi = -8191;
        }
        return createPitchBendEvent(bendFactorInMidi, channel, (long) startTick);
    }

    /**
     * Write a midi file
     * @param fileName
     * @throws IOException
     */
    public void export(final String fileName) throws IOException {
        MidiSystem.write(sequence, 0, new File(fileName));
    }

    public void play() throws MidiUnavailableException, InvalidMidiDataException {
        final Sequencer sequencer;
        final Synthesizer synthesizer;
        /*
         * Now, we need a Sequencer to play the sequence. Here, we simply
         * request the default sequencer without an implicitly connected
         * synthesizer
         */
        sequencer = MidiSystem.getSequencer(false);

        /*
         * The Sequencer is still a dead object. We have to open() it to become
         * live. This is necessary to allocate some ressources in the native
         * part.
         */
        sequencer.open();

        /*
         * Next step is to tell the Sequencer which Sequence it has to play. In
         * this case, we set it as the Sequence object created above.
         */
        sequencer.setSequence(sequence);

        /*
         * We try to get the default synthesizer, open() it and chain it to the
         * sequencer with a Transmitter-Receiver pair.
         */
        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        
    	Instrument[] available = synthesizer.getAvailableInstruments();
		Instrument configuredInstrument = available[Configuration.getInt(ConfKey.midi_instrument_index)];
		// synth.loadInstrument(configuredInstrument);
		MidiChannel channel = synthesizer.getChannels()[0];
		Patch patch = configuredInstrument.getPatch();
		channel.programChange(patch.getBank(), patch.getProgram());
		
        
        final Receiver synthReceiver = synthesizer.getReceiver();
        final Transmitter seqTransmitter = sequencer.getTransmitter();
        seqTransmitter.setReceiver(synthReceiver);

        /*
         * To free system resources, it is recommended to close the synthesizer
         * and sequencer properly. To accomplish this, we register a Listener to
         * the Sequencer. It is called when there are "meta" events. Meta event
         * 47 is end of track. Thanks to Espen Riskedal for finding this trick.
         */
        sequencer.addMetaEventListener(new MetaEventListener() {
            public void meta(final MetaMessage event) {
                if (event.getType() == 47) {
                    sequencer.close();
                    if (synthesizer != null) {
                        synthesizer.close();
                    }
                }
            }
        });

        /*
         * Now, we can start over.
         */
        sequencer.start();
    }

    /*
     * While almost all channel voice messages assign a single data byte to a
     * single parameter such as key # or velocity (128 values because they start
     * with '0,' so = 2^7=128), the exception is pitch bend. If pitch bend used
     * only 128 values, discreet steps might be heard if the bend range were
     * large (this range is set on the instrument, not by MIDI). So the 7
     * non-zero bits of the first data byte (called the most significant byte or
     * MSB) are combined with the 7 non-zero bits from the second data byte
     * (called the least significant byte or LSB) to create a 14-bit data value,
     * giving pitch bend data a range of 16,384 values. Pitch Bend Range: <pre>
     * RPN LSB = 0: Bn 64 00 RPN MSB = 0: Bn 65 00 Data MSB: Bn 06 mm (mm sets
     * bend range in semitones. mm can be from 00 to 18 for 0 to 24 (+/- 12)
     * semitones both up and down) Data LSB=0 (usually not required): Bn 26 00
     * So to set Pitch Bend Range to +/- 12 semitones: HEX ; DECIMAL Bn 65 00 ;
     * 101 00 MSB Bn 64 00 ; 100 00 LSB Bn 06 18 ; 06 24 MSB Bn 26 00 ; 38 00
     * LSB </pre>
     */
    public static MidiEvent createPitchBendEvent(final int bendFactor,final int channel, final long startTick) {

        final ShortMessage message = new ShortMessage();

        // -8191 <= bendfactor <= +8192
        final int actualBendFactor = bendFactor + 8191;
        if (0 > actualBendFactor || actualBendFactor >= 16384) {
            throw new IllegalArgumentException("BendFactor " + bendFactor
                    + " invalid:  -8191 <= bendFactor <= +8192");
        }

        final String binary = toBinaryString(actualBendFactor);
        final int msb = Integer.parseInt(binary.substring(0, 7), 2);
        final int lsb = Integer.parseInt(binary.substring(7, 14), 2);

        try {
            message.setMessage(ShortMessage.PITCH_BEND, channel, lsb, msb);
        } catch (final InvalidMidiDataException e) {
            LOG.log(Level.SEVERE, "Invalid midi data", e);
            // Will this exception ever occur?
        }
        return new MidiEvent(message, startTick);
    }

    private static String toBinaryString(final int integer) {
        final String binary = Integer.toBinaryString(integer);
        final StringBuffer buffer = new StringBuffer(binary);
        while (buffer.length() < 14) {
            buffer.insert(0, '0');
        }
        return buffer.toString();
    }

    private MidiEvent createNoteEvent(final int nCommand, final int nKey, final int nVelocity,
            final long lTick) {
        final ShortMessage message = new ShortMessage();
        try {
            message.setMessage(nCommand, 0, // always on channel 1
                    nKey, nVelocity);
        } catch (final InvalidMidiDataException e) {
            LOG.log(Level.SEVERE, "Invalid midi data", e);
            // Will this exception ever occur?
        }
        return new MidiEvent(message, lTick);
    }
}
