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
	private final List<Annotation> samples;
	private final String name;

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
	public PitchDetectionMix(final List<PitchDetector> subDetectors) {
		this.detectors = subDetectors;

		final List<String> names = new ArrayList<String>();
		for (final PitchDetector detector : subDetectors) {
			names.add(detector.getName());
		}
		Collections.sort(names);
		final StringBuilder sb = new StringBuilder();
		for (final String subDetectorName : names) {
			sb.append(subDetectorName).append("_");
		}
		name = "mix_" + sb.toString();

		samples = new ArrayList<Annotation>();
	}

	public void executePitchDetection() {

		for (final PitchDetector detector : detectors) {
			if (detector.getAnnotations().size() == 0) {
				detector.executePitchDetection();
			}
		}

		for (final PitchDetector detector : detectors) {
			samples.addAll(detector.getAnnotations());
		}

		// order by sample start and source
		Collections.sort(samples);
	}

	public String getName() {
		return this.name;
	}

	public List<Annotation> getAnnotations() {
		return this.samples;
	}

}
