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
package be.tarsos.sampled.pitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.util.AudioFile;
import be.tarsos.util.Command;


/**
 * Calls polyphon.
 * @author Joren Six
 */
public class Polyphon implements PitchDetector {
	private final List<Annotation> annotations;
	private final AudioFile file;
	private final PitchDetectionMode mode;
	private final int polyphony;

	 
	public Polyphon(final AudioFile audioFile,PitchDetectionMode mode){
		annotations=new ArrayList<Annotation>();
		file = audioFile;
		this.mode = mode;
		polyphony=6;
	}

	public List<Annotation> executePitchDetection() {
		Command cmd = new Command("polyphon");
		//Define the degree of polyphony
		cmd.addArgument(String.valueOf(polyphony));
		cmd.addFileArgument(file.transcodedPath());
		try {
			String output = cmd.execute();
			String[] rows = output.split("\n");
			for(String row : rows){
				String[] data = row.split("\\s+");
				double timeStamp = Double.valueOf(data[0]);
				// 0        1           2            3          4            = 5   5/2 = 2
				//timestamp frequency1 frequency2... salience1 salience2...
				for(int i = 1 ; i < (data.length+1)/ 2 ; i++){
					double pitchInHz = Double.valueOf(data[i]);
					double salience = Double.valueOf(data[i+data.length/2]);
					Annotation a = new Annotation(timeStamp, pitchInHz, mode,salience);
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
