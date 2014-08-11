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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import be.tarsos.util.AudioFile;
import be.tarsos.util.FileUtils;

/**
 * Caches the results of a pitch detector by serializing annotations to a file.
 * If a file with annotations exists it reads the contents, otherwise the pitch
 * detector is executed and the annotations are stored.
 * 
 * @author Joren Six
 */
public final class CachingDetector implements PitchDetector {
	private List<Annotation> annotations;
	private final AudioFile file;
	private final PitchDetector detector;

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(CachingDetector.class.getName());

	/**
	 * Create a new caching pitch detector.
	 * 
	 * @param audioFile
	 *            The file to cache results for.
	 * @param pitchDetector
	 *            The detector used.
	 */
	public CachingDetector(final AudioFile audioFile, final PitchDetector pitchDetector) {
		file = audioFile;
		detector = pitchDetector;
		annotations = new ArrayList<Annotation>();
	}

	/* (non-Javadoc)
	 * @see be.tarsos.sampled.pitch.PitchDetector#executePitchDetection()
	 */
	public List<Annotation> executePitchDetection() {
		String directory = file.transcodedDirectory();
		String annotationsFileName = detector.getName() + "_" + file.originalBasename() + ".txt";
		annotationsFileName = FileUtils.combine(directory, annotationsFileName);
		if (FileUtils.exists(annotationsFileName)) {
			annotations = FileUtils.readPitchAnnotations(annotationsFileName);
			LOG.info(String.format("Read " + annotations.size() +  " cached annotations for %s from %s", detector.getName(),
					annotationsFileName));
		} else {
			detector.executePitchDetection();
			// Do not copy the annotations, use the same list:
			annotations = detector.getAnnotations();
			FileUtils.writePitchAnnotations(annotationsFileName, annotations);
			LOG.info(String.format("Cached annotation results for %s to %s", detector.getName(),
					annotationsFileName));
		}
		return annotations;
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public String getName() {
		return "cached_" + detector.getName();
	}

	
	public double progress() {
		return detector.progress();
	}

}
