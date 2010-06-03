package be.hogent.tarsos.util;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.apps.Tarsos;

public final class MediaPlayer implements Runnable {
    File file;
    AudioInputStream in;
    SourceDataLine line;
    int frameSize;
    byte[] buffer = new byte[32 * 1024]; // 32k is arbitrary
    Thread playThread;
    boolean playing;
    boolean notYetEOF;

    public MediaPlayer(final String fileName) throws IOException, UnsupportedAudioFileException,
    LineUnavailableException {
        file = new File(fileName);
        in = AudioSystem.getAudioInputStream(file);
        final AudioFormat format = in.getFormat();
        final AudioFormat.Encoding formatEncoding = format.getEncoding();
        if (!(formatEncoding.equals(AudioFormat.Encoding.PCM_SIGNED) || formatEncoding
                .equals(AudioFormat.Encoding.PCM_UNSIGNED))) {
            throw new UnsupportedAudioFileException(file.getName() + " is not PCM audio");
        }
        Tarsos.println("got PCM format");
        frameSize = format.getFrameSize();
        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        Tarsos.println("got info");
        line = (SourceDataLine) AudioSystem.getLine(info);
        Tarsos.println("got line");
        line.open();
        Tarsos.println("opened line");
        playThread = new Thread(this);
        playing = false;
        notYetEOF = true;
    }

    public void run() {
        playThread.start();
        int readPoint = 0;
        int bytesRead = 0;

        try {
            while (notYetEOF) {
                if (playing) {
                    bytesRead = in.read(buffer, readPoint, buffer.length - readPoint);
                    if (bytesRead == -1) {
                        notYetEOF = false;
                        break;
                    }
                    // how many frames did we get,
                    // and how many are left over?
                    final int leftover = bytesRead % frameSize;
                    // send to line
                    line.write(buffer, readPoint, bytesRead - leftover);
                    // save the leftover bytes
                    System.arraycopy(buffer, bytesRead, buffer, 0, leftover);
                    readPoint = leftover;
                } else {
                    // if not playing
                    // Thread.yield();
                    try {
                        Thread.sleep(10);
                    } catch (final InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            } // while notYetEOF
            Tarsos.println("reached eof");
            line.drain();
            line.stop();
        } catch (final IOException ioe) {
            ioe.printStackTrace();
        }
    } // run

    public void start() {
        playing = true;
        if (!playThread.isAlive()) {
            playThread.start();
        }
        line.start();
    }

    public void stop() {
        playing = false;
        line.stop();
    }

    public SourceDataLine getLine() {
        return line;
    }

    public File getFile() {
        return file;
    }
}
