package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import be.hogent.tarsos.sampled.BlockingAudioPlayer;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.ui.WaveForm;
import be.hogent.tarsos.util.AudioFile;

import com.jgoodies.forms.builder.ButtonBarBuilder2;

public class ControlPanel extends JPanel implements AudioFileChangedListener, AnnotationListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5542665958725558290L;

	private AudioPlayingThread playerThread;

	/**
	 * Play or pauze music.
	 */
	private final JButton playButton;

	/**
	 * Play the selection or start from end mark?
	 */
	private final JCheckBox loopSelectionCheckBox;

	private AudioFile audioFile;
	private final WaveForm waveForm;

	public ControlPanel(final WaveForm waveFormComponent) {
		super(new BorderLayout());
		ButtonBarBuilder2 builder = ButtonBarBuilder2.createLeftToRightBuilder();

		playButton = new JButton("|>");
		playButton.setEnabled(false);
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (playerThread.isAlive()) {
					stopPlayback();
					((JButton) e.getSource()).setText("|>");
				} else {
					((JButton) e.getSource()).setText("||");
					startPlayback(waveForm.getMarker(false));
				}
			}
		});

		loopSelectionCheckBox = new JCheckBox("Play selection?");
		loopSelectionCheckBox.setToolTipText("Or play starting from the end marker.");

		this.waveForm = waveFormComponent;

		builder.addButton(playButton);

		this.add(builder.getPanel(), BorderLayout.EAST);
		this.add(waveFormComponent, BorderLayout.CENTER);
		waveFormComponent.setSize(getPreferredSize());
		waveFormComponent.setControlPanel(this);
	}

	public final class AudioPlayingThread extends Thread {
		private final AudioFile file;
		private boolean running;
		private final double offsetInSeconds;
		private final AnnotationPublisher publisher;

		AudioPlayingThread(final AudioFile audioFile, final double startAtSeconds) {
			super("Audio playing thread.");
			file = audioFile;
			running = false;
			offsetInSeconds = startAtSeconds;
			publisher = AnnotationPublisher.getInstance();
		}

		void stopPlaying() {
			running = false;
		}

		@Override
		public void run() {
			running = true;
			AudioFileFormat format = file.fileFormat();
			// Frame rate in frames per second.
			float frameRate = format.getFormat().getFrameRate();
			// Frame size in bytes per frame.
			int frameSize = format.getFormat().getFrameSize();
			// Bytes to skip in bytes.
			long bytesToSkip = frameSize * Math.round(offsetInSeconds * frameRate);
			// Byte count to count the number of processed bytes.
			long byteCount = bytesToSkip;

			publisher.delegateClearAnnotations();
			publisher.delegateAddAnnotations(waveForm.getMarker(true), offsetInSeconds);

			try {
				AudioInputStream stream = AudioSystem.getAudioInputStream(new File(file.transcodedPath()));
				// skip to offset in seconds:
				stream.skip(bytesToSkip);

				int bufferSize = 2048;
				byte[] buffer = new byte[bufferSize];
				float[] fakeBuffer = new float[0];
				BlockingAudioPlayer player = new BlockingAudioPlayer(format.getFormat(), bufferSize
						/ frameSize, 0);
				double previousTime = offsetInSeconds;
				while (running && stream.read(buffer) != -1) {
					byteCount += buffer.length;
					player.processOverlapping(fakeBuffer, buffer);
					double currentTime = byteCount / frameSize / frameRate;
					if (currentTime - previousTime > 0.03) {
						publisher.delegateAddAnnotations(previousTime, currentTime);
						previousTime = currentTime;
					}
				}
				if (running) {
					playButton.setText("|>");
					playButton.repaint();
				}

				player.processingFinished();

			} catch (UnsupportedAudioFileException e1) {
				assert false : "Converted audio should be supported.";
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void startPlayback(double seconds) {
		stopPlayback();
		playerThread = new AudioPlayingThread(audioFile, seconds);
		playerThread.start();
		playButton.setText("||");
	}

	private void stopPlayback() {
		if (playerThread != null && playerThread.isAlive()) {
			playerThread.stopPlaying();
		}
		playButton.setText("|>");
	}

	public void audioFileChanged(AudioFile newAudioFile) {
		audioFile = newAudioFile;
		AnnotationPublisher.getInstance().getCurrentSelection().setTimeSelection(0, 0);
		waveForm.setMarker(0, true);
		waveForm.setMarker(0, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.hogent.tarsos.ui.pitch.AnnotationListener#extractionFinished()
	 */
	public void extractionFinished() {
		if (playerThread != null && playerThread.isAlive()) {
			playerThread.stopPlaying();
		}
		startPlayback(0);
		playButton.setEnabled(true);
	}

	public void addAnnotations(List<Annotation> annotations) {
		// TODO Auto-generated method stub

	}

	public void clearAnnotations() {
		// TODO Auto-generated method stub

	}

	public void extractionStarted() {
		// TODO Auto-generated method stub

	}

	public void addAnnotation(Annotation annotation) {
		// TODO Auto-generated method stub

	}

	public void annotationsAdded() {
		AnnotationSelection selection = AnnotationPublisher.getInstance().getCurrentSelection();

		if (selection.getTimeSpan() > 1.0) {
			final double startTime;
			if (selection.getStartTime() > audioFile.getLengthInMilliSeconds() / 1000) {
				startTime = 0;
			} else {
				startTime = selection.getStartTime();
			}
			waveForm.setMarker(startTime, true);
		}

		final double stopTime;
		if (selection.getStopTime() > audioFile.getLengthInMilliSeconds() / 1000) {
			stopTime = audioFile.getLengthInMilliSeconds() / 1000;
		} else {
			stopTime = selection.getStopTime();
		}
		waveForm.setMarker(stopTime, false);

	}

	public boolean shouldPlay() {
		return playButton.getText().equals("||");
	}
}
