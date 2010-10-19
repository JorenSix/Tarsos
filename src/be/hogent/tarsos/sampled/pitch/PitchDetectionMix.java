package be.hogent.tarsos.sampled.pitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PitchDetectionMix uses a mix of pitch detectors to annotate a song.
 * 
 * @author Joren Six
 */
public final class PitchDetectionMix implements PitchDetector {
	private final List<PitchDetector> detectors;
	private final List<Sample> samples;
	private final String name;
	private final double pitchDeviation;

	/**
	 * Creates a new PitchdetectionMix.
	 * 
	 * @param subDetectors
	 *            a list of initialized pitch detectors. For now only a list of
	 *            two detectors is allowed
	 * @param errorMargin
	 *            a percentage that defines when an annotation of two
	 *            consecutive samples is accepted: e.g. a sample of 100HZ
	 *            detected with AUBIO_YIN and the next sample of 101HZ detected
	 *            with AUBIO_SCHMITT is accepted when pitchDeviation >= 0.01
	 */
	public PitchDetectionMix(final List<PitchDetector> subDetectors, final double errorMargin) {
		this.detectors = subDetectors;
		this.pitchDeviation = errorMargin;

		final List<String> names = new ArrayList<String>();
		for (final PitchDetector detector : subDetectors) {
			names.add(detector.getName());
		}
		Collections.sort(names);
		final StringBuilder sb = new StringBuilder();
		for (final String subDetectorName : names) {
			sb.append(subDetectorName).append("_");
		}
		name = "mix_" + sb.toString() + errorMargin;

		samples = new ArrayList<Sample>();
	}

	public void executePitchDetection() {

		for (final PitchDetector detector : detectors) {
			if (detector.getSamples().size() == 0) {
				detector.executePitchDetection();
			}
		}

		final List<Sample> allSamples = new ArrayList<Sample>();
		for (final PitchDetector detector : detectors) {
			allSamples.addAll(detector.getSamples());
		}

		// order by sample start and source
		Collections.sort(allSamples);

		// accept some samples
		for (int i = 0; i < allSamples.size() - 1; i++) {
			final Sample currentSample = allSamples.get(i);
			final Sample nextSample = allSamples.get(i + 1);
			if (currentSample.getSource() != nextSample.getSource()) {
				final double pitch = currentSample.returnMatchingPitch(nextSample, pitchDeviation);

				if (pitch > 0) {
					samples.add(new Sample((nextSample.getStart() + nextSample.getStart()) / 2, pitch));
				}
			}
		}
	}

	public String getName() {
		return this.name;
	}

	public List<Sample> getSamples() {
		return this.samples;
	}

}
