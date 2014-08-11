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
import java.awt.geom.Rectangle2D;

/**
 * Uses code from the gervill package licensed under the GPL with the classpath
 * exception. <a
 * href="https://gervill.dev.java.net/source/browse/gervill/src.demos/"> Gervill
 * source code</a>
 * 
 * @author Joren Six
 */
public final class VirtualKeyboard19 extends VirtualKeyboard {

	private static final long serialVersionUID = 3155583325975723313L;

	/**
     */
	public VirtualKeyboard19() {
		super(19, 19 * 5); // 19*5 = 95
	}

	
	public int getMidiNote(final int x, final int y) {
		final int w = getWidth();
		final int h = getHeight();
		final float nw = w / 47f;

		final int wn = (int) (x / nw);
		final int oct = wn / 7;
		int n = oct * 19;
		final int nb = wn % 7;
		if (nb == 1) {
			n += 3;
		}
		if (nb == 2) {
			n += 6;
		}
		if (nb == 3) {
			n += 8;
		}
		if (nb == 4) {
			n += 11;
		}
		if (nb == 5) {
			n += 14;
		}
		if (nb == 6) {
			n += 17;
		}
		if (y < h * 4.0 / 7.0) {
			final int xb = x - (int) (oct * 7 * nw);
			float cx = 0;
			final float black_note_width = nw * 0.7f;
			for (int b = 0; b < 19; b++) {
				final boolean a = !(b == 0 || b == 3 || b == 6 || b == 8 || b == 11 || b == 14 || b == 17);
				if (!a) {
					cx += nw;
				} else {
					if (b == 7 || b == 18) {
						final float cstart = cx - black_note_width / 2;
						final float cend = cstart + black_note_width;
						if (xb > cstart && xb < cend) {
							return oct * 19 + b;
						}
					} else {
						final float cstart = cx - black_note_width / 2;
						final float cend = cstart + black_note_width;
						if (xb > cstart && xb < cend) {
							if (y > h * 4.0 / 7.0 / 2.0) {
								return oct * 19 + b + 1;
							} else {
								return oct * 19 + b;
							}
						}
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

	
	public void paint(final Graphics g) {
		super.paint(g);
		final Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		final int w = getWidth();
		final int h = getHeight();

		final float nw = w / 47f;
		float cx = 0;
		final Rectangle2D rect = new Rectangle2D.Double();
		for (int i = 0; i < 128; i++) {
			final int b = i % 19;
			final boolean a = !(b == 0 || b == 3 || b == 6 || b == 8 || b == 11 || b == 14 || b == 17);
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

				if (hasFocus() && i >= getLowestAssignedKey()) {
					if (i - getLowestAssignedKey() < getMappedKeys().length()) {
						g2.setColor(Color.GRAY);
						final char k = getMappedKeys().charAt(i - getLowestAssignedKey());
						g2.drawString("" + k, cx + 2, h - 4);
					}
				}

				cx += nw;
			}
		}
		cx = 0;
		final float black_note_width = nw * 0.7f;
		int black_note_pos = 0;
		for (int i = 0; i < 128; i++) {
			final int b = i % 19;
			final boolean a = !(b == 0 || b == 3 || b == 6 || b == 8 || b == 11 || b == 14 || b == 17);
			if (!a) {
				cx += nw;
				black_note_pos = 0;
			} else {
				// 7,18

				if (b == 7 || b == 18) {
					rect.setRect(cx - black_note_width / 2, 0, black_note_width, h * 4.0 / 7.0);
					if (isKeyDown(i)) {
						g2.setColor(new Color(0.8f, 0.8f, 0.95f));
					} else {
						g2.setColor(Color.BLACK);
					}
					g2.fill(rect);
					g2.setColor(Color.BLACK);
					g2.draw(rect);

					if (hasFocus() && i >= getLowestAssignedKey()) {
						if (i - getLowestAssignedKey() < getMappedKeys().length()) {
							g2.setColor(Color.LIGHT_GRAY);
							final char k = getMappedKeys().charAt(i - getLowestAssignedKey());
							g2.drawString("" + k, cx - black_note_width / 2 + 1, h * 4.0f / 7.0f - 3);
						}
					}
				} else {
					if (black_note_pos == 0) {
						rect.setRect(cx - black_note_width / 2, 0, black_note_width, h * 4.0 / 7.0 / 2 - 2);
						if (isKeyDown(i)) {
							g2.setColor(new Color(0.8f, 0.8f, 0.95f));
						} else {
							g2.setColor(Color.BLACK);
						}
						g2.fill(rect);
						g2.setColor(Color.BLACK);
						g2.draw(rect);

						if (hasFocus() && i >= getLowestAssignedKey()) {
							if (i - getLowestAssignedKey() < getMappedKeys().length()) {
								g2.setColor(Color.LIGHT_GRAY);
								final char k = getMappedKeys().charAt(i - getLowestAssignedKey());
								g2.drawString("" + k, cx - black_note_width / 2 + 1, h * 2.0f / 7.0f - 5);
							}
						}

					}

					if (black_note_pos == 1) {
						rect.setRect(cx - black_note_width / 2, h * 4.0 / 7.0 / 2 + 1, black_note_width, h
								* 4.0 / 7.0 / 2 - 1);
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
				black_note_pos++;
			}
		}
	}
}
