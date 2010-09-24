package be.hogent.tarsos.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import be.hogent.tarsos.util.AudioFile;

public final class WaveFormPanel extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6632342971285925463L;

	protected static final Color BACKGROUND_COLOR = Color.WHITE;
	protected static final Color REFERENCE_LINE_COLOR = Color.BLACK;
	protected static final Color WAVEFORM_COLOR = Color.RED;
	protected static final int X_BORDER = 5;

	private final AudioFile audioFile;

	public WaveFormPanel(final AudioFile file) {
		this.audioFile = file;
	}

	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setBackground(BACKGROUND_COLOR);
		g2.clearRect(0, 0, getWidth(), getHeight());

		drawReferenceLine(g2);
		drawWaveform(g2);
	}

	private void drawReferenceLine(final Graphics2D g2) {
		g2.setColor(REFERENCE_LINE_COLOR);
		double lengthInSeconds = audioFile.getLengthInMilliSeconds() / 1000.0;
		g2.drawLine(0 + X_BORDER, getHeight() / 2, getWidth() - X_BORDER, getHeight() / 2);
		double lineWidth = getWidth() - 2 * X_BORDER;
		for (int i = 0; i < lengthInSeconds; i += 60) {
			int x = (int) (i / lengthInSeconds * lineWidth + X_BORDER);
			g2.drawString(String.valueOf(i), x, getHeight() / 2 + 10);
		}
	}

	private void drawWaveform(final Graphics2D g2) {
		g2.setColor(WAVEFORM_COLOR);
	}

}
