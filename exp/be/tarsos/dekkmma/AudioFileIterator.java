package be.tarsos.dekkmma;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;

public class AudioFileIterator {
	
	String processedFilesList = "processed.txt";
	
	public AudioFileIterator(File directory){
		List<String> files = FileUtils.glob(directory.getAbsolutePath(), Configuration.get(ConfKey.audio_file_name_pattern), true);
		Collections.sort(files);
		if(!FileUtils.exists(processedFilesList)){
			FileUtils.writeFile("", processedFilesList);
		}
		String[] processedFilesArray = FileUtils.readFile(processedFilesList).split("\n");
		List<String> processedFiles = Arrays.asList(processedFilesArray);
		files.removeAll(processedFiles);		
	}
}
