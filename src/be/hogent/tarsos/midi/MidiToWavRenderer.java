package be.hogent.tarsos.midi;

/*
 * Uses a modified version of a file of the
 * <a href="http://www.jfugue.org/download.html">JFugue</a>
 * API for Music Programming.
 * JFugue - API for Music Programming
 * Copyright (C) 2003-2008  Karl Helgason and David Koelle
 * http://www.jfugue.org
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import be.hogent.tarsos.apps.PlayAlong;

import com.sun.media.sound.AudioSynthesizer;

/**
 * <p>
 * Uses a modified version of a file of the <a
 * href="http://www.jfugue.org/download.html">JFugue</a> API for Music
 * Programming. More precisely the <a
 * href="http://www.jfugue.org/code/Midi2WavRenderer.java"
 * >Midi2WavRenderer.java</a> file.
 * </p>
 * @author Karl Helgason
 * @author David Koelle
 * @author Joren Six
 */
public final class MidiToWavRenderer {
    private final transient AudioSynthesizer synth;
    /**
     * Log messages.
     */
    private static final Logger LOG = Logger.getLogger(MidiToWavRenderer.class.getName());

    public MidiToWavRenderer() throws MidiUnavailableException, InvalidMidiDataException, IOException {
        synth = (AudioSynthesizer) MidiSystem.getSynthesizer();
    }

    private Soundbank loadSoundbank(final File soundbankFile) throws MidiUnavailableException,
    InvalidMidiDataException, IOException {
        return MidiSystem.getSoundbank(soundbankFile);
    }

    private double[] rebasedTuning;

    /**
     * Changes the tuning of the synth.
     * 
     * @param tuning
     * @throws InvalidMidiDataException
     * @throws IOException
     * @throws MidiUnavailableException
     */
    public void setTuning(final double[] tuning) throws IOException, InvalidMidiDataException,
    MidiUnavailableException {
        rebasedTuning = PlayAlong.tuningFromPeaks(tuning);
    }

    /**
     * Creates a WAV file based on the Sequence, using the sounds from the
     * specified soundbank; to prevent memory problems, this method asks for an
     * array of patches (instruments) to load.
     * @param soundbankFile
     * @param midiFile
     * @param outputFile
     * @throws MidiUnavailableException
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public final void createWavFile(final File soundbankFile, final File midiFile, final File outputFile)
    throws MidiUnavailableException, InvalidMidiDataException, IOException {
        // Load soundbank
        final Soundbank soundbank = loadSoundbank(soundbankFile);

        // Open the Synthesizer and load the requested instruments
        this.synth.open();
        this.synth.unloadAllInstruments(soundbank);
        final Instrument[] instruments = soundbank.getInstruments();
        for (final Instrument instrument : instruments) {
            synth.loadInstrument(instrument);
        }
        createWavFile(midiFile, outputFile);
    }

    /**
     * Creates a WAV file based on the Pattern, using the default soundbank.
     * @param pattern
     * @param outputFile
     * @throws MidiUnavailableException
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public void createWavFile(final File midiFile, final File outputFile) throws MidiUnavailableException,
    InvalidMidiDataException, IOException {
        // Create a Player with this Synthesizer, and get a Sequence
        final Sequence sequence = MidiSystem.getSequence(midiFile);
        final Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.getTransmitter().setReceiver(synth.getReceiver());
        createWavFile(sequence, outputFile);
    }

    /**
     * Creates a WAV file based on the Sequence, using the default soundbank.
     * @param sequence
     * @param outputFile
     * @throws MidiUnavailableException
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public void createWavFile(final Sequence sequence,final File outputFile) throws MidiUnavailableException,
    InvalidMidiDataException, IOException {
        /*
         * Open synthesizer in pull mode in the format 96000hz 24 bit stereo
         * using Sinc interpolation for highest quality. With 1024 in max
         * polyphony.
         */
        final AudioFormat format = new AudioFormat(96000, 24, 2, true, false);
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put("interpolation", "sinc");
        p.put("max polyphony", "1024");
        AudioInputStream stream = synth.openStream(format, p);

        // Play Sequence into AudioSynthesizer Receiver.
        final double total = send(sequence, synth.getReceiver());

        // Calculate how long the WAVE file needs to be.
        final long len = (long) (stream.getFormat().getFrameRate() * (total + 40));
        stream = new AudioInputStream(stream, stream.getFormat(), len);

        // Write WAVE file to disk.
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, outputFile);

        this.synth.close();
    }

    /**
     * Send entry MIDI Sequence into Receiver using time stamps.
     * @return The total length of the sequence.
     */
    private double send(final Sequence seq, final Receiver recv) {
        final float divtype = seq.getDivisionType();
        assert seq.getDivisionType() == Sequence.PPQ;
        final Track[] tracks = seq.getTracks();

        try {
            if (rebasedTuning != null) {
                for (int i = 0; i < 16; i++) {
                    MidiUtils.sendTunings(recv, i, 0, "african", rebasedTuning);
                    MidiUtils.sendTuningChange(recv, i, 0);
                }
            }
        } catch (final IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        } catch (final InvalidMidiDataException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }

        final int[] trackspos = new int[tracks.length];
        int mpq = 500000;
        final int seqres = seq.getResolution();
        long lasttick = 0;
        long curtime = 0;
        while (true) {
            MidiEvent selevent = null;
            int seltrack = -1;
            for (int i = 0; i < tracks.length; i++) {
                final int trackpos = trackspos[i];
                final Track track = tracks[i];
                if (trackpos < track.size()) {
                    final MidiEvent event = track.get(trackpos);
                    if (selevent == null || event.getTick() < selevent.getTick()) {
                        selevent = event;
                        seltrack = i;
                    }
                }
            }
            if (seltrack == -1) {
                break;
            }
            trackspos[seltrack]++;
            final long tick = selevent.getTick();
            if (divtype == Sequence.PPQ) {
                curtime += ((tick - lasttick) * mpq) / seqres;
            } else {
                curtime = (long) ((tick * 1000000.0 * divtype) / seqres);
            }
            lasttick = tick;
            final MidiMessage msg = selevent.getMessage();
            if (msg instanceof MetaMessage) {
                if (divtype == Sequence.PPQ) {
                    if (((MetaMessage) msg).getType() == 0x51) {
                        final byte[] data = ((MetaMessage) msg).getData();
                        mpq = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
                    }
                }
            } else {
                if (recv != null) {
                    recv.send(msg, curtime);
                }
            }
        }
        return curtime / 1000000.0;
    }
}
