package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileDrop;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

public class BrowserPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2721032191436893433L;

	private final JPanel scalePanel;
	private final JComboBox comboBox;

	public BrowserPanel() {
		super(new BorderLayout());

		comboBox = new JComboBox(PitchDetectionMode.values());

		JPanel controlPanel = new JPanel();
		controlPanel.add(comboBox);

		scalePanel = new JPanel(new GridLayout(0, 1));

		this.add(controlPanel, BorderLayout.NORTH);
		this.add(scalePanel, BorderLayout.CENTER);

		new FileDrop(this, new FileDrop.Listener() {
			@Override
			public void filesDropped(final java.io.File[] files) {
				for (final File droppedFile : files) {
					if (FileUtils.isAudioFile(droppedFile)) {
						addAudioFile(droppedFile.getAbsolutePath());
					}
				}
			}
		});
	}

	private void addAudioFile(final String fileName) {
		Runnable task = new Runnable() {

			@Override
			public void run() {
				final AudioFile audioFile = new AudioFile(fileName);
				PitchDetectionMode mode = (PitchDetectionMode) comboBox.getSelectedItem();
				final PitchDetector pitchDetector = mode.getPitchDetector(audioFile);
				pitchDetector.executePitchDetection();
				final List<Sample> samples = pitchDetector.getSamples();
				final AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
				final ToneScaleHistogram toneScaleHisto = ambitusHistogram.toneScaleHistogram();
				JComponent component = new ToneScalePanel(toneScaleHisto);
				JPanel panel = new JPanel(new BorderLayout());
				panel.setSize(128, 128);
				panel.setBorder(new EmptyBorder(5, 5, 5, 5));
				panel.setMinimumSize(new Dimension(128, 128));
				panel.add(component, BorderLayout.CENTER);

				scalePanel.add(panel);
				scalePanel.invalidate();
			}
		};
		new Thread(task).start();

	}
}
