package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.sampled.pitch.Sample;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

import com.jgoodies.forms.builder.ButtonBarBuilder2;

public class ControlPanel extends JPanel implements AudioFileChangedListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5542665958725558290L;

	private AudioFileSampleProcessor processorThread;

	private final List<SampleHandler> handlers;

	public ControlPanel() {
		super(new BorderLayout());
		ButtonBarBuilder2 builder = ButtonBarBuilder2.createLeftToRightBuilder();
		JButton playButton = new JButton("Play");
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (processorThread != null) {
					processorThread.start();
				}
			}
		});
		JButton pauseButton = new JButton("pause");
		JButton stopButton = new JButton("stop");
		JSlider speedSlider = new JSlider(0, 200);
		speedSlider.setValue(10);
		speedSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				final JSlider source = (JSlider) e.getSource();
				final double value = source.getValue();
				if (processorThread != null) {
					processorThread.setSpeedFactor(value / 100);
				}
			}

		});
		builder.addButton(playButton);
		builder.addButton(pauseButton);
		builder.addButton(stopButton);
		builder.addButton(speedSlider);
		this.add(builder.getPanel(), BorderLayout.CENTER);
		processorThread = null;
		handlers = new ArrayList<SampleHandler>();
	}

	public final class AudioFileSampleProcessor extends Thread {
		private double speedFactor;
		private final AudioFile file;

		private final List<SampleHandler> handlers;
		private boolean running;

		AudioFileSampleProcessor(final AudioFile audioFile, final List<SampleHandler> someHandlers) {
			super("Sample data publisher.");
			file = audioFile;
			speedFactor = 10.0;
			this.handlers = someHandlers;
			running = false;
		}

		@Override
		public void run() {
			running = true;
			PitchDetectionMode mode = Configuration.getPitchDetectionMode(ConfKey.pitch_tracker_current);
			final PitchDetector pitchDetector = mode.getPitchDetector(file);
			pitchDetector.executePitchDetection();
			final List<Sample> samples = pitchDetector.getSamples();
			long previousSample = 0L;
			for (Sample sample : samples) {
				if (!running) {
					break;
				}
				List<Double> pitchList = sample.getPitchesIn(PitchUnit.HERTZ);
				if (pitchList.size() > 0) {
					try {
						for (SampleHandler handler : handlers) {
							handler.addSample(sample);
						}
						if (getSpeedFactor() == 0.0) {
							while (getSpeedFactor() == 0.0) {
								Thread.sleep(100);
							}
						} else {
							long sleepTime = (long) ((sample.getStart() - previousSample) / getSpeedFactor());
							Thread.sleep(sleepTime);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					previousSample = sample.getStart();
				}
			}
		}

		void setSpeedFactor(double speedFactor) {
			this.speedFactor = speedFactor;
		}

		double getSpeedFactor() {
			return speedFactor;
		}

		void stopAnalysis() {
			running = false;
		}
	}

	public void addHandler(SampleHandler handler) {
		handlers.add(handler);
	}

	@Override
	public void audioFileChanged(AudioFile newAudioFile) {
		if (processorThread != null && processorThread.isAlive()) {
			processorThread.stopAnalysis();
		}
		processorThread = new AudioFileSampleProcessor(newAudioFile, handlers);
	}

	public interface SampleHandler {
		public void addSample(Sample sample);

		public void removeSample(Sample sample);
	}

}
