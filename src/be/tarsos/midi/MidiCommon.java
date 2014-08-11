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
 * MidiCommon.java
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

/*
 |<---            this code is formatted to fit into 80 columns             --->|
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

import be.tarsos.Tarsos;

/** Utility methods for MIDI examples. */
public final class MidiCommon {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(MidiCommon.class.getName());

	public static final class MoreMidiInfo {
		private final MidiDevice.Info deviceInfo;
		private final boolean allowsInput;
		private final boolean allowsOutput;

		public MoreMidiInfo(final MidiDevice.Info info, final boolean input, final boolean output) {
			deviceInfo = info;
			allowsInput = input;
			allowsOutput = output;
		}

		@Override
		public String toString() {
			String descr = "MIDI";
			if (allowsInput) {
				descr = descr + " IN";
			}
			if (allowsInput && allowsOutput) {
				descr = descr + " &";
			}
			if (allowsOutput) {
				descr = descr + " OUT";
			}
			return String.format("%s - %s", descr, deviceInfo.getName());
		}

		@Override
		public boolean equals(final Object other) {
			return deviceInfo.equals(((MoreMidiInfo) other).deviceInfo);
		}

		@Override
		public int hashCode() {
			return deviceInfo.hashCode();
		}

		public MidiDevice.Info getInfo() {
			return deviceInfo;
		}
	}

	private MidiCommon() {
	}

	public static Vector<MoreMidiInfo> listDevices(final boolean input, final boolean output) {
		final Vector<MoreMidiInfo> deviceInfos = new Vector<MoreMidiInfo>();

		final MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
		for (Info aInfo : aInfos) {
			try {
				final MidiDevice device = MidiSystem.getMidiDevice(aInfo);
				final boolean deviceAllowsInput = device.getMaxTransmitters() != 0;
				final boolean deviceAllowsOutput = device.getMaxReceivers() != 0;
				final MoreMidiInfo moreInfo = new MoreMidiInfo(aInfo, deviceAllowsInput, deviceAllowsOutput);
				if (deviceAllowsInput && input || deviceAllowsOutput && output) {
					deviceInfos.add(moreInfo);
				}
			} catch (final MidiUnavailableException e) {
				LOG.warning(String.format("Ignored unavailable MIDI device %s in list.", aInfo));
			}
		}
		return deviceInfos;
	}

	/**
	 * Retrieve a MidiDevice.Info for a given name. This method tries to return
	 * a MidiDevice.Info whose name matches the passed name. If no matching
	 * MidiDevice.Info is found, null is returned. If bForOutput is true, then
	 * only output devices are searched, otherwise only input devices.
	 * 
	 * @param strDeviceName
	 *            the name of the device for which an info object should be
	 *            retrieved.
	 * @param bForOutput
	 *            If true, only output devices are considered. If false, only
	 *            input devices are considered.
	 * @return A MidiDevice.Info object matching the passed device name or null
	 *         if none could be found.
	 */
	public static MidiDevice.Info getMidiDeviceInfo(final String strDeviceName, final boolean bForOutput) {
		final MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
		for (Info aInfo : aInfos) {
			if (aInfo.getName().equals(strDeviceName)) {
				try {
					final MidiDevice device = MidiSystem.getMidiDevice(aInfo);
					final boolean bAllowsInput = device.getMaxTransmitters() != 0;
					final boolean bAllowsOutput = device.getMaxReceivers() != 0;
					if (bAllowsOutput && bForOutput || bAllowsInput && !bForOutput) {
						return aInfo;
					}
				} catch (final MidiUnavailableException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * Retrieve a MidiDevice.Info by index number. This method returns a
	 * MidiDevice.Info whose index is specified as parameter. This index matches
	 * the number printed in the listDevicesAndExit method. If index is too
	 * small or too big, null is returned.
	 * 
	 * @param index
	 *            the index of the device to be retrieved
	 * @return A MidiDevice.Info object of the specified index or null if none
	 *         could be found.
	 */
	public static MidiDevice.Info getMidiDeviceInfo(final int index) {
		final MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
		if (index < 0 || index >= aInfos.length) {
			return null;
		}
		return aInfos[index];
	}

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
	public static MidiDevice chooseMidiDevice(final boolean inputDevice, final boolean outputDevice) {
		MidiDevice device = null;
		try {
			// choose MIDI input device
			final List<MidiDevice.Info> aInfos = Arrays.asList(MidiSystem.getMidiDeviceInfo());
			Vector<MoreMidiInfo> devices = listDevices(inputDevice, outputDevice);
			for(MoreMidiInfo info : devices){
				
				Tarsos.println(aInfos.indexOf(info.getInfo()) + " " + info.toString());
			}
			String deviceType = "";
			if (inputDevice) {
				deviceType += " IN ";
			}
			if (outputDevice) {
				deviceType += " OUT ";
			}
			Tarsos.println("Choose the MIDI" + deviceType + "device: ");
			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			final int deviceIndex = Integer.parseInt(bufferedReader.readLine());
			Tarsos.println("");
			final Info midiDeviceInfo = MidiSystem.getMidiDeviceInfo()[deviceIndex];

			device = MidiSystem.getMidiDevice(midiDeviceInfo);
			if (device.getMaxTransmitters() == 0 == inputDevice
					&& device.getMaxReceivers() == 0 == outputDevice) {
				Tarsos.println("Invalid choise, please try again");
				device = chooseMidiDevice(inputDevice, outputDevice);
			}
		} catch (final NumberFormatException e) {
			Tarsos.println("Invalid number, please try again");
			device = chooseMidiDevice(inputDevice, outputDevice);
		} catch (final IOException e) {
			Logger.getLogger(Tarsos.class.getName()).log(Level.SEVERE,
					"Exception while reading from STD IN.", e);
		} catch (final MidiUnavailableException e) {
			Tarsos.println("The device is not available ( " + e.getMessage() + " ), "
					+ "please choose another device.");
			device = chooseMidiDevice(inputDevice, outputDevice);
		} catch (final ArrayIndexOutOfBoundsException e) {
			Tarsos.println("Number out of bounds, please try again");
			device = chooseMidiDevice(inputDevice, outputDevice);
		}
		return device;
	}


	/**
	 * Choose a Mixer device using CLI.
	 * @return A chosen mixer object.
	 */
	public static Mixer chooseMixerDevice() {
		Mixer mixer = null;
		try {
			final Mixer.Info[] mixers = AudioSystem.getMixerInfo();
			for (int i = 0; i < mixers.length; i++) {
				final javax.sound.sampled.Mixer.Info mixerinfo = mixers[i];
				if (AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0) {
					Tarsos.println(i + " " + mixerinfo.toString());
				}
			}
			Tarsos.println("Choose the Mixer device: ");
			final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			final int deviceIndex = Integer.parseInt(reader.readLine());
			mixer = AudioSystem.getMixer(mixers[deviceIndex]);
		} catch (final NumberFormatException e) {
			Tarsos.println("Invalid number, please try again");
			mixer = chooseMixerDevice();
		} catch (final ArrayIndexOutOfBoundsException e) {
			Tarsos.println("Number out of bounds, please try again");
			mixer = chooseMixerDevice();
		} catch (final IOException e) {
			Logger.getLogger(Tarsos.class.getName()).log(Level.SEVERE,
					"Exception while reading from STD IN.", e);
		}
		return mixer;
	}

	/**
	 * Create a MIDI tuning from a tone scale (defined by peaks).
	 * @param peaks The peaks (from e.g. a Scala file or histogram).
	 * @return  An array with 128 double values that can be used in a MIDI tuning dump.
	 */
	public static double[] tuningFromPeaks(final double[] peaks) {
		// c4 = midi key 60, the most explosive key in the known universe.
		final int startMidiKey = 60;
		final int startOctave = 4;
		
		//check if the array is sorted
		double[] newPeaks = peaks.clone();
		Arrays.sort(newPeaks);
		for(int i = 0 ; i < peaks.length ; i++){
			if(peaks[i] !=newPeaks[i]){
				throw new IllegalArgumentException();
			}
		}
		
		final double[] tuning = new double[128];
		// from startoctave up
		for (int i = startMidiKey; i < tuning.length; i++) {
			final int iteration = (i - startMidiKey);
			final int octave = startOctave + iteration / peaks.length;
			final double tuningInCents = octave * 1200 + peaks[iteration % peaks.length];
			tuning[i] = tuningInCents;
		}
		// from startoctave down
		for (int i = startMidiKey - 1; i >= 0; i--) {
			final int iteration = (startMidiKey-1-i);
			final int octave = startOctave - 1 - iteration / peaks.length;
			tuning[i] = octave * 1200 + peaks[peaks.length - 1 - ((iteration) % peaks.length) ];
		}
		return tuning;
	}
	
	
}

/*** MidiCommon.java ***/

