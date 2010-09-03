package be.hogent.tarsos.ui.pitch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import be.hogent.tarsos.pitch.PitchConverter;
import be.hogent.tarsos.pitch.pure.DetectedPitchHandler;
import be.hogent.tarsos.pitch.pure.PurePitchDetector;
import be.hogent.tarsos.pitch.pure.Yin;
import be.hogent.tarsos.sampled.AudioDispatcher;
import be.hogent.tarsos.sampled.AudioProcessor;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.SimplePlot;

public class IntervalCurve {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1744724271250082834L;

	public IntervalCurve(final double start, final double stop, final AudioFile audioFile) {
		final SimplePlot plot = new SimplePlot("Interval Curve");
		final ArrayList<Double> times = new ArrayList<Double>();
		final ArrayList<Double> pitches = new ArrayList<Double>();

		AudioInputStream ais;
		try {
			ais = AudioSystem.getAudioInputStream(new File(audioFile.transcodedPath()));
			final float sampleRate = ais.getFormat().getSampleRate();
			final int bufferSize = Yin.DEFAULT_BUFFER_SIZE;
			final int overlapSize = Yin.DEFAULT_OVERLAP;
			final PurePitchDetector pureDetector = new Yin(sampleRate, bufferSize);
			final int bufferStepSize = bufferSize - overlapSize;
			final DetectedPitchHandler detectedPitchHandler = new DetectedPitchHandler() {
				private final List<Double> markers = new ArrayList<Double>();

				@Override
				public void handleDetectedPitch(final float time, final float pitch) {
					double pichInRelCent = PitchConverter.hertzToRelativeCent(pitch);
					if (pitch != -1 && pichInRelCent > start && pichInRelCent < stop) {
						plot.addData(time, pichInRelCent);
						times.add((double) time);
						pitches.add(pichInRelCent);
					}
				}
			};

			final AudioDispatcher dispatcher = new AudioDispatcher(ais, bufferSize, overlapSize);
			dispatcher.addAudioProcessor(new AudioProcessor() {
				private long samplesProcessed = 0;
				private float time = 0;

				@Override
				public void processFull(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
					samplesProcessed += audioFloatBuffer.length;
					processBuffer(audioFloatBuffer);
				}

				@Override
				public void processOverlapping(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
					samplesProcessed += bufferStepSize;
					processBuffer(audioFloatBuffer);
				}

				private void processBuffer(final float[] audioFloatBuffer) {
					final float pitch = pureDetector.getPitch(audioFloatBuffer);
					time = samplesProcessed / sampleRate;
					detectedPitchHandler.handleDetectedPitch(time, pitch);
				}

				@Override
				public void processingFinished() {
					StringBuilder sb = new StringBuilder();
					sb.append("time;pitch\n");
					for (int i = 0; i < times.size(); i++) {
						sb.append(times.get(i)).append(";").append(pitches.get(i)).append("\n");
					}
					FileUtils.writeFile(sb.toString(), "interval_curve_" + start + "_" + stop + "_"
							+ audioFile.basename() + ".csv");
					plot.save("interval_curve_" + start + "_" + stop + "_" + audioFile.basename() + ".png");
				}
			});

			new Thread(dispatcher).start();

		} catch (Exception e) {

		}

	}
}
