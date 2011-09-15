package be.hogent.tarsos.cli.temp;

import java.util.List;

import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.TarsosPitchDetection;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.histogram.HistogramFactory;
import be.hogent.tarsos.util.histogram.PitchHistogram;
import be.hogent.tarsos.util.histogram.PitchClassHistogram;

/**
 * @author Joren Six
 */
public final class PolyOrMonophonicPitchTracker {

    /**
     */
    private PolyOrMonophonicPitchTracker() {
    }

    /**
     * @param args
     *            Arguments.
     */
    public static void main(final String[] args) {
        final List<AudioFile> files = AudioFile.audioFiles("channels");
        for (final AudioFile file : files) {
            final PitchDetector detector = new TarsosPitchDetection(file, PitchDetectionMode.TARSOS_YIN);
            detector.executePitchDetection();
            final List<Annotation> samples = detector.getAnnotations();
            final PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(samples);
            final PitchClassHistogram pitchClassHistogram = pitchHistogram.pitchClassHistogram();
            final String title = detector.getName() + "_" + file.originalBasename();
            pitchClassHistogram.plot("data/tests/" + title + ".png", file.originalBasename());
        }
    }

}
