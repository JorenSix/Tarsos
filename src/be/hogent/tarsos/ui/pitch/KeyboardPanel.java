package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import be.hogent.tarsos.ui.virtualkeyboard.VirtualKeyboard;
import be.hogent.tarsos.util.ScalaFile;

public final class KeyboardPanel extends JPanel implements ScaleChangedListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6644107804492842166L;

	private final VirtualKeyboard keyboard;

	private static final Logger LOG = Logger.getLogger(KeyboardPanel.class.getName());

	public KeyboardPanel() {
		super(new BorderLayout());
		// Create a new keyboard and tune it using the default western tuning.
		final double[] scale = ScalaFile.westernTuning().getPitches();
		keyboard = VirtualKeyboard.createVirtualKeyboard(scale.length);
		keyboard.connectToTunedSynth(scale);
		add(keyboard, BorderLayout.CENTER);
		setMaximumSize(new Dimension(800, 70));
		setMinimumSize(new Dimension(200, 50));
		setPreferredSize(new Dimension(200, 50));
	}

	public void scaleChanged(final double[] newScale, final boolean isChanging) {
		if (!isChanging) {
			// tuning takes some time, run it in a separate thread to keep the
			// UI responsive.
			Runnable tuneRunnable = new Runnable() {
				public void run() {
					keyboard.connectToTunedSynth(newScale);
					LOG.log(Level.FINE, String.format("Tuned the keboard using a new scale %s",
							Arrays.toString(newScale)));
				}
			};
			new Thread(tuneRunnable, "Piano tuner.").start();
		}
	}
}
