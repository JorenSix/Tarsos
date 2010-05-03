package be.hogent.tarsos.apps;

import be.hogent.tarsos.pitch.PitchFunctions;

public class PitchTable {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.printf("%4s %10s %17s %14s %15s %10s\n","MIDI","NAME","FREQUENCY","ABS CENTS","REL CENTS","OCTAVE");
		System.out.println("---------------------------------------------------------------------------");
		for(int i = 0 ; i < 128 ; i++){
			double frequency = 440 * Math.pow(2, (i - 69) / 12d );
			double absoluteCents = PitchFunctions.convertHertzToAbsoluteCent(frequency);
			double relativeCents = PitchFunctions.convertHertzToRelativeCent(frequency);
			int octaveIndex = PitchFunctions.octaveIndex(frequency);
			//sanity check
			assert i == PitchFunctions.convertHertzToMidiKey(frequency);
			System.out.printf("%4d %10s %14.4f Hz %14.0f  %14.0f %10d\n",i,PitchFunctions.noteName(frequency), frequency,absoluteCents,relativeCents,octaveIndex);
		}
	}
}