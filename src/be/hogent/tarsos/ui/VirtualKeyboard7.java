package be.hogent.tarsos.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JComponent;

public class VirtualKeyboard7 extends JComponent implements Receiver, Transmitter, VirtualKeyboard {

    private static final long serialVersionUID = 1L;

    private char[] virtualKeys = "zxcvbnmasdfghjqwertyu1234567".toCharArray();
    
    private boolean[] keyDown = new boolean[virtualKeys.length];
    
    private int lowestKey = 35;
    
    private Receiver recv = null;
    
    private int velocity = 80;
    
    private int channel = 0;
    
    private boolean[] noteDown = new boolean[128];
    
    private int midiNoteDown = -1;
    
    public int getMidiNote(int x, int y)
    {
        int w = getWidth();
        float nw = w / 128f;
        
        int wn = (int)(x / nw);
        int oct = wn / 7;
        int n = oct * 7 + wn % 7;
        if(n < 0) n = 0;
        if(n > 127) n = 127;
        return n;
    }
    
    private void allKeyboardKeyOff()
    {
        for (int i = 0; i < keyDown.length; i++) {
            if(keyDown[i])
            if((i + lowestKey) < 128)
            {
                ShortMessage sm = new ShortMessage();
                try {
                    sm.setMessage(ShortMessage.NOTE_OFF, channel, (i + lowestKey), 0);
                    if(recv != null)
                        recv.send(sm, -1);
                    send(sm, -1);
                } catch (InvalidMidiDataException e1) {
                    e1.printStackTrace();
                }                                        
                keyDown[i] = false;                        
            }
        }        
    }
    
    public void setChannel(int c) {
        channel = c;
    }

    public void setVelocity(int v) {
        velocity = v;
    }
    
    public VirtualKeyboard7() {
        super();
        setFocusable(true);
                
        addMouseListener(new MouseAdapter() 
        {
            public void mousePressed(MouseEvent e) {
                grabFocus();      
                Point p = e.getPoint();
                midiNoteDown = getMidiNote(p.x, p.y);
                
                ShortMessage sm = new ShortMessage();
                try {
                    sm.setMessage(ShortMessage.NOTE_ON, channel, getMidiNote(p.x, p.y), velocity);
                    if(recv != null)
                        recv.send(sm, -1);
                    send(sm, -1);
                } catch (InvalidMidiDataException e1) {
                    e1.printStackTrace();
                }                
            }

            public void mouseReleased(MouseEvent e) {
                //Point p = e.getPoint();
                //int midiNoteDown = getMidiNote(p.x, p.y);
                if(midiNoteDown == -1) return;
                ShortMessage sm = new ShortMessage();
                try {
                    sm.setMessage(ShortMessage.NOTE_OFF, channel, midiNoteDown, 0);
                    if(recv != null)
                        recv.send(sm, -1);
                    send(sm, -1);
                } catch (InvalidMidiDataException e1) {
                    e1.printStackTrace();
                }        
                midiNoteDown = -1;
            }            
        });
        
        addKeyListener(new KeyListener()
        {

            
            public void keyPressed(KeyEvent e) {
                char lc = Character.toLowerCase(e.getKeyChar());
                for (int i = 0; i < virtualKeys.length; i++) {
                    if(virtualKeys[i] == lc)
                    {
                        if(!keyDown[i])
                        if((i + lowestKey) < 128)
                        {
                            ShortMessage sm = new ShortMessage();
                            try {
                                sm.setMessage(ShortMessage.NOTE_ON, channel, (i + lowestKey), velocity);
                                if(recv != null)
                                    recv.send(sm, -1);
                                send(sm, -1);
                            } catch (InvalidMidiDataException e1) {
                                e1.printStackTrace();
                            }                                        
                            keyDown[i] = true;
                        }                        
                        return;
                    }
                }
            }

            public void keyReleased(KeyEvent e) {
                char lc = Character.toLowerCase(e.getKeyChar());
                for (int i = 0; i < virtualKeys.length; i++) {
                    if(virtualKeys[i] == lc)
                    {
                        if(keyDown[i])
                        if((i + lowestKey) < 128)
                        {
                            ShortMessage sm = new ShortMessage();
                            try {
                                sm.setMessage(ShortMessage.NOTE_OFF, channel, (i + lowestKey), 0);
                                if(recv != null)
                                    recv.send(sm, -1);
                                send(sm, -1);
                            } catch (InvalidMidiDataException e1) {
                                e1.printStackTrace();
                            }                                        
                            keyDown[i] = false;
                        }                        
                        return;
                    }
                }                
            }

            public void keyTyped(KeyEvent e) {
                
                if(e.getKeyChar() == '-')
                {
                    allKeyboardKeyOff();
                    lowestKey -= 7;
                    if(lowestKey < 0) lowestKey = 0;
                    repaint();
                }
                if(e.getKeyChar() == '+')
                {
                    allKeyboardKeyOff();
                    lowestKey += 7;
                    if(lowestKey > 127) lowestKey -= 7;
                    repaint();
                }
            }
            
        });
        
        addFocusListener(new FocusListener()
        {
            public void focusGained(FocusEvent e) {
                repaint();
            }

            public void focusLost(FocusEvent e) {
                repaint();
                allKeyboardKeyOff();
            }
            
        });
    }
    
        
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        
        int w = getWidth();
        int h = getHeight();
        
        float nw = w / 128f;
        float cx = 0;
        Rectangle2D rect = new Rectangle2D.Double();        
        for (int i = 0; i < 128; i++) {
            
                rect.setRect(cx, 0, nw, h);
                if(noteDown[i])
                    g2.setColor(new Color(0.8f, 0.8f, 0.95f));
                else
                    g2.setColor(Color.WHITE);
                g2.fill(rect);
                g2.setColor(Color.BLACK);
                g2.draw(rect);             
                              
                if(i % 7 == 0)
                g2.drawString("C", cx + 2, 12);
                
                if(hasFocus() && (i >= lowestKey))
                if(i >= lowestKey)
                {
                    if(i - lowestKey < virtualKeys.length)
                    {
                        g2.setColor(Color.GRAY);
                        char k = virtualKeys[i - lowestKey];
                        g2.drawString("" + k, cx + 2, h - 4);
                    }
                }
                
                cx += nw;
        }
    }

    public void close() {
        
        
    }

    public void send(MidiMessage message, long timeStamp) {
        if(message instanceof ShortMessage)
        {
            ShortMessage sm = (ShortMessage)message;
            if(sm.getChannel() == channel)
            if(sm.getCommand() == ShortMessage.NOTE_ON 
                || sm.getCommand() == ShortMessage.NOTE_OFF)
            {
                noteDown[sm.getData1()] = 
                    (sm.getCommand() == ShortMessage.NOTE_ON) && (sm.getData2() != 0);
                repaint();
            }              
        }
    }

    public Receiver getReceiver() {
        return recv;
    }

    public void setReceiver(Receiver receiver) {
        recv = receiver;
    }

}
