/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.AnnotationListener;
import be.hogent.tarsos.util.AudioFile;

public final class PitchClassKdePanel extends JPanel implements ScaleChangedListener, AudioFileChangedListener,
		AnnotationListener {
	/**
     */
	private static final long serialVersionUID = 5473280409705136547L;

	public PitchClassKdePanel() {
		super(new BorderLayout());
	}

	public void audioFileChanged(final AudioFile newAudioFile) {

	}

	
	public void paint(final Graphics g) {
		final Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setBackground(Color.WHITE);
		graphics.clearRect(0, 0, getWidth(), getHeight());

	}



	public void scaleChanged(final double[] newScale, final boolean isChanging, boolean shiftHisto) {

	}


	public void addAnnotation(Annotation annotation) {		

	}

	public void clearAnnotations() {
		
	}

	public void annotationsAdded() {
		repaint();
	}

	
	public void extractionStarted() {
		// TODO Auto-generated method stub
	}

	
	public void extractionFinished() {
		// TODO Auto-generated method stub
		
	}
}
