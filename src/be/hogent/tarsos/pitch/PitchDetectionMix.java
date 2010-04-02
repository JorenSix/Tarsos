package be.hogent.tarsos.pitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PitchDetectionMix uses a mix of pitch detectors to annotate a song.
 * @author Joren Six
 */
public class PitchDetectionMix implements PitchDetector {
	List<PitchDetector> detectors;

	private final List<Sample> samples;
	private final String name;
	private final double pitchDeviation;

	/**
	 * Create a new PitchdetectionMix
	 * @param detectors a list of initialized pitch detectors. For the moment only a list of two detectors is allowed
	 * @param pitchDeviation a percentage that defines when an annotation of two consecutive samples is accepted:
	 * e.g. a sample of 100HZ detected with YIN and the next sample of 101HZ detected with SCHMITT is accepted when
	 * pitchDeviation >= 0.01
	 *
	 */
	public PitchDetectionMix(List<PitchDetector> detectors,double pitchDeviation){
		this.detectors = detectors;
		this.pitchDeviation = pitchDeviation;

		List<String> names = new ArrayList<String>();
		for(PitchDetector detector : detectors){
			names.add(detector.getName());
		}
		Collections.sort(names);
		StringBuilder sb = new StringBuilder();
		for(String name:names){
			sb.append(name).append("_");
		}
		name = "mix_" + sb.toString() + pitchDeviation;

		samples = new ArrayList<Sample>();
	}

	@Override
	public void executePitchDetection() {

		for(PitchDetector detector : detectors){
			if(detector.getSamples().size() == 0)
				detector.executePitchDetection();
		}

		List<Sample> allSamples = new ArrayList<Sample>();
		for(PitchDetector detector : detectors){
			allSamples.addAll(detector.getSamples());
		}

		//order by sample start and source
		Collections.sort(allSamples);

		//accept some samples
		for(int i = 0;i<allSamples.size()-1;i++){
			Sample currentSample = allSamples.get(i);
			Sample nextSample = allSamples.get(i+1);
			if(currentSample.source != nextSample.source){
				double pitch = currentSample.returnMatchingPitch(nextSample,pitchDeviation);

				if(pitch > 0)
					samples.add(new Sample((nextSample.getStart() + nextSample.getStart() )/ 2,pitch));
			}
		}
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public List<Sample> getSamples() {
		return this.samples;
	}

}
