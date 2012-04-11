package be.hogent.tarsos.ui.pitch;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Locale;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.sampled.Player;
import be.hogent.tarsos.sampled.PlayerState;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.AnnotationListener;
import be.hogent.tarsos.sampled.pitch.AnnotationPublisher;
import be.hogent.tarsos.util.AudioFile;

public class PlayerControlPanel extends JPanel implements AudioFileChangedListener, AnnotationListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1430643166254950304L;
	
	private JButton playButton;
	private JSlider tempoSlider;
	private JSlider positionSlider;
	//position value in the slider
	private int newPositionValue;
	
	private JLabel progressLabel;
	private JLabel totalLabel;
	
	private JLabel tempoLabel;
	
	private Player player;
	
	private PropertyChangeListener stateChanged = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			if(evt.getPropertyName() == "state"){
				PlayerState newState = (PlayerState) evt.getNewValue();
				playButton.setEnabled(newState!=PlayerState.NO_FILE_LOADED);
				positionSlider.setEnabled(newState!=PlayerState.NO_FILE_LOADED);
				if(newState == PlayerState.PLAYING) {
					playButton.setText("Pauze");
				} else if(newState == PlayerState.STOPPED)  {
					playButton.setText("Play");
				}
			}
		}
	};
	
	private PropertyChangeListener tempoChanged = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			if(evt.getPropertyName() == "tempo"){
				double tempo = (Double) evt.getNewValue();
				tempoSlider.setValue((int) (tempo*100));
			}
		}
	};
	
	final AudioProcessor reportProgressProcessor = new AudioProcessor() {
		public boolean process(AudioEvent audioEvent) {
			double timeStamp =  audioEvent.getTimeStamp();
			if(!positionSlider.getValueIsAdjusting()){
				newPositionValue = (int) (audioEvent.getProgress() * 1000);
				positionSlider.setValue(newPositionValue);
				setProgressLabelText(timeStamp,player.getDurationInSeconds());
			}
			return true;
		}
		
		public void processingFinished() {
			newPositionValue = (int) (player.getStartAt() / player.getDurationInSeconds() *1000); 
			positionSlider.setValue(newPositionValue);
		}
	};
	
	final AudioProcessor addAnnotationsProcessor = new AudioProcessor() {
		
		double previousTime = 0;
		
		public boolean process(AudioEvent audioEvent) {
			AnnotationPublisher publisher = AnnotationPublisher.getInstance();
			double currentTime = audioEvent.getTimeStamp();
			if (currentTime - previousTime > 0.03) {
				publisher.alterSelection(publisher.getCurrentSelection().getStartTime(),currentTime);
				publisher.delegateAddAnnotations(previousTime, currentTime);
				previousTime = currentTime;
			}
			return true;
		}
		
		public void processingFinished() {
			previousTime=0;
		}
	};
	
	public PlayerControlPanel(){
		playButton = new JButton("Play");
		playButton.setEnabled(false);
		playButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event) {
				if(player.getState() == PlayerState.PLAYING) {
					player.pauze();
				} else if(player.getState() == PlayerState.STOPPED)  {
					player.play();
				}
			}});
		JPanel p = new JPanel(new GridBagLayout());
		p.add(playButton);
		createSlider();
		createProgressSlider();
		doGroupLayout();
		player = Player.getInstance();
		player.addProcessorBeforeTimeStrechting(reportProgressProcessor);
		player.addProcessorBeforeTimeStrechting(addAnnotationsProcessor);
		player.addPropertyChangeListener(stateChanged);
	}

	private void doGroupLayout(){		
		playButton.setMinimumSize(new Dimension(85,10));
		tempoSlider.setMinimumSize(new Dimension(75,40));
		tempoSlider.setMaximumSize(new Dimension(75,20));
		tempoLabel.setMinimumSize(new Dimension(95,10));
		tempoLabel.setMaximumSize(new Dimension(95,40));

		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateGaps(true);
		
		JSeparator firstSeparator = new JSeparator(SwingConstants.VERTICAL);
		firstSeparator.setMaximumSize(new Dimension(5,300));
		
		JSeparator secondSeparator = new JSeparator(SwingConstants.VERTICAL);
		secondSeparator.setMaximumSize(new Dimension(5,300));
		
		JSeparator thirdSeparator = new JSeparator(SwingConstants.VERTICAL);
		thirdSeparator.setMaximumSize(new Dimension(5,300));
		
		JCheckBox loopCheckBox =  new JCheckBox();
		loopCheckBox.setText("Loop selection");
		
		
		layout.setHorizontalGroup(
				   layout.createSequentialGroup()
				      .addComponent(playButton)
				      .addComponent(firstSeparator)
				      .addComponent(tempoLabel)
				      .addComponent(tempoSlider)
				      .addComponent(secondSeparator)
				      .addComponent(progressLabel)
				      .addComponent(positionSlider)
				      .addComponent(totalLabel)
				      .addComponent(thirdSeparator)
				      .addComponent(loopCheckBox)
		);
		
		layout.setVerticalGroup(
				   layout.createSequentialGroup()
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
				           .addComponent(playButton)
				           .addComponent(firstSeparator)
				           .addComponent(tempoLabel)
				           .addComponent(tempoSlider)
				           .addComponent(secondSeparator)
				           .addComponent(progressLabel)
				           .addComponent(positionSlider)
				           .addComponent(totalLabel)
				           .addComponent(thirdSeparator)
				           .addComponent(loopCheckBox)
				           )
				);
		
	}
	
	private void createSlider() {	
		tempoSlider = new JSlider(0,300);
		tempoSlider.setValue(100);
		tempoSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				double newTempo = tempoSlider.getValue()/100.0;
				tempoLabel.setText(String.format("Tempo: %3d",  tempoSlider.getValue())+"%");
				player.setTempo(newTempo);
			}
		});
		Player.getInstance().addPropertyChangeListener(tempoChanged);
		tempoLabel = new JLabel("Tempo: 100%");
		tempoLabel.setToolTipText("The time stretching factor in % (100 is no change).");
	}
	
	private void createProgressSlider(){
		positionSlider = new JSlider(0,1000);
		positionSlider.setValue(0);
		positionSlider.setPaintLabels(false);
		positionSlider.setPaintTicks(false);
		positionSlider.setEnabled(false);
		
		positionSlider.addChangeListener(new ChangeListener() {
		
			public void stateChanged(ChangeEvent arg0) {				
				if (newPositionValue != positionSlider.getValue()) {
					double promille = positionSlider.getValue() / 1000.0;
					double currentPosition = player.getDurationInSeconds() * promille;
					if (positionSlider.getValueIsAdjusting()) {
						setProgressLabelText(currentPosition, player.getDurationInSeconds());
					} else {
						double secondsToSkip = currentPosition;
						PlayerState currentState = player.getState();
						player.pauze(secondsToSkip);
						if(currentState == PlayerState.PLAYING){
							player.play();							
						}
						
					}
				}
			}
		});

		progressLabel = new JLabel();
		totalLabel = new JLabel();
		setProgressLabelText(0, 0);
	}
	
	private void setProgressLabelText(double current, double max){
		progressLabel.setText(formattedToString(current));
		totalLabel.setText(formattedToString(max));
	}
	
	public String formattedToString(double seconds) {
		int minutes = (int) (seconds / 60);
		int completeSeconds = (int) seconds - (minutes * 60);
		int hundred =  (int) ((seconds - (int) seconds) * 100);
		return String.format(Locale.US, "%02d:%02d:%02d", minutes , completeSeconds, hundred);
	}


	public void audioFileChanged(AudioFile newAudioFile) {
		player.load(new File(newAudioFile.transcodedPath()));
	}

	public void addAnnotation(Annotation annotation) {
		double duration = player.getDurationInSeconds();
		int stop = (int) (AnnotationPublisher.getInstance().getCurrentSelection().getStopTime() / duration * 1000);
		positionSlider.setValue(stop);
	}

	public void clearAnnotations() {
		// TODO Auto-generated method stub
		
	}

	public void annotationsAdded() {
		// TODO Auto-generated method stub
		
	}

	public void extractionStarted() {
		// TODO Auto-generated method stub
		
	}

	public void extractionFinished() {
		// TODO Auto-generated method stub
	}
}
