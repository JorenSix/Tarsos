package be.hogent.tarsos.midi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.PitchUnit;

public class AnnotationsToMidi {
	private final List<Annotation> annotations;
	
	public AnnotationsToMidi(final List<Annotation> annotations){
		this.annotations = annotations;
		try {
			MidiSequenceBuilder builder = new MidiSequenceBuilder();
			double ticksPerSecond = builder.getTicksPerSecond();
			for(int i = 0; i < annotations.size() - 1 ; i++){
				Annotation current = this.annotations.get(i);
				Annotation next = this.annotations.get(i+1);
				int ticks = (int) (( next.getStart() - current.getStart() ) * ticksPerSecond);
				builder.addNoteByFrequency(current.getPitch(PitchUnit.HERTZ),ticks);
			}
			builder.export("test.midi");
		} catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String... args){
		new AnnotationsToMidi(new ArrayList<Annotation>());
	}
}
