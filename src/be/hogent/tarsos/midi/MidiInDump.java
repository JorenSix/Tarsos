package be.hogent.tarsos.midi;

/*
 *	MidiInDump.java
 *
 *	This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer
 * Copyright (c) 2003 by Florian Bomers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
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

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;




/**	<titleabbrev>MidiInDump</titleabbrev>
	<title>Listens to a MIDI port and dump the received event to the console</title>

	<formalpara><title>Purpose</title>
	<para>Listens to a MIDI port and dump the received event to the console.</para></formalpara>

	<formalpara><title>Usage</title>
	<para>
	<cmdsynopsis>
	<command>java MidiInDump</command>
	<arg choice="plain"><option>-l</option></arg>
	</cmdsynopsis>
	<cmdsynopsis>
	<command>java MidiInDump</command>
	<arg choice="plain"><option>-d <replaceable>devicename</replaceable></option></arg>
	<arg choice="plain"><option>-n <replaceable>device#</replaceable></option></arg>
	</cmdsynopsis>
	</para></formalpara>

	<formalpara><title>Parameters</title>
	<variablelist>
	<varlistentry>
	<term><option>-l</option></term>
	<listitem><para>list the availabe MIDI devices</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-d <replaceable>devicename</replaceable></option></term>
	<listitem><para>reads from named device (see <option>-l</option>)</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-n <replaceable>device#</replaceable></option></term>
	<listitem><para>reads from device with given index (see <option>-l</option>)</para></listitem>
	</varlistentry>
	</variablelist>
	</formalpara>

	<formalpara><title>Bugs, limitations</title>
	<para>
	For the Sun J2SDK 1.3.x or 1.4.0, MIDI IN does not work. See the <olink targetdoc="faq_midi" targetptr="faq_midi">FAQ</olink> for alternatives.
	</para></formalpara>

	<formalpara><title>Source code</title>
	<para>
	<ulink url="MidiInDump.java.html">MidiInDump.java</ulink>,
	<ulink url="DumpReceiver.java.html">DumpReceiver.java</ulink>,
	<ulink url="MidiCommon.java.html">MidiCommon.java</ulink>,
	<ulink url="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</ulink>
	</para>
	</formalpara>

*/
public class MidiInDump
{

	public static void main(String[] args)
		throws Exception
	{

		MidiDevice.Info	info;
		info = MidiCommon.getMidiDeviceInfo("Keystation 49e", false);
		
		MidiDevice	inputDevice = null;
		inputDevice = MidiSystem.getMidiDevice(info);
		
		Transmitter	t = inputDevice.getTransmitter();
		inputDevice.open();
		
		boolean bUseDefaultSynthesizer = true;
		Receiver r = null;
		if (bUseDefaultSynthesizer){
			    MidiDevice.Info synthInfo = MidiCommon.getMidiDeviceInfo("Microsoft GS Wavetable Synth",true);
			    MidiDevice	outputDevice = null;
			    outputDevice = MidiSystem.getMidiDevice(synthInfo);
			    outputDevice.open();
			   // synth.loadInstrument(synth.getAvailableInstruments()[9]);
			    //synth.open();
			    r = outputDevice.getReceiver();
				t.setReceiver(r);
				//MidiChannel[]	channels = synth.getChannels();
				//MidiChannel	channel = channels[0];
				//channel.noteOn(69,127);				
		}else{
			r = new DumpReceiver(System.out);
			t.setReceiver(r);
		}
		out("now running; interupt the program with [ENTER] when finished");

		System.in.read();

		inputDevice.close();
		if(!bUseDefaultSynthesizer){
			out("Received "+ DumpReceiver.seCount + " sysex messages with a total of " + DumpReceiver.seCount  + " bytes");
			out("Received "+ DumpReceiver.seCount +" short messages with a total of " + DumpReceiver.seCount  + " bytes");
			out("Received a total of "+ DumpReceiver.smByteCount +  DumpReceiver.seByteCount  +" bytes");
		}
		
		Thread.sleep(1000);
	}

	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
}



/*** MidiInDump.java ***/

