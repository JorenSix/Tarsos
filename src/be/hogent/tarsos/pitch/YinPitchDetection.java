package be.hogent.tarsos.pitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.pitch.Yin.DetectedPitchHandler;
import be.hogent.tarsos.util.AudioFile;

public class YinPitchDetection implements PitchDetector {
    private static final Logger log = Logger.getLogger(YinPitchDetection.class.getName());

    private final AudioFile file;
    private final List<Sample> samples;
    private final String name;

    public YinPitchDetection(AudioFile audioFile) {
        this.file = audioFile;
        this.samples = new ArrayList<Sample>();
        this.name = "tarsos_yin";
    }

    @Override
    public void executePitchDetection() {
        try {
            Yin.processFile(file.path(), new DetectedPitchHandler() {
                @Override
                public void handleDetectedPitch(float time, float pitch) {
                    long start = (long) time * 1000;
                    Sample s = pitch == -1 ? new Sample(start) : new Sample(start, pitch);
                    samples.add(s);
                }
            });
        } catch (UnsupportedAudioFileException e) {
            log.severe("Unsupported audio file: " + file.basename() + " " + e.getMessage());
        } catch (IOException e) {
            log.severe("Exception while reading audio file: " + file.basename() + " " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Sample> getSamples() {
        return samples;
    }

}
