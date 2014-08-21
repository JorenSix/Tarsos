/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.ui.pitch;

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

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.AnnotationListener;
import be.tarsos.sampled.pitch.AnnotationPublisher;
import be.tarsos.sampled.pitch.AnnotationSelection;
import be.tarsos.util.AudioFile;
import be.tarsos.util.TimeUnit;

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
					startPlayback(waveForm.getMarker(false),audioFile.getLengthIn(TimeUnit.SECONDS));
				}
			}
		});
		
		loopSelectionCheckBox = new JCheckBox("Loop selection?");
		loopSelectionCheckBox.setToolTipText("Or play starting from the end marker.");
		loopSelectionCheckBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				stopPlayback();
				playerThread = new AudioPlayingThread(audioFile, waveForm.getMarker(true),waveForm.getMarker(false));
				playerThread.start();
			}
		});

		this.waveForm = waveFormComponent;

		builder.addButton(playButton);
		builder.addButton(loopSelectionCheckBox);

		this.add(builder.getPanel(), BorderLayout.EAST);
		this.add(waveFormComponent, BorderLayout.CENTER);
		waveFormComponent.setSize(getPreferredSize());
	}

	public final class AudioPlayingThread extends Thread {
		private final AudioFile file;
		private boolean running;
		private final double offsetInSeconds;
		private final double stopAtSeconds;
		private final AnnotationPublisher publisher;

		AudioPlayingThread(final AudioFile audioFile, final double startAtSeconds,final double stopAtSeconds) {
			super("Audio playing thread.");
			file = audioFile;
			running = false;
			offsetInSeconds = startAtSeconds;
			this.stopAtSeconds = stopAtSeconds;
			publisher = AnnotationPublisher.getInstance();
		}

		void stopPlaying() {
			running = false;
		}

		@Override
		public void run() {
			running = true;
			AudioFileFormat format = file.fileFormat();
			// TarsosFrame rate in frames per second.
			float frameRate = format.getFormat().getFrameRate();
			// TarsosFrame size in bytes per frame.
			int frameSize = format.getFormat().getFrameSize();
			// Bytes to skip in bytes.
			long bytesToSkip = frameSize * Math.round(offsetInSeconds * frameRate);
			// Byte count to count the number of processed bytes.
			long byteCount = bytesToSkip;

			publisher.clear();
			publisher.alterSelection(waveForm.getMarker(true), offsetInSeconds);
			publisher.delegateAddAnnotations(waveForm.getMarker(true), offsetInSeconds);
			
			
			try {
				AudioInputStream stream = AudioSystem.getAudioInputStream(new File(file.transcodedPath()));
				// skip to offset in seconds:
				stream.skip(bytesToSkip);

				int bufferSize = 2048;
				double bufferSizeInSeconds = bufferSize / frameSize / frameRate;
				byte[] buffer = new byte[bufferSize];
				float[] floatBuffer = new float[bufferSize/frameSize];
				AudioPlayer player = new AudioPlayer(format.getFormat());
				TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter.getConverter(JVMAudioInputStream.toTarsosDSPFormat(stream.getFormat()));
				
				double previousTime = offsetInSeconds;
				AudioEvent event = new AudioEvent(JVMAudioInputStream.toTarsosDSPFormat(format.getFormat()),-1);
				while (running && stream.read(buffer) != -1) {
					byteCount += buffer.length;
					converter.toFloatArray(buffer, floatBuffer);
					event.setFloatBuffer(floatBuffer);
					player.process(event);
					double currentTime = byteCount / frameSize / frameRate;
					if (currentTime - previousTime > 0.03) {
						publisher.alterSelection(publisher.getCurrentSelection().getStartTime(),currentTime);
						publisher.delegateAddAnnotations(previousTime, currentTime);
						previousTime = currentTime;
					}
					//stop running at stop at seconds 
					if(currentTime +  bufferSizeInSeconds > stopAtSeconds){
						running = false;
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
	
	

	public void startPlayback(double startAtSeconds,double stopAtSeconds) {
		stopPlayback();
		playerThread = new AudioPlayingThread(audioFile, startAtSeconds,stopAtSeconds);
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
		
		waveForm.setMarker(0, true);
		waveForm.setMarker(0, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.tarsos.ui.pitch.AnnotationListener#extractionFinished()
	 */
	public void extractionFinished() {
		if (playerThread != null && playerThread.isAlive()) {
			playerThread.stopPlaying();
		}
		startPlayback(0,audioFile.getLengthIn(TimeUnit.SECONDS));
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

		if (audioFile != null) {

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

	}

	public boolean shouldPlay() {
		return playButton.getText().equals("||");
	}
}
