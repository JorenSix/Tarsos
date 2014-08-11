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

package be.tarsos.exp.cli;

import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.IPEMPitchDetection;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
import be.tarsos.sampled.pitch.TarsosPitchDetection;
import be.tarsos.sampled.pitch.VampPitchDetection;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.AudioFile;
import be.tarsos.util.histogram.HistogramFactory;
import be.tarsos.util.histogram.PitchClassHistogram;
import be.tarsos.util.histogram.PitchHistogram;

public final class HistogramSummationTest {
	private HistogramSummationTest() {
	}

	/**
	 * Tests if summing histograms constructed with different pitch extractors
	 * is useful.
	 * @param args 
	 * @throws EncoderException 
	 * 
	 * @throws UnsupportedAudioFileException
	 */
	public static void main(final String[] args) throws EncoderException {
		final AudioFile audioFile = new AudioFile("audio\\maghreb\\4_ABERDAG___LA_DANSE.wav");
		PitchDetector pitchDetector = new TarsosPitchDetection(audioFile, PitchDetectionMode.TARSOS_YIN);
		pitchDetector.executePitchDetection();
		List<Annotation> samples = pitchDetector.getAnnotations();
		PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(samples);
		final PitchClassHistogram toneScaleHistogramTarsosYin = pitchHistogram.pitchClassHistogram();

		pitchDetector = new VampPitchDetection(audioFile, PitchDetectionMode.VAMP_YIN);
		pitchDetector.executePitchDetection();
		samples = pitchDetector.getAnnotations();
		pitchHistogram = HistogramFactory.createPitchHistogram(samples);
		final PitchClassHistogram toneScaleHistogramAbioYin = pitchHistogram.pitchClassHistogram();

		pitchDetector = new IPEMPitchDetection(audioFile, PitchDetectionMode.IPEM_SIX);
		pitchDetector.executePitchDetection();
		samples = pitchDetector.getAnnotations();
		pitchHistogram = HistogramFactory.createPitchHistogram(samples);
		final PitchClassHistogram toneScaleHistogramIPEM = pitchHistogram.pitchClassHistogram();

		toneScaleHistogramTarsosYin.normalize();
		toneScaleHistogramAbioYin.normalize();
		toneScaleHistogramIPEM.normalize();

		toneScaleHistogramTarsosYin.plot("data/tests/tarsos_yin.png", "tarsos_yin");
		toneScaleHistogramAbioYin.plot("data/tests/audbio_yin.png", "aubio_yin");
		toneScaleHistogramIPEM.plot("data/tests/ipem.png", "ipem");

		toneScaleHistogramTarsosYin.add(toneScaleHistogramAbioYin).add(toneScaleHistogramIPEM).normalize()
				.gaussianSmooth(1.0).plot("data/tests/added.png", "Aubio + Tarsos + Ipem");
	}
}
