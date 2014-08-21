/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.tarsos.exp.ui.fingerprinter;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.util.KernelDensityEstimate;

public class InterOnsetIntervalHistogram {
	final KernelDensityEstimate kde;
	
	public InterOnsetIntervalHistogram(String file) throws UnsupportedAudioFileException, IOException{
		kde = new KernelDensityEstimate(new KernelDensityEstimate.GaussianKernel(30),1200);
       		
		AudioFormat format = AudioSystem.getAudioInputStream(new File(file)).getFormat();

		AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(new File(file),1024,0);
	
		dispatcher.run();
	}
	
	public void printKDE(){
		kde.normalize();
		double[] estimate = kde.getEstimate();
		for(int i = 0 ; i < estimate.length - 300 ; i++){
			System.out.println((i/20.0) + "\t" + estimate[i]);
		}
	}
	
	public double correlate(InterOnsetIntervalHistogram other){
		kde.normalize();
		other.kde.normalize();
		int positionsToShiftOther = kde.shiftForOptimalCorrelation(other.kde);
		return kde.correlation(other.kde, positionsToShiftOther);
	}
	
	
	public static void main(String...strings) throws UnsupportedAudioFileException, IOException{
		 String file = "/home/joren/Desktop/Fingerprinting/07. Pleasant Shadow Song_original.wav.20_percent_slower.wav";
		 InterOnsetIntervalHistogram IOIH = new InterOnsetIntervalHistogram(file);
		 IOIH.printKDE();
		 
		 System.out.println();
		 System.out.println();
		 
		 file = "/home/joren/Desktop/Fingerprinting/07. Pleasant Shadow Song_original.wav.20_percent_faster.wav";
		 IOIH = new InterOnsetIntervalHistogram(file);
		 IOIH.printKDE();
	}
	


}
