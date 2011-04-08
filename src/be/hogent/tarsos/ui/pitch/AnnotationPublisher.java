package be.hogent.tarsos.ui.pitch;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.AnnotationTree;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.StopWatch;

/**
 * This class is responsible for the extraction and delegation of annotations.
 * It notifies listeners of annotations.
 */
public final class AnnotationPublisher{

	private AnnotationTree tree;
	private final List<AnnotationListener> listeners;
	private final AnnotationSelection selection;
	private final PitchUnit unit;

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(AnnotationPublisher.class.getName());

	/**
	 * Hides the default constructor;
	 */
	private AnnotationPublisher() {
		listeners = new ArrayList<AnnotationListener>();
		selection = new AnnotationSelection();
		unit = PitchUnit.valueOf(Configuration.get(ConfKey.pitch_contour_unit));
		tree = new AnnotationTree(unit);
	}

	/**
	 * Adds an annotation listener.
	 * 
	 * @param listener
	 *            The listener to add.
	 */
	public void addListener(final AnnotationListener listener) {
		listeners.add(listener);
	}

	public void addAnnotations(final List<Annotation> annotations) {
		int beforeSize = tree.size();
		tree.add(annotations);
		
		assert tree.size() == beforeSize+annotations.size();
	}

	public AnnotationTree getAnnotationTree() {
		return tree;
	}
	
	public void clear(){
		for (AnnotationListener listener : listeners) {
			listener.clearAnnotations();
		}
	}
	
	public void clearTree(){
		tree = new AnnotationTree(unit);
	}

	/**
	 * Adds a list of annotations to listeners.
	 * 
	 * @param annotations
	 *            The annotations to add to the listeners.
	 */
	public void delegateAddAnnotations(final List<Annotation> annotations) {
		if (annotations.size() > 0) {
			for (AnnotationListener listener : listeners) {
				StopWatch watch = new StopWatch();
				for (Annotation annotation : annotations) {
					listener.addAnnotation(annotation);
				}
				if (annotations.size() > 1000) {
					LOG.fine(String.format("Adding %s annotations to %s took %s.", annotations.size(),
							listener.getClass().toString(), watch.formattedToString()));
				}
			}
			for (AnnotationListener listener : listeners) {
				listener.annotationsAdded();
			}
		}
	}

	/**
	 * Adds annotations to listeners. The annotations are defined by a search on
	 * time.
	 * 
	 * @param startTime
	 *            The start time.
	 * @param stopTime
	 *            The stop time.
	 */
	public void delegateAddAnnotations(final double startTime, final double stopTime) {
		selection.setTimeSelection(startTime, stopTime);
		List<Annotation> annotations = tree.select(selection);
		delegateAddAnnotations(annotations);
	}

	/**
	 * Adds annotations to listeners. The annotations are defined by a search on
	 * time.
	 * 
	 * @param startTime
	 *            The start time.
	 * @param stopTime
	 *            The stop time.
	 * @param startPitch
	 *            The start pitch.
	 * @param stopPitch
	 *            The stop pitch.
	 */
	public void delegateAddAnnotations(final double startTime, final double stopTime,
			final double startPitch, final double stopPitch) {
		selection.setSelection(startTime, stopTime, startPitch, stopPitch);
		List<Annotation> annotations = tree.select(selection);
		delegateAddAnnotations(annotations);
		
	}

	public void delegateAddAnnotations(final double newMinProbability) {
		selection.setMinProbability(newMinProbability);
		List<Annotation> annotations = tree.select(selection);
		delegateAddAnnotations(annotations);
	}

	public void extractionFinished(){
		for (AnnotationListener listener : listeners) {
			listener.extractionFinished();
		}
	}
	
	public void extractionStarted(){
		for (AnnotationListener listener : listeners) {
			listener.extractionStarted();
		}
	}

	public AnnotationSelection getCurrentSelection() {
		return selection;
	}

	/**
	 * The single annotation publisher in the program: is a singleton.
	 */
	private static final AnnotationPublisher INSTANCE = new AnnotationPublisher();

	/**
	 * 
	 * @return The single instance of the annotation publisher.
	 */
	public static AnnotationPublisher getInstance() {
		return INSTANCE;
	}
}