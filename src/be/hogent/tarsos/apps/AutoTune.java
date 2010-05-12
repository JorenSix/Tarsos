package be.hogent.tarsos.apps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import be.hogent.tarsos.util.FFT;

import com.sun.media.sound.AudioFloatConverter;
import com.sun.media.sound.AudioFloatInputStream;

public class AutoTune {

    /**
     * Choose a Mixer device using CLI.
     */
    public static int chooseDevice() {
        try {
            javax.sound.sampled.Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            for (int i = 0; i < mixers.length; i++) {
                javax.sound.sampled.Mixer.Info mixerinfo = mixers[i];
                if (AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0) {
                    System.out.println(i + " " + mixerinfo.toString());
                }
            }
            // choose MIDI input device
            System.out.print("Choose the Mixer device: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            int deviceIndex = Integer.parseInt(br.readLine());
            return deviceIndex;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number, please try again");
            return chooseDevice();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void main(String... args) throws LineUnavailableException {
        new Thread(new AudioProcessor(chooseDevice())).start();
    }

    private static class Speaker {
        private static final float SAMPLERATE = 44100;
        private static final int CHANNELS = 1;
        private static final int BITS = 16;

        private final AudioFormat format = new AudioFormat(SAMPLERATE, BITS, CHANNELS, true, false);
        private final AudioFloatConverter converter = AudioFloatConverter.getConverter(format);

        private SourceDataLine line;

        private Speaker() {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new Error("Line not supported");
            }

            // Obtain and open the line.
            try {
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
            } catch (LineUnavailableException ex) {
                System.out.println("Line not available.");
            }
        }

        public void write(final float[] originalData, final int start, final int end) {
            byte[] convertedData = new byte[originalData.length * 2];
            converter.toByteArray(originalData, convertedData);
            line.write(convertedData, start, end - start);
        }
    }

    private static class AudioProcessor implements Runnable {

        AudioFloatInputStream afis;
        float[] audioBuffer;
        FFT fft;
        Speaker speaker;

        float sampleRate = 44100;

        private AudioProcessor(int inputDevice) throws LineUnavailableException {
            speaker = new Speaker();
            javax.sound.sampled.Mixer.Info selected = AudioSystem.getMixerInfo()[inputDevice];
            Mixer mixer = AudioSystem.getMixer(selected);
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
            int numberOfSamples = (int) (0.1 * sampleRate);
            line.open(format, numberOfSamples);
            line.start();
            AudioInputStream stream = new AudioInputStream(line);
            afis = AudioFloatInputStream.getInputStream(stream);

            audioBuffer = new float[2048];
            fft = new FFT(1024);
        }

        @Override
        public void run() {

            try {
                boolean hasMoreBytes = afis.read(audioBuffer, 0, audioBuffer.length) != -1;
                while (hasMoreBytes) {

                    // float pitch = Yin.processBuffer(audioBuffer, SAMPLERATE);

                    // if(pitch > 440 && pitch < 3520){
                    // System.out.println(pitch);

                    // calculate fft
                    fft.forwardTransform(audioBuffer);

                    // scale pitch
                    /*
                     * int originalBin = (int) (pitch * audioBuffer.length /
                     * SAMPLERATE); int newBin = (int) (1760 audioBuffer.length
                     * / SAMPLERATE); int diff = newBin - originalBin; if(diff >
                     * 0) for(int i = audioBuffer.length - 1; i >= 0 ; i--){
                     * audioBuffer[i] = i - diff >= 0 ? audioBuffer[i-diff] : 0;
                     * } else for(int i = 0; i < audioBuffer.length ; i++){
                     * audioBuffer[i] = i-diff < audioBuffer.length ?
                     * audioBuffer[i-diff] : 0; }
                     */
                    // inverse fft
                    fft.backwardsTransform(audioBuffer);

                    // play resulting audio
                    speaker.write(audioBuffer, 0, 1024);
                    // }

                    for (int i = 0; i < 1024; i++) {
                        audioBuffer[i] = audioBuffer[1024 + i];
                    }
                    hasMoreBytes = afis.read(audioBuffer, audioBuffer.length - 1024, 1024) != -1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
