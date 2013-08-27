package be.hogent.tarsos.ui.link.layers.coordinatesystemlayers;

import java.awt.Color;
import java.awt.Graphics2D;

import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.ViewPort;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.hogent.tarsos.ui.link.layers.LayerUtilities;

public class TimeCoordinateSystemLayer extends AxisLayer {

	private int[] intervals = { 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000};
	private int intervalIndex;
	public TimeCoordinateSystemLayer(LinkedPanel parent, char direction) {
		super(parent, direction);
		this.name = "Time axis";
	}

	public void draw(Graphics2D graphics) {
		CoordinateSystem cs = parent.getCoordinateSystem();
		// draw legend
		graphics.setColor(Color.black);
		// every second
		int minOpposite = Math.round(cs.getMin(oppositeDirection));
		boolean horizontal = direction == DIRECTION_X ? true : false;
		float deltaX = cs.getDelta(DIRECTION_X); //Breedte in milisec.
		int beginDrawInterval = 1000;
		int smallDrawInterval = beginDrawInterval*intervals[intervalIndex];
		intervalIndex = 0;
		while(deltaX/smallDrawInterval*(1200/parent.getWidth()) > 30){
			intervalIndex++;
			smallDrawInterval = beginDrawInterval*intervals[intervalIndex];
		}
		
		for (int i = (int) cs.getMin(direction); i < cs.getMax(direction); i++) {
			
			if (i % (smallDrawInterval*5) == 0) {
				int lineHeight = Math.round(LayerUtilities.pixelsToUnits(
						graphics, 8, !horizontal));
				graphics.drawLine(i, minOpposite, i, minOpposite + lineHeight);
				int textOffset = Math.round(LayerUtilities.pixelsToUnits(
						graphics, 12, !horizontal));
				String text = String.valueOf(i / 1000);
				LayerUtilities.drawString(graphics, text, i, minOpposite
						+ textOffset, horizontal, !horizontal);
			} else if (i % smallDrawInterval == 0) {
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
