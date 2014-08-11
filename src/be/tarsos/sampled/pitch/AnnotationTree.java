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

import java.util.List;
import java.util.logging.Logger;

import be.tarsos.util.StopWatch;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * Annotation tree can be used for range selection on a list of annotations. To
 * make the range search efficient it backed by a KD-tree. One dimension is
 * time, the second dimension pitch and the third dimension is salience. 
 * The unit of pitch is defined during the construction phase.
 */
public final class AnnotationTree {

	private static final Logger LOG = Logger.getLogger(AnnotationTree.class.getName());

	/**
	 * The backing kd-tree.
	 */
	private final KDTree<Annotation> tree;
	private final PitchUnit unit;

	/**
	 * Create a new annotation tree. 
	 * @param pitchUnit
	 *            The pitch unit. The pitch unit needs to be defined here to be
	 *            able to do range selection in a certain unit.
	 *            
	 */
	public AnnotationTree(final PitchUnit pitchUnit) {
		// Three dimensional tree
		tree = new KDTree<Annotation>(3);
		unit = pitchUnit;
	}
	
	/**
	 * Add a list of annotations to the KD-tree.
	 * @param annotations
	 *            The annotations to add to the tree.
	 */
	public void add(final List<Annotation> annotations){
		StopWatch watch = new StopWatch();
		for (Annotation annotation : annotations) {
			add(annotation);
		}
		LOG.fine(String.format("Added %s annotations (new size %s) to KD Tree in %s.", annotations.size(),tree.size(), watch));
	}

	/**
	 * Select a subset of all annotations within a pitch - time range.
	 * @param selection A selection defines a pitch-time range.
	 * 
	 * @return A range selection of annotations.
	 */
	public List<Annotation> select(final AnnotationSelection selection) {
		final StopWatch watch = new StopWatch();
		double startTime = selection.getStartTime();
		double stopTime = selection.getStopTime();
		double startPitch = selection.getStartPitch();
		double stopPitch = selection.getStopPitch();
		double startProbability = selection.getMinProbability();
		double stopProbability = AnnotationSelection.MAX_PROBABILITY;

		double[] lowKey = { startTime, startPitch, startProbability };
		double[] upperKey = { stopTime, stopPitch, stopProbability };

		List<Annotation> selectedAnnotations = null;
		try {
			selectedAnnotations = tree.range(lowKey, upperKey);
		} catch (KeySizeException e) {
			new IllegalStateException("The dimenstion of the tree is two, the dimension of the key also.");
		}
		LOG.finer(String.format("Selected %s annotations from a KD-tree of %s annotations in %s.",
				selectedAnnotations.size(), tree.size(), watch.formattedToString()));
		return selectedAnnotations;
	}

	public int size() {
		return tree.size();
	}
	
	

	/**
	 * Add one annotation to the tree.
	 * @param annotation
	 *            The annotation to add.
	 */
	public void add(final Annotation annotation) {
		double[] key = { annotation.getStart(), annotation.getPitch(unit), annotation.getProbability() };
		try {
			tree.insert(key, annotation);
		} catch (KeySizeException e) {
			new IllegalStateException("The dimenstion of the tree is 3," + " the dimension of the key also.");
		} catch (KeyDuplicateException e) {
			new IllegalStateException("No two annotations with the same timestamp and starttime should be present!");
		}

	}
}
