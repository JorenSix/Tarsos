package be.hogent.tarsos.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import be.hogent.tarsos.midi.MidiCommon;
import be.hogent.tarsos.midi.PlayAlong;
import be.hogent.tarsos.test.data.VirtualKeyboard;


public class PianoTestFrame extends JFrame{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6063312726815482475L;
	
	Piano piano;
	ChannelData cc;    // current channel

	
	
	public PianoTestFrame(JComponent keyboard, double[] tuning){
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		setLayout(new BorderLayout());
	    Dimension dimension = new Dimension(650, 100);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        setMaximumSize(dimension);
        
        JPanel keyboardPanel = new JPanel(new BorderLayout());
        keyboardPanel.setBorder(new EmptyBorder(10,20,10,5));
        keyboardPanel.add(keyboard,BorderLayout.CENTER);
        
        
        
        Receiver recv;
        
        try {
        	 MidiDevice.Info synthInfo = MidiCommon.getMidiDeviceInfo("LoopBe Internal MIDI",true);
			    MidiDevice	outputDevice = null;
			    outputDevice = MidiSystem.getMidiDevice(synthInfo);
			    outputDevice.open();
			    recv = outputDevice.getReceiver();
				
			
			((VirtualKeyboard) keyboard).setReceiver(recv);

			sendTunings(recv, 0, 5, "african", tuning);
			sendTuningChange(recv, 0, 5);
			
			MidiDevice midiInputDevice = PlayAlong.getMidiDeviceInfo("Keystation 49e",false);
			midiInputDevice.open();
			Transmitter	midiInputTransmitter = midiInputDevice.getTransmitter();
			midiInputTransmitter.setReceiver(recv);
			
			//DumpReceiver dumpReceiver = new DumpReceiver(System.out);
			//midiInputTransmitter.setReceiver(dumpReceiver);
			
			//midiChannels[0].noteOn(69, 80);

			this.add(keyboard,BorderLayout.CENTER);
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	/**
	 * Black and white keys or notes on the piano.
	 */
	class Key extends Rectangle {
	    /**
		 * 
		 */
		private static final long serialVersionUID = 6063212726815482475L;
		boolean noteOn = false;
	    int midiKeyNumber;
	    public Key(int x, int y, int width, int height, int keyNumber) {
	        super(x, y, width, height);
	        midiKeyNumber = keyNumber;
	    }
	    public boolean isNoteOn() {
	        return noteOn;
	    }
	    public void on() {
	        setNoteState(true);
	        cc.channel.noteOn(midiKeyNumber, cc.velocity);
	    }
	    public void off() {
	    	noteOn = false;
	        cc.channel.noteOff(midiKeyNumber, cc.velocity);
	    }
	    public void setNoteState(boolean on) {
	    	noteOn = on;
	    }
	} // End class Key
		/**
	     * Piano renders black & white keys and plays the notes for a MIDI 
	     * channel.  
	     */
	    class Piano extends JPanel implements MouseListener {

			private static final long serialVersionUID = -1495278340286220238L;
			Vector<Key> blackKeys = new Vector<Key>();
	        Vector<Key> whiteKeys = new Vector<Key>();
	        Vector<Key> keys = new Vector<Key>();
	        Key prevKey;
	        final int kw = 16, kh = 80;
	        final Color jfcBlue = new Color(204, 204, 255);


	        public Piano() {
	            setLayout(new BorderLayout());
	            setPreferredSize(new Dimension(42*kw, kh+1));
	            int transpose = 24;  
	            int whiteIDs[] = { 0, 2, 4, 5, 7, 9, 11 }; 
	          
	            for (int i = 0, x = 0; i < 6; i++) {
	                for (int j = 0; j < 7; j++, x += kw) {
	                    int keyNum = i * 12 + whiteIDs[j] + transpose;
	                    whiteKeys.add(new Key(x, 0, kw, kh, keyNum));
	                }
	            }
	            for (int i = 0, x = 0; i < 6; i++, x += kw) {
	                int keyNum = i * 12 + transpose;
	                blackKeys.add(new Key((x += kw)-4, 0, kw/2, kh/2, keyNum+1));
	                blackKeys.add(new Key((x += kw)-4, 0, kw/2, kh/2, keyNum+3));
	                x += kw;
	                blackKeys.add(new Key((x += kw)-4, 0, kw/2, kh/2, keyNum+6));
	                blackKeys.add(new Key((x += kw)-4, 0, kw/2, kh/2, keyNum+8));
	                blackKeys.add(new Key((x += kw)-4, 0, kw/2, kh/2, keyNum+10));
	            }
	            keys.addAll(blackKeys);
	            keys.addAll(whiteKeys);

	            /*addMouseMotionListener(new MouseMotionAdapter() {
	                public void mouseMoved(MouseEvent e) {
                        Key key = getKey(e.getPoint());
                        if (prevKey != null && prevKey != key) {
                            prevKey.off();
                        } 
                        if (key != null && prevKey != key) {
                            key.on();
                        }
                        prevKey = key;
                        repaint();
	                }
	            });*/
	            addMouseListener(this);
	        }
	        
	        public void mousePressed(MouseEvent e) { 
	            prevKey = getKey(e.getPoint());
	            if (prevKey != null) {
	                prevKey.on();
	                repaint();
	            }
	        }
	        public void mouseReleased(MouseEvent e) { 
	            if (prevKey != null) {
	                prevKey.off();
	                repaint();
	            }
	        }
	        public void mouseExited(MouseEvent e) { 
	           /* if (prevKey != null) {
	                prevKey.off();
	                repaint();
	                prevKey = null;
	            }*/
	        }
	        public void mouseClicked(MouseEvent e) { }
	        public void mouseEntered(MouseEvent e) { }


	        public Key getKey(Point point) {
	            for (int i = 0; i < keys.size(); i++) {
	                if (((Key) keys.get(i)).contains(point)) {
	                    return (Key) keys.get(i);
	                }
	            }
	            return null;
	        }

	        public void paint(Graphics g) {
	            Graphics2D g2 = (Graphics2D) g;
	            Dimension d = getSize();

	            g2.setBackground(getBackground());
	            g2.clearRect(0, 0, d.width, d.height);

	            g2.setColor(Color.white);
	            g2.fillRect(0, 0, 42*kw, kh);

	            for (int i = 0; i < whiteKeys.size(); i++) {
	                Key key = (Key) whiteKeys.get(i);
	                if (key.isNoteOn()) {
	                    g2.setColor(jfcBlue);
	                    g2.fill(key);
	                }
	                g2.setColor(Color.black);
	                g2.draw(key);
	            }
	            for (int i = 0; i < blackKeys.size(); i++) {
	                Key key = (Key) blackKeys.get(i);
	                if (key.isNoteOn()) {
	                    g2.setColor(jfcBlue);
	                    g2.fill(key);
	                    g2.setColor(Color.black);
	                    g2.draw(key);
	                } else {
	                    g2.setColor(Color.black);
	                    g2.fill(key);
	                }
	            }
	        }
	    }
	    
	    public static void sendTuningChange(Receiver recv, int channel,
	            int tuningpreset) throws InvalidMidiDataException {
	        // Data Entry
	        ShortMessage sm1 = new ShortMessage();
	        sm1.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x64, 03);
	        ShortMessage sm2 = new ShortMessage();
	        sm2.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x65, 00);
	        // Tuning program 19
	        ShortMessage sm3 = new ShortMessage();
	        sm3
	                .setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x06,
	                        tuningpreset);

	        // Data Increment
	        ShortMessage sm4 = new ShortMessage();
	        sm4.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x60, 0x7F);
	        // Data Decrement
	        ShortMessage sm5 = new ShortMessage();
	        sm5.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x61, 0x7F);

	        recv.send(sm1, -1);
	        recv.send(sm2, -1);
	        recv.send(sm3, -1);
	        recv.send(sm4, -1);
	        recv.send(sm5, -1);
	    }

	    public static void sendTunings(Receiver recv, int bank, int preset,
	            String name, double[] tunings) throws IOException,
	            InvalidMidiDataException {
	        int[] itunings = new int[128];
	        for (int i = 0; i < itunings.length; i++) {
	            itunings[i] = (int) (tunings[i] * 16384.0 / 100.0);
	        }

	        SysexMessage msg = UniversalSysExBuilder.MidiTuningStandard
	                .keyBasedTuningDump(UniversalSysExBuilder.ALL_DEVICES, bank,
	                        preset, name, itunings);
	        recv.send(msg, -1);
	    }

	    

	    
	    /**
	     * Stores MidiChannel information.
	     */
	    class ChannelData {

	        MidiChannel channel;
	        boolean solo, mono, mute, sustain;
	        int velocity, pressure, bend, reverb;
	        int row, col, num;
	 
	        public ChannelData(MidiChannel channel, int num) {
	            this.channel = channel;
	            this.num = num;
	            velocity = pressure = bend = reverb = 64;
	        }
	    } // End class ChannelData
}


