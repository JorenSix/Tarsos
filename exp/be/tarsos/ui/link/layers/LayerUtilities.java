package be.tarsos.ui.link.layers;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class LayerUtilities {

	private LayerUtilities() {

	}

	/**
	 * Transforms pixels to time and frequency.
	 * 
	 * @param g
	 *            The current graphics, with a meaningful transform applied to
	 *            it.
	 * @param x
	 *            The x coordinate, in pixels.
	 * @param y
	 *            The y coordinate, in pixels.
	 * @return A point with time (in milliseconds) as x coordinate, and
	 *         frequency (in cents) as y coordinate.
	 */
	public static Point2D pixelsToUnits(Graphics2D g, int x, int y) {
		Point2D units = null;
		try {
			units = g.getTransform().inverseTransform(new Point2D.Double(x, y),
					null);
		} catch (NoninvertibleTransformException e1) {
			e1.printStackTrace();
		}
		return units;
	}

	/**
	 * Transforms a number of pixels into a corresponding time or frequency
	 * span. E.g. 10 horizontal pixels could translate to 320 milliseconds. 10
	 * vertical pixels could translate to 32cents.
	 * 
	 * @param g
	 *            The current graphics, with a meaningful transform applied to
	 *            it.
	 * @param pixels
	 *            The number of pixels
	 * @param horizontal
	 *            Is it the horizontal or vertical axis?
	 * @return A number of cents or milliseconds.
	 */
	public static float pixelsToUnits(Graphics2D g, int pixels,
			boolean horizontal) {
		float numberOfUnits = 0;
		try {
			Point2D originSrc = new Point2D.Double(0, 0);
			Point2D originDest;
			originDest = g.getTransform().inverseTransform(originSrc, null);
			Point2D destSrc = new Point2D.Double(pixels, pixels);
			Point2D destDest;
			destDest = g.getTransform().inverseTransform(destSrc, null);
			if (horizontal) {
				numberOfUnits = (float) (destDest.getX() - originDest.getX());
			} else {
				numberOfUnits = (float) (-destDest.getY() + originDest.getY());
			}
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		return numberOfUnits;
	}

	
	//The AffineTransform transforms coordinates in a certain quantity to pixel coordinates
	//x and y are in a certain quantity -> need to be transformed to pixels (transform.transform(source, destination))
	//Graphics has the transformation from quantity to pixels, we add the inverse to undo this so we can insert pixels
	//We draw the string with pixel coordinates on the graphics
	//We make a rectangle(position) with the location of the string in pixels
		//ATTENTION: the Y coordinates are mirrored in the quantity representation, so we need to substract the height from the y-coordinates
	//We transform the rectangle and the graphics (undo the invert)
	public static Rectangle2D drawString(Graphics2D graphics, String text,
			double x, double y, boolean centerHorizontal, boolean centerVertical) {

		AffineTransform transform = graphics.getTransform();
		Point2D source = new Point2D.Double(x, y);
		Point2D destination = new Point2D.Double();
		transform.transform(source, destination);
		try {
			transform.invert();
		} catch (NoninvertibleTransformException e1) {
			e1.printStackTrace();
		}
		graphics.transform(transform);
		// r are the coordinates in pixels (not units)
		Rectangle2D r = graphics.getFontMetrics().getStringBounds(text,
				graphics);
		int xPosition = Math
				.round((float) (destination.getX() - (centerHorizontal ? r
						.getWidth() / 2.0f - 1 : 0)));
		int yPosition = Math
				.round((float) (destination.getY() + (centerVertical ? r
						.getHeight() / 2.0f - 1.5 : 0)));
		graphics.drawString(text, xPosition, yPosition);
		Rectangle2D position = new Rectangle2D.Double(xPosition, yPosition-Math.max(15, r.getHeight()),
				Math.max(25, r.getWidth()), Math.max(15, r.getHeight()));
		position = transform.createTransformedShape(position).getBounds2D();
		try {
			transform.invert();
		} catch (NoninvertibleTransformException e1) {
			e1.printStackTrace();
		}
		
		graphics.transform(transform);
		return position;
	}

	public static void drawVerticalString(Graphics2D graphics, String text,
			double x, double y, boolean centerHorizontal, boolean centerVertical) {

		AffineTransform transform = graphics.getTransform();
		Point2D source = new Point2D.Double(x, y);
		Point2D destination = new Point2D.Double();
		transform.transform(source, destination);
		try {
			transform.invert();
		} catch (NoninvertibleTransformException e1) {
			e1.printStackTrace();
		}
		graphics.transform(transform);
		Rectangle2D r = graphics.getFontMetrics().getStringBounds(text,
				graphics);
		Rectangle2D r2 = graphics.getFontMetrics().getStringBounds("M",
				graphics);
		int xPosition = Math
				.round((float) (destination.getX() + (centerHorizontal ? r2
						.getWidth() + r2.getWidth() / 2.0f - 1 : r2.getWidth())));
		int yPosition = Math
				.round((float) (destination.getY() - (centerVertical ? r
						.getHeight() * (text.length() + 2) / 2.0f : 0)));
		// graphics.drawString(text,xPosition,yPosition);
		String newText = "";// text.charAt(0) + "\n";

		for (int i = 0; i < text.length() - 1; i++) {
			newText = newText.concat(text.charAt(i) + "\n");
		}
		newText = newText
				.concat(String.valueOf(text.charAt(text.length() - 1)));
		text = newText;
		for (String line : text.split("\n")) {
			graphics.drawString(line, Math.round(xPosition), Math
					.round(yPosition -= LayerUtilities.pixelsToUnits(graphics,
							graphics.getFontMetrics().getHeight(), false)));
		}
		try {
			transform.invert();
		} catch (NoninvertibleTransformException e1) {
			e1.printStackTrace();
		}
		graphics.transform(transform);
	}
}
