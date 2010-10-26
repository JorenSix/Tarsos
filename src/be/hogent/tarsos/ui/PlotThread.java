package be.hogent.tarsos.ui;

import java.util.Iterator;
import java.util.List;

import ptolemy.plot.Plot;
import ptolemy.plot.PlotApplication;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.util.StopWatch;

public final class PlotThread extends Thread {

	// private String title;
	private final List<Annotation> samples;
	private final StopWatch watch;

	public PlotThread(final String title, final List<Annotation> sampleList, final StopWatch stopWatch) {
		// this.title = title;
		this.samples = sampleList;
		this.watch = stopWatch;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		final Plot livePlot = new Plot();
		livePlot.setSize(1024, 786);
		// livePlot.setTitle(title);
		livePlot.setMarksStyle("dots");

		livePlot.setYRange(2000, 6000);
		// livePlot.setYLog(true);

		final Iterator<Annotation> sampleIterator = samples.iterator();
		Annotation currentSample = sampleIterator.next();

		new PlotApplication(livePlot);

		long currentTick = 0;
		for (; currentTick <= samples.get(samples.size() - 1).getStart(); currentTick += 100) {

			while (sampleIterator.hasNext() && currentSample.getStart() <= currentTick) {
				final double yValue = currentSample.getPitch(PitchUnit.HERTZ);
				livePlot.addPoint(1, currentTick, yValue, false);
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
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
