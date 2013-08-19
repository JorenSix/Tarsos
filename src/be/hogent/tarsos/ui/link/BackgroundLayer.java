package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Graphics2D;

public class BackgroundLayer implements Layer{


	public void draw(Graphics2D graphics) {
		//draw background
		ViewPort viewport = ViewPort.getInstance();
		graphics.setColor(Color.white);
		float timeDelta = viewport.getTimeDelta();
		float frequencyDelta = viewport.getFrequencyDelta();
		
		graphics.fillRect(
				Math.round(viewport.getMinTime()), 
				Math.round(viewport.getMinFrequencyInCents()), 
				Math.round(timeDelta), 
				Math.round(frequencyDelta));
	}
}
