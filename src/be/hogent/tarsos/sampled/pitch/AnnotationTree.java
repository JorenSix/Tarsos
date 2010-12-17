package be.hogent.tarsos.sampled.pitch;

import java.util.List;
import java.util.logging.Logger;

import be.hogent.tarsos.ui.pitch.AnnotationSelection;
import be.hogent.tarsos.util.StopWatch;
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

	private static final Logger LOG = Logger.getLogger(AnnotationTree.class.getName());

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
		StopWatch watch = new StopWatch();
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
		LOG.fine(String.format("KD Tree with %s annotations constructed in %s.", tree.size(), watch));
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
	public List<Annotation> selectByTimeAndPitch(final AnnotationSelection selection) {
		final StopWatch watch = new StopWatch();
		double startTime = selection.getStartTime();
		double stopTime = selection.getStopTime();
		double startPitch = selection.getStartPitch();
		double stopPitch = selection.getStopPitch();

		double[] lowKey = { startTime, startPitch };
		double[] upperKey = { stopTime, stopPitch };

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
}
