/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.TextAreaHandler;

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

	/**
	 * Logs messages.
	 */
	private static final Logger LOG = Logger.getLogger(Frame.class.getName());

	private final JTabbedPane tabbedPane;

	public Frame() {
		super("Tarsos");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		setMinimumSize(new Dimension(INITIAL_WIDTH, INITIAL_HEIGHT));
		setLocationRelativeTo(null);
		setProgramIcon();

		tabbedPane = new JTabbedPane();
		tabbedPane.setBorder(new EmptyBorder(10, 5, 5, 5));
		ImageIcon icon;

		icon = null; // createImageIcon("temp.gif");
		JComponent tarsosPanel = makeTarsosPanel();
		tarsosPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Analysis", icon, tarsosPanel, "Analysis");

		icon = null;
		JComponent configurationPanel = makeConfigurationPanel();
		configurationPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Config", icon, configurationPanel, "Configuration");

		icon = null;
		JComponent logPanel = makeLogPanel();
		configurationPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Log", icon, logPanel, "Log");

		icon = null;
		JComponent helpPanel = makeHelpanel();
		helpPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Help", icon, helpPanel, "Help?");

		add(new HeaderPanel(), BorderLayout.NORTH);

		add(tabbedPane, BorderLayout.CENTER);
	}

	/*
	 * private JComponent makeBrowserPanel() { return new JScrollPane(new
	 * BrowserPanel()); }
	 */

	private void setProgramIcon() {
		try {
			final BufferedImage image;
			final String iconPath = "/be/hogent/tarsos/ui/resources/tarsos_logo_small.png";
			image = ImageIO.read(this.getClass().getResource(iconPath));
			setIconImage(image);
		} catch (IOException e) {
			// fail silently, a lacking icon is not that bad
			LOG.warning("Failed to set program icon");
		}
	}

	private JComponent makeLogPanel() {
		JTextArea output = new JTextArea();
		output.setFont(new Font("Monospaced", Font.PLAIN, 12));
		output.setAutoscrolls(true);
		output.setEditable(false);
		TextAreaHandler.setupLoggerHandler(output);
		return new JScrollPane(output);
	}

	private JComponent makeTarsosPanel() {
		AnalysisPanel analysisPanel = new AnalysisPanel();
		analysisPanel.addAudioFileChangedListener(new AudioFileChangedListener() {
			public void audioFileChanged(AudioFile newAudioFile) {
				String newTitle = String.format("Analysis - %s", newAudioFile.basename());
				tabbedPane.setTitleAt(0, newTitle);
			}
		});
		return analysisPanel;
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
		URL imgURL = Frame.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			LOG.warning(String.format("Unable to find icon: %s", imgURL));
			return null;
		}
	}

	protected JComponent makeConfigurationPanel() {
		return new JScrollPane(new ConfigurationPanel());
	}
}
