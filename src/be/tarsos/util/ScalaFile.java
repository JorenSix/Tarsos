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

package be.tarsos.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.histogram.PitchClassHistogram;

/**
 * A representation of a scala file.  See the <a href="http://www.huygens-fokker.org/scala/scl_format.html"> Scala scale file format</a>.
 * 
 * @author Joren Six
 */
public final class ScalaFile {
	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(ScalaFile.class.getName());

	/**
	 * The octave is 1200 cents.
	 */
	// private static final double OCTAVE_IN_CENTS = 1200.0;
	/**
	 * A description of the tone scale.
	 */
	private transient String description;
	/**
	 * A list of pitches.
	 */
	private final transient double[] pitches;
	/**
	 * A list of names.
	 */
	private final transient String[] pitchNames;

	/**
	 * Create a new Scala file object.
	 * 
	 * @param desc
	 *            The description of the tone scale.
	 * @param notes
	 *            The pitches (notes) used in the scale.
	 * @param names
	 *            The names of the notes used (or null).
	 */
	public ScalaFile(final String desc, final double[] notes, final String[] names) {
		if (notes == null) {
			throw new IllegalArgumentException("The notes (pitches) should be defined");
		} else if (names != null && names.length != notes.length) {
			throw new AssertionError("Notes and names should have the same length.");
		}
		description = desc;
		pitches = notes.clone();
		if (names == null) {
			pitchNames = null;
		} else {
			pitchNames = names.clone();
		}
	}

	/**
	 * Create a new Scala file object.
	 * 
	 * @param desc
	 *            The description of the tone scale.
	 * @param notes
	 *            The pitches (notes) used in the scale.
	 */
	public ScalaFile(final String desc, final double[] notes) {
		this(desc, notes, null);
	}

	/**
	 * Reads a Scala file from disk and returns a new instance.
	 * <p>
	 * The <a href="http://www.huygens-fokker.org/scala/scl_format.html"> Scala
	 * scale file format</a>: <bufferCount>This file format for musical tunings
	 * is becoming a standard for exchange of scales, owing to the size of the
	 * scale archive of over 3700+ scales and the popularity of the Scala
	 * program.</bufferCount>
	 * </p>
	 * <p>
	 * Usually it has <code>.scl</code> as extension.
	 * </p>
	 * 
	 * @param scalaFile
	 *            The Scala file to read.
	 */
	public ScalaFile(final String scalaFile) {
		final String contents = FileUtils.readFile(scalaFile);
		final String[] lines = contents.split("\n");
		final List<String> validPitchRows = new ArrayList<String>();
		String descriptionLine = "";
		int numberOfDataLines = 0;
		for (final String line : lines) {
			final boolean isComment = line.trim().startsWith("!");
			// Skip comments.
			if (isComment) {
				continue;
			} // else {
			numberOfDataLines++;
			// The first data line is the description
			if (numberOfDataLines == 1) {
				descriptionLine = line;
				// The second data line is the number of notes.
				// The other data lines should be valid pitches.
			} else if (numberOfDataLines > 2) {
				final boolean isValidRatio = line.matches("\\s*[0-9]+(|/[0-9]+)(| .*)");
				final boolean isValidCent = line.matches("\\s*(-|\\+)?[0-9]+\\.[0-9]*(| .*)");
				if (isValidRatio || isValidCent) {
					validPitchRows.add(line);
				}
			}
		}

		pitches = new double[validPitchRows.size()];
		pitchNames = new String[validPitchRows.size()];
		description = descriptionLine;
		String[] lineData;
		for (int i = 0; i < pitches.length; i++) {
			lineData = validPitchRows.get(i).trim().split("\\s", 2);
			final String pitchData = lineData[0];
			String nameData;
			if (lineData.length == 2) {
				nameData = lineData[1].trim();
			} else {
				nameData = "";
			}
			pitches[i] = parsePitch(pitchData);
			pitchNames[i] = nameData;
		}
	}

	/**
	 * Parses a row from a scala file and returns a double value representing
	 * cents.These lines are all valid pitch lines:
	 * 
	 * <pre>
	 * 81/64
	 * 408.0
	 * 408.
	 * 5
	 * -5.0
	 * 10/20
	 * 100.0 cents
	 * 100.0 C#
	 * 5/4   E\
	 * </pre>
	 * 
	 * @param row
	 *            The row to parse.
	 * @return The parsed pitch.
	 */
	private double parsePitch(final String row) {
		double parsedPitch;
		if (row.contains("/") || !row.contains(".")) {
			final String[] data = row.split("/");
			final double denominator = Double.parseDouble(data[0]);
			double quotient;
			if (data.length == 2) {
				quotient = Double.parseDouble(data[1]);
			} else {
				quotient = 1;
			}
			final double absCentDenom = PitchUnit.hertzToAbsoluteCent(denominator);
			final double absCentQuotient = PitchUnit.hertzToAbsoluteCent(quotient);
			parsedPitch = Math.abs(absCentDenom - absCentQuotient);
		} else {
			parsedPitch = Double.parseDouble(row);
		}
		return parsedPitch;
	}

	/**
	 * Writes a Scala file to disk. The peaks use cent values.
	 * <p>
	 * The <a href="http://www.huygens-fokker.org/scala/scl_format.html"> Scala
	 * scale file format</a>: <bufferCount>This file format for musical tunings
	 * is becoming a standard for exchange of scales, owing to the size of the
	 * scale archive of over 3700+ scales and the popularity of the Scala
	 * program.
	 * </p>
	 * <p>
	 * Usually it has <code>.scl</code> as extension.
	 * </p>
	 * 
	 * @param scalaFile
	 *            The location to write to.
	 */
	public void write(final String scalaFile) {
		if (pitches.length > 0) {
			/*
			 * if (pitches[pitches.length - 1] != OCTAVE_IN_CENTS) {
			 * contents.append(OCTAVE_IN_CENTS).append("\n"); }
			 */
			String content = ("! " + FileUtils.basename(scalaFile) +".scl \n") + toString();
			FileUtils.writeFile(content, scalaFile);
		} else {
			LOG.warning("No pitches defined, file: " + scalaFile + " not created.");
		}
	}
	
	public String toString(){
		final StringBuilder contents = new StringBuilder();
		contents.append("!\n");
		contents.append(getDescription()).append("\n");
		contents.append(pitches.length).append("\n!\n");
		for (int i = 0; i < pitches.length; i++) {
			final double peakPosition = pitches[i];
			contents.append(peakPosition);
			if (pitchNames != null && pitchNames[i] != null) {
				contents.append(" ").append(pitchNames[i]);
			}
			contents.append("\n");
		}
		return contents.toString();
	}

	/**
	 * @return The list of pitches. It returns a clone so please cache appropriately.
	 */
	public double[] getPitches() {
		return pitches.clone();
	}

	/**
	 * @return The tone scale description.
	 */
	public String getDescription() {
		return this.description;
	}

	public void setDescription(final String newDescription) {
		this.description = newDescription;
	}

	/**
	 * @return A list of pitch names. Or null.
	 */
	public String[] getPitchNames() {
		String[] names = null;
		if (hasNames()) {
			names = pitchNames.clone();
		}
		return names;
	}

	/**
	 * @return True if the pitch classes are named, false otherwise.
	 */
	public boolean hasNames() {
		return pitchNames != null;
	}

	/**
	 * @return The western scale.
	 */
	public static ScalaFile westernTuning() {
		final double[] notes = { 0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, };
		final String[] names = { "C", "C#/Db", "D", "D#/Eb", "E", "F", "F#/Gb", "G", "G#/Ab", "A", "A#/Bb", "B" };
		return new ScalaFile("The western tone scale", notes, names);
	}

	/**
	 * In a list of scala files it finds the closest one. This is currently defined by histogram overlap.
	 * @param haystack a list of Scala files.
	 * @return the closest Scala file.
	 */
	public ScalaFile findClosest(List<ScalaFile> haystack) {
		PitchClassHistogram needle = PitchClassHistogram.createToneScale(pitches.clone());
		ScalaFile closest = haystack.get(0);
		double closestCorrelation = -1;
		for(ScalaFile other : haystack){
			PitchClassHistogram otherHisto = PitchClassHistogram.createToneScale(other.pitches.clone());
			int displacement = needle.displacementForOptimalCorrelation(otherHisto);
			double correlation = needle.correlationWithDisplacement(displacement, otherHisto);
			if(correlation > closestCorrelation){
				closest = other;
				closestCorrelation = correlation;
			}
		}
		return closest;
	}

	/**
	 * @return all possible intervals.
	 */
	public List<Integer> getIntervals(boolean onlyUniqeIntervals) {
		List<Integer> values = new ArrayList<Integer>();
		for(int i = 0; i < pitches.length ; i++){
			for(int j = i+1; j < pitches.length ; j++){
				int value; 
				value = (int) Math.round(Math.abs(pitches[j] - pitches[i]));
				if(value > 600)
					value=1200-value;
				if(onlyUniqeIntervals){
					if(!values.contains(value)){
						values.add(value);
					}
				}else{
					values.add(value);
				}
			}
		}
		
		//Sort by interval size
		Collections.sort(values);
		
		return values;		
	}
}
