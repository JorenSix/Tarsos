package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileDrop;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.PitchHistogram;
import be.hogent.tarsos.util.histogram.PitchClassHistogram;

public class BrowserPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2721032191436893433L;

	private final JPanel scalePanel;

	public BrowserPanel() {
		super(new BorderLayout());

		scalePanel = new JPanel(new GridLayout(0, 1));
		this.add(scalePanel, BorderLayout.CENTER);

		new FileDrop(this, new FileDrop.Listener() {
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

			public void run() {
				AudioFile audioFile;
				try {
					audioFile = new AudioFile(fileName);
					PitchDetectionMode mode = Configuration
							.getPitchDetectionMode(ConfKey.pitch_tracker_current);
					final PitchDetector pitchDetector = mode.getPitchDetector(audioFile);
					pitchDetector.executePitchDetection();
					final List<Annotation> samples = pitchDetector.getAnnotations();
					final PitchHistogram pitchHistogram = Annotation.pitchHistogram(samples);
					final PitchClassHistogram toneScaleHisto = pitchHistogram.pitchClassHistogram();
					JComponent component = new PitchClassHistogramLayer(toneScaleHisto, null);
					JPanel panel = new JPanel(new BorderLayout());
					panel.setSize(128, 128);
					panel.setBorder(new EmptyBorder(5, 5, 5, 5));
					panel.setMinimumSize(new Dimension(128, 128));
					panel.add(component, BorderLayout.CENTER);

					scalePanel.add(panel);
					scalePanel.invalidate();
					scalePanel.repaint();
				} catch (EncoderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		};
		new Thread(task).start();

	}
}
