package be.hogent.tarsos.sampled.pitch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.Execute;
import be.hogent.tarsos.util.FileUtils;

public final class VampPitchDetection implements PitchDetector {
	private final List<Annotation> annotations;
	private final AudioFile file;
	private final PitchDetectionMode mode;

	private static final Logger LOG = Logger.getLogger(VampPitchDetection.class.getName());

	public VampPitchDetection(final AudioFile audioFile, final PitchDetectionMode pitchDetectionMode) {
		annotations = new ArrayList<Annotation>();
		file = audioFile;
		mode = pitchDetectionMode;
		copyDefaultSettings();
	}

	private void copyDefaultSettings() {
		String setting = mode.getParametername() + ".n3";
		String fileName = FileUtils.combine(FileUtils.temporaryDirectory(), setting);
		if (!FileUtils.exists(fileName)) {
			FileUtils.copyFileFromJar("/be/hogent/tarsos/sampled/pitch/resources/" + setting, fileName);
			LOG.info(String.format("Copied % from jar file to %s .", setting, fileName));
		}
	}

	public List<Annotation> executePitchDetection() {
		String setting = mode.getParametername() + ".n3";
		final String settingsFile = FileUtils.combine(FileUtils.temporaryDirectory(), setting);
		final String command = String.format(
				"sonic-annotator -t %s  %s -w csv --csv-one-file h --csv-basedir %s --csv-force",
				settingsFile, file.transcodedPath(), file.transcodedDirectory());
		Execute.command(command);
		final String csvFile;
		if (mode == PitchDetectionMode.VAMP_MAZURKA_PITCH) {
			csvFile = FileUtils.combine(file.transcodedDirectory(), FileUtils.basename(file.transcodedPath())
					+ "_vamp_mazurka-plugins_mzharmonicspectrum_rawpitch.csv");
		} else {
			csvFile = FileUtils.combine(file.transcodedDirectory(), FileUtils.basename(file.transcodedPath())
					+ "_vamp_vamp-aubio_aubiopitch_frequency.csv");
		}
		// CSV file should exist
		assert FileUtils.exists(csvFile);
		// parse CSV File
		parseVamp(csvFile);

		// Is keeping the intermediate CSV file required?
		// I don't think so:
		if (new File(csvFile).delete()) {
			LOG.fine(String.format("Deleted intermediate CSV file %s", csvFile));
		} else {
			LOG.fine(String.format("Failed to deleted intermediate CSV file %s", csvFile));
			// mark for deletion when JVM closes
			new File(csvFile).deleteOnExit();
		}

		return annotations;
	}

	/**
	 * Parse a CSV file and create sample objects.
	 * 
	 * @param csvFileName
	 *            The (absolute) path to the CSV file.
	 */
	private void parseVamp(final String csvFileName) {
		final List<String[]> csvData = FileUtils.readCSVFile(csvFileName, ",", 2);
		for (final String[] row : csvData) {
			double pitch = Double.parseDouble(row[1]);
			double time = Double.parseDouble(row[0]);
			final Annotation sample = new Annotation(time, pitch, mode);
			annotations.add(sample);
		}
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public String getName() {
		return "vamp_" + mode.getParametername();
	}

	@Override
	public double progress() {
		return -1;
	}

}
