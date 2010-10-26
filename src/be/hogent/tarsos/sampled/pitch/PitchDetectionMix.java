package be.hogent.tarsos.sampled.pitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

/**
 * PitchDetectionMix uses a mix of pitch detectors to annotate a song.
 * 
 * @author Joren Six
 */
public final class PitchDetectionMix implements PitchDetector {
	private final List<PitchDetector> detectors;
	private final List<Annotation> annotations;
	private final String name;

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(PitchDetectionMix.class.getName());

	/**
	 * Creates a new PitchdetectionMix.
	 * 
	 * @param subDetectors
	 *            a list of initialized pitch detectors. For now only a list of
	 *            two detectors is allowed
	 * @param errorMargin
	 *            a percentage that defines when an annotation of two
	 *            consecutive annotations is accepted: e.g. a sample of 100HZ
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

		LOG.fine(String.format("Created a pitch detector mix with %s detectors", subDetectors.size()));

		annotations = new ArrayList<Annotation>();
	}

	public void executePitchDetection() {
		int nThreads = Configuration.getInt(ConfKey.annotation_threads);
		ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);

		for (final PitchDetector detector : detectors) {
			Runnable command = new Runnable() {
				public void run() {
					detector.executePitchDetection();
					annotations.addAll(detector.getAnnotations());
				}
			};
			threadPool.execute(command);
		}

		// Request the pool to shutdown when the tasks are done.
		threadPool.shutdown();

		try {
			// Wait for the tasks or time out after six minutes.
			boolean done = threadPool.awaitTermination(6 * 60, TimeUnit.SECONDS);
			if (done) {
				LOG.info(String.format("Finished executing %s detectors on %s threads", detectors.size(),
						nThreads));
			} else {
				LOG.warning(String.format("Timeout while executing %s detectors on %s threads. One or "
						+ "more detection results not available.", detectors.size(), nThreads));
			}
		} catch (InterruptedException e) {
			LOG.warning(String.format("Interrupted while executing %s detectors on %s threads. One or "
					+ "more detection results not available.", detectors.size(), nThreads));
		}

		// order by sample start and source
		Collections.sort(annotations);
	}

	public String getName() {
		return this.name;
	}

	public List<Annotation> getAnnotations() {
		return this.annotations;
	}

}
