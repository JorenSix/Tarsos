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

import be.hogent.tarsos.pitch.Yin;
import be.hogent.tarsos.util.FFT;

import com.sun.media.sound.AudioFloatInputStream;

public class AutoTune {

	/**
	 * Choose a Mixer device using CLI.
	 */
	public static int chooseDevice(){
		try {
			javax.sound.sampled.Mixer.Info mixers[] = AudioSystem.getMixerInfo();
			for (int i = 0; i < mixers.length; i++) {
				javax.sound.sampled.Mixer.Info mixerinfo = mixers[i];
				if (AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0)
					System.out.println(i + " " + mixerinfo.toString());
			}
			//choose MIDI input device
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

	public static void main(String... args) throws LineUnavailableException{
		new Thread(new AudioProcessor(chooseDevice())).start();
	}

	private static class Speaker implements Runnable{

		    Mixer mixer;
			SourceDataLine line;
			static int sr = 44100;
			static float floatSr = 44100;
			static int channels = 1;
			static int bits = 16;
			static int bufferSize = 4096*4;
			AudioFormat format = new AudioFormat(floatSr,bits,channels,true,false);
		        //AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, floatSr, bits, channels, 2, floatSr, false); //2
			private Speaker() {
				DataLine.Info info = new DataLine.Info(SourceDataLine.class, format); // format is an AudioFormat object
				if (!AudioSystem.isLineSupported(info)) {
					System.exit(0);
			    	}

			    // Obtain and open the line.
				try {
				    line = (SourceDataLine) AudioSystem.getLine(info);
			   	 bufferSize = (int) format.getSampleRate() * format.getFrameSize();
				    //line.open(format,bufferSize);
				    line.open(format);
			            line.start();


				} catch (LineUnavailableException ex) {
					System.out.println("WTF open error");
					System.exit(0);
				}
			}
			void out_realtime(int [] b) {
				int length  = 2*b.length;
				if (length <= 0) {
					return ;
				}
				byte [] out = new byte[length];
				int tmp;
				for (int i = 0; i < b.length; i++) {
					tmp = b[i];
					tmp = tmp << 24;
					tmp = tmp >> 24;
					out[2*i+0]  = (byte)tmp;

					tmp = b[i];
					tmp = tmp << 16;
					tmp = tmp >> 24;
					out[2*i+1] = (byte)tmp;
				}
			}
			public void out_realtime(float [] data) {
				out_realtime(data,0,data.length);
			}
			public void out_realtime(float [] data,int start, int end) {
				try {
				int hi = 0;
				byte [] buffer = new byte[data.length*2];
				for (int i = start; i < end; i++) {
					hi = (int)data[i];
					//low = hi;
					//hi <<= 16;
					//hi >>= 24;
					//low <<= 24;
					//low >>= 24;
					//buffer[0+count] = (byte)low;
					//buffer[1+count] = (byte)hi;
					//buffer[1+2*i] = (byte)low; 	//little endian
					//buffer[0+2*i] = (byte)hi;
					buffer[2*i + 0] = (byte) (hi & 0xFF);
		                        buffer[2*i + 1] = (byte) ((hi >>> 8) & 0xFF);
				}
				line.write(buffer,0,buffer.length);
				} catch (Exception e) {
					System.err.println(e + "WTF");
				}
				//fileOut.close();
			}
			@Override
			public void run() {
				// TODO Auto-generated method stub

			}
	}
	private static class AudioProcessor implements Runnable{

		AudioFloatInputStream afis;
		float[] audioBuffer;
		FFT fft;
		FFT ifft;
		Speaker speaker;

		float sampleRate = 44100;

		private AudioProcessor(int inputDevice) throws LineUnavailableException{
			speaker = new Speaker();
			javax.sound.sampled.Mixer.Info selected = AudioSystem.getMixerInfo()[inputDevice];
			Mixer mixer = AudioSystem.getMixer(selected);
			AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
			DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class,
					format);
			TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
			int numberOfSamples = (int) (0.1 * sampleRate);
			line.open(format, numberOfSamples);
			line.start();
			AudioInputStream stream = new AudioInputStream(line);
			afis = AudioFloatInputStream.getInputStream(stream);

			audioBuffer = new float[2048];
			fft = new FFT(1024,-1);
			ifft = new FFT(1024,1);
		}

		@Override
		public void run() {

			try {
				boolean hasMoreBytes = afis.read(audioBuffer,0,audioBuffer.length) != -1;
				while(hasMoreBytes){

					float pitch = Yin.processBuffer(audioBuffer, sampleRate);

					if(pitch > 440 && pitch <  3520){
						System.out.println(pitch);

						//calculate fft
						fft.transform(audioBuffer);

						//scale pitch
						int originalBin = (int) (pitch * audioBuffer.length / sampleRate);
						int newBin = (int) (1760 * audioBuffer.length / sampleRate);
						int diff = newBin - originalBin;

						if(diff > 0)
							for(int i = audioBuffer.length - 1; i >= 0 ; i--){
								 audioBuffer[i] = i - diff >= 0 ? audioBuffer[i-diff] : 0;
							}
						else
							for(int i = 0; i < audioBuffer.length ; i++){
								 audioBuffer[i] = i-diff < audioBuffer.length ? audioBuffer[i-diff] : 0;
							}
						//inverse fft
						ifft.transform(audioBuffer);

						//play resulting audio
						speaker.out_realtime(audioBuffer);
					}



					for(int i = 0 ; i < 1024 ; i++)
						audioBuffer[i]=audioBuffer[1024+i];
					hasMoreBytes = afis.read(audioBuffer,audioBuffer.length-1024,1024) != -1;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
