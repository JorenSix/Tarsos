package be.hogent.tarsos.ui.link.layers.coordinatesystemlayers;

import java.awt.Color;
import java.awt.Graphics2D;

import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.ViewPort;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.hogent.tarsos.ui.link.layers.LayerUtilities;

public class TimeCoordinateSystemLayer extends AxisLayer {

	public TimeCoordinateSystemLayer(LinkedPanel parent, char direction) {
		super(parent, direction);
	}

	public void draw(Graphics2D graphics) {
		CoordinateSystem cs = parent.getCoordinateSystem();
		// draw legend
		graphics.setColor(Color.black);
		// every second
		int minOpposite = Math.round(cs.getMin(oppositeDirection));
		boolean horizontal = direction == DIRECTION_X ? true : false;
		for (int i = (int) cs.getMin(direction); i < cs.getMax(direction); i++) {
			if (i % 10000 == 0) {
				int lineHeight = Math.round(LayerUtilities.pixelsToUnits(
						graphics, 8, !horizontal));
				graphics.drawLine(i, minOpposite, i, minOpposite + lineHeight);
				int textOffset = Math.round(LayerUtilities.pixelsToUnits(
						graphics, 12, !horizontal));
				String text = String.valueOf(i / 1000);
				LayerUtilities.drawString(graphics, text, i, minOpposite
						+ textOffset, horizontal, !horizontal);
			} else if (i % 1000 == 0) {
				int lineHeight = Math.round(LayerUtilities.pixelsToUnits(
						graphics, 4, !horizontal));
				graphics.drawLine(i, minOpposite, i, minOpposite + lineHeight);
				int textOffset = Math.round(LayerUtilities.pixelsToUnits(
						graphics, 9, !horizontal));
				String text = String.valueOf(i / 1000);
				LayerUtilities.drawString(graphics, text, i, minOpposite
						+ textOffset, horizontal, !horizontal);
			}
		}
	}
}
