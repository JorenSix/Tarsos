package be.hogent.tarsos.ui.pitch;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.AnnotationTree;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.StopWatch;

/**
 * This class is responsible for the extraction and delegation of annotations.
 * It notifies listeners of annotations.
 */
public final class AnnotationPublisher implements AudioFileChangedListener {

	private AnnotationTree tree;
	private final List<AnnotationListener> listeners;
	private final AnnotationSelection selection;

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

	public void audioFileChanged(final AudioFile newAudioFile) {
		Thread extractorThread = new AnnotationExtractor(newAudioFile, this);
		extractorThread.start();
	}

	private void buildTree(final List<Annotation> annotations) {
		// Build a new KD-tree.
		tree = new AnnotationTree(annotations, PitchUnit.valueOf(Configuration
				.get(ConfKey.pitch_contour_unit)));

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
		if (tree != null) {
			selection.setTimeSelection(startTime, stopTime);
			List<Annotation> annotations = tree.selectByTimeAndPitch(selection);
			delegateAddAnnotations(annotations);
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
	 * @param startPitch
	 *            The start pitch.
	 * @param stopPitch
	 *            The stop pitch.
	 */
	public void delegateAddAnnotations(final double startTime, final double stopTime,
			final double startPitch, final double stopPitch) {
		if (tree != null) {
			selection.setSelection(startTime, stopTime, startPitch, stopPitch);
			List<Annotation> annotations = tree.selectByTimeAndPitch(selection);
			delegateAddAnnotations(annotations);
		}
	}

	/**
	 * Clears the annotations for each listener.
	 */
	public void delegateClearAnnotations() {
		for (AnnotationListener listener : listeners) {
			listener.clearAnnotations();
		}
	}

	public AnnotationSelection getCurrentSelection() {
		return selection;
	}

	/**
	 * Notifies each listener that the annotation extraction process has
	 * started.
	 */
	private void delegateExtractionStarted() {
		// delegate to the actual listeners.
		for (AnnotationListener listener : listeners) {
			listener.extractionStarted();
		}
	}

	/**
	 * Notifies each listener that the annotation extraction process has
	 * finished
	 */
	private void delegateExtractionFinished() {
		// delegate to the actual listeners.
		for (AnnotationListener listener : listeners) {
			listener.extractionFinished();
		}
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

	/**
	 * This Thread is responsible for the actual extraction of pitch
	 * annotations. It starts the extraction and notifies one listener when it
	 * is finished. The listener can then e.g. delegate the events to other
	 * listeners.
	 */
	private final class AnnotationExtractor extends Thread {
		/**
		 * The file to annotate.
		 */
		private final AudioFile file;
		/**
		 * The listener to notify.
		 */
		private final AnnotationPublisher publisher;

		private AnnotationExtractor(final AudioFile audioFile, final AnnotationPublisher annotationPublisher) {
			super("Annotation data publisher.");
			file = audioFile;
			this.publisher = annotationPublisher;
		}

		@Override
		public void run() {
			// Pitch extractor setup.
			PitchDetectionMode mode = Configuration.getPitchDetectionMode(ConfKey.pitch_tracker_current);
			final PitchDetector pitchDetector = mode.getPitchDetector(file);

			// Do pitch extraction and notify listener of state.
			publisher.delegateExtractionStarted();
			pitchDetector.executePitchDetection();

			// Build a KD-tree
			final List<Annotation> annotations = pitchDetector.getAnnotations();
			publisher.buildTree(annotations);

			// notify listeners the annotations can be consumed.
			publisher.delegateExtractionFinished();

		}
	}
}