package be.hogent.tarsos.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import be.hogent.tarsos.sampled.AudioDispatcher;
import be.hogent.tarsos.sampled.AudioProcessor;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.ui.pitch.AudioFileChangedListener;
import be.hogent.tarsos.ui.pitch.ControlPanel.SampleHandler;
import be.hogent.tarsos.util.AudioFile;

public final class WaveForm extends JComponent implements AudioFileChangedListener, SampleHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3730361987954996673L;

	private AudioFile audioFile;
	private double markerPosition; // position in seconds

	/**
	 * A cached waveform.
	 */
	private BufferedImage waveFormImage;

	/**
	 * The font used to draw axis labels.
	 */
	private static final Font AXIS_FONT = new Font("SansSerif", Font.TRUETYPE_FONT, 10);

	public WaveForm() {

		this.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(final MouseEvent event) {
				setMarkerInPixels(event.getX());
			}
		});
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent event) {
				setMarkerInPixels(event.getX());
			}
		});
		setSize(640, 80);
		setMinimumSize(new Dimension(640, 80));
	}

	/**
	 * Sets the marker position in pixels.
	 * 
	 * @param newPosition
	 *            The new position in pixels.
	 */
	private void setMarkerInPixels(final int newPosition) {
		double pixelsToSeconds = getLengthInMilliSeconds() / 1000.0 / getWidth();
		setMarker(pixelsToSeconds * newPosition);
	}

	/**
	 * Sets the marker position in seconds.
	 * 
	 * @param newPosition
	 *            The new position of the marker in seconds.
	 */
	public void setMarker(final double newPosition) {
		markerPosition = newPosition;
		requestRepaint();
	}

	private void requestRepaint() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				repaint();
			}
		});
	}

	@Override
	public void paint(final Graphics g) {
		Graphics2D graphics = (Graphics2D) g;
		graphics.setTransform(new AffineTransform());
		initializeGraphics(graphics);
		if (audioFile != null) {
			drawWaveForm(graphics);
		} else {
			graphics.setTransform(getSaneTransform());
			drawReference(graphics);
			graphics.setTransform(new AffineTransform());
		}
		drawMarker(graphics);
	}

	/**
	 * <pre>
	 *  (0,h/2)              (+w,h/2)
	 *      -------------------|
	 *      |                  |
	 *      |                  |
	 *      |                  |
	 * (0,0)|------------------| (0,h/2)
	 *      |                  |
	 *      |                  |
	 *      |                  |
	 *      -------------------
	 *  (0,-h/2)            (w,-h/2)
	 * </pre>
	 * 
	 * @return A transform where (0,0) is in the middle left of the screen.
	 *         Positive y is up, negative y down.
	 */
	private AffineTransform getSaneTransform() {
		return new AffineTransform(1.0, 0.0, 0.0, -1.0, 0, (float) getHeight() / 2);
	}

	private void drawMarker(final Graphics2D graphics) {
		graphics.setTransform(getSaneTransform());
		int x = (int) secondsToPixels(markerPosition);
		graphics.setColor(Color.red);
		graphics.drawLine(x, getHeight() / 2, x, -getHeight() / 2);
		graphics.setTransform(new AffineTransform());
	}

	private void initializeGraphics(final Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.BLACK);
	}

	private void drawWaveForm(final Graphics2D g) {
		// if there is no image create a cached image
		if (waveFormImage == null || getWidth() != waveFormImage.getWidth()
				|| getHeight() != waveFormImage.getHeight()) {
			waveFormImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_BGR);
			final Graphics2D waveFormGraphics = waveFormImage.createGraphics();
			initializeGraphics(waveFormGraphics);
			waveFormGraphics.setTransform(getSaneTransform());
			drawReference(waveFormGraphics);
			AudioDispatcher adp;
			try {
				final float frameRate = audioFile.fileFormat().getFormat().getFrameRate();
				int framesPerPixel = audioFile.fileFormat().getFrameLength() / getWidth() / 10;
				final int one = (int) (getHeight() / 2 * 0.85);
				final double secondsToX;
				secondsToX = 1000 * getWidth() / (float) audioFile.getLengthInMilliSeconds();
				adp = AudioDispatcher.fromFile(new File(audioFile.transcodedPath()), framesPerPixel);
				adp.addAudioProcessor(new AudioProcessor() {

					private int frame = 0;

					public void processingFinished() {
					}

					public void processOverlapping(final float[] audioFloatBuffer,
							final byte[] audioByteBuffer) {
						double seconds = frame / frameRate;
						frame += audioFloatBuffer.length;
						int x = (int) (secondsToX * seconds);
						int y = (int) (audioFloatBuffer[0] * one);
						waveFormGraphics.drawLine(x, 0, x, y);
					}

					public void processFull(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
						processOverlapping(audioFloatBuffer, audioByteBuffer);
					}
				});
				adp.run();
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Render the cached image.
		g.setTransform(new AffineTransform());
		g.drawImage(waveFormImage, 0, 0, null);
	}

	/**
	 * Draws a string on canvas using the current transform but makes sure the
	 * text is not flipped.
	 * 
	 * @param graphics
	 *            The canvas.
	 * @param text
	 *            The string to draw.
	 * @param x
	 *            The ( by the current affine transform transformed) x location.
	 * @param y
	 *            The ( by the current affine transform transformed) y location.
	 */
	private void drawString(final Graphics2D graphics, final String text, final double x, final double y) {
		Point2D source = new Point2D.Double(x, y);
		Point2D destination = new Point2D.Double();
		AffineTransform transform = graphics.getTransform();
		graphics.setTransform(new AffineTransform());
		transform.transform(source, destination);
		graphics.drawString(text, (int) destination.getX(), (int) destination.getY());
		graphics.setTransform(transform);
	}

	private double secondsToPixels(double seconds) {
		return seconds * 1000 * getWidth() / getLengthInMilliSeconds();
	}

	private long getLengthInMilliSeconds() {
		final long lengthInMilliSeconds;
		if (audioFile == null) {
			// default length = 200 sec;
			lengthInMilliSeconds = 200000;
		} else {
			lengthInMilliSeconds = audioFile.getLengthInMilliSeconds();
		}
		return lengthInMilliSeconds;
	}

	/**
	 * Draw reference lines on the canvas: minute markers and 1 and -1 amplitude
	 * markers.
	 * 
	 * @param g
	 *            The canvas.
	 */
	private void drawReference(final Graphics2D g) {
		final int width = getWidth();
		final int height = getHeight();
		final int one = (int) (height / 2 * 0.85);
		g.setColor(Color.GRAY);
		// line in center
		g.drawLine(0, 0, width, 0);
		// mark one and minus one left (y axis)
		g.drawLine(0, one, 3, one);
		g.drawLine(0, -one, 3, -one);

		g.setFont(AXIS_FONT);
		drawString(g, " 1.0", 6, one - 3);
		drawString(g, "-1.0", 6, -one - 3);

		// mark one and minus one right (y axis)
		g.drawLine(width, one, width - 3, one);
		g.drawLine(width, -one, width - 3, -one);

		// start at 10 sec;
		for (int i = 10; i < getLengthInMilliSeconds() / 1000; i += 10) {
			int x = (int) secondsToPixels(i);
			int y = height / 2;

			final int markerSize;
			if (i % 60 == 0) {
				markerSize = (int) (height / 2 * 0.15);
				// minute markers
				drawString(g, i / 60 + ":00", x - 8, y - markerSize - 9);
			} else {
				// marker every 10 sec
				markerSize = (int) (height / 2 * 0.05);
			}
			g.drawLine(x, y, x, y - markerSize);
			g.drawLine(x, -y, x, -y + markerSize);
		}
		g.setColor(Color.BLACK);
	}

	public static void main(final String... strings) {
		JFrame f = new JFrame();
		f.setMinimumSize(new Dimension(640, 480));
		f.setSize(new Dimension(550, 100));
		f.setLayout(new BorderLayout());
		String fileName = "C:\\Users\\jsix666\\eclipse_workspace\\Tarsos\\audio\\dekkmma_voice_all\\MR.1954.1.8-1.wav";
		AudioFile audioFile = new AudioFile(fileName);
		WaveForm waveForm = new WaveForm();

		f.add(waveForm, BorderLayout.CENTER);
		f.setVisible(true);

		try {
			Thread.sleep(2500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		waveForm.audioFileChanged(audioFile);
	}

	public void audioFileChanged(final AudioFile newAudioFile) {
		this.audioFile = newAudioFile;
		this.waveFormImage = null;
		requestRepaint();
	}

	public void addSample(Annotation sample) {
		setMarker(sample.getStart());

	}

	public void removeSample(Annotation sample) {
		// TODO Auto-generated method stub
	}
}
