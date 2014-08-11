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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;

import javax.swing.JPanel;

import be.tarsos.Tarsos;
import be.tarsos.midi.TarsosSynth;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.AnnotationListener;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.ui.pitch.AudioFileChangedListener;
import be.tarsos.ui.pitch.ScaleChangedListener;
import be.tarsos.util.AudioFile;
import be.tarsos.util.KernelDensityEstimate;

public final class PitchClassKdePanel extends JPanel implements ScaleChangedListener, AudioFileChangedListener,
		AnnotationListener {
	/**
     */
	private static final long serialVersionUID = 5473280409705136547L;
	
	private final MouseDragListener kdeDrag;
	private final MouseDragListener scaleDrag;
	private final ScaleEditor editor;
	private double[] scale;
	
	private final MouseListener clickForPitchListener = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			final double pitchCents = scaleDrag.getCents(e, 1200);
			final int velocity = 100;
			TarsosSynth.getInstance().playRelativeCents(pitchCents, velocity);
		}
	};

	public PitchClassKdePanel() {
		super();
		//Focus should be enabled for the key listener (Scala layer editor)...
		setFocusable(true);
		
		//add mouse listeners for dragging KDE
		kdeDrag = new MouseDragListener(this, MouseEvent.BUTTON1);
		addMouseMotionListener(kdeDrag);
		addMouseListener(kdeDrag);
	
		//add mouse listeners for dragging scale
		scaleDrag = new MouseDragListener(this, MouseEvent.BUTTON2);
		addMouseMotionListener(scaleDrag);
		addMouseListener(scaleDrag);
		
		//wire listeners for scale editor
		editor = new ScaleEditor(scaleDrag, this);
		addMouseListener(editor);
		addMouseMotionListener(editor);
		addKeyListener(editor);
		
		//wire click for pitch listener
		addMouseListener(clickForPitchListener);
	}
	
	public void paint(final Graphics g) {
		final Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setBackground(Color.WHITE);
		graphics.clearRect(0, 0, getWidth(), getHeight());
		paintScale(g);
		paintKDEs(g);
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
	
	private void paintKDEs(final Graphics g){
		HashMap<PitchDetectionMode,KernelDensityEstimate> kdes = KDEData.getInstance().getKDEs();
		int index = 0;
		for(PitchDetectionMode mode : kdes.keySet()){
			paintKDE(g,mode,kdes.get(mode),index);
			index++;
		}
	}
	
	private void paintKDE(final Graphics g, PitchDetectionMode mode, KernelDensityEstimate kernelDensityEstimate, int index){
		double xOffset = kdeDrag.calculateXOffset();
		int yOffset = 20;

		double maxCount = kernelDensityEstimate.getMaxElement();
		double[] values = kernelDensityEstimate.getEstimate();

		final int width = getWidth();
		final int height = getHeight();

		final int xOffsetPixels = (int) Math.round(xOffset * width);
		int x = xOffsetPixels;

		int y = (int) (height - yOffset - values[(int) (values.length - 1)] / maxCount * height * 0.9);
		Point previousPoint = new Point(x, y);

		g.setColor(Color.GRAY);
		g.drawLine(0, height - yOffset, width, height - yOffset);

		Color modeColor = Tarsos.COLORS[mode.ordinal() % Tarsos.COLORS.length];
		
		if(kernelDensityEstimate.getSumFreq() > 0){
			g.setColor(modeColor);
			//draw graph
			for (int i = 0; i < values.length; i++) {
				x = (int) ((i / Double.valueOf(values.length)  * width + xOffsetPixels) % width);
				y = height - yOffset - (int) (values[i] / maxCount * height * 0.9);
				if (x >= previousPoint.x) {
					g.drawLine(previousPoint.x, previousPoint.y, x, y);
				}
				previousPoint = new Point(x, y);
			}
			
			//draw legend
			int legendElementWidth = 100;
			int legendElementHeight = 18;
			g.setColor(new Color(1.0f, 1.0f, 1.0f, 0.7f));
			g.fillRect(width - legendElementWidth, index * legendElementHeight, legendElementWidth, legendElementHeight );
			g.setColor(modeColor);
			g.drawString(mode.getParametername(), width - legendElementWidth, legendElementHeight + index * legendElementHeight - 5);
		}
	}


	public void audioFileChanged(final AudioFile newAudioFile) {

	}

	public void scaleChanged(final double[] newScale, final boolean isChanging, boolean shiftHisto) {
		//propagate tha changed scale to the editor
		editor.scaleChanged(newScale, isChanging, shiftHisto);
		this.scale = newScale;
		repaint();
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
