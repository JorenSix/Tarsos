package be.hogent.tarsos.ui;

import java.util.Iterator;
import java.util.List;

import ptolemy.plot.Plot;
import ptolemy.plot.PlotApplication;
import be.hogent.tarsos.pitch.PitchUnit;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.util.StopWatch;

public class PlotThread extends Thread {

    // private String title;
    private final List<Sample> samples;
    private final StopWatch watch;

    public PlotThread(String title, List<Sample> samples, StopWatch watch) {
        // this.title = title;
        this.samples = samples;
        this.watch = watch;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

        Plot livePlot = new Plot();
        livePlot.setSize(1024, 786);
        // livePlot.setTitle(title);
        livePlot.setPointsPersistence(3000);
        livePlot.setMarksStyle("dots");

        livePlot.setYRange(2000, 6000);
        // livePlot.setYLog(true);

        Iterator<Sample> sampleIterator = samples.iterator();
        Sample currentSample = sampleIterator.next();

        new PlotApplication(livePlot);

        for (long currentTick = 0; currentTick <= samples.get(
                samples.size() - 1).getStart(); currentTick += 100) {

            while (sampleIterator.hasNext()
                    && currentSample.getStart() <= currentTick) {
                for (Double pitch : currentSample.getPitchesWithoutHarmonicsIn(
                        PitchUnit.ABSOLUTE_CENTS, 0.07)) {
                    double yValue = pitch;
                    livePlot.addPoint(1, currentTick, yValue, false);
                }
                currentSample = sampleIterator.next();
            }

            int numberOfTicksToSleep = (int) (currentTick - watch.ticksPassed());
            if (numberOfTicksToSleep > 0) {
                livePlot.setXRange(currentTick - 9900, currentTick + 1000);

                livePlot.repaint();
                numberOfTicksToSleep = (int) (currentTick - watch.ticksPassed());
                if (numberOfTicksToSleep > 0) {
                    try {
                        Thread.sleep(numberOfTicksToSleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
