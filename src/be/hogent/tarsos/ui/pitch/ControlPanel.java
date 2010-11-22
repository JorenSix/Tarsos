package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.hogent.tarsos.sampled.BlockingAudioPlayer;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.ui.WaveForm;
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
	private AudioPlayingThread playerThread;

	private final List<SampleHandler> handlers;
	private final JButton playButton;
	private final JButton stopButton;
	private final JSpinner speedSpinner;
	private AudioFile audioFile;

	public ControlPanel(final WaveForm waveForm) {
		super(new BorderLayout());
		ButtonBarBuilder2 builder = ButtonBarBuilder2.createLeftToRightBuilder();
		playButton = new JButton("Play");
		playButton.setEnabled(false);
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (processorThread.isPaused()) {
					processorThread.resumeAnalysis();
					playerThread.resumePlaying();
					((JButton) e.getSource()).setText("Pause");
				} else {
					processorThread.pauzeAnalysis();
					((JButton) e.getSource()).setText("Play");
					playerThread.pauzePlaying();
				}
			}
		});

		stopButton = new JButton("Stop");
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent arg0) {
				if (processorThread != null) {
					processorThread.stopAnalysis();
				}
				if (playerThread != null) {
					playerThread.stopPlaying();
				}
			}
		});

		SpinnerModel model = new SpinnerNumberModel(1.0, 0.0, 20.0, 0.5);
		speedSpinner = new JSpinner(model);
		speedSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent e) {
				final JSpinner source = (JSpinner) e.getSource();
				final double value = (Double) source.getValue();
				if (processorThread != null) {
					processorThread.setSpeedFactor(value);
				}
				if (playerThread != null) {
					if (value == 1.0) {
						playerThread = new AudioPlayingThread(audioFile, processorThread
								.getCurrentSampleStart(), processorThread);
						playerThread.start();
					} else {
						playerThread.stopPlaying();
					}
				}
			}
		});
		speedSpinner.setEnabled(false);

		builder.addButton(playButton);
		builder.addButton(stopButton);
		builder.addButton(speedSpinner);

		this.add(builder.getPanel(), BorderLayout.EAST);
		this.add(waveForm, BorderLayout.CENTER);
		waveForm.setSize(getPreferredSize());
		processorThread = null;
		handlers = new ArrayList<SampleHandler>();

		setMaximumSize(new Dimension(1500, 45));
		setMinimumSize(new Dimension(200, 25));
		setPreferredSize(new Dimension(200, 30));
	}

	public final class AudioFileSampleProcessor extends Thread {
		private double speedFactor;
		private final AudioFile file;

		private final List<SampleHandler> handlers;
		private boolean running;
		private boolean isPaused;
		private double currentSampleStart;
		private boolean extractingPitch;

		AudioFileSampleProcessor(final AudioFile audioFile, final List<SampleHandler> someHandlers) {
			super("Annotation data publisher.");
			file = audioFile;
			speedFactor = 10.0;
			this.handlers = someHandlers;
			running = false;
			isPaused = false;
			extractingPitch = true;
		}

		public boolean isExtractingPitch() {
			return extractingPitch;
		}

		public void pauzeAnalysis() {
			isPaused = true;
		}

		public void resumeAnalysis() {
			isPaused = false;
		}

		public boolean isPaused() {
			return isPaused;
		}

		@Override
		public void run() {
			running = true;
			PitchDetectionMode mode = Configuration.getPitchDetectionMode(ConfKey.pitch_tracker_current);
			final PitchDetector pitchDetector = mode.getPitchDetector(file);
			Frame.getInstance().setWaitState(true);
			pitchDetector.executePitchDetection();
			Frame.getInstance().setWaitState(false);
			final List<Annotation> samples = pitchDetector.getAnnotations();
			extractingPitch = false;
			double previousSample = 0.0;
			for (Annotation sample : samples) {
				if (!running) {
					break;
				}
				while (isPaused) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// sleep interrupted
					}
				}
				currentSampleStart = sample.getStart();

				for (SampleHandler handler : handlers) {
					handler.addSample(sample);
				}

				try {
					long sleepTime = (long) ((currentSampleStart - previousSample) * 1000 / getSpeedFactor());
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					// sleep interrupted
				}
				previousSample = currentSampleStart;
			}
		}

		/**
		 * @return the starting position of the current sample in seconds.
		 */
		public double getCurrentSampleStart() {
			return currentSampleStart;
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

	public final class AudioPlayingThread extends Thread {
		private final AudioFile file;
		private boolean running;
		private boolean isPaused;
		private final double offsetInSeconds;
		private final AudioFileSampleProcessor sampleProcessor;

		AudioPlayingThread(final AudioFile audioFile, final double startAtSeconds,
				final AudioFileSampleProcessor sampleProcessor) {
			super("Audio playing thread.");
			file = audioFile;
			running = false;
			isPaused = false;
			offsetInSeconds = startAtSeconds;
			this.sampleProcessor = sampleProcessor;
		}

		public void pauzePlaying() {
			isPaused = true;
		}

		public void resumePlaying() {
			isPaused = false;
		}

		public boolean isPaused() {
			return isPaused;
		}

		void stopPlaying() {
			running = false;
		}

		@Override
		public void run() {
			running = true;
			AudioFileFormat format = file.fileFormat();
			float frameRate = format.getFormat().getFrameRate(); // frames /
																	// second
			int frameSize = format.getFormat().getFrameSize(); // bytes / frame
			// bytes = bytes / frame * frame/second * second
			long bytesToSkip = frameSize * Math.round(offsetInSeconds * frameRate);

			try {
				while (sampleProcessor.isExtractingPitch()) {
					Thread.sleep(10);
				}
				AudioInputStream stream = AudioSystem.getAudioInputStream(new File(file.transcodedPath()));
				stream.skip(bytesToSkip);
				int bufferSize = 2048;
				byte[] buffer = new byte[bufferSize];
				float[] fakeBuffer = new float[0];
				BlockingAudioPlayer player = new BlockingAudioPlayer(format.getFormat(), bufferSize
						/ frameSize, 0);
				while (running && stream.read(buffer) != -1) {
					while (isPaused) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// sleep interrupted
						}
					}
					player.processOverlapping(fakeBuffer, buffer);
				}
				player.processingFinished();
			} catch (UnsupportedAudioFileException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void addHandler(SampleHandler handler) {
		handlers.add(handler);
	}

	public void audioFileChanged(AudioFile newAudioFile) {
		audioFile = newAudioFile;
		if (processorThread != null && processorThread.isAlive()) {
			processorThread.stopAnalysis();
		}
		if (playerThread != null && playerThread.isAlive()) {
			playerThread.stopPlaying();
		}

		processorThread = new AudioFileSampleProcessor(newAudioFile, handlers);
		processorThread.setSpeedFactor((Double) speedSpinner.getValue());
		processorThread.start();
		if (1.0 == (Double) speedSpinner.getValue()) {
			playerThread = new AudioPlayingThread(newAudioFile, 0, processorThread);
			playerThread.start();
		}
		playButton.setText("Pauze");
		playButton.setEnabled(true);
		stopButton.setEnabled(true);
		speedSpinner.setEnabled(true);

	}

	public interface SampleHandler {
		public void addSample(Annotation sample);

		public void removeSample(Annotation sample);
	}

}
