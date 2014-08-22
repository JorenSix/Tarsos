package be.tarsos.ui.link.layers.featurelayers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.ui.link.LinkedFrame;
import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.ViewPort;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.tarsos.ui.link.layers.LayerUtilities;

public class PitchContourLayer extends FeatureLayer {

	public PitchContourLayer(LinkedPanel parent, int frameSize, int overlap) {
		super(parent, frameSize, overlap);
		this.name = "Pitch contour layer";
	}

	public void draw(Graphics2D graphics) {
		ICoordinateSystem cs = parent.getCoordinateSystem();

		graphics.setColor(Color.green);
		int ovalWidth = Math.round(LayerUtilities.pixelsToUnits(graphics, 4,
				true));
		int ovalHeight = Math.round(LayerUtilities.pixelsToUnits(graphics, 4,
				false));
		// every second
		for (Map.Entry<Double, float[]> entry : features.subMap(
				cs.getMin(ICoordinateSystem.X_AXIS) / 1000.0,
				cs.getMax(ICoordinateSystem.X_AXIS) / 1000.0).entrySet()) {
			double time = entry.getKey();// in seconds
			double pitch = entry.getValue()[0];// in cents
			if (pitch > cs.getMin(ICoordinateSystem.Y_AXIS)
					&& pitch < cs.getMax(ICoordinateSystem.Y_AXIS)) {
				graphics.drawOval((int) (time * 1000), (int) pitch, ovalWidth,
						ovalHeight);
			}
		}
	}

	@Override
	public void initialise() {
		try {
			adp = AudioDispatcherFactory.fromFile(new File(LinkedFrame.getInstance()
					.getAudioFile().originalPath()), this.getFrameSize(),
					this.getOverlap());
		} catch (UnsupportedAudioFileException e) {
			// @TODO: errorafhandeling
			e.printStackTrace();
		} catch (IOException e2){
			e2.printStackTrace();
		}
		final double timeLag = 1024 / 44100.0;

		adp.addAudioProcessor(new PitchProcessor(
				PitchEstimationAlgorithm.FFT_YIN, 44100, this.getFrameSize(),
				new PitchDetectionHandler() {

					public void handlePitch(
							PitchDetectionResult pitchDetectionResult,
							AudioEvent audioEvent) {
						if (pitchDetectionResult.isPitched()) {
							float[] pitch = new float[1];
							pitch[0] = (float) PitchUnit
									.hertzToAbsoluteCent(pitchDetectionResult
											.getPitch());
							features.put(audioEvent.getTimeStamp() - timeLag,
									pitch);
						}
					}
				}));
	}

//	@Override
//	protected void setProperties() {
//		// TODO Auto-generated method stub
//		
//	}

}
