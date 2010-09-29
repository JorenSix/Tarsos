package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileDrop;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

public final class AnalysisPanel extends JPanel implements ScaleChangedListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4447342085724047798L;

	private static final Logger LOG = Logger.getLogger(AnalysisPanel.class.getName());

	private AudioFile audioFile;
	private double[] scale;

	public AnalysisPanel() {
		super(new BorderLayout());
		scaleChangedListeners = new ArrayList<ScaleChangedListener>();
		audioFileChangedListeners = new ArrayList<AudioFileChangedListener>();

		addFileDropListener();

		final ToneScalePanel toneScalePanel = new ToneScalePanel(new ToneScaleHistogram(), this);
		addAudioFileChangedListener(toneScalePanel);
		addScaleChangedListener(toneScalePanel);

		final ToneScalePanel ambitusPanel = new ToneScalePanel(new AmbitusHistogram(), this);
		addAudioFileChangedListener(ambitusPanel);
		addScaleChangedListener(ambitusPanel);

		final PitchContour pitchContourPanel = new PitchContour();
		addAudioFileChangedListener(pitchContourPanel);
		addScaleChangedListener(pitchContourPanel);

		final IntervalTable intervalTable = new IntervalTable();
		addScaleChangedListener(intervalTable);

		final ControlPanel controlPanel = new ControlPanel();
		addAudioFileChangedListener(controlPanel);
		controlPanel.addHandler(toneScalePanel);
		controlPanel.addHandler(ambitusPanel);
		controlPanel.addHandler(pitchContourPanel);

		final KeyboardPanel keyboardPanel = new KeyboardPanel();
		addScaleChangedListener(keyboardPanel);

		final JPanel dynamicPanel = new JPanel(new GridLayout(0, 1));
		dynamicPanel.add(toneScalePanel);
		dynamicPanel.setInheritsPopupMenu(true);

		List<Layer> layers = toneScalePanel.getLayers();
		JPanel layersJPanel = new JPanel(new GridLayout(0, 1));
		for (int i = 0; i < layers.size(); i++) {
			Component comp = layers.get(i).ui();
			layersJPanel.add(comp);
		}
		layersJPanel.setInheritsPopupMenu(true);

		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(
				layersJPanel), dynamicPanel);
		splitPane.setOneTouchExpandable(true);

		final JCheckBoxMenuItem toneScaleCheckBox = new JCheckBoxMenuItem("Show tone scale");
		toneScaleCheckBox.getModel().setSelected(true);
		toneScaleCheckBox.addActionListener(new ShowPanelActionListener(dynamicPanel, toneScalePanel,
				splitPane));

		final JCheckBoxMenuItem ambitusCheckBox = new JCheckBoxMenuItem("Show ambitus");
		ambitusCheckBox.addActionListener(new ShowPanelActionListener(dynamicPanel, ambitusPanel, splitPane));

		final JCheckBoxMenuItem keyboardCheckBox = new JCheckBoxMenuItem("Show keyboard");
		keyboardCheckBox
				.addActionListener(new ShowPanelActionListener(dynamicPanel, keyboardPanel, splitPane));

		final JCheckBoxMenuItem intervalTableCheckBox = new JCheckBoxMenuItem("Show interval Table");
		intervalTableCheckBox.addActionListener(new ShowPanelActionListener(dynamicPanel, intervalTable,
				splitPane));

		final JCheckBoxMenuItem pitchContourCheckBox = new JCheckBoxMenuItem("Show annotations");
		pitchContourCheckBox.addActionListener(new ShowPanelActionListener(dynamicPanel, pitchContourPanel,
				splitPane));

		final JCheckBoxMenuItem controlsCheckBox = new JCheckBoxMenuItem("Controls");
		controlsCheckBox
				.addActionListener(new ShowPanelActionListener(dynamicPanel, controlPanel, splitPane));

		final JPopupMenu popup = new JPopupMenu();
		popup.add(toneScaleCheckBox);
		popup.add(ambitusCheckBox);
		popup.add(keyboardCheckBox);
		popup.add(pitchContourCheckBox);
		popup.add(controlsCheckBox);
		popup.add(intervalTableCheckBox);
		setComponentPopupMenu(popup);

		splitPane.setInheritsPopupMenu(true);

		add(splitPane, BorderLayout.CENTER);

	}

	private class ShowPanelActionListener implements ActionListener {
		private final JComponent parent;
		private final JComponent panel;
		private final JComponent toValidate;

		public ShowPanelActionListener(final JComponent parentComponent, final JComponent panelToShow,
				final JComponent componentToValidate) {
			this.parent = parentComponent;
			this.panel = panelToShow;
			this.toValidate = componentToValidate;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final JCheckBoxMenuItem checkBox = (JCheckBoxMenuItem) e.getSource();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (checkBox.getModel().isSelected()) {
						parent.add(panel);
					} else {
						parent.remove(panel);
					}
					toValidate.validate();
				}
			});
		}
	}

	private void addFileDropListener() {
		new FileDrop(this, new FileDrop.Listener() {
			@Override
			public void filesDropped(final java.io.File[] files) {
				if (files.length != 0) {
					LOG.log(Level.WARNING, "Dropped %s files. For the moment only 1 file should be dropped",
							files.length);
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
		notifyAudioFileChangedListeners();
	}

	private AudioFile getAudioFile() {
		return audioFile;
	}

	private void notifyAudioFileChangedListeners() {
		LOG.log(Level.FINE, "Notify listeners of audio file change: %s .", getAudioFile().basename());
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

	@Override
	public void scaleChanged(double[] newScale, boolean isChanging) {
		scale = newScale;
		notifyScaleChangedListeners(isChanging);
	}

	private double[] getScale() {
		return scale;
	}

}
