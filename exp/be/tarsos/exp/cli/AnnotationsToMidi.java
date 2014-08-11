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

package be.tarsos.exp.cli;

import java.io.IOException;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;

import be.tarsos.midi.MidiSequenceBuilder;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.AudioFile;

public class AnnotationsToMidi {
	private final List<Annotation> annotations;
	
	public AnnotationsToMidi(final List<Annotation> annotations,double scale[]){
		this.annotations = annotations;
			
		//delete all annotations that are too far from a pitch class
		int maxDeviationInCents = 35;//cents
		for(int i = 0 ; i < annotations.size() ; i++){
			double annotationPitchClass = this.annotations.get(i).getPitch(PitchUnit.RELATIVE_CENTS);
			boolean delete = true;
			for(double scalePitchClass : scale){
				if(Math.abs(scalePitchClass-annotationPitchClass)<maxDeviationInCents){
					delete = false;
				}
			}
			if(delete){
				//this.annotations.remove(i);
				//i--;
			}
		}
		
		//keep only steady 'notes'
		//a 'note' is steady if it lasts for 150 milliseconds within 20 cents.
		//AnnotationPublisher.getInstance().steadyStateAnnotationFilter(annotations, maxDeviationInCents, 0.1);

		
		try {
			MidiSequenceBuilder builder = new MidiSequenceBuilder();
			for(int i = 0; i < annotations.size(); i++){
				Annotation current = this.annotations.get(i);
				double start = current.getStart();
				double stop = current.getStart();
				int j = i + 1;
				for(; j < annotations.size() ; j++){
					Annotation next = this.annotations.get(j);
					stop = next.getStart();
					if(Math.abs(next.getPitch(PitchUnit.ABSOLUTE_CENTS)-current.getPitch(PitchUnit.ABSOLUTE_CENTS)) > maxDeviationInCents){
						break;
					}
				}
				i = j +1;
				int silentTicks = (int) ((start - builder.getCurrentTime())*100000);
				builder.addSilence(silentTicks);
				
				System.out.println(current.getStart() + "\t" + builder.getCurrentTime());
				int ticks = (int) (( stop - start ) * 100000);
				System.out.println("note of " + ((stop - start) * 1000) + "ms ");
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
	
	public static void main(String... args) throws EncoderException{
		AudioFile file = new AudioFile("/media/share/olmo/selection for paper/actual_selection/MR.1954.1.18-4.WMA");
		double scale[] = {165,423,675,885,1113};
		PitchDetector detector =  PitchDetectionMode.TARSOS_YIN.getPitchDetector(file);
		detector.executePitchDetection();
		List<Annotation> annotations  = detector.getAnnotations();
		new AnnotationsToMidi(annotations,scale);
	}
}
