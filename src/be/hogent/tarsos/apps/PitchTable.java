package be.hogent.tarsos.apps;

import be.hogent.tarsos.pitch.Pitch;
import be.hogent.tarsos.pitch.PitchUnit;

public class PitchTable {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.printf("%4s %10s %17s %14s %15s %10s\n","MIDI","NAME","FREQUENCY","ABS CENTS","REL CENTS","OCTAVE");
		System.out.println("---------------------------------------------------------------------------");
		for(int i = 0 ; i < 128 ; i++){
			Pitch p = Pitch.getInstance(PitchUnit.MIDI_KEY, i);

			double frequency = p.getPitch(PitchUnit.HERTZ);
			double absoluteCents = p.getPitch(PitchUnit.ABSOLUTE_CENTS);
			double relativeCents = p.getPitch(PitchUnit.RELATIVE_CENTS);
			int octaveIndex = p.octaveIndex();

			System.out.printf("%4d %10s %14.5f Hz %14.0f  %14.0f %10d\n",i,p.noteName(), frequency,absoluteCents,relativeCents,octaveIndex);
		}
	}
}