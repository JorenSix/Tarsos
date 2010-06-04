package be.hogent.tarsos.apps.temp;

import java.util.ArrayList;
import java.util.List;

import be.hogent.tarsos.pitch.AubioPitchDetection;
import be.hogent.tarsos.pitch.IPEMPitchDetection;
import be.hogent.tarsos.pitch.PitchDetectionMix;
import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

public final class PeakExtractor {

    private static void appendFile(List<Peak> peaks) {
        StringBuilder sb = new StringBuilder();
        sb.append(peaks.size()).append(";");
        for (int i = 0; i < peaks.size(); i++) {
            sb.append(peaks.get(i).getPosition()).append(";");
            sb.append(peaks.get(i).getHeight()).append(";");
        }
        sb.append(";\n");
        FileUtils.appendFile(sb.toString(), "peaks.csv");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String[] globDirectories = { "dekkmma_random", "makam", "maghreb" };
        List<AudioFile> files = AudioFile.audioFiles(globDirectories);

        double[] gaussians = { 0, 0.5, 1.0 };
        int[] windowsize = { 5, 10, 15, 20 };
        double[] threshold = { 0.5, 0.8, 1.0, 1.5 };

        FileUtils
        .writeFile(
                "file;detector;windowsize;threshold;gaussian smoothing factor;number of peaks;peakx;heightx\n",
        "peaks.csv");

        for (AudioFile file : files) {

            String baseName = file.basename();

            String annotationsDirectory = FileUtils.combine("data", "annotations", baseName);
            FileUtils.mkdirs(annotationsDirectory);
            String peaksDirectory = FileUtils.combine(annotationsDirectory, "peaks");
            FileUtils.mkdirs(peaksDirectory);

            List<Sample> samples;
            List<PitchDetector> detectors = new ArrayList<PitchDetector>();

            detectors.add(new AubioPitchDetection(file, PitchDetectionMode.AUBIO_YIN));
            detectors.add(new IPEMPitchDetection(file));

            PitchDetector mix = new PitchDetectionMix(new ArrayList<PitchDetector>(detectors), 0.02);
            detectors.add(mix);

            for (PitchDetector detector : detectors) {
                detector.executePitchDetection();
                samples = detector.getSamples();
                AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
                String ambitusTextFileName = FileUtils.combine(annotationsDirectory, baseName + "_"
                        + detector.getName() + "_ambitus.txt");
                String ambitusPNGFileName = FileUtils.combine(annotationsDirectory, baseName + "_"
                        + detector.getName() + "_ambitus.png");
                String coloredToneScalePNGFileName = FileUtils.combine(annotationsDirectory, baseName + "_"
                        + detector.getName() + "_tone_scale_colored.png");
                ambitusHistogram.plotToneScaleHistogram(coloredToneScalePNGFileName, true);
                ambitusHistogram.export(ambitusTextFileName);
                ambitusHistogram.plot(ambitusPNGFileName, "Ambitus " + baseName + " " + detector.getName());
                for (int i = 0; i < gaussians.length; i++) {

                    ToneScaleHistogram toneScaleHistogram = ambitusHistogram.toneScaleHistogram();
                    String toneScaleTextFileName = FileUtils.combine(annotationsDirectory, baseName + "_"
                            + detector.getName() + "_tone_scale.txt");
                    String toneScalePNGFileName = FileUtils.combine(annotationsDirectory, baseName + "_"
                            + detector.getName() + "_tone_scale.png");

                    toneScaleHistogram.export(toneScaleTextFileName);
                    toneScaleHistogram.plot(toneScalePNGFileName, "Tone scale " + baseName + " "
                            + detector.getName());
                    toneScaleHistogram.gaussianSmooth(gaussians[i]);

                    for (int j = 0; j < windowsize.length; j++) {
                        for (int k = 0; k < threshold.length; k++) {
                            FileUtils.appendFile(baseName + ";" + detector.getName() + ";" + threshold[k]
                                                                                                       + ";" + windowsize[j] + ";" + gaussians[i] + ";", "peaks.csv");
                            List<Peak> peaks = PeakDetector.detect(toneScaleHistogram, windowsize[j],
                                    threshold[k]);
                            Histogram peakHistogram = PeakDetector.newPeakDetection(peaks);
                            String peaksTitle = detector.getName() + "_" + baseName + "_peaks_"
                            + gaussians[i] + "_" + windowsize[j] + "_" + threshold[k];
                            SimplePlot p = new SimplePlot(peaksTitle);
                            appendFile(peaks);
                            p.addData(0, toneScaleHistogram);
                            p.addData(1, peakHistogram);
                            p.save(FileUtils.combine(peaksDirectory, peaksTitle + ".png"));
                            ToneScaleHistogram.exportPeaksToScalaFileFormat(FileUtils.combine(peaksDirectory,
                                    peaksTitle + ".scl"), peaksTitle, peaks);
                        }
                    }
                }
                samples.clear();
            }
        }
    }
}
