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

package be.tarsos.ui.virtualkeyboard;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

/**
 * A normal keyboard with 12 tones. Uses refactored code from the Gervill
 * package licensed under the GPL with the classpath exception. <a
 * href="https://gervill.dev.java.net/source/browse/gervill/src.demos/"> Gervill
 * source code</a>
 * 
 * @author Joren Six
 */
public final class VirtualKeyboard12 extends VirtualKeyboard {

	/**
     */
	private static final long serialVersionUID = -2901538187056239209L;

	public VirtualKeyboard12() {
		super(12, 12 * 4);
		setToolTipText("");
	}

	@Override
	public String getToolTipText(final MouseEvent event) {
		int note = getMidiNote(event.getX(), event.getY());
		return String.format("%.0f", getTuning()[note] % 1200);
	}

	@Override
	public int getMidiNote(final int x, final int y) {
		final int w = getWidth();
		final int h = getHeight();
		final float nw = w / 75f;

		final int wn = (int) (x / nw);
		final int oct = wn / 7;
		int n = oct * 12;
		final int nb = wn % 7;
		if (nb == 1) {
			n += 2;
		}
		if (nb == 2) {
			n += 4;
		}
		if (nb == 3) {
			n += 5;
		}
		if (nb == 4) {
			n += 7;
		}
		if (nb == 5) {
			n += 9;
		}
		if (nb == 6) {
			n += 11;
		}
		if (y < h * 4.0 / 7.0) {
			final int xb = x - (int) (oct * 7 * nw);
			float cx = 0;
			final float black_note_width = nw * 0.7f;
			for (int b = 0; b < 12; b++) {
				final boolean a = b == 1 || b == 3 | b == 6 | b == 8 | b == 10;
				if (!a) {
					cx += nw;
				} else {
					final float cstart = cx - black_note_width / 2;
					final float cend = cstart + black_note_width;
					if (xb > cstart && xb < cend) {
						return oct * 12 + b;
					}
				}
			}

		}
		if (n < 0) {
			n = 0;
		}
		if (n > 127) {
			n = 127;
		}
		return n;

	}

	@Override
	public void paint(final Graphics g) {
		super.paint(g);
		final Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		final int w = getWidth();
		final int h = getHeight();

		final float nw = w / 75f;
		float cx = 0;
		final Rectangle2D rect = new Rectangle2D.Double();
		for (int i = 0; i < 128; i++) {
			final int b = i % 12;
			final boolean a = b == 1 || b == 3 | b == 6 | b == 8 | b == 10;
			if (!a) {
				rect.setRect(cx, 0, nw, h);
				if (isKeyDown(i)) {
					g2.setColor(new Color(0.8f, 0.8f, 0.95f));
				} else {
					g2.setColor(Color.WHITE);
				}
				g2.fill(rect);
				g2.setColor(Color.BLACK);
				g2.draw(rect);

				if (hasFocus() && i >= getLowestAssignedKey()
						&& i - getLowestAssignedKey() < getMappedKeys().length()) {
					g2.setColor(Color.GRAY);
					final char k = getMappedKeys().charAt(i - getLowestAssignedKey());
					g2.drawString("" + k, cx + 2, h - 4);
				}
				cx += nw;
			}
		}
		cx = 0;
		final float black_note_width = nw * 0.7f;
		for (int i = 0; i < 128; i++) {
			final int b = i % 12;
			final boolean a = b == 1 || b == 3 | b == 6 | b == 8 | b == 10;
			if (!a) {
				cx += nw;
			} else {
				rect.setRect(cx - black_note_width / 2, 0, black_note_width, h * 4.0 / 7.0);
				if (isKeyDown(i)) {
					g2.setColor(new Color(0.8f, 0.8f, 0.95f));
				} else {
					g2.setColor(Color.BLACK);
				}
				g2.fill(rect);
				g2.setColor(Color.BLACK);
				g2.draw(rect);

				if (hasFocus() && i >= getLowestAssignedKey()
						&& i - getLowestAssignedKey() < getMappedKeys().length()) {
					g2.setColor(Color.LIGHT_GRAY);
					final char k = getMappedKeys().charAt(i - getLowestAssignedKey());
					g2.drawString("" + k, cx - black_note_width / 2 + 1, h * 4.0f / 7.0f - 3);
				}
			}
		}
	}
}
