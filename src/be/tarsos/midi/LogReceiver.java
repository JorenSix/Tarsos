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

/*
 * DumpReceiver.java
 * This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer
 * Copyright (c) 2003 by Florian Bomers
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.logging.Logger;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

/**
 * Displays the file format information of a MIDI file.
 */
public final class LogReceiver implements Receiver {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(LogReceiver.class.getName());

	private static long seByteCount = 0;
	private static long smByteCount = 0;
	private static long seCount = 0;
	private static long smCount = 0;

	private static final String[] KEYNAMES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#",
			"B", };

	private static final String[] KEYSIGNATURES = { "Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", "C", "G", "D",
			"A", "E", "B", "F#", "C#", };
	private static final String[] SYSTEM_MESSAGE_TEXT = {
			"System Exclusive (should not be in ShortMessage!)", "MTC Quarter TarsosFrame: ", "Song Position: ",
			"Song Select: ", "Undefined", "Undefined", "Tune Request",
			"End of SysEx (should not be in ShortMessage!)", "Timing clock", "Undefined", "Start",
			"Continue", "Stop", "Undefined", "Active Sensing", "System Reset", };

	private static final String[] QUARTER_FRAME_MESSAGE_TEXT = { "frame count LS: ", "frame count MS: ",
			"seconds count LS: ", "seconds count MS: ", "minutes count LS: ", "minutes count MS: ",
			"hours count LS: ", "hours count MS: ", };

	private static final String[] FRAME_TYPE_TEXT = { "24 frames/second", "25 frames/second",
			"30 frames/second (drop)", "30 frames/second (non-drop)", };

	private final boolean m_bPrintTimeStampAsTicks;

	public LogReceiver() {
		this(false);
	}

	public LogReceiver(final boolean bPrintTimeStampAsTicks) {
		m_bPrintTimeStampAsTicks = bPrintTimeStampAsTicks;
	}

	
	public void close() {
	}

	
	public void send(final MidiMessage message, final long lTimeStamp) {
		String strMessage = null;
		if (message instanceof ShortMessage) {
			strMessage = decodeMessage((ShortMessage) message);
		} else if (message instanceof SysexMessage) {
			strMessage = decodeMessage((SysexMessage) message);
		} else if (message instanceof MetaMessage) {
			strMessage = decodeMessage((MetaMessage) message);
		} else {
			strMessage = "unknown message type";
		}
		String strTimeStamp = null;
		if (m_bPrintTimeStampAsTicks) {
			strTimeStamp = "tick " + lTimeStamp + ": ";
		} else {
			if (lTimeStamp == -1L) {
				strTimeStamp = "timestamp [unknown]: ";
			} else {
				strTimeStamp = "timestamp " + lTimeStamp + " us: ";
			}
		}
		LOG.fine("MIDI-DATA: " + strTimeStamp + strMessage);
	}

	public String decodeMessage(final ShortMessage message) {
		String strMessage = null;
		switch (message.getCommand()) {
		case 0x80:
			strMessage = "note Off " + message.getData1() + " velocity: " + message.getData2();
			break;

		case 0x90:
			strMessage = "note On  " + message.getData1() + " velocity: " + message.getData2();
			break;

		case 0xa0:
			strMessage = "polyphonic key pressure " + getKeyName(message.getData1()) + " pressure: "
					+ message.getData2();
			break;

		case 0xb0:
			strMessage = "control change " + message.getData1() + " value: " + message.getData2();
			break;

		case 0xc0:
			strMessage = "program change " + message.getData1();
			break;

		case 0xd0:
			strMessage = "key pressure " + getKeyName(message.getData1()) + " pressure: "
					+ message.getData2();
			break;

		case 0xe0:
			strMessage = "pitch wheel change " + get14bitValue(message.getData1(), message.getData2());
			break;

		case 0xF0:
			strMessage = SYSTEM_MESSAGE_TEXT[message.getChannel()];
			switch (message.getChannel()) {
			case 0x1:
				final int nQType = (message.getData1() & 0x70) >> 4;
				int nQData = message.getData1() & 0x0F;
				if (nQType == 7) {
					nQData = nQData & 0x1;
				}
				strMessage += QUARTER_FRAME_MESSAGE_TEXT[nQType] + nQData;
				if (nQType == 7) {
					final int nFrameType = (message.getData1() & 0x06) >> 1;
					strMessage += ", frame type: " + FRAME_TYPE_TEXT[nFrameType];
				}
				break;

			case 0x2:
				strMessage += get14bitValue(message.getData1(), message.getData2());
				break;

			case 0x3:
				strMessage += message.getData1();
				break;
			default:
				break;
			}
			break;

		default:
			strMessage = "unknown message: status = " + message.getStatus() + ", byte1 = "
					+ message.getData1() + ", byte2 = " + message.getData2();
			break;
		}
		if (message.getCommand() != 0xF0) {
			final int nChannel = message.getChannel() + 1;
			final String strChannel = "channel " + nChannel + ": ";
			strMessage = strChannel + strMessage;
		}
		smCount++;
		smByteCount += message.getLength();
		return "[" + getHexString(message) + "] " + strMessage;
	}

	public String decodeMessage(final SysexMessage message) {
		final byte[] abData = message.getData();
		String strMessage = null;
		// System.out.println("sysex status: " + message.getStatus());
		if (message.getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) {
			strMessage = "Sysex message: F0" + getHexString(abData);
		} else if (message.getStatus() == SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE) {
			strMessage = "Continued Sysex message F7" + getHexString(abData);
			seByteCount--; // do not count the F7
		}
		seByteCount += abData.length + 1;
		seCount++; // for the status byte
		return strMessage;
	}

	public String decodeMessage(final MetaMessage message) {

		final byte[] abData = message.getData();
		String strMessage = null;
		// System.out.println("data array length: " + abData.length);
		switch (message.getType()) {
		case 0:
			final int nSequenceNumber = (abData[0] & 0xFF) << 8 | abData[1] & 0xFF;
			strMessage = "Sequence Number: " + nSequenceNumber;
			break;

		case 1:
			final String strText = new String(abData);
			strMessage = "Text Event: " + strText;
			break;

		case 2:
			final String strCopyrightText = new String(abData);
			strMessage = "Copyright Notice: " + strCopyrightText;
			break;

		case 3:
			final String strTrackName = new String(abData);
			strMessage = "Sequence/Track Name: " + strTrackName;
			break;

		case 4:
			final String strInstrumentName = new String(abData);
			strMessage = "Instrument Name: " + strInstrumentName;
			break;

		case 5:
			final String strLyrics = new String(abData);
			strMessage = "Lyric: " + strLyrics;
			break;

		case 6:
			final String strMarkerText = new String(abData);
			strMessage = "Marker: " + strMarkerText;
			break;

		case 7:
			final String strCuePointText = new String(abData);
			strMessage = "Cue Point: " + strCuePointText;
			break;

		case 0x20:
			final int nChannelPrefix = abData[0] & 0xFF;
			strMessage = "MIDI Channel Prefix: " + nChannelPrefix;
			break;

		case 0x2F:
			strMessage = "End of Track";
			break;

		case 0x51:
			final int nTempo = (abData[0] & 0xFF) << 16 | (abData[1] & 0xFF) << 8 | abData[2] & 0xFF; // tempo
			// in
			// microseconds
			// per
			// beat
			float bpm = convertTempo(nTempo);
			// truncate it to 2 digits after dot
			bpm = Math.round(bpm * 100.0f) / 100.0f;
			strMessage = "Set Tempo: " + bpm + " bpm";
			break;

		case 0x54:
			// System.out.println("data array length: " + abData.length);
			strMessage = "SMTPE Offset: " + (abData[0] & 0xFF) + ":" + (abData[1] & 0xFF) + ":"
					+ (abData[2] & 0xFF) + "." + (abData[3] & 0xFF) + "." + (abData[4] & 0xFF);
			break;

		case 0x58:
			strMessage = "Time Signature: " + (abData[0] & 0xFF) + "/" + (1 << (abData[1] & 0xFF))
					+ ", MIDI clocks per metronome tick: " + (abData[2] & 0xFF)
					+ ", 1/32 per 24 MIDI clocks: " + (abData[3] & 0xFF);
			break;

		case 0x59:
			final String strGender = abData[1] == 1 ? "minor" : "major";
			strMessage = "Key Signature: " + KEYSIGNATURES[abData[0] + 7] + " " + strGender;
			break;

		case 0x7F:
			final String strDataDump = getHexString(abData);
			strMessage = "Sequencer-Specific Meta event: " + strDataDump;
			break;

		default:
			final String strUnknownDump = getHexString(abData);
			strMessage = "unknown Meta event: " + strUnknownDump;
			break;

		}
		return strMessage;
	}

	public static String getKeyName(final int nKeyNumber) {
		if (nKeyNumber > 127) {
			return "illegal value";
		} else {
			final int nNote = nKeyNumber % 12;
			final int nOctave = nKeyNumber / 12;
			return KEYNAMES[nNote] + (nOctave - 1);
		}
	}

	public static int get14bitValue(final int nLowerPart, final int nHigherPart) {
		return nLowerPart & 0x7F | (nHigherPart & 0x7F) << 7;
	}

	// convert from microseconds per quarter note to beats per minute and vice
	// versa
	private static float convertTempo(final float value) {
		float actualValue = value;
		if (value <= 0) {
			actualValue = 0.1f;
		}
		return 60000000.0f / actualValue;
	}

	private static char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
			'E', 'F', };

	public static String getHexString(final byte[] aByte) {
		final StringBuffer sbuf = new StringBuffer(aByte.length * 3 + 2);
		for (byte element : aByte) {
			sbuf.append(' ');
			sbuf.append(hexDigits[(element & 0xF0) >> 4]);
			sbuf.append(hexDigits[element & 0x0F]);
		}
		return new String(sbuf);
	}

	private static String intToHex(final int i) {
		return "" + hexDigits[(i & 0xF0) >> 4] + hexDigits[i & 0x0F];
	}

	public static String getHexString(final ShortMessage sm) {
		// bug in J2SDK 1.4.1
		// return getHexString(sm.getMessage());
		final int status = sm.getStatus();
		String res = intToHex(sm.getStatus());
		// if one-byte message, return
		switch (status) {
		case 0xF6: // Tune Request
		case 0xF7: // EOX
			// System real-time messages
		case 0xF8: // Timing Clock
		case 0xF9: // Undefined
		case 0xFA: // Start
		case 0xFB: // Continue
		case 0xFC: // Stop
		case 0xFD: // Undefined
		case 0xFE: // Active Sensing
		case 0xFF:
			return res;
		default:
			break;
		}
		res += ' ' + intToHex(sm.getData1());
		// if 2-byte message, return
		// 0xF1: MTC Quarter TarsosFrame
		// 0xF3: // Song Select
		if (status == 0xF3 || status == 0xF1) {
			return res;
		}

		if (sm.getCommand() == 0xC0 || sm.getCommand() == 0xD0) {
			return res;
		}
		// 3-byte messages left
		res += ' ' + intToHex(sm.getData2());
		return res;
	}
}
