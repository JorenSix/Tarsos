package be.hogent.tarsos.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class PianoTestFrame extends JFrame {

    private static final long serialVersionUID = 6063312726815482475L;

    public PianoTestFrame(VirtualKeyboard keyboard, double[] tuning) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        Dimension dimension = new Dimension(650, 100);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        setMaximumSize(dimension);

        JPanel keyboardPanel = new JPanel(new BorderLayout());
        keyboardPanel.setBorder(new EmptyBorder(10, 20, 10, 5));
        keyboardPanel.add(keyboard, BorderLayout.CENTER);

        this.add(keyboard, BorderLayout.CENTER);
    }
}
