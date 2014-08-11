/**
 *
 *  Tarsos is developed by Joren Six at 
 *  The Royal Academy of Fine Arts & Royal Conservatory,
 *  University College Ghent,
 *  Hoogpoort 64, 9000 Ghent - Belgium
 *
 **/
package be.tarsos.exp.ui.fingerprinter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Logger;

import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.AnnotationSelection;
import be.tarsos.sampled.pitch.AnnotationTree;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.AudioFile;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;
import be.tarsos.util.KernelDensityEstimate;
import be.tarsos.util.StopWatch;
import be.tarsos.util.TimeUnit;
import be.tarsos.util.histogram.HistogramFactory;

public class FragmentFinder {

	private static String DATASET = "/media/data/AUDIO ARCHIEF KMMA";
	private static String FRAGMENTS = "/home/joren/Desktop/audio tempo/fragments";
	private static double TIME_STEP = 3;//second
	private static PitchDetectionMode detectionMode = PitchDetectionMode.TARSOS_FFT_YIN;
	
	
	private static final Logger LOG = Logger.getLogger(FragmentFinder.class.getName());
	
	private static List<Annotation> executePitchDetection(String file, List<AudioFile> audioFiles) throws EncoderException{
		AudioFile audioFile;
		audioFile = new AudioFile(file);
		audioFiles.add(audioFile);
		PitchDetector detector = detectionMode.getPitchDetector(audioFile);
		return detector.executePitchDetection();
	}

	public static void main(String... strings) throws EncoderException {

		Logger globalLogger = Logger.getLogger("");
		Handler[] handlers = globalLogger.getHandlers();
		for (Handler handler : handlers) {
			//globalLogger.removeHandler(handler);
		}

		String pattern = Configuration.get(ConfKey.audio_file_name_pattern);
		List<String> dataset = FileUtils.glob(DATASET, pattern, true);
		dataset = dataset.subList(3300, dataset.size()-1);
		List<String> fragments = FileUtils.glob(FRAGMENTS, pattern, false);
		List<KernelDensityEstimate> fragmentHistograms = new ArrayList<KernelDensityEstimate>();
		List<AnnotationTree> annotationTrees = new ArrayList<AnnotationTree>();
		List<AudioFile> datasetAudioFiles = new ArrayList<AudioFile>();
		List<AudioFile> fragmentAudioFiles = new ArrayList<AudioFile>();

		for (String fragment : fragments) {
			try {
				StopWatch w = new StopWatch();
				List<Annotation> annotations = executePitchDetection(fragment,fragmentAudioFiles);
				KernelDensityEstimate kde = HistogramFactory.createPichClassKDE(annotations, 7);
				fragmentHistograms.add(kde);
				LOG.info("Analysed pitch for: " + FileUtils.basename(fragment) + " It took " + w.toString());
			} catch (EncoderException e) {
				e.printStackTrace();
				LOG.severe("Failed to analyse pitch for: " + fragment + " " + e.getMessage());
			}
		}

		for (String file : dataset) {
			try {
				StopWatch w = new StopWatch();
				List<Annotation> annotations = executePitchDetection(file,datasetAudioFiles);
				AnnotationTree tree = new AnnotationTree(PitchUnit.ABSOLUTE_CENTS);
				tree.add(annotations);
				annotationTrees.add(tree);
				LOG.info("Analysed pitch for: " + FileUtils.basename(file) + " It took " + w.toString());
			} catch (EncoderException e) {
				e.printStackTrace();
				LOG.severe("Failed to analyse pitch for: " + file + " " + e.getMessage());
			}
		}

		List<String> messages = new ArrayList<String>();
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	    Date reference = null;
		try {
			reference = dateFormat.parse("00:00:00");
		} catch (ParseException e) {
		}
	    Calendar cal = Calendar.getInstance();
	    
		System.out.println("Fragment;Match (0-1);Hit;start;stop");
		for (int i = 0; i < fragmentHistograms.size(); i++) {
			KernelDensityEstimate fragmentKDE = fragmentHistograms.get(i);
			double length = fragmentAudioFiles.get(i).getLengthIn(TimeUnit.SECONDS);
			AnnotationSelection selection = new AnnotationSelection();
			StopWatch watch = new StopWatch();
			boolean foundMatch = false;
			for (int j = 0; j < datasetAudioFiles.size() && !foundMatch; j++) {
				AnnotationTree tree = annotationTrees.get(j);
				double lengthInSeconds = datasetAudioFiles.get(j).getLengthIn(TimeUnit.SECONDS);
				selection.setTimeSelection(0, 0 + length);
				KernelDensityEstimate kde = HistogramFactory.createPichClassKDE(tree.select(selection), 7);
				String fragmentBaseName = FileUtils.basename(fragments.get(i));
				String dataSetFileBaseName = FileUtils.basename(dataset.get(j));
				for (double t = 0; t < lengthInSeconds - length; t += TIME_STEP) {
					double value = kde.correlation(fragmentKDE, 0);
					if(value > 0.95)
						foundMatch=true;
				    cal.setTime(reference);
				    cal.add(Calendar.SECOND, (int) t);
				    String start = dateFormat.format(cal.getTime());
				    cal.add(Calendar.SECOND, (int) (length));
				    String stop = dateFormat.format(cal.getTime()); 

					String message = String.format("%s;%.4f;%s;%s;%s",fragmentBaseName, value,dataSetFileBaseName, start,stop);
					messages.add(message);
					if (messages.size() > 100) {
						Collections.sort(messages);
						Collections.reverse(messages);
						messages = messages.subList(0, 15);
					}
		
					//change kde: remove annotations before new start point
					selection.setTimeSelection(t, t + TIME_STEP);
					for(Annotation a : tree.select(selection)){
						kde.remove(a.getPitch(PitchUnit.RELATIVE_CENTS));
					}
					//add annotations after stop point
					selection.setTimeSelection(t + length, t + length + TIME_STEP);
					for(Annotation a : tree.select(selection)){
						kde.add(a.getPitch(PitchUnit.RELATIVE_CENTS));
					}
				}
			}
			Collections.sort(messages);
			Collections.reverse(messages);
			messages = messages.subList(0, Math.min(messages.size() - 1, 15));
			for (String message : messages) {
				System.out.println(message+";" + watch.formattedToString() + "\n");
			}
			messages = new ArrayList<String>();
		}
	}
}
