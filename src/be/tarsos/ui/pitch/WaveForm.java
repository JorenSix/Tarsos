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

package be.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.sampled.Player;
import be.tarsos.sampled.PlayerState;
import be.tarsos.sampled.pitch.AnnotationPublisher;
import be.tarsos.ui.TarsosFrame;
import be.tarsos.util.AudioFile;
import be.tarsos.util.StopWatch;

public final class WaveForm extends JPanel implements AudioFileChangedListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3730361987954996673L;

	/**
	 * Logs messages.
	 */
	private static final Logger LOG = Logger.getLogger(TarsosFrame.class.getName());

	private AudioFile audioFile;
	private double minMarkerPosition; // position in seconds
	private double maxMarkerPosition; // position in seconds

	/**
	 * A cached waveform image used to scale to the correct height and width.
	 */
	private BufferedImage waveFormImage;

	/**
	 * The same image scaled to the current height and width.
	 */
	private BufferedImage scaledWaveFormImage;

	/**
	 * The font used to draw axis labels.
	 */
	private static final Font AXIS_FONT = new Font("SansSerif", Font.TRUETYPE_FONT, 10);

	public WaveForm() {

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent event) {
				if (event.getButton() == MouseEvent.BUTTON1) {
					setMarkerInPixels(event.getX(), false);
				} else {
					setMarkerInPixels(event.getX(), true);
				}
				
				Player player = Player.getInstance();
				PlayerState previousState = player.getState();
				if(previousState!=PlayerState.NO_FILE_LOADED){
					player.pauze(maxMarkerPosition);
					if(previousState == PlayerState.PLAYING) {
						player.play();
					}
				}
				AnnotationPublisher.getInstance().clear();
				AnnotationPublisher.getInstance().alterSelection(minMarkerPosition, maxMarkerPosition);
				AnnotationPublisher.getInstance().delegateAddAnnotations(minMarkerPosition, maxMarkerPosition);
			}
		});
		setMarker(0, true);
		setMarker(0, false);
		
		Player player = Player.getInstance();
		player.addProcessorBeforeTimeStrechting(new AudioProcessor() {
			public boolean process(AudioEvent audioEvent) {
				setMarker(audioEvent.getTimeStamp(), false);
				return true;
			}
			public void processingFinished() {
			}
		});
	}

	/**
	 * Sets the marker position in pixels.
	 * 
	 * @param newPosition
	 *            The new position in pixels.
	 */
	private void setMarkerInPixels(final int newPosition, final boolean minMarker) {
		double pixelsToSeconds = getLengthInMilliSeconds() / 1000.0 / getWidth();
		setMarker(pixelsToSeconds * newPosition, minMarker);
	}

	private boolean waveFormCreationFinished = false;

	private void setWaveFormCreationFinished(boolean isFinished) {
		waveFormCreationFinished = isFinished;
	}

	/**
	 * Sets the marker position in seconds.
	 * 
	 * @param newPosition
	 *            The new position of the marker in seconds.
	 * @param minMarker 
	 * 			   True if the marker to place is the marker at the left, the minimum. False otherwise.
	 */
	public void setMarker(final double newPosition, final boolean minMarker) {
		if (minMarker ) {
			minMarkerPosition = newPosition;
			if(newPosition > maxMarkerPosition){
				maxMarkerPosition = minMarkerPosition;
			}
		} else if (!minMarker) {
			maxMarkerPosition = newPosition;
			if(newPosition < minMarkerPosition){
				minMarkerPosition = maxMarkerPosition;
			}
		}
		requestRepaint();
	}

	public double getMarker(final boolean minMarker) {
		final double markerValue;
		if (minMarker) {
			markerValue = minMarkerPosition;
		} else {
			markerValue = maxMarkerPosition;
		}
		return markerValue;
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
		initializeGraphics(graphics);
		if (waveFormImage != null && waveFormCreationFinished) {
			drawWaveForm(graphics);
		} else {
			graphics.transform(getSaneTransform());
			drawReference(graphics);
			graphics.transform(getInverseSaneTransform());
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
		return getSaneTransform(getHeight());
	}

	private AffineTransform getSaneTransform(final float heigth) {
		return new AffineTransform(1.0, 0.0, 0.0, -1.0, 0, heigth / 2);
	}

	private AffineTransform getInverseSaneTransform() {
		return getInverseSaneTransform(getHeight());
	}

	private AffineTransform getInverseSaneTransform(final float heigth) {
		return new AffineTransform(1.0, 0.0, 0.0, -1.0, 0, heigth / 2);
	}

	private void drawMarker(final Graphics2D graphics) {
		int minX = (int) secondsToPixels(minMarkerPosition);
		graphics.transform(getSaneTransform());
		graphics.setColor(Color.black);
		graphics.drawLine(minX, getHeight() / 2, minX, -getHeight() / 2);
		int maxX = (int) secondsToPixels(maxMarkerPosition);
		graphics.setColor(Color.black);
		graphics.drawLine(maxX, getHeight() / 2, maxX, -getHeight() / 2);
		Color color = new Color(0.0f, 0.0f, 0.0f, 0.15f); // black
		// graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
		// 0.5f));
		graphics.setPaint(color);
		Rectangle rectangle = new Rectangle(minX + 1, -getHeight() / 2, maxX - minX, getHeight() * 4);
		graphics.fill(rectangle);
		graphics.transform(getInverseSaneTransform());
	}

	private void initializeGraphics(final Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.BLACK);
	}

	private void drawWaveForm(final Graphics2D g) {
		// Render the cached image.
		scaleWaveFormImage();
		g.drawImage(scaledWaveFormImage, 0, 0, null);
	}

	public BufferedImage scaleWaveFormImage() {
		if (scaledWaveFormImage == null || scaledWaveFormImage.getHeight() != getHeight()
				|| scaledWaveFormImage.getWidth() != getWidth()) {
			StopWatch watch = new StopWatch();
			int sourceWidth = waveFormImage.getWidth();
			int sourceHeight = waveFormImage.getHeight();
			int destWidth = getWidth();
			int destHeight = getHeight();

			double xScale = (double) getWidth() / (double) sourceWidth;
			double yScale = (double) getHeight() / (double) sourceHeight;

			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice gd = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gd.getDefaultConfiguration();

			scaledWaveFormImage = gc.createCompatibleImage(destWidth, destHeight, waveFormImage
					.getColorModel().getTransparency());
			Graphics2D g2d = null;
			try {
				g2d = scaledWaveFormImage.createGraphics();
				g2d.getTransform();
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
						RenderingHints.VALUE_FRACTIONALMETRICS_ON);
				AffineTransform at = AffineTransform.getScaleInstance(xScale, yScale);
				g2d.drawRenderedImage(waveFormImage, at);
				g2d.transform(getInverseSaneTransform());
				drawReference(g2d);
			} finally {
				if (g2d != null) {
					g2d.dispose();
				}
			}
			LOG.finer("Rescaled wave form image in " + watch.formattedToString());
		}
		return scaledWaveFormImage;
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
		graphics.transform(getInverseSaneTransform());
		transform.transform(source, destination);
		graphics.drawString(text, (int) destination.getX(), (int) destination.getY());
		graphics.transform(getSaneTransform());
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

	public void audioFileChanged(final AudioFile newAudioFile) {
		this.audioFile = newAudioFile;
		this.waveFormImage = null;
		this.scaledWaveFormImage = null;
		setWaveFormCreationFinished(false);
		createWaveFormImage();
		requestRepaint();
	}

	private void createWaveFormImage() {
		final StopWatch watch = new StopWatch();

		try {
			final int waveFormHeight = 200;
			final int waveFormWidth = 2000;
			waveFormImage = new BufferedImage(waveFormWidth, waveFormHeight, BufferedImage.TYPE_INT_RGB);
			final Graphics2D waveFormGraphics = waveFormImage.createGraphics();
			initializeGraphics(waveFormGraphics);
			waveFormGraphics.clearRect(0, 0, waveFormWidth, waveFormHeight);
			waveFormGraphics.transform(getSaneTransform(waveFormHeight));
			final float frameRate = audioFile.fileFormat().getFormat().getFrameRate();
			int framesPerPixel = audioFile.fileFormat().getFrameLength() / waveFormWidth / 8;

			waveFormGraphics.setColor(Color.black);

			final int one = (int) (waveFormHeight / 2 * 0.85);

			final double secondsToX;
			secondsToX = 1000 * waveFormWidth / (float) audioFile.getLengthInMilliSeconds();
			AudioDispatcher adp = AudioDispatcherFactory.fromFile(new File(audioFile.transcodedPath()),framesPerPixel,0);
			adp.addAudioProcessor(new AudioProcessor() {

				private int frame = 0;

				public void processingFinished() {
					setWaveFormCreationFinished(true);
					invalidate();
					requestRepaint();
					LOG.fine("Created wave form image in " + watch.formattedToString());
				}

				public boolean process(AudioEvent audioEvent) {
					float[] audioFloatBuffer = audioEvent.getFloatBuffer();
					double seconds = frame / frameRate;
					frame += audioFloatBuffer.length;
					int x = (int) (secondsToX * seconds);
					int y = (int) (audioFloatBuffer[0] * one);
					waveFormGraphics.drawLine(x, 0, x, y);
					return true;
				}
			});

			new Thread(adp, "Waveform image builder").start();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
