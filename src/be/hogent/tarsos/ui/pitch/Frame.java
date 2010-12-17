/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.noos.xing.mydoggy.Content;
import org.noos.xing.mydoggy.ContentManager;
import org.noos.xing.mydoggy.MultiSplitConstraint;
import org.noos.xing.mydoggy.MultiSplitContentManagerUI;
import org.noos.xing.mydoggy.TabbedContentManagerUI;
import org.noos.xing.mydoggy.TabbedContentUI;
import org.noos.xing.mydoggy.ToolWindow;
import org.noos.xing.mydoggy.ToolWindowAnchor;
import org.noos.xing.mydoggy.ToolWindowManager;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;
import org.noos.xing.mydoggy.plaf.ui.content.MyDoggyMultiSplitContentManagerUI;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.ui.WaveForm;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileDrop;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.JLabelHandler;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.TextAreaHandler;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

/**
 * @author Joren Six
 */
public final class Frame extends JFrame implements ScaleChangedListener, AnnotationListener {
	/**
	 * Default height.
	 */
	private static final int INITIAL_HEIGHT = 600;
	/**
	 * Default width.
	 */
	private static final int INITIAL_WIDTH = 800;
	/**
     */
	private static final long serialVersionUID = -8095965296377515567L;

	/**
	 * Logs messages.
	 */
	private static final Logger LOG = Logger.getLogger(Frame.class.getName());

	private static Frame instance;

	public static Frame getInstance() {
		if (instance == null) {
			instance = new Frame();
		}
		return instance;
	}

	private final ToolWindowManager toolWindowManager;

	private AudioFile audioFile;

	private double[] scale;

	/**
	 * Is there processing going on?
	 */
	private synchronized void setWaitState(final boolean isWaiting) {
		Component glassPane = this.getGlassPane();
		if (isWaiting) {
			glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			glassPane.setVisible(true);
		} else {
			glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			glassPane.setVisible(false);
		}
	}

	private Frame() {
		super("Tarsos");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		// setMinimumSize(new Dimension( , INITIAL_HEIGHT));
		// center
		setLocationRelativeTo(null);
		setProgramIcon();

		// initialize listener lists
		scaleChangedListeners = new ArrayList<ScaleChangedListener>();
		audioFileChangedListeners = new ArrayList<AudioFileChangedListener>();

		// react to drag and drop
		addFileDropListener();

		// all the components to add to the frame
		JComponent configurationPanel = makeConfigurationPanel();
		JComponent logPanel = makeLogPanel();
		JComponent helpPanel = makeHelpanel();
		JComponent headerPanel = new HeaderPanel();
		JComponent statusBar = makeStatusBar();

		final ToneScalePane toneScalePane = new ToneScalePane(new ToneScaleHistogram(), this);
		final ToneScalePanel ambitusPanel = new ToneScalePanel(new AmbitusHistogram(), this);
		final PitchContour pitchContourPanel = new PitchContour();
		final PitchContour regression = new PitchContour();
		final IntervalTable intervalTable = new IntervalTable();
		final WaveForm waveForm = new WaveForm();
		final ControlPanel controlPanel = new ControlPanel(waveForm);
		final KeyboardPanel keyboardPanel = new KeyboardPanel();

		// The annotation publisher is not a ui element.
		final AnnotationPublisher annotationPublisher = AnnotationPublisher.getInstance();

		AudioFileBrowserPanel browser = new AudioFileBrowserPanel(new GridLayout(0, 2));
		browser.setBackground(Color.WHITE);

		// patch the scale changed listeners
		addScaleChangedListener(toneScalePane);
		addScaleChangedListener(ambitusPanel);
		addScaleChangedListener(pitchContourPanel);
		addScaleChangedListener(regression);
		addScaleChangedListener(intervalTable);
		addScaleChangedListener(keyboardPanel);

		// Patch the audio file changed listeners.
		addAudioFileChangedListener(annotationPublisher);
		addAudioFileChangedListener(toneScalePane);
		addAudioFileChangedListener(ambitusPanel);
		addAudioFileChangedListener(pitchContourPanel);
		addAudioFileChangedListener(regression);
		addAudioFileChangedListener(waveForm);
		addAudioFileChangedListener(controlPanel);
		addAudioFileChangedListener(browser);

		// Patch the annotation listeners
		annotationPublisher.addListener(toneScalePane);
		annotationPublisher.addListener(ambitusPanel);
		annotationPublisher.addListener(pitchContourPanel);
		annotationPublisher.addListener(controlPanel);
		annotationPublisher.addListener(this);

		// initialize content and tool window manager of the 'mydoggy'
		// framework.
		// Create a new instance of MyDoggyToolWindowManager passing the frame.
		MyDoggyToolWindowManager windowManager = new MyDoggyToolWindowManager();
		toolWindowManager = windowManager;
		ContentManager contentManager = toolWindowManager.getContentManager();
		MultiSplitContentManagerUI contentManagerUI = new MyDoggyMultiSplitContentManagerUI();
		contentManager.setContentManagerUI(contentManagerUI);
		contentManagerUI.setShowAlwaysTab(true);
		contentManagerUI.setTabPlacement(TabbedContentManagerUI.TabPlacement.TOP);

		// add the components to the frame
		add(headerPanel, BorderLayout.NORTH);
		add(windowManager, BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);

		// add components to the window manager.

		Content content = contentManager.addContent("Tone scale", "Tone scale", null, toneScalePane);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(false);

		MultiSplitConstraint constraint = new MultiSplitConstraint(content, 1);
		content = contentManager.addContent("Configuration", "Configuration", null, configurationPanel, null,
				constraint);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(false);

		constraint = new MultiSplitConstraint(content, 2);
		content = contentManager.addContent("Ambitus", "Ambitus", null, ambitusPanel, null, constraint);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);

		constraint = new MultiSplitConstraint(content, 3);
		content = contentManager.addContent("Annotations", "Annotations", null, pitchContourPanel, null,
				constraint);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);

		content = contentManager.addContent("Interval table", "Interval table", null, intervalTable);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);

		content = contentManager.addContent("Controls", "Controls", null, controlPanel);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(false);

		constraint = new MultiSplitConstraint(content, 1);
		content = contentManager.addContent("Keyboard", "Keyboard", null, keyboardPanel, null, constraint);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);

		toolWindowManager.registerToolWindow("Help", "Help", null, helpPanel, ToolWindowAnchor.RIGHT);
		toolWindowManager.registerToolWindow("Browser", "Browser", null, browser, ToolWindowAnchor.RIGHT);
		toolWindowManager.registerToolWindow("Logging", "Logging", null, logPanel, ToolWindowAnchor.BOTTOM);

		// Make all tools available
		for (ToolWindow window : toolWindowManager.getToolWindows()) {
			window.setAvailable(true);
		}
	}

	private JComponent makeStatusBar() {
		JLabel statusBarLabel = new JLabel();
		statusBarLabel.setForeground(Color.GRAY);
		JLabelHandler.setupLoggerHandler(statusBarLabel);
		return statusBarLabel;
	}

	private void setDefaultTabbedContentOptions(final Content content) {
		TabbedContentUI tabbedContent = (TabbedContentUI) content.getContentUI();
		tabbedContent.setCloseable(false);
		tabbedContent.setDetachable(true);
		tabbedContent.setTransparentMode(false);
		tabbedContent.setMinimizable(true);
	}

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
		output.setLineWrap(true);
		TextAreaHandler.setupLoggerHandler(output);
		output.setBorder(new EmptyBorder(5, 2, 5, 2));
		JScrollPane scrollPane = new JScrollPane(output);
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		return scrollPane;
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
		JScrollPane configurationPane = new JScrollPane(new ConfigurationPanel());
		configurationPane.getVerticalScrollBar().setUnitIncrement(16);
		return configurationPane;
	}

	private void addFileDropListener() {

		new FileDrop(this, new FileDrop.Listener() {
			public void filesDropped(final java.io.File[] files) {
				if (files.length != 1) {
					LOG.log(Level.WARNING, String.format(
							"Dropped %s files. For the moment only ONE file should be dropped", files.length));
				}
				final File droppedFile = files[0];
				if (droppedFile.getName().endsWith(".scl")) {
					ScalaFile scalaFile = new ScalaFile(droppedFile.getAbsolutePath());
					scaleChanged(scalaFile.getPitches(), false);
				} else if (FileUtils.isAudioFile(droppedFile)) {
					final AudioFile newAudioFile = new AudioFile(droppedFile.getAbsolutePath());
					setAudioFile(newAudioFile);
				}
				LOG.fine(String.format("Dropped %s .", droppedFile.getAbsolutePath()));
			}
		});
	}

	/* -- Audio file publish subscribe -- */
	private final List<AudioFileChangedListener> audioFileChangedListeners;

	private void setAudioFile(final AudioFile newAudioFile) {
		this.audioFile = newAudioFile;
		// set a title
		notifyAudioFileChangedListeners();
	}

	private AudioFile getAudioFile() {
		return audioFile;
	}

	private void notifyAudioFileChangedListeners() {
		LOG.log(Level.FINE,
				String.format("Notify listeners of audio file change: %s .", getAudioFile().basename()));
		for (AudioFileChangedListener listener : audioFileChangedListeners) {
			listener.audioFileChanged(getAudioFile());
		}
	}

	public synchronized void addAudioFileChangedListener(AudioFileChangedListener listener) {
		audioFileChangedListeners.add(listener);
	}

	/* -- Scale publish subscribe -- */
	private final List<ScaleChangedListener> scaleChangedListeners;

	private void notifyScaleChangedListeners(boolean isChanging) {
		LOG.log(Level.FINE,
				String.format("Notify listeners of scale change %s \t %s.", isChanging,
						Arrays.toString(getScale())));

		for (ScaleChangedListener listener : scaleChangedListeners) {
			listener.scaleChanged(getScale(), isChanging);
		}
	}

	public synchronized void addScaleChangedListener(ScaleChangedListener listener) {
		scaleChangedListeners.add(listener);
	}

	public void scaleChanged(double[] newScale, boolean isChanging) {
		scale = newScale;
		notifyScaleChangedListeners(isChanging);
	}

	private double[] getScale() {
		return scale;
	}

	public void addAnnotations(List<Annotation> annotations) {
		// NO OP
	}

	public void clearAnnotations() {
		// NO OP
	}

	public void extractionStarted() {
		// Show waiting cursor.
		setWaitState(true);
	}

	public void extractionFinished() {
		// Show normal cursor again.
		setWaitState(false);
		LOG.fine("Wait state disabled.");
	}

	public void addAnnotation(Annotation annotation) {
		// NO OP

	}

	public void annotationsAdded() {
		// TODO Auto-generated method stub

	}
}
