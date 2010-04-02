package be.hogent.tarsos.pitch;

import java.util.ArrayList;
import java.util.List;

import be.hogent.tarsos.pitch.Sample.SampleSource;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.Execute;
import be.hogent.tarsos.util.FileUtils;


/**
 *
 * Calls aubio (http://aubio.org/) to execute pitch detection.
 *  Aubio should be installed correctly and available in PATH.
 *  This command is called when executing pitch detection:
 * <code>
 * aubiopitch  -u freq --mode yin -s -70 -i in.wav
 * </code>
 *
 * The output uses the frequency (Hz) unit. The algorithm used is yin.
 * The default silence threshold of -70 is used. The file that is annoted is in.wav
 *
 * For more information see the <a href="http//aubio.org">aubio</a> man pages or website.
 *
 * The pitch detection algorithm is defined by {@link AubioPitchDetectionMode}.
 *
 * See http://www.elec.qmul.ac.uk/research/thesis/Brossier07-phdthesis.pdf for more info.
 *
 * @author Joren Six
 */
public class AubioPitchDetection implements PitchDetector{

	/**
	 * @author Joren Six
	 * The pitch detection mode defines which algorithm is used to detect pitch.
	 */
	public enum AubioPitchDetectionMode{
		/**
		 * The YIN algorithm
		 */
		YIN("yin"),
		/**
		 * A faster version of YIN: spectral YIN. It should yield very similar
		 * results as YIN, only faster.
		 */
		YINFFT("yinfft"),
		/**
		 * Fast spectral comb
		 */
		FCOMB("fcomb"),
		/**
		 * Multi comb with spectral smoothing
		 */
		MCOMB("mcomb"),
		/**
		 * Schmitt trigger
		 */
		SCHMITT("schmitt");

		private final String parameterName;
		private AubioPitchDetectionMode(String parameterName){
			this.parameterName=parameterName;
		}

		/**
		 * @return  The name used in the aubio command.
		 */
		public String getParametername(){
			return this.parameterName;
		}
	}

	private final AudioFile file;
	private final AubioPitchDetectionMode pitchDetectionMode;
	private final List<Sample> samples;
	private final String name;

	public AubioPitchDetection(AudioFile file,AubioPitchDetectionMode pitchDetectionMode){
		this.file = file;
		this.pitchDetectionMode = pitchDetectionMode;
		this.samples = new ArrayList<Sample>();
		this.name = "aubio_" + pitchDetectionMode.getParametername();
	}

	@Override
	public void executePitchDetection() {
		String annotationsDirectory = Configuration.get(ConfKey.raw_aubio_annotations_directory);
		String csvFileName = FileUtils.combine(annotationsDirectory,this.name + "_" + file.basename() + ".txt");


		if(! FileUtils.exists(csvFileName)){
			String command = "aubiopitch  -u freq --mode " + this.pitchDetectionMode.parameterName + "  -s -70  -i " + file.transcodedPath();
			Execute.command(command,csvFileName);
		}

		List<String[]> csvData = FileUtils.readCSVFile(csvFileName,"\t", 2);
		for(String[] row : csvData){
			long start = (long) (Double.parseDouble(row[0]) * 1000);
			Double pitch = Double.parseDouble(row[1]);
			Sample sample = pitch == -1 ? new Sample(start) : new Sample(start,pitch);
			switch(pitchDetectionMode){
			case YIN:
				sample.source = SampleSource.AUBIO_YIN;
				break;
			case YINFFT:
				sample.source = SampleSource.AUBIO_YINFFT;
				break;
			case MCOMB:
				sample.source = SampleSource.AUBIO_MCOMB;
				break;
			case FCOMB:
				sample.source = SampleSource.AUBIO_FCOMB;
				break;
			case SCHMITT:
				sample.source = SampleSource.AUBIO_SCHMITT;
				break;
			}
			samples.add(sample);
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
