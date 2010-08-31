/**
 */
package be.hogent.tarsos.ui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.hogent.tarsos.pitch.PitchFunctions;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

/**
 * @author Joren Six
 */
public class Frame extends JFrame {
    /**
     */
    private static final long serialVersionUID = -8095965296377515567L;

    public Frame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(640, 480);
        final String loc = "C:\\Users\\jsix666\\eclipse_workspace\\be.hogent.tarsos.test\\files\\input\\data\\other_african_octave_frequency_table.txt";
        final Histogram histo = PitchFunctions.readFrequencyTable(loc);
        final ToneScaleFrame panel = new ToneScaleFrame(histo);

        final JSlider silder = new JSlider(0, 100);
        silder.setValue(0);
        silder.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = ((JSlider) e.getSource());
                // if (!source.getValueIsAdjusting()) {
                final double value = source.getValue();
                // panel.getHistogram().gaussianSmooth(value);
                // panel.draw(0, 0);
                final List<Peak> peaks = PeakDetector.detect(panel.getHistogram(), (int) value, 0.5);
                final double[] peaksInCents = new double[peaks.size()];
                int i = 0;
                for (final Peak peak : peaks) {
                    peaksInCents[i++] = peak.getPosition();
                }
                panel.setReferenceScale(peaksInCents);
                // }
            }
        });

        add(panel, BorderLayout.CENTER);
        add(silder, BorderLayout.SOUTH);
    }

    public static void main(final String... strings) {
        final Frame frame = new Frame();
        frame.setVisible(true);
    }
}
