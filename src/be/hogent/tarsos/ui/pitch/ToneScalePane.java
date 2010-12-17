package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.histogram.Histogram;

public final class ToneScalePane extends JPanel implements ScaleChangedListener, AudioFileChangedListener,
		AnnotationListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6648601350759001731L;

	private final ToneScalePanel plot;

	public ToneScalePane(final Histogram histogram, final ScaleChangedListener scaleChangedPublisher) {
		super(new BorderLayout());
		plot = new ToneScalePanel(histogram, scaleChangedPublisher);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, plot, plot.controls());
		splitPane.setOneTouchExpandable(true);
		splitPane.setPreferredSize(new Dimension(640, 480));
		splitPane.setDividerLocation(400);
		splitPane.setResizeWeight(1.0);
		add(splitPane, BorderLayout.CENTER);
	}

	public void audioFileChanged(final AudioFile newAudioFile) {
		plot.audioFileChanged(newAudioFile);
	}

	public void scaleChanged(final double[] newScale, final boolean isChanging) {
		plot.scaleChanged(newScale, isChanging);
	}

	public void addAnnotation(final Annotation annotation) {
		plot.addAnnotation(annotation);
	}

	public void clearAnnotations() {
		plot.clearAnnotations();
	}

	public void extractionStarted() {
		// NOOP
	}

	public void extractionFinished() {
		// NOOB?
	}

	public void annotationsAdded() {
		plot.annotationsAdded();

	}
}
