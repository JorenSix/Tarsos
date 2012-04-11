/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.ui.pitch.ph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JPanel;

import be.hogent.tarsos.midi.TarsosSynth;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.AnnotationListener;
import be.hogent.tarsos.ui.pitch.AudioFileChangedListener;
import be.hogent.tarsos.ui.pitch.ScaleChangedListener;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

public final class PitchClassKdePanel extends JPanel implements ScaleChangedListener, AudioFileChangedListener,
		AnnotationListener {
	/**
     */
	private static final long serialVersionUID = 5473280409705136547L;
	
	private final MouseDragListener kdeDrag;
	private final MouseDragListener scaleDrag;
	private final ScaleEditor editor;
	
	private final MouseListener clickForPitchListener = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			final double pitchCents = scaleDrag.getCents(e, 1200);
			final int velocity = 100;
			TarsosSynth.getInstance().playRelativeCents(pitchCents, velocity);
		}
	};

	public PitchClassKdePanel() {
		super();
		
		kdeDrag = new MouseDragListener(this, MouseEvent.BUTTON1);
		scaleDrag = new MouseDragListener(this, MouseEvent.BUTTON2);
		editor = new ScaleEditor(scaleDrag, this);
		
		//add mouse motion listeners for dragging scales or histo
		addMouseMotionListener(kdeDrag);
		addMouseMotionListener(scaleDrag);
		
		//wire listeners for scale editor
		addMouseListener(editor);
		addMouseMotionListener(editor);
		addKeyListener(editor);
		
		//wire click for pitch listener
		addMouseListener(clickForPitchListener);
		
		List<String> trackers = Configuration.getList(ConfKey.pitch_tracker_list);
	}



	
	public void paint(final Graphics g) {
		final Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setBackground(Color.WHITE);
		graphics.clearRect(0, 0, getWidth(), getHeight());
	}
	
	private void paintScale(final Graphics g){
		final double xOffset = scaleDrag.calculateXOffset();
		final int yOffset = 20;
		final int yLabelsOffset = 5;

		final int width = getWidth();
		final int height = getHeight();
		final int xOffsetPixels = (int) Math.round(xOffset * width);

		g.setColor(Color.GRAY);

		for (final double reference : scale) {
			final int x = (int) (reference / 1200 * width + xOffsetPixels) % width;
			final String text = Integer.valueOf((int) reference).toString();
			final int labelLength = text.length();
			final double labelWidth = g.getFontMetrics().getStringBounds(text, g).getWidth();
			final int start = (int) labelWidth / 2 - labelLength / 2;
			if (editor != null && editor.getMovingElement() == reference) {
				g.setColor(Color.BLUE);
				g.drawLine(x, 0, x, height - yOffset);
				g.drawString(text, x - start, height - yLabelsOffset);
				g.setColor(Color.GRAY);
			} else {
				g.drawLine(x, 0, x, height - yOffset);
				g.drawString(text, x - start, height - yLabelsOffset);
			}
		}
	}
	
	public void paintKDEs(final Graphics g){
		
	}
	
	private void paintKDE(final Graphics g){
		double xOffset = kdeDrag.calculateXOffset();
		int yOffset = 20;
		double maxCount = values[0];

		for (int i = 1; i < values.length; i++) {
			maxCount = Math.max(maxCount, values[i]);
		}

		final int width = getWidth();
		final int height = getHeight();

		final int xOffsetPixels = (int) Math.round(xOffset * width);
		int x = xOffsetPixels;

		int y = (int) (height - yOffset - values[(int) (values.length - 1)] / maxCount * height * 0.9);
		Point previousPoint = new Point(x, y);

		g.setColor(Color.GRAY);
		g.drawLine(0, height - yOffset, width, height - yOffset);

		g.setColor(Color.RED);

		for (int i = 0; i < values.length; i++) {
			x = (int) ((i / values.length  * width + xOffsetPixels) % width);
			y = height - yOffset - (int) (values[i] / maxCount * height * 0.9);
			if (x > previousPoint.x) {
				g.drawLine(previousPoint.x, previousPoint.y, x, y);
			}
			previousPoint = new Point(x, y);
		}
	}


	public void audioFileChanged(final AudioFile newAudioFile) {

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
