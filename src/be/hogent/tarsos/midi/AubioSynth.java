package be.hogent.tarsos.midi;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import be.hogent.tarsos.midi.ToneSequenceBuilder.CSVFileHandler;
import be.hogent.tarsos.util.FileUtils;




/**
 * @author jsix666
 * Generates audio from a set of annotations.
 * The 
 *
 */
public class AubioSynth {
	
	public static void main(String[] args) throws IOException
	{	

		LongOpt[] longopts = new LongOpt[4];		
		longopts[0] = new LongOpt("out", LongOpt.REQUIRED_ARGUMENT, null, 'o');
		longopts[1] = new LongOpt("filter", LongOpt.REQUIRED_ARGUMENT, null, 'f'); 
		longopts[2] = new LongOpt("in", LongOpt.REQUIRED_ARGUMENT, null, 'i');
		longopts[3] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');		
		Getopt g = new Getopt("AubioSynth", args, "-o:f:i:h", longopts);		
		
		String outputFile = "out.wav";
		String inputFile = null;
		int medianFilterWindowSize = 0;
		
		int c;
		while ((c = g.getopt()) != -1)
		{
			String arg = g.getOptarg();
			switch(c)
			{
			case 'i':
				inputFile = arg;
				break;
			case 'o':
				outputFile = arg;
				break;
			case 'm':
				medianFilterWindowSize = Integer.parseInt(arg);
				break;
			case 'h':
				System.out.println("aubiosynth [--out out.wav] [--filter 5] [--in bla.csv]");
				System.exit(-1);
				return;
			}
		}

		CSVFileHandler handler = ToneSequenceBuilder.AUBIO_CSVFILEHANDLER;
		ToneSequenceBuilder builder = new ToneSequenceBuilder();
		String separator = handler.getSeparator();
		if(inputFile == null){
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String s;
			while ((s = in.readLine()) != null && s.length() != 0){
				String[] row = s.split(separator);
				handler.handleRow(builder, row);
			}
		}else{				
			List<String[]> rows = FileUtils.readCSVFile(inputFile,separator,2);
			for(String[] row : rows){
				handler.handleRow(builder, row);
			}
		}
		try {
			builder.writeFile(outputFile,medianFilterWindowSize);
		} catch (Exception e) {
			System.out.println("Could not write: " + outputFile + "\n");
			e.printStackTrace();
		}		
	}
}
