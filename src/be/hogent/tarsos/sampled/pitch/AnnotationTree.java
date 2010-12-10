package be.hogent.tarsos.sampled.pitch;

import java.util.List;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * Annotation tree can be used for range selection on a list of annotations. To
 * make the range search efficient it backed by a KD-tree. One dimension is
 * time, the other dimension pitch. The unit of pitch is defined during the
 * construction phase.
 */
public final class AnnotationTree {

	/**
	 * The backing kd-tree.
	 */
	private final KDTree<Annotation> tree;

	/**
	 * Create a new annotation tree.
	 * 
	 * @param annotations
	 *            A list of annotations.
	 * @param unit
	 *            The pitch unit. The pitch unit needs to be defined here to be
	 *            able to do range selection in a certain unit.
	 */
	public AnnotationTree(final List<Annotation> annotations, final PitchUnit unit) {
		tree = new KDTree<Annotation>(2);
		for (Annotation annotation : annotations) {
			double[] key = { annotation.getStart(), annotation.getPitch(unit) };
			try {
				tree.insert(key, annotation);
			} catch (KeySizeException e) {
				new IllegalStateException("The dimenstion of the tree is two,"
						+ " the dimension of the key also.");
			} catch (KeyDuplicateException e) {
				new IllegalStateException(
						"No two annotations with the same timestamp and starttime should be present!");
			}
		}
	}

	/**
	 * Select all annotations in a pitch range.
	 * 
	 * @param startPitch
	 *            The start pitch (in a predefined unit).
	 * @param stopPitch
	 *            The stop pitch (in a predefined unit).
	 * @return A range selection of annotations.
	 */
	public List<Annotation> selectByPitch(final double startPitch, final double stopPitch) {
		// max song length is 5000000 seconds! (possible bug)
		return selectByTimeAndPitch(0, 5000000, startPitch, stopPitch);
	}

	/**
	 * Select all annotations in a time range (in seconds).
	 * 
	 * @param startTime
	 *            The start time (in seconds).
	 * @param stopTime
	 *            The stop time (in seconds).
	 * @return A range selection of annotations.
	 */
	public List<Annotation> selectByTime(final double startTime, final double stopTime) {
		// max pitch is 1000000 in an undefined pitch unit (possible bug);
		return selectByTimeAndPitch(startTime, stopTime, 0, 1000000);
	}

	/**
	 * Select a subset of all annotations within a pitch - time range.
	 * 
	 * @param startTime
	 *            The start time (in seconds).
	 * @param stopTime
	 *            The stop time (in seconds).
	 * @param startPitch
	 *            The start pitch (in a predefined unit).
	 * @param stopPitch
	 *            The stop pitch (in a predefined unit).
	 * @return A range selection of annotations.
	 */
	public List<Annotation> selectByTimeAndPitch(final double startTime, final double stopTime,
			final double startPitch, final double stopPitch) {
		assert stopTime >= startTime;
		assert stopPitch >= startPitch;
		double[] lowKey = { startTime, startPitch };
		// max pitch is 100000 in an undefined unit (possible bug);
		double[] upperKey = { stopTime, stopPitch };
		List<Annotation> selectedAnnotations = null;
		try {
			selectedAnnotations = tree.range(lowKey, upperKey);
		} catch (KeySizeException e) {
			new IllegalStateException("The dimenstion of the tree is two, the dimension of the key also.");
		}
		return selectedAnnotations;
	}
}
