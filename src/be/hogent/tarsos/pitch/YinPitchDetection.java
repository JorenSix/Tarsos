package be.hogent.tarsos.pitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.pitch.Yin.DetectedPitchHandler;
import be.hogent.tarsos.util.AudioFile;

public class YinPitchDetection implements PitchDetector {
    private static final Logger LOG = Logger.getLogger(YinPitchDetection.class.getName());
    private static final String NAME = "tarsos_yin";

    private final AudioFile file;
    private final List<Sample> samples;

    public YinPitchDetection(final AudioFile audioFile) {
        this.file = audioFile;
        this.samples = new ArrayList<Sample>();
    }

    @Override
    public void executePitchDetection() {
        try {
            Yin.processFile(file.path(), new DetectedPitchHandler() {
                @Override
                public void handleDetectedPitch(final float time, final float pitch) {
                    final long start = (long) (time * 1000);
                    final Sample s = pitch == -1 ? new Sample(start) : new Sample(start, pitch);
                    samples.add(s);
                }
            });
        } catch (final UnsupportedAudioFileException e) {
            LOG.log(Level.SEVERE, "Unsupported audio file: " + file.basename() + " " + e.getMessage(), e);
        } catch (final IOException e) {
            LOG.log(Level.SEVERE, "Exception while reading audio file: " + file.basename() + " "
                    + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<Sample> getSamples() {
        return samples;
    }
}
