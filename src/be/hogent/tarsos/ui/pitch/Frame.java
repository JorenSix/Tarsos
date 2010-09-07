/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.TextAreaHandler;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

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

		icon = createImageIcon("list_extensions.gif");
		JComponent logPanel = makeLogPanel();
		configurationPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Log", icon, logPanel, "Log");

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
			final String iconPath = "/be/hogent/tarsos/ui/resources/micro.png";
			image = ImageIO.read(this.getClass().getResource(iconPath));
			setIconImage(image);
		} catch (IOException e) {
			// fail silently, a lacking icon is not that bad
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
		final JPanel tarsosPanel = new JPanel(new BorderLayout());

		final ToneScalePanel panel = new ToneScalePanel(new ToneScaleHistogram());
		tarsosPanel.add(panel, BorderLayout.CENTER);

		List<Layer> layers = panel.getLayers();
		JPanel layersJPanel = new JPanel(new GridLayout(0, layers.size()));
		for (int i = 0; i < layers.size(); i++) {
			layersJPanel.add(layers.get(i).ui());
		}
		tarsosPanel.add(layersJPanel, BorderLayout.SOUTH);

		final JCheckBox checkbox = new JCheckBox();
		checkbox.setSelected(false);
		checkbox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean play = checkbox.getModel().isSelected();
				panel.setShouldPlay(play);

			}
		});
		FormLayout layout = new FormLayout("right:pref, 3dlu, min:grow");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.setRowGroupingEnabled(true);
		builder.append("Should play:", checkbox, true);
		tarsosPanel.add(builder.getPanel(), BorderLayout.NORTH);
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
