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

package be.tarsos.sampled.pitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import be.tarsos.util.AudioFile;
import be.tarsos.util.Command;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;
import be.tarsos.util.histogram.PitchHistogram;

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
			FileUtils.rm(fileName);
			FileUtils.copyFileFromJar("/be/tarsos/sampled/pitch/resources/" + setting, fileName);
			LOG.info(String.format("Copied %s from jar file to %s .", setting, fileName));
		}
	}

	public List<Annotation> executePitchDetection() {
		String setting = mode.getParametername() + ".n3";
		final String settingsFile = FileUtils.combine(FileUtils.temporaryDirectory(), setting);
		final String csvFileDir = FileUtils.combine(file.transcodedDirectory(),mode.getParametername());
		FileUtils.mkdirs(csvFileDir);
		
		Command cmd = new Command("sonic-annotator");
		
		final String csvFile;
		
		if(mode != PitchDetectionMode.VAMP_CONSTANT_Q_200 && mode != PitchDetectionMode.VAMP_CONSTANT_Q_400){
			csvFile = FileUtils.combine(csvFileDir, FileUtils.basename(file.transcodedPath())
					+ "_vamp_vamp-aubio_aubiopitch_frequency.csv");
		}else{
			csvFile = FileUtils.combine(csvFileDir, FileUtils.basename(file.transcodedPath()) + "_vamp_qm-vamp-plugins_qm-constantq_constantq.csv");
		}
		
		cmd.addArgument("-t").addFileArgument(settingsFile);
		cmd.addFileArgument(file.transcodedPath());
		cmd.addArgument("-w").addArgument("csv");
		cmd.addArgument("--csv-one-file").addArgument(csvFile);
		cmd.addArgument("--csv-basedir").addFileArgument(csvFileDir);
		
		try {
			
		
			
			if(!FileUtils.exists(csvFile)){
				LOG.info(cmd.execute());
			}
			
			// CSV file should exist
			assert FileUtils.exists(csvFile);
			// parse CSV File
			if(mode != PitchDetectionMode.VAMP_CONSTANT_Q_200 && mode != PitchDetectionMode.VAMP_CONSTANT_Q_400){				
				parseVamp(csvFile);
			}else{
				parseConstantQFile(csvFile);
			}

			// Is keeping the intermediate CSV file required?
			// I don't think so:
			//if (new File(csvFile).delete()) {
			//	LOG.fine(String.format("Deleted intermediate CSV file %s", csvFile));
			//} else {
			//	LOG.fine(String.format("Failed to deleted intermediate CSV file %s", csvFile));
				// mark for deletion when JVM closes
			//	new File(csvFile).deleteOnExit();
			//}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return annotations;
	}
	public double[][] constantQValues;
	
	private void parseConstantQFile(String csvFile) {
		String contents = FileUtils.readFile(csvFile);

		int octaves = 4;
		
		final int binsPerOctave;
		
		if(mode == PitchDetectionMode.VAMP_CONSTANT_Q_200){
			binsPerOctave = 200;
		}else{
			binsPerOctave = 400;
		}
		
		String[] rows = contents.split("\n");
		double[] timings = new double[rows.length];
		
		constantQValues = new double[rows.length][octaves*binsPerOctave];
		double maxValue = -1;
		double minValue = Double.MAX_VALUE;
		int rowIndex=0;
		for(String row:rows){
			String[] rowData = row.split(",");
			timings[rowIndex] = Double.valueOf(rowData[0]);
		    for(int colIndex = 1; colIndex < constantQValues[rowIndex].length  ; colIndex ++ ){
		    	double value = Double.valueOf(rowData[colIndex]);
		    	constantQValues[rowIndex][colIndex-1] = value;
		    	maxValue = Math.max(maxValue,value);
		    	minValue = Math.min(minValue,value);
		    }
		    rowIndex++;
		}
		
		
		//Normalize.
		for(rowIndex = 0 ; rowIndex < constantQValues.length ; rowIndex++){
			 for(int colIndex = 0; colIndex < constantQValues[rowIndex].length; colIndex ++ ){
				 constantQValues[rowIndex][colIndex] = (constantQValues[rowIndex][colIndex] - minValue)/maxValue;
			 }
		}
		
		/*
		for(rowIndex = 0 ; rowIndex < constantQValues.length ; rowIndex++){
			double time = timings[rowIndex];
			for(int colIndex = 0; colIndex < constantQValues[rowIndex].length; colIndex ++ ){
				double pitchInMidiCents = colIndex * 12 / (float) binsPerOctave  + 36;
				double pitchInHz = PitchConverter.midiCentToHertz(pitchInMidiCents);
				for(int i = 0; i < constantQValues[rowIndex][colIndex] * 100; i++){
					annotations.add(new Annotation(time + i * 0.0000000000001,pitchInHz,mode));
				}
			 }
		}
		*/

		//Construct a pitch histogram. 
		double[] pitchHistogram = new double[constantQValues[0].length];
		//double[] pitchClassHistogram = new double[constantQValues[0].length/octaves];
		for (rowIndex = 0; rowIndex < constantQValues.length; rowIndex++) {
			for (int colIndex = 0; colIndex < constantQValues[rowIndex].length; colIndex++) {
				pitchHistogram[colIndex] += constantQValues[rowIndex][colIndex];
				//pitchClassHistogram[colIndex % pitchClassHistogram.length] += constantQValues[rowIndex][colIndex];
			}
		}
		
		
		for(int i = 0; i < pitchHistogram.length ; i++){
			double pitchInCents = i * 12 / (float) binsPerOctave * 100 + 24 * 100;
			System.out.println(pitchInCents + ";" + pitchHistogram[i]);
		}
		
		 
		Random rnd = new Random();
		int lengthInMS = (int) file.getLengthInMilliSeconds();
		for(int i = 0; i < pitchHistogram.length ; i++){
			double pitchInMidiCents = i * 12 / (float) binsPerOctave  + 36;
			double pitchInHz = PitchUnit.midiCentToHertz(pitchInMidiCents);
			for(int j = 0 ; j < pitchHistogram[i] * 100 ; j++){
				annotations.add(new Annotation(rnd.nextInt(lengthInMS)/1000.0,pitchInHz,mode));
			}
		}
		
		double[] accumulator = PitchHistogram.createAccumulator(annotations, 3.0);
		annotations.clear();
		int pitchHistogramMinimum = Configuration.getInt(ConfKey.pitch_histogram_start);		
		for(int i = 0 ; i < accumulator.length ; i ++){
			double pitchInHz = PitchUnit.absoluteCentToHertz(i + pitchHistogramMinimum);
			for(int j = 0 ; j < accumulator[i]/300 ; j++){
				annotations.add(new Annotation(rnd.nextInt(lengthInMS)/1000.0,pitchInHz,mode));
			}
		}		
		Collections.sort(annotations);
	}



	/**
	 * Parse a CSV file and create sample objects.
	 * 
	 * @param csvFileName
	 *            The (absolute) path to the CSV file.
	 */
	private void parseVamp(final String csvFileName) {
		final List<String[]> csvData = FileUtils.readCSVFile(csvFileName, ",",3);
		for (final String[] row : csvData) {
			double pitch = Double.parseDouble(row[2]);
			double time = Double.parseDouble(row[1]);
			if(pitch > 0){
				final Annotation sample = new Annotation(time, pitch, mode);
				annotations.add(sample);
			}
		}
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public String getName() {
		return "vamp_" + mode.getParametername();
	}

	
	public double progress() {
		return -1;
	}

}
