package be.hogent.tarsos.sampled.pitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.Command;


/**
 * Calls Swipe.
 * @author Joren Six
 */
public class SwipeOctave implements PitchDetector {
	private final List<Annotation> annotations;
	private final AudioFile file;
	private final PitchDetectionMode mode;
	 
	public SwipeOctave(final AudioFile audioFile,PitchDetectionMode mode){
		annotations=new ArrayList<Annotation>();
		file = audioFile;
		this.mode = mode;
	}

	public List<Annotation> executePitchDetection() {
		Command cmd = new Command("swipe_octave");
		cmd.addFileArgument(file.transcodedPath());
		
		try {
			String output = cmd.execute();
			String[] rows = output.split("\n");
			for(String row : rows){
				String[] data = row.split("\\s+");
				if(!data[1].equals("NaN")){
					double timeStamp = Double.valueOf(data[0]);
					double pitchInHz = Double.valueOf(data[1]);
					double strength = Double.valueOf(data[2]);
					Annotation a = new Annotation(timeStamp, pitchInHz, mode,strength);
					annotations.add(a);
				}
			}
		} catch (IOException e) {
			
		}
		
		
		return annotations;
	}

	public double progress() {
		return -1;
	}

	public List<Annotation> getAnnotations() {
		
		return annotations;
	}

	public String getName() {
		return mode.getDetectionModeName();
	}
}