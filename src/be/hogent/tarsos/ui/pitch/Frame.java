/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

/**
 * @author Joren Six
 */
public class Frame extends JFrame {
	/**
	 * Default height.
	 */
	private static final int INITIAL_HEIGHT = 480;
	/**
	 * Default width.
	 */
	private static final int INITIAL_WIDTH = 640;
	/**
     */
	private static final long serialVersionUID = -8095965296377515567L;

	public Frame() {
		super("Tarsos");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		setMinimumSize(new Dimension(INITIAL_WIDTH, INITIAL_HEIGHT));
		setLocationRelativeTo(null);
		setProgramIcon();

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBorder(new EmptyBorder(10, 5, 5, 5));
		ImageIcon icon;

		icon = createImageIcon("temp.gif");
		JComponent tarsosPanel = makeTarsosPanel();
		tarsosPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Analysis", icon, tarsosPanel, "Analysis");

		icon = createImageIcon("list_extensions.gif");
		JComponent browserPanel = makeBrowserPanel();
		browserPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Browser", icon, browserPanel, "Browser");

		icon = createImageIcon("list_extensions.gif");
		JComponent configurationPanel = makeConfigurationPanel();
		configurationPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Config", icon, configurationPanel, "Configuration");

		icon = createImageIcon("icon_info.gif");
		JComponent helpPanel = makeHelpanel();
		helpPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Help", icon, helpPanel, "Help?");

		add(tabbedPane, BorderLayout.CENTER);
	}

	private JComponent makeBrowserPanel() {
		return new JScrollPane(new BrowserPanel());
	}

	private void setProgramIcon() {
		try {
			final BufferedImage image;
			final String iconPath = "/be/hogent/tarsos/ui/resources/sound.png";
			image = ImageIO.read(this.getClass().getResource(iconPath));
			setIconImage(image);
		} catch (IOException e) {
			// fail silently, lacking icon
		}

	}

	private JComponent makeTarsosPanel() {
		final JPanel tarsosPanel = new JPanel(new BorderLayout());

		final ToneScalePanel panel = new ToneScalePanel(new ToneScaleHistogram());
		final JSlider slider = new JSlider(0, 100);
		slider.setValue(0);
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final JSlider source = (JSlider) e.getSource();
				// if (!source.getValueIsAdjusting()) {
				final double value = source.getValue();
				// panel.getHistogram().gaussianSmooth(value);
				// panel.draw(0, 0);
				final List<Peak> peaks = PeakDetector.detect(panel.getHistogram(), (int) value, 0.5);
				final double[] peaksInCents = new double[peaks.size()];
				int i = 0;
				for (final Peak peak : peaks) {
					peaksInCents[i++] = peak.getPosition();
				}
				panel.setReferenceScale(peaksInCents);
			}
		});

		tarsosPanel.add(slider, BorderLayout.NORTH);
		tarsosPanel.add(panel, BorderLayout.CENTER);

		List<Layer> layers = panel.getLayers();
		JPanel layersJPanel = new JPanel(new GridLayout(layers.size(), 1));
		for (int i = 0; i < layers.size(); i++) {
			layersJPanel.add(layers.get(i).ui());
		}
		tarsosPanel.add(layersJPanel, BorderLayout.SOUTH);
		return tarsosPanel;
	}

	private JComponent makeHelpanel() {
		String contents = FileUtils.readFileFromJar("/be/hogent/tarsos/ui/resources/help.html");
		JEditorPane helpLabel = new JEditorPane();
		helpLabel.setEditable(false);
		helpLabel.setContentType("text/html");
		helpLabel.setPreferredSize(new Dimension(500, 300));
		helpLabel.setText(contents);
		helpLabel.setCaretPosition(0);
		return new JScrollPane(helpLabel);
	}

	public static ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = Frame.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			// log.severe("Couldn't find file: " + path);
			return null;
		}
	}

	protected JComponent makeConfigurationPanel() {
		return new JScrollPane(new ConfigurationPanel());
	}

	public static void main(final String... strings) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					// log.severe("Error setting native L&F: " + e);
				}
				final Frame frame = new Frame();
				frame.setVisible(true);
			}
		});
	}
}
