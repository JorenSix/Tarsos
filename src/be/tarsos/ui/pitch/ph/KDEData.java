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

package be.tarsos.ui.pitch.ph;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.AnnotationListener;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.ui.pitch.AudioFileChangedListener;
import be.tarsos.util.AudioFile;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.KernelDensityEstimate;
import be.tarsos.util.KernelDensityEstimate.Kernel;

public class KDEData  implements AudioFileChangedListener, AnnotationListener{

	private static final int AMBITUS_STOP = Configuration.getInt(ConfKey.pitch_histogram_stop);
	private static final int AMBITUS_START = Configuration.getInt(ConfKey.pitch_histogram_start);
	/**
	 * Logs messages.
	 */
	private static final Logger LOG = Logger.getLogger(KDEData.class.getName());
	
	private static KDEData instance; 
	
	public static synchronized KDEData getInstance(){
		if(instance == null){
			instance = new KDEData(true);
		}
		return instance;
	}
	
	private final HashMap<PitchDetectionMode, KernelDensityEstimate> kdes;

	/**
	 * Defines the kernel used to build the KDE.
	 */
	private final Kernel kernel = new KernelDensityEstimate.GaussianKernel(6);
	
	private KDEData(boolean containsPCH){
		kdes = new HashMap<PitchDetectionMode, KernelDensityEstimate>();
	}

	public boolean isEmpty(){
		return kdes.isEmpty();
	}

	public void audioFileChanged(AudioFile newAudioFile) {
		
	}

	public void addAnnotation(Annotation annotation) {
		double pitchInAbsCents = annotation.getPitch(PitchUnit.ABSOLUTE_CENTS);
		if (pitchInAbsCents > AMBITUS_START && pitchInAbsCents <= AMBITUS_STOP) {
			kdes.get(annotation.getSource()).add(pitchInAbsCents);
		} else {
			String message;
			message = String.format("Ignored pitch annotation outside range: %s not in [%s,%s]",pitchInAbsCents,AMBITUS_START,AMBITUS_STOP);
			LOG.finer(message);
		}
	}
	
	public HashMap<PitchDetectionMode, KernelDensityEstimate> getKDEs(){
		return kdes;
	}


	public void clearAnnotations() {
		clear();
	}
	
	private void clear(){
		kdes.clear();
		List<String> trackers = Configuration.getList(ConfKey.pitch_tracker_list);
		for(String tracker : trackers){
			PitchDetectionMode mode = PitchDetectionMode.valueOf(tracker);
			if(!kdes.containsKey(mode)){
				kdes.put(mode, new KernelDensityEstimate(kernel,1200));
			}
		}
	}


	public void annotationsAdded() {
		// TODO Auto-generated method stub
	}


	public void extractionStarted() {
		clear();
	}


	public void extractionFinished() {
		// TODO Auto-generated method stub
	}
}
