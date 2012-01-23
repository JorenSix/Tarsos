/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

public class AudioFileItem extends JComponent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7321286762169383597L;

	private static final Insets INNER_MARGIN = new Insets(4, 4, 4, 4);
	private static final Insets OUTER_MARGIN = new Insets(8, 8, 8, 8);

	private static final int ITEM_WIDTH = 54;
	private static final int ITEM_HEIGHT = 60;
	private static final int CLIP_HEIGHT = (int) (ITEM_HEIGHT * 0.8);

	private static final int SHADOW_SIZE = 7;
	private static BufferedImage shadowBuffer;

	private BufferedImage image;
	private BufferedImage buffer;

	private final String name;
	private final double ratio;

	private final Font font;
	private RenderingHints hints;

	public AudioFileItem(String fileName) {
		this.name = fileName;
		try {
			this.image = loadCompatibleImage(AudioFileItem.class
					.getResource("/be/hogent/tarsos/ui/resources/file_sound.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.ratio = (double) image.getWidth() / (double) image.getHeight();
		this.font = new Font("Tahoma", Font.PLAIN, 9);

		createRenderingHints();
		// DragSource dragSource = DragSource.getDefaultDragSource();
		// dragSource.createDefaultDragGestureRecognizer(this,
		// DnDConstants.ACTION_COPY, this);
		// dragSource.addDragSourceMotionListener(this);
	}

	public BufferedImage loadCompatibleImage(URL resource) throws IOException {
		BufferedImage image = ImageIO.read(resource);
		GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice().getDefaultConfiguration();
		BufferedImage compatibleImage = configuration.createCompatibleImage(image.getWidth(),
				image.getHeight());
		Graphics g = compatibleImage.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return compatibleImage;
	}

	private void createRenderingHints() {
		hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		Object value = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
		try {
			Field declaredField = RenderingHints.class.getDeclaredField("VALUE_TEXT_ANTIALIAS_LCD_HRGB");
			value = declaredField.get(null);
		} catch (Exception e) {
		}
		hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, value);
	}

	
	public Dimension getPreferredSize() {
		return new Dimension(getItemWidth(), getItemHeight());
	}

	private void renderOffscreen() {
		buffer = new BufferedImage(getItemWidth(), getItemHeight(), BufferedImage.TYPE_INT_ARGB);

		int left = OUTER_MARGIN.left;
		int top = OUTER_MARGIN.top;
		int width = ITEM_WIDTH + INNER_MARGIN.left + INNER_MARGIN.right;
		int height = ITEM_HEIGHT + INNER_MARGIN.top + INNER_MARGIN.bottom;

		Graphics2D g2 = buffer.createGraphics();
		g2.setRenderingHints(hints);

		g2.drawImage(getPhotoBackground(), OUTER_MARGIN.left - 3 - SHADOW_SIZE / 2, OUTER_MARGIN.top - 3,
				getPhotoBackground().getWidth() + 6, getPhotoBackground().getHeight() + 6, null);

		g2.fillRect(left, top, width, height);

		left += INNER_MARGIN.left;
		top += INNER_MARGIN.top;
		width = ITEM_WIDTH;
		height = (int) (width / ratio);

		g2.setClip(left, top, width, CLIP_HEIGHT);
		if (ratio < 1.0) {
			top -= (int) ((height - CLIP_HEIGHT) / 2.0);
		}

		g2.drawImage(image, left, top, width, height, null);

		g2.dispose();
	}

	public BufferedImage getItemPicture() {
		if (buffer == null) {
			renderOffscreen();
		}
		return buffer;
	}

	
	protected void paintComponent(Graphics g) {
		if (isVisible()) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHints(hints);

			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.drawImage(getItemPicture(), 0, 0, this);

			g2.setColor(Color.DARK_GRAY);
			g2.setFont(font);
			FontMetrics metrics = g2.getFontMetrics();

			g2.drawString(name, OUTER_MARGIN.left + INNER_MARGIN.left, getItemHeight() - OUTER_MARGIN.bottom
					- INNER_MARGIN.bottom - metrics.getDescent());
		}
	}

	private static BufferedImage getPhotoBackground() {

		if (shadowBuffer == null) {
			final Color shadowColor = Color.BLACK;
			ShadowFactory factory = new ShadowFactory(SHADOW_SIZE, 0.25f, shadowColor);
			BufferedImage image = new BufferedImage(ITEM_WIDTH + INNER_MARGIN.left + INNER_MARGIN.right,
					ITEM_HEIGHT + INNER_MARGIN.top + INNER_MARGIN.bottom, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = image.createGraphics();
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, image.getWidth(), image.getHeight());
			g2.dispose();

			shadowBuffer = factory.createShadow(image);
		}
		return shadowBuffer;
	}

	public static int getItemWidth() {
		return ITEM_WIDTH + INNER_MARGIN.left + INNER_MARGIN.right + OUTER_MARGIN.left + OUTER_MARGIN.right;
	}

	public static int getItemHeight() {
		return ITEM_HEIGHT + INNER_MARGIN.top + INNER_MARGIN.bottom + OUTER_MARGIN.top + OUTER_MARGIN.bottom;
	}

}
