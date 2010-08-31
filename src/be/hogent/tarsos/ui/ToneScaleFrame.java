/**
 */
package be.hogent.tarsos.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JPanel;

import be.hogent.tarsos.midi.PitchSynth;
import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.pure.TarsosPitchDetection;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileDrop;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.Histogram;

/**
 * @author Joren Six
 */
public final class ToneScaleFrame extends JPanel {

    /**
     */
    private static final long serialVersionUID = 5493280409705136547L;

    private BufferedImage image;
    private Graphics2D graphics;
    private Histogram histo;

    private class MouseDragListener extends MouseAdapter implements MouseMotionListener {
        private final Point referenceDragPoint;
        private int prevButton = -1;
        private PitchSynth synth;

        public MouseDragListener() {
            referenceDragPoint = new Point(0, 0);
            synth = null;

            try {
                synth = new PitchSynth(false, true, 4);
            } catch (final MidiUnavailableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() == 2) {
                xOffset = 0;
                xReferenceOffset = 0;
                draw(0, 0);
            } else if (e.getButton() == MouseEvent.BUTTON1) {

            }
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            final double pitchInRelativeCents = e.getX() / ((double) getWidth()) * 1200.0;
            final int velocity = (int) (e.getY() / ((double) getHeight()) * 127);
            synth.play(pitchInRelativeCents, velocity);
        }



        @Override
        public void mousePressed(final MouseEvent e) {
            prevButton = e.getButton();
            referenceDragPoint.setLocation(e.getPoint());
        };

        @Override
        public void mouseDragged(final MouseEvent e) {
            if (!e.getPoint().equals(referenceDragPoint)) {
                final int histTranslation;
                final int refTranslation;
                if (prevButton == MouseEvent.BUTTON1) {
                    histTranslation = e.getPoint().x - referenceDragPoint.x;
                    refTranslation = 0;
                } else {
                    histTranslation = 0;
                    refTranslation = e.getPoint().x - referenceDragPoint.x;
                }
                draw(histTranslation, refTranslation);
                referenceDragPoint.setLocation(e.getPoint());
            }
        }
    }


    private final MouseDragListener listener = new MouseDragListener();

    public ToneScaleFrame(final Histogram histo) {
        this.setSize(640, 480);

        this.histo = histo;

        this.addMouseMotionListener(listener);
        this.addMouseListener(listener);

        initializeGraphics();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                initializeGraphics();
            }
        });

        new FileDrop(this, new FileDrop.Listener() {
            public void filesDropped(final java.io.File[] files) {
                for(final File droppedFile: files){
                    if(droppedFile.getName().endsWith(".scl")) {
                        setReferenceScale(new ScalaFile(droppedFile.getAbsolutePath()).getPitches());
                    } else if(FileUtils.isAudioFile(droppedFile)){
                        final AudioFile audioFile = new AudioFile(droppedFile.getAbsolutePath());

                        final PitchDetector pitchDetector = new TarsosPitchDetection(audioFile,
                                PitchDetectionMode.TARSOS_YIN);
                        pitchDetector.executePitchDetection();
                        final List<Sample> samples = pitchDetector.getSamples();
                        final AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
                        ToneScaleFrame.this.histo = ambitusHistogram.toneScaleHistogram();
                        draw(0, 0);
                    }
                }

            }   // end filesDropped
        }); // end FileDrop.Listener
    }

    public void setReferenceScale(final double[] referenceScale) {
        this.referenceScale = referenceScale;
        // align the peak picking with graph
        xReferenceOffset = xOffset;
        draw(0, 0);
    }

    private void initializeGraphics() {
        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_BGR);
        graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        graphics.setColor(Color.RED);
        graphics.setBackground(Color.WHITE);
        draw(0, 0);
    }

    @Override
    public void paint(final Graphics g) {
        g.drawImage(image, 0, 0, null);
    }

    public Histogram getHistogram() {
        return this.histo;
    }

    private double xOffset;
    private double xReferenceOffset = 0;
    double referenceScale[] = { 0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100 };

    public void draw(final int histTranslation, final int refTranslation) {

        xOffset = xOffset + histTranslation / ((double) getWidth());
        if (xOffset < 0) {
            xOffset = 1.0 + xOffset;
        }
        final double delta = histo.getStop() - histo.getStart();
        final long maxCount = histo.getMaxBinCount();

        final int xOffsetPixels = (int) Math.round(xOffset * getWidth());
        int x = xOffsetPixels;

        int y = (getHeight() - 5)
        - (int) (histo.getCount(histo.getStop()) / ((double) maxCount) * getHeight() * 0.9);
        Point previousPoint = new Point(x, y);

        graphics.clearRect(0, 0, getWidth(), getHeight());
        drawRefence(refTranslation);
        for (final double key : histo.keySet()) {
            x = (int) ((key / delta * getWidth()) + xOffsetPixels) % getWidth();
            y = (getHeight() - 5) - (int) (histo.getCount(key) / ((double) maxCount) * getHeight() * 0.9);
            graphics.setColor(Color.RED);
            if (x > previousPoint.x) {
                graphics.drawLine(previousPoint.x, previousPoint.y, x, y);
            }
            previousPoint = new Point(x, y);
        }

        repaint();
    }

    public void drawRefence(final int xTranslation) {
        xReferenceOffset = xReferenceOffset + xTranslation / (double) getWidth();
        if (xReferenceOffset < 0) {
            xReferenceOffset = 1.0 + xReferenceOffset;
        }

        final double delta = histo.getStop() - histo.getStart();
        graphics.setColor(Color.GRAY);

        final int xOffsetPixels = (int) Math.round(xReferenceOffset * getWidth());

        for (final double reference : referenceScale) {
            final int x = (int) ((reference / delta * getWidth()) + xOffsetPixels) % getWidth();
            graphics.drawLine(x, 40, x, getHeight());
            final String text = Integer.valueOf((int) reference).toString();
            final int stringLen = text.length();
            final double width = graphics.getFontMetrics().getStringBounds(text, graphics).getWidth();
            final int start = (int) width / 2 - stringLen / 2;
            graphics.drawString(text, x - start, 20);
        }
        graphics.setColor(Color.RED);
    }

}
