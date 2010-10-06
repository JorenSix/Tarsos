// DFilter.java (C) 2005 by Paul Falstad, www.falstad.com
package be.hogent.tarsos.sampled;

import java.applet.Applet;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.LayoutManager;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.MemoryImageSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

class DFilterCanvas extends Canvas {
	DFilterFrame pg;

	DFilterCanvas(DFilterFrame p) {
		pg = p;
	}

	
	public Dimension getPreferredSize() {
		return new Dimension(300, 400);
	}

	
	public void update(Graphics g) {
		pg.updateDFilter(g);
	}

	
	public void paint(Graphics g) {
		pg.updateDFilter(g);
	}
};

class DFilterLayout implements LayoutManager {
	public DFilterLayout() {
	}

	public void addLayoutComponent(String name, Component c) {
	}

	public void removeLayoutComponent(Component c) {
	}

	public Dimension preferredLayoutSize(Container target) {
		return new Dimension(500, 500);
	}

	public Dimension minimumLayoutSize(Container target) {
		return new Dimension(100, 100);
	}

	public void layoutContainer(Container target) {
		Insets insets = target.insets();
		int targetw = target.size().width - insets.left - insets.right;
		int cw = targetw * 7 / 10;
		int targeth = target.size().height - (insets.top + insets.bottom);
		target.getComponent(0).move(insets.left, insets.top);
		target.getComponent(0).resize(cw, targeth);
		int barwidth = targetw - cw;
		cw += insets.left;
		int i;
		int h = insets.top;
		for (i = 1; i < target.getComponentCount(); i++) {
			Component m = target.getComponent(i);
			if (m.isVisible()) {
				Dimension d = m.getPreferredSize();
				if (m instanceof Scrollbar) {
					d.width = barwidth;
				}
				if (m instanceof Choice) {
					d.width = barwidth;
				}
				if (m instanceof Label) {
					h += d.height / 5;
					d.width = barwidth;
				}
				m.move(cw, h);
				m.resize(d.width, d.height);
				h += d.height;
			}
		}
	}
};

public class DFilter extends Applet implements ComponentListener {
	DFilterFrame ogf;

	void destroyFrame() {
		if (ogf != null) {
			ogf.dispose();
		}
		ogf = null;
		repaint();
	}

	boolean started = false;

	
	public void init() {
		addComponentListener(this);
	}

	void showFrame() {
		if (ogf == null) {
			started = true;
			try {
				ogf = new DFilterFrame(this);
				ogf.init();
			} catch (Exception e) {
				e.printStackTrace();
				ogf = null;
				security = true;
				repaint();
			}
			repaint();
		}
	}

	boolean security = false;

	
	public void paint(Graphics g) {
		String s = "Applet is open in a separate window.";
		if (security) {
			s = "Security exception, use nosound version";
		} else if (!started) {
			s = "Applet is starting.";
		} else if (ogf == null) {
			s = "Applet is finished.";
		} else {
			ogf.show();
		}
		g.drawString(s, 10, 30);
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
		showFrame();
	}

	public void componentResized(ComponentEvent e) {
	}

	
	public void destroy() {
		if (ogf != null) {
			ogf.dispose();
		}
		ogf = null;
		repaint();
	}
};

class DFilterFrame extends Frame implements ComponentListener, ActionListener, AdjustmentListener,
		MouseMotionListener, MouseListener, ItemListener {

	Dimension winSize;
	Image dbimage;
	View respView, impulseView, phaseView, stepView, spectrumView, waveformView, poleInfoView, polesView;

	Random random;
	int maxSampleCount = 70; // was 50
	int sampleCountR, sampleCountTh;
	int modeCountR, modeCountTh;
	int maxDispRModes = 5, maxDispThModes = 5;
	public static final double epsilon = .00001;
	public static final double epsilon2 = .003;
	public static final double log10 = 2.30258509299404568401;
	public static int WINDOW_KAISER = 4;

	public String getAppletInfo() {
		return "DFilter Series by Paul Falstad";
	}

	Checkbox soundCheck;
	Checkbox displayCheck;
	Checkbox shiftSpectrumCheck;
	// Checkbox woofCheck;
	CheckboxMenuItem freqCheckItem;
	CheckboxMenuItem phaseCheckItem;
	CheckboxMenuItem spectrumCheckItem;
	CheckboxMenuItem impulseCheckItem;
	CheckboxMenuItem stepCheckItem;
	CheckboxMenuItem waveformCheckItem;
	CheckboxMenuItem logFreqCheckItem;
	CheckboxMenuItem logAmpCheckItem;
	CheckboxMenuItem allWaveformCheckItem;
	CheckboxMenuItem ferrisCheckItem;
	MenuItem exitItem;
	Choice filterChooser;
	int selection;
	final int SELECT_RESPONSE = 1;
	final int SELECT_SPECTRUM = 2;
	final int SELECT_POLES = 3;
	int filterSelection;
	Choice inputChooser;
	Choice windowChooser;
	Choice rateChooser;
	Scrollbar auxBars[];
	Label auxLabels[];
	Label inputLabel;
	Scrollbar inputBar;
	Label shiftFreqLabel;
	Scrollbar shiftFreqBar;
	Label kaiserLabel;
	Scrollbar kaiserBar;
	boolean editingFunc;
	boolean dragStop;
	double inputW;
	static final double pi = 3.14159265358979323846;
	double step;
	double waveGain = 1. / 65536;
	double outputGain = 1;
	int sampleRate;
	int xpoints[] = new int[4];
	int ypoints[] = new int[4];
	int dragX, dragY;
	int dragStartX, dragStartY;
	int mouseX, mouseY;
	int selectedPole, selectedZero;
	int lastPoleCount = 2, lastZeroCount = 2;
	boolean dragSet, dragClear;
	boolean dragging;
	boolean unstable;
	MemoryImageSource imageSource;
	Image memimage;
	int pixels[];
	double t;
	int pause;
	PlayThread playThread;
	Filter curFilter;
	FilterType filterType;
	double spectrumBuf[];
	FFT spectrumFFT;
	Waveform wformInfo;
	PhaseColor phaseColors[];
	static final int phaseColorCount = 50 * 8;
	boolean filterChanged;

	class View extends Rectangle {
		View(Dimension r) {
			super(r);
		}

		View(int a, int b, int c, int d) {
			super(a, b, c, d);
			right = a + c - 1;
			bottom = b + d - 1;
		}

		int right, bottom;

		void drawLabel(Graphics g, String str) {
			g.setColor(Color.white);
			centerString(g, str, y - 5);
		}
	}

	int getrand(int x) {
		int q = random.nextInt();
		if (q < 0) {
			q = -q;
		}
		return q % x;
	}

	DFilterCanvas cv;
	DFilter applet;
	NumberFormat showFormat;

	DFilterFrame(DFilter a) {
		super("Digital Filters Applet v1.2");
		applet = a;
	}

	boolean java2 = false;
	String mp3List[];
	String mp3Error;

	public void init() {
		mp3List = new String[20];

		try {
			String param = applet.getParameter("PAUSE");
			if (param != null) {
				pause = Integer.parseInt(param);
			}
			int i;
			for (i = 0; i < mp3List.length; i++) {
				param = applet.getParameter("mp3File" + (i + 1));
				if (param == null) {
					break;
				}
				mp3List[i] = param;
			}
		} catch (Exception e) {
		}

		String jv = System.getProperty("java.class.version");
		double jvf = new Double(jv).doubleValue();
		if (jvf >= 48) {
			java2 = true;
		}

		int j;
		int pc8 = phaseColorCount / 8;
		phaseColors = new PhaseColor[phaseColorCount];
		int i;
		for (i = 0; i != 8; i++) {
			for (j = 0; j != pc8; j++) {
				double ang = Math.atan(j / (double) pc8);
				phaseColors[i * pc8 + j] = genPhaseColor(i, ang);
			}
		}

		customPoles = new Complex[20];
		customZeros = new Complex[20];
		for (i = 0; i != customPoles.length; i++) {
			customPoles[i] = new Complex();
		}
		for (i = 0; i != customZeros.length; i++) {
			customZeros[i] = new Complex();
		}

		setLayout(new DFilterLayout());
		cv = new DFilterCanvas(this);
		cv.addComponentListener(this);
		cv.addMouseMotionListener(this);
		cv.addMouseListener(this);
		add(cv);

		MenuBar mb = new MenuBar();
		Menu m = new Menu("File");
		mb.add(m);
		m.add(exitItem = getMenuItem("Exit"));
		m = new Menu("View");
		mb.add(m);
		m.add(freqCheckItem = getCheckItem("Frequency Response", true));
		m.add(phaseCheckItem = getCheckItem("Phase Response", false));
		m.add(spectrumCheckItem = getCheckItem("Spectrum", true));
		m.add(waveformCheckItem = getCheckItem("Waveform", java2));
		m.add(impulseCheckItem = getCheckItem("Impulse Response", true));
		m.add(stepCheckItem = getCheckItem("Step Response", false));
		m.addSeparator();
		m.add(logFreqCheckItem = getCheckItem("Log Frequency Scale", false));
		m.add(allWaveformCheckItem = getCheckItem("Show Entire Waveform", false));
		m.add(ferrisCheckItem = getCheckItem("Ferris Plot", false));
		// this doesn't fully work when turned off
		logAmpCheckItem = getCheckItem("Log Amplitude Scale", true);

		setMenuBar(mb);

		soundCheck = new Checkbox("Sound On");
		if (java2) {
			soundCheck.setState(true);
		} else {
			soundCheck.disable();
		}
		soundCheck.addItemListener(this);
		add(soundCheck);

		displayCheck = new Checkbox("Stop Display");
		displayCheck.addItemListener(this);
		add(displayCheck);

		shiftSpectrumCheck = new Checkbox("Shift Spectrum");
		shiftSpectrumCheck.addItemListener(this);
		add(shiftSpectrumCheck);

		/*
		 * woofCheck = new Checkbox("Woof"); woofCheck.addItemListener(this);
		 * add(woofCheck);
		 */

		add(inputChooser = new Choice());
		inputChooser.add("Input = Noise");
		inputChooser.add("Input = Sine Wave");
		inputChooser.add("Input = Sawtooth");
		inputChooser.add("Input = Triangle Wave");
		inputChooser.add("Input = Square Wave");
		inputChooser.add("Input = Periodic Noise");
		inputChooser.add("Input = Sweep");
		inputChooser.add("Input = Impulses");
		for (i = 0; mp3List[i] != null; i++) {
			inputChooser.add("Input = " + mp3List[i]);
		}
		inputChooser.addItemListener(this);

		add(filterChooser = new Choice());
		filterChooser.add("Filter = FIR Low-pass");
		filterChooser.add("Filter = FIR High-pass");
		filterChooser.add("Filter = FIR Band-pass");
		filterChooser.add("Filter = FIR Band-stop");
		filterChooser.add("Filter = Custom FIR");
		filterChooser.add("Filter = None");
		filterChooser.add("Filter = Butterworth Low-pass");
		filterChooser.add("Filter = Butterworth High-pass");
		filterChooser.add("Filter = Butterworth Band-pass");
		filterChooser.add("Filter = Butterworth Band-stop");
		filterChooser.add("Filter = Chebyshev Low-pass");
		filterChooser.add("Filter = Chebyshev High-pass");
		filterChooser.add("Filter = Chebyshev Band-pass");
		filterChooser.add("Filter = Chebyshev Band-stop");
		filterChooser.add("Filter = Inv Cheby Low-pass");
		filterChooser.add("Filter = Inv Cheby High-pass");
		filterChooser.add("Filter = Inv Cheby Band-pass");
		filterChooser.add("Filter = Inv Cheby Band-stop");
		filterChooser.add("Filter = Elliptic Low-pass");
		filterChooser.add("Filter = Elliptic High-pass");
		filterChooser.add("Filter = Elliptic Band-pass");
		filterChooser.add("Filter = Elliptic Band-stop");
		filterChooser.add("Filter = Comb (+)");
		filterChooser.add("Filter = Comb (-)");
		filterChooser.add("Filter = Delay");
		filterChooser.add("Filter = Plucked String");
		filterChooser.add("Filter = Inverse Comb");
		filterChooser.add("Filter = Reson");
		filterChooser.add("Filter = Reson w/ Zeros");
		filterChooser.add("Filter = Notch");
		filterChooser.add("Filter = Moving Average");
		filterChooser.add("Filter = Triangle");
		filterChooser.add("Filter = Allpass");
		filterChooser.add("Filter = Gaussian");
		filterChooser.add("Filter = Random");
		filterChooser.add("Filter = Custom IIR");
		filterChooser.addItemListener(this);
		filterSelection = -1;

		add(windowChooser = new Choice());
		windowChooser.add("Window = Rectangular");
		windowChooser.add("Window = Hamming");
		windowChooser.add("Window = Hann");
		windowChooser.add("Window = Blackman");
		windowChooser.add("Window = Kaiser");
		windowChooser.add("Window = Bartlett");
		windowChooser.add("Window = Welch");
		windowChooser.addItemListener(this);
		windowChooser.select(1);

		add(rateChooser = new Choice());
		rateChooser.add("Sampling Rate = 8000");
		rateChooser.add("Sampling Rate = 11025");
		rateChooser.add("Sampling Rate = 16000");
		rateChooser.add("Sampling Rate = 22050");
		rateChooser.add("Sampling Rate = 32000");
		rateChooser.add("Sampling Rate = 44100");
		rateChooser.select(3);
		sampleRate = 22050;
		rateChooser.addItemListener(this);

		auxLabels = new Label[5];
		auxBars = new Scrollbar[5];
		for (i = 0; i != 5; i++) {
			add(auxLabels[i] = new Label("", Label.CENTER));
			add(auxBars[i] = new Scrollbar(Scrollbar.HORIZONTAL, 25, 1, 1, 999));
			auxBars[i].addAdjustmentListener(this);
		}

		add(inputLabel = new Label("Input Frequency", Label.CENTER));
		add(inputBar = new Scrollbar(Scrollbar.HORIZONTAL, 40, 1, 1, 999));
		inputBar.addAdjustmentListener(this);

		add(shiftFreqLabel = new Label("Shift Frequency", Label.CENTER));
		add(shiftFreqBar = new Scrollbar(Scrollbar.HORIZONTAL, 10, 1, 0, 1001));
		shiftFreqBar.addAdjustmentListener(this);
		shiftFreqLabel.hide();
		shiftFreqBar.hide();

		add(kaiserLabel = new Label("Kaiser Parameter", Label.CENTER));
		add(kaiserBar = new Scrollbar(Scrollbar.HORIZONTAL, 500, 1, 1, 999));
		kaiserBar.addAdjustmentListener(this);

		random = new Random();
		setInputLabel();
		reinit();
		cv.setBackground(Color.black);
		cv.setForeground(Color.lightGray);

		showFormat = DecimalFormat.getInstance();
		showFormat.setMaximumFractionDigits(2);

		resize(640, 640);
		handleResize();
		Dimension x = getSize();
		Dimension screen = getToolkit().getScreenSize();
		setLocation((screen.width - x.width) / 2, (screen.height - x.height) / 2);
		show();
	}

	void reinit() {
		setupFilter();
		setInputW();
	}

	MenuItem getMenuItem(String s) {
		MenuItem mi = new MenuItem(s);
		mi.addActionListener(this);
		return mi;
	}

	CheckboxMenuItem getCheckItem(String s, boolean b) {
		CheckboxMenuItem mi = new CheckboxMenuItem(s);
		mi.setState(b);
		mi.addItemListener(this);
		return mi;
	}

	int getPower2(int n) {
		int o = 2;
		while (o < n) {
			o *= 2;
		}
		return o;
	}

	PhaseColor genPhaseColor(int sec, double ang) {
		// convert to 0 .. 2*pi angle
		ang += sec * pi / 4;
		// convert to 0 .. 6
		ang *= 3 / pi;
		int hsec = (int) ang;
		double a2 = ang % 1;
		double a3 = 1. - a2;
		PhaseColor c = null;
		switch (hsec) {
		case 6:
		case 0:
			c = new PhaseColor(1, a2, 0);
			break;
		case 1:
			c = new PhaseColor(a3, 1, 0);
			break;
		case 2:
			c = new PhaseColor(0, 1, a2);
			break;
		case 3:
			c = new PhaseColor(0, a3, 1);
			break;
		case 4:
			c = new PhaseColor(a2, 0, 1);
			break;
		case 5:
			c = new PhaseColor(1, 0, a3);
			break;
		}
		return c;
	}

	class PhaseColor {
		public double r, g, b;

		PhaseColor(double rr, double gg, double bb) {
			r = rr;
			g = gg;
			b = bb;
		}
	}

	void handleResize() {
		Dimension d = winSize = cv.getSize();
		if (winSize.width == 0) {
			return;
		}
		int ct = 1;
		respView = spectrumView = impulseView = phaseView = stepView = waveformView = null;
		if (freqCheckItem.getState()) {
			ct++;
		}
		if (phaseCheckItem.getState()) {
			ct++;
		}
		if (spectrumCheckItem.getState()) {
			ct++;
		}
		if (waveformCheckItem.getState()) {
			ct++;
		}
		if (impulseCheckItem.getState()) {
			ct++;
		}
		if (stepCheckItem.getState()) {
			ct++;
		}

		int dh3 = d.height / ct;
		dbimage = createImage(d.width, d.height);
		int bd = 15;

		int i = 0;
		if (freqCheckItem.getState()) {
			respView = getView(i++, ct);
		}
		if (phaseCheckItem.getState()) {
			phaseView = getView(i++, ct);
		}
		if (spectrumCheckItem.getState()) {
			spectrumView = getView(i++, ct);
		}
		if (waveformCheckItem.getState()) {
			waveformView = getView(i++, ct);
		}
		if (impulseCheckItem.getState()) {
			impulseView = getView(i++, ct);
		}
		if (stepCheckItem.getState()) {
			stepView = getView(i++, ct);
		}
		poleInfoView = getView(i++, ct);
		if (poleInfoView.height > 200) {
			poleInfoView.height = 200;
		}
		polesView = new View(poleInfoView.x, poleInfoView.y, poleInfoView.height, poleInfoView.height);
		getPoleBuffer();
	}

	View getView(int i, int ct) {
		int dh3 = winSize.height / ct;
		int bd = 5;
		int tpad = 15;
		return new View(bd, bd + i * dh3 + tpad, winSize.width - bd * 2, dh3 - bd * 2 - tpad);
	}

	void getPoleBuffer() {
		int i;
		pixels = null;
		if (java2) {
			try {
				/*
				 * simulate the following code using reflection: dbimage = new
				 * BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
				 * DataBuffer db = (DataBuffer)(((BufferedImage)memimage).
				 * getRaster().getDataBuffer()); DataBufferInt dbi =
				 * (DataBufferInt) db; pixels = dbi.getData();
				 */
				Class biclass = Class.forName("java.awt.image.BufferedImage");
				Class dbiclass = Class.forName("java.awt.image.DataBufferInt");
				Class rasclass = Class.forName("java.awt.image.Raster");
				Constructor cstr = biclass.getConstructor(new Class[] { int.class, int.class, int.class });
				memimage = (Image) cstr.newInstance(new Object[] { new Integer(polesView.width),
						new Integer(polesView.height), new Integer(1) }); // BufferedImage.TYPE_INT_RGB)});
				Method m = biclass.getMethod("getRaster", null);
				Object ras = m.invoke(memimage, null);
				Object db = rasclass.getMethod("getDataBuffer", null).invoke(ras, null);
				pixels = (int[]) dbiclass.getMethod("getData", null).invoke(db, null);
			} catch (Exception ee) {
				// ee.printStackTrace();
				System.out.println("BufferedImage failed");
			}
		}
		if (pixels == null) {
			pixels = new int[polesView.width * polesView.height];
			for (i = 0; i != polesView.width * polesView.height; i++) {
				pixels[i] = 0xFF000000;
			}
			imageSource = new MemoryImageSource(polesView.width, polesView.height, pixels, 0, polesView.width);
			imageSource.setAnimated(true);
			imageSource.setFullBufferUpdates(true);
			memimage = cv.createImage(imageSource);
		}
	}

	void centerString(Graphics g, String s, int y) {
		FontMetrics fm = g.getFontMetrics();
		g.drawString(s, (winSize.width - fm.stringWidth(s)) / 2, y);
	}

	
	public void paint(Graphics g) {
		cv.repaint();
	}

	long lastTime;
	double minlog, logrange;

	public void updateDFilter(Graphics realg) {
		Graphics g = dbimage.getGraphics();
		if (winSize == null || winSize.width == 0 || dbimage == null) {
			return;
		}

		if (curFilter == null) {
			Filter f = filterType.genFilter();
			curFilter = f;
			if (playThread != null) {
				playThread.setFilter(f);
			}
			filterChanged = true;
			unstable = false;
		}

		if (playThread == null && !unstable && soundCheck.getState()) {
			playThread = new PlayThread();
			playThread.start();
		}

		if (displayCheck.getState()) {
			return;
		}

		g.setColor(cv.getBackground());
		g.fillRect(0, 0, winSize.width, winSize.height);
		g.setColor(cv.getForeground());

		double minf = 40. / sampleRate;
		minlog = Math.log(minf);
		logrange = Math.log(.5) - minlog;
		Complex cc = new Complex();

		int i;
		if (respView != null) {
			respView.drawLabel(g, "Frequency Response");
			g.setColor(Color.darkGray);
			g.fillRect(respView.x, respView.y, respView.width, respView.height);
			g.setColor(Color.black);
			/*
			 * i = respView.x + respView.width/2; g.drawLine(i, respView.y, i,
			 * respView.y+respView.height);
			 */
			double ym = .069;
			for (i = 0;; i += 2) {
				double q = ym * i;
				if (q > 1) {
					break;
				}
				int y = respView.y + (int) (q * respView.height);
				g.drawLine(respView.x, y, respView.right, y);
			}
			for (i = 1;; i++) {
				double ll = logrange - i * Math.log(2);
				int x = 0;
				if (logFreqCheckItem.getState()) {
					x = (int) (ll * respView.width / logrange);
				} else {
					x = respView.width / (1 << i);
				}
				if (x <= 0) {
					break;
				}
				x += respView.x;
				g.drawLine(x, respView.y, x, respView.bottom);
			}
			g.setColor(Color.white);
			int ox = -1, oy = -1, ox2 = -1, oy2 = -1;
			for (i = 0; i != respView.width; i++) {
				double w = 0;
				if (!logFreqCheckItem.getState()) {
					w = pi * i / respView.width;
				} else {
					double f = Math.exp(minlog + i * logrange / respView.width);
					w = 2 * pi * f;
				}
				filterType.getResponse(w, cc);
				double bw = cc.magSquared();
				double val = -ym * Math.log(bw * bw) / log10;
				int x = i + respView.x;
				if (val > 1) {
					if (ox != -1) {
						g.drawLine(ox, oy, ox, respView.bottom);
					}
					ox = -1;
				} else {
					int y = respView.y + (int) (respView.height * val);
					if (ox != -1) {
						g.drawLine(ox, oy, x, y);
					} else if (x > respView.x) {
						g.drawLine(x, respView.bottom, x, y);
					}
					ox = x;
					oy = y;
				}
				if (filterType instanceof CustomFIRFilter) {
					g.setColor(Color.white);
					CustomFIRFilter cf = (CustomFIRFilter) filterType;
					bw = cf.getUserResponse(w);
					val = -ym * Math.log(bw * bw) / log10;
					if (val > 1) {
						if (ox2 != -1) {
							g.drawLine(ox2, oy2, ox2, respView.bottom);
						}
						ox2 = -1;
					} else {
						int y = respView.y + (int) (respView.height * val);
						if (ox2 != -1) {
							g.drawLine(ox2, oy2, x, y);
						} else if (x > respView.x) {
							g.drawLine(x, respView.bottom, x, y);
						}
						ox2 = x;
						oy2 = y;
					}
					g.setColor(Color.red);
				}
			}
		}
		g.setColor(Color.white);

		if (phaseView != null) {
			phaseView.drawLabel(g, "Phase Response");
			g.setColor(Color.darkGray);
			g.fillRect(phaseView.x, phaseView.y, phaseView.width, phaseView.height);
			g.setColor(Color.black);
			for (i = 0; i < 5; i++) {
				double q = i * .25;
				int y = phaseView.y + (int) (q * phaseView.height);
				g.drawLine(phaseView.x, y, phaseView.right, y);
			}
			for (i = 1;; i++) {
				double ll = logrange - i * Math.log(2);
				int x = 0;
				if (logFreqCheckItem.getState()) {
					x = (int) (ll * phaseView.width / logrange);
				} else {
					x = phaseView.width / (1 << i);
				}
				if (x <= 0) {
					break;
				}
				x += phaseView.x;
				g.drawLine(x, phaseView.y, x, phaseView.bottom);
			}
			g.setColor(Color.white);
			int ox = -1, oy = -1;
			for (i = 0; i != phaseView.width; i++) {
				double w = 0;
				if (!logFreqCheckItem.getState()) {
					w = pi * i / phaseView.width;
				} else {
					double f = Math.exp(minlog + i * logrange / phaseView.width);
					w = 2 * pi * f;
				}
				filterType.getResponse(w, cc);
				double val = .5 + cc.phase / (2 * pi);
				int y = phaseView.y + (int) (phaseView.height * val);
				int x = i + phaseView.x;
				if (ox != -1) {
					g.drawLine(ox, oy, x, y);
				} else if (x > phaseView.x) {
					g.drawLine(x, phaseView.bottom, x, y);
				}
				ox = x;
				oy = y;
			}
		}

		int polect = filterType.getPoleCount();
		int zeroct = filterType.getZeroCount();
		int infoX = 10;
		int ph = 0, pw = 0, cx = 0, cy = 0;
		if (poleInfoView != null && (polect > 0 || zeroct > 0 || ferrisCheckItem.getState())) {
			ph = polesView.height / 2;
			pw = ph;
			cx = polesView.x + pw;
			cy = polesView.y + ph;
			infoX = cx + pw + 10;

			if (!ferrisCheckItem.getState()) {
				g.setColor(Color.white);
				FontMetrics fm = g.getFontMetrics();
				String s = "Poles/Zeros";
				g.drawString(s, cx - fm.stringWidth(s) / 2, polesView.y - 5);
				g.drawOval(cx - pw, cy - ph, pw * 2, ph * 2);
				g.drawLine(cx, cy - ph, cx, cy + ph);
				g.drawLine(cx - ph, cy, cx + ph, cy);
				Complex c1 = new Complex();
				for (i = 0; i != polect; i++) {
					filterType.getPole(i, c1);
					g.setColor(i == selectedPole ? Color.yellow : Color.white);
					int c1x = cx + (int) (pw * c1.re);
					int c1y = cy - (int) (ph * c1.im);
					g.drawLine(c1x - 3, c1y - 3, c1x + 3, c1y + 3);
					g.drawLine(c1x - 3, c1y + 3, c1x + 3, c1y - 3);
				}
				for (i = 0; i != zeroct; i++) {
					filterType.getZero(i, c1);
					g.setColor(i == selectedZero ? Color.yellow : Color.white);
					int c1x = cx + (int) (pw * c1.re);
					int c1y = cy - (int) (ph * c1.im);
					g.drawOval(c1x - 3, c1y - 3, 6, 6);
				}
				if (filterChanged) {
					setCustomPolesZeros();
				}
			} else {
				if (filterChanged) {
					int ri, ii;
					Complex c1 = new Complex();
					for (ri = 0; ri != polesView.width; ri++) {
						for (ii = 0; ii != polesView.height; ii++) {
							c1.set((ri - pw) / (double) pw, (ii - pw) / (double) pw);
							if (c1.re == 0 && c1.im == 0) {
								c1.set(1e-30);
							}
							curFilter.evalTransfer(c1);
							double cv = 0, wv = 0;
							double m = Math.sqrt(c1.mag);
							if (m < 1) {
								cv = m;
								wv = 1 - cv;
							} else if (m < 2) {
								cv = 2 - m;
							}
							cv *= 255;
							wv *= 255;
							double p = c1.phase;
							if (p < 0) {
								p += 2 * pi;
							}
							if (p >= 2 * pi) {
								p -= 2 * pi;
							}
							PhaseColor pc = phaseColors[(int) (p * phaseColorCount / (2 * pi))];
							pixels[ri + ii * polesView.width] = 0xFF000000 + 0x10000 * (int) (pc.r * cv + wv)
									+ 0x00100 * (int) (pc.g * cv + wv) + 0x00001 * (int) (pc.b * cv + wv);
						}
					}
				}
				if (imageSource != null) {
					imageSource.newPixels();
				}
				g.drawImage(memimage, polesView.x, polesView.y, null);
			}
		}
		if (poleInfoView != null) {
			g.setColor(Color.white);
			String info[] = new String[10];
			filterType.getInfo(info);
			for (i = 0; i != 10; i++) {
				if (info[i] == null) {
					break;
				}
			}
			if (wformInfo.needsFrequency()) {
				info[i++] = "Input Freq = " + (int) (inputW * sampleRate / (2 * pi));
			}
			info[i++] = "Output adjust = " + showFormat.format(-10 * Math.log(outputGain) / Math.log(.1))
					+ " dB";
			for (i = 0; i != 10; i++) {
				if (info[i] == null) {
					break;
				}
				g.drawString(info[i], infoX, poleInfoView.y + 5 + 20 * i);
			}
			if (respView != null && respView.contains(mouseX, mouseY) || spectrumView != null
					&& spectrumView.contains(mouseX, mouseY)) {
				double f = getFreqFromX(mouseX, respView);
				if (f >= 0) {
					double fw = 2 * pi * f;
					f *= sampleRate;
					g.setColor(Color.yellow);
					String s = "Selected Freq = " + (int) f;
					if (respView.contains(mouseX, mouseY)) {
						filterType.getResponse(fw, cc);
						double bw = cc.magSquared();
						bw = Math.log(bw * bw) / (2 * log10);
						s += ", Response = " + showFormat.format(10 * bw) + " dB";
					}
					g.drawString(s, infoX, poleInfoView.y + 5 + 20 * i);
					if (ph > 0) {
						int x = cx + (int) (pw * Math.cos(fw));
						int y = cy - (int) (pw * Math.sin(fw));
						if (ferrisCheckItem.getState()) {
							g.setColor(Color.black);
							g.fillOval(x - 3, y - 3, 7, 7);
						}
						g.setColor(Color.yellow);
						g.fillOval(x - 2, y - 2, 5, 5);
					}
				}
			}
		}

		if (impulseView != null) {
			impulseView.drawLabel(g, "Impulse Response");
			g.setColor(Color.darkGray);
			g.fillRect(impulseView.x, impulseView.y, impulseView.width, impulseView.height);
			g.setColor(Color.black);
			g.drawLine(impulseView.x, impulseView.y + impulseView.height / 2, impulseView.x
					+ impulseView.width - 1, impulseView.y + impulseView.height / 2);
			g.setColor(Color.white);
			int offset = curFilter.getImpulseOffset();
			double impBuf[] = curFilter.getImpulseResponse(offset);
			int len = curFilter.getImpulseLen(offset, impBuf);
			int ox = -1, oy = -1;
			double mult = .5 / max(impBuf);
			int flen = len < 50 ? 50 : len;
			if (len < flen && flen < impBuf.length - offset) {
				len = flen;
			}
			// System.out.println("cf " + offset + " " + len + " " +
			// impBuf.length);
			for (i = 0; i != len; i++) {
				int k = offset + i;
				double q = impBuf[k] * mult;
				int y = impulseView.y + (int) (impulseView.height * (.5 - q));
				int x = impulseView.x + impulseView.width * i / flen;
				if (len < 100) {
					g.drawLine(x, impulseView.y + impulseView.height / 2, x, y);
					g.fillOval(x - 2, y - 2, 5, 5);
				} else {
					if (ox != -1) {
						g.drawLine(ox, oy, x, y);
					}
					ox = x;
					oy = y;
				}
			}
		}

		if (stepView != null) {
			stepView.drawLabel(g, "Step Response");
			g.setColor(Color.darkGray);
			g.fillRect(stepView.x, stepView.y, stepView.width, stepView.height);
			g.setColor(Color.black);
			g.drawLine(stepView.x, stepView.y + stepView.height / 2, stepView.x + stepView.width - 1,
					stepView.y + stepView.height / 2);
			g.setColor(Color.white);
			int offset = curFilter.getStepOffset();
			double impBuf[] = curFilter.getStepResponse(offset);
			int len = curFilter.getStepLen(offset, impBuf);
			int ox = -1, oy = -1;
			double mult = .5 / max(impBuf);
			int flen = len < 50 ? 50 : len;
			if (len < flen && flen < impBuf.length - offset) {
				len = flen;
			}
			// System.out.println("cf " + offset + " " + len + " " +
			// impBuf.length);
			for (i = 0; i != len; i++) {
				int k = offset + i;
				double q = impBuf[k] * mult;
				int y = stepView.y + (int) (stepView.height * (.5 - q));
				int x = stepView.x + stepView.width * i / flen;
				if (len < 100) {
					g.drawLine(x, stepView.y + stepView.height / 2, x, y);
					g.fillOval(x - 2, y - 2, 5, 5);
				} else {
					if (ox != -1) {
						g.drawLine(ox, oy, x, y);
					}
					ox = x;
					oy = y;
				}
			}
		}

		if (playThread != null) {
			int splen = playThread.spectrumLen;
			if (spectrumBuf == null || spectrumBuf.length != splen * 2) {
				spectrumBuf = new double[splen * 2];
			}
			int off = playThread.spectrumOffset;
			int i2;
			int mask = playThread.fbufmask;
			for (i = i2 = 0; i != splen; i++, i2 += 2) {
				int o = mask & off + i;
				spectrumBuf[i2] = playThread.fbufLo[o] + playThread.fbufRo[o];
				spectrumBuf[i2 + 1] = 0;
			}
		} else {
			spectrumBuf = null;
		}

		if (waveformView != null && spectrumBuf != null) {
			waveformView.drawLabel(g, "Waveform");
			g.setColor(Color.darkGray);
			g.fillRect(waveformView.x, waveformView.y, waveformView.width, waveformView.height);
			g.setColor(Color.black);
			g.drawLine(waveformView.x, waveformView.y + waveformView.height / 2, waveformView.x
					+ waveformView.width - 1, waveformView.y + waveformView.height / 2);
			g.setColor(Color.white);
			int ox = -1, oy = -1;

			if (waveGain < .1) {
				waveGain = .1;
			}
			double max = 0;
			for (i = 0; i != spectrumBuf.length; i += 2) {
				if (spectrumBuf[i] > max) {
					max = spectrumBuf[i];
				}
				if (spectrumBuf[i] < -max) {
					max = -spectrumBuf[i];
				}
			}
			if (waveGain > 1 / max) {
				waveGain = 1 / max;
			} else if (waveGain * 1.05 < 1 / max) {
				waveGain *= 1.05;
			}
			double mult = .5 * waveGain;
			int nb = waveformView.width;
			if (nb > spectrumBuf.length || allWaveformCheckItem.getState()) {
				nb = spectrumBuf.length;
			}
			for (i = 0; i < nb; i += 2) {
				double bf = .5 - spectrumBuf[i] * mult;
				int ya = (int) (waveformView.height * bf);
				if (ya > waveformView.height) {
					ox = -1;
					continue;
				}
				int y = waveformView.y + ya;
				int x = waveformView.x + i * waveformView.width / nb;
				if (ox != -1) {
					g.drawLine(ox, oy, x, y);
				}
				ox = x;
				oy = y;
			}
		}

		if (spectrumView != null && spectrumBuf != null) {
			spectrumView.drawLabel(g, "Spectrum");
			g.setColor(Color.darkGray);
			g.fillRect(spectrumView.x, spectrumView.y, spectrumView.width, spectrumView.height);
			g.setColor(Color.black);
			double ym = .138;
			for (i = 0;; i++) {
				double q = ym * i;
				if (q > 1) {
					break;
				}
				int y = spectrumView.y + (int) (q * spectrumView.height);
				g.drawLine(spectrumView.x, y, spectrumView.x + spectrumView.width, y);
			}
			for (i = 1;; i++) {
				double ll = logrange - i * Math.log(2);
				int x = 0;
				if (logFreqCheckItem.getState()) {
					x = (int) (ll * spectrumView.width / logrange);
				} else {
					x = spectrumView.width / (1 << i);
				}
				if (x <= 0) {
					break;
				}
				x += spectrumView.x;
				g.drawLine(x, spectrumView.y, x, spectrumView.bottom);
			}

			g.setColor(Color.white);
			int isub = spectrumBuf.length / 2;
			double cosmult = 2 * pi / (spectrumBuf.length - 2);
			for (i = 0; i != spectrumBuf.length; i += 2) {
				double ht = .54 - .46 * Math.cos(i * cosmult);
				spectrumBuf[i] *= ht;
			}
			if (spectrumFFT == null || spectrumFFT.size != spectrumBuf.length / 2) {
				spectrumFFT = new FFT(spectrumBuf.length / 2);
			}
			spectrumFFT.transform(spectrumBuf, false);
			double logmult = spectrumView.width / Math.log(spectrumBuf.length / 2 + 1);

			int ox = -1, oy = -1;
			double bufmult = 1. / (spectrumBuf.length / 2);
			if (logAmpCheckItem.getState()) {
				bufmult /= 65536;
			} else {
				bufmult /= 768;
			}
			bufmult *= bufmult;

			double specArray[] = new double[spectrumView.width];
			if (logFreqCheckItem.getState()) {
				// freq = i*rate/(spectrumBuf.length)
				// min frequency = 40 Hz
				for (i = 0; i != spectrumBuf.length / 2; i += 2) {
					double f = i / (double) spectrumBuf.length;
					int ix = (int) (specArray.length * (Math.log(f) - minlog) / logrange);
					if (ix < 0) {
						continue;
					}
					specArray[ix] += spectrumBuf[i] * spectrumBuf[i] + spectrumBuf[i + 1]
							* spectrumBuf[i + 1];
				}
			} else {
				for (i = 0; i != spectrumBuf.length / 2; i += 2) {
					int ix = specArray.length * i * 2 / spectrumBuf.length;
					specArray[ix] += spectrumBuf[i] * spectrumBuf[i] + spectrumBuf[i + 1]
							* spectrumBuf[i + 1];
				}
			}

			int maxi = specArray.length;
			for (i = 0; i != spectrumView.width; i++) {
				double bf = specArray[i] * bufmult;
				if (logAmpCheckItem.getState()) {
					bf = -ym * Math.log(bf) / log10;
				} else {
					bf = 1 - bf;
				}

				int ya = (int) (spectrumView.height * bf);
				if (ya > spectrumView.height) {
					continue;
				}
				int y = spectrumView.y + ya;
				int x = spectrumView.x + i * spectrumView.width / maxi;
				g.drawLine(x, y, x, spectrumView.y + spectrumView.height - 1);
			}
		}
		if (spectrumView != null && !java2) {
			g.setColor(Color.white);
			centerString(g, "Need java 2 for sound", spectrumView.y + spectrumView.height / 2);
		}

		if (unstable) {
			g.setColor(Color.red);
			centerString(g, "Filter is unstable", winSize.height / 2);
		}
		if (mp3Error != null) {
			g.setColor(Color.red);
			centerString(g, mp3Error, winSize.height / 2 + 20);
		}

		if (respView != null && respView.contains(mouseX, mouseY)) {
			g.setColor(Color.yellow);
			g.drawLine(mouseX, respView.y, mouseX, respView.y + respView.height - 1);
		}
		if (spectrumView != null && spectrumView.contains(mouseX, mouseY)) {
			g.setColor(Color.yellow);
			g.drawLine(mouseX, spectrumView.y, mouseX, spectrumView.y + spectrumView.height - 1);
		}
		filterChanged = false;

		realg.drawImage(dbimage, 0, 0, this);
	}

	void setCutoff(double f) {
	}

	void setCustomPolesZeros() {
		if (filterType instanceof CustomIIRFilter) {
			return;
		}
		int polect = filterType.getPoleCount();
		int zeroct = filterType.getZeroCount();
		int i, n;
		Complex c1 = new Complex();
		for (i = n = 0; i != polect; i++) {
			filterType.getPole(i, c1);
			if (c1.im >= 0) {
				customPoles[n++].set(c1);
				customPoles[n++].set(c1.re, -c1.im);
				if (n == customPoles.length) {
					break;
				}
			}
		}
		lastPoleCount = n;
		for (i = n = 0; i != zeroct; i++) {
			filterType.getZero(i, c1);
			if (c1.im >= 0) {
				customZeros[n++].set(c1);
				customZeros[n++].set(c1.re, -c1.im);
				if (n == customZeros.length) {
					break;
				}
			}
		}
		lastZeroCount = n;
	}

	int countPoints(double buf[], int offset) {
		int len = buf.length;
		double max = 0;
		int i;
		int result = 0;
		double last = 123;
		for (i = offset; i < len; i++) {
			double qa = Math.abs(buf[i]);
			if (qa > max) {
				max = qa;
			}
			if (Math.abs(qa - last) > max * .003) {
				result = i - offset + 1;
				// System.out.println(qa + " " + last + " " + i + " " + max);
			}
			last = qa;
		}
		return result;
	}

	double max(double buf[]) {
		int i;
		double max = 0;
		for (i = 0; i != buf.length; i++) {
			double qa = Math.abs(buf[i]);
			if (qa > max) {
				max = qa;
			}
		}
		return max;
	}

	// get freq (from 0 to .5) given an x coordinate
	double getFreqFromX(int x, View v) {
		double f = .5 * (x - v.x) / v.width;
		if (f <= 0 || f >= .5) {
			return -1;
		}
		if (logFreqCheckItem.getState()) {
			return Math.exp(minlog + 2 * f * logrange);
		}
		return f;
	}

	void setupFilter() {
		int filt = filterChooser.getSelectedIndex();
		switch (filt) {
		case 0:
			filterType = new SincLowPassFilter();
			break;
		case 1:
			filterType = new SincHighPassFilter();
			break;
		case 2:
			filterType = new SincBandPassFilter();
			break;
		case 3:
			filterType = new SincBandStopFilter();
			break;
		case 4:
			filterType = new CustomFIRFilter();
			break;
		case 5:
			filterType = new NoFilter();
			break;
		case 6:
			filterType = new ButterLowPass();
			break;
		case 7:
			filterType = new ButterHighPass();
			break;
		case 8:
			filterType = new ButterBandPass();
			break;
		case 9:
			filterType = new ButterBandStop();
			break;
		case 10:
			filterType = new ChebyLowPass();
			break;
		case 11:
			filterType = new ChebyHighPass();
			break;
		case 12:
			filterType = new ChebyBandPass();
			break;
		case 13:
			filterType = new ChebyBandStop();
			break;
		case 14:
			filterType = new InvChebyLowPass();
			break;
		case 15:
			filterType = new InvChebyHighPass();
			break;
		case 16:
			filterType = new InvChebyBandPass();
			break;
		case 17:
			filterType = new InvChebyBandStop();
			break;
		case 18:
			filterType = new EllipticLowPass();
			break;
		case 19:
			filterType = new EllipticHighPass();
			break;
		case 20:
			filterType = new EllipticBandPass();
			break;
		case 21:
			filterType = new EllipticBandStop();
			break;
		case 22:
			filterType = new CombFilter(1);
			break;
		case 23:
			filterType = new CombFilter(-1);
			break;
		case 24:
			filterType = new DelayFilter();
			break;
		case 25:
			filterType = new PluckedStringFilter();
			break;
		case 26:
			filterType = new InverseCombFilter();
			break;
		case 27:
			filterType = new ResonatorFilter();
			break;
		case 28:
			filterType = new ResonatorZeroFilter();
			break;
		case 29:
			filterType = new NotchFilter();
			break;
		case 30:
			filterType = new MovingAverageFilter();
			break;
		case 31:
			filterType = new TriangleFilter();
			break;
		case 32:
			filterType = new AllPassFilter();
			break;
		case 33:
			filterType = new GaussianFilter();
			break;
		case 34:
			filterType = new RandomFilter();
			break;
		case 35:
			filterType = new CustomIIRFilter();
			break;
		}
		if (filterSelection != filt) {
			filterSelection = filt;
			int i;
			for (i = 0; i != auxBars.length; i++) {
				auxBars[i].setMaximum(999);
			}
			int ax = filterType.select();
			for (i = 0; i != ax; i++) {
				auxLabels[i].show();
				auxBars[i].show();
			}
			for (i = ax; i != auxBars.length; i++) {
				auxLabels[i].hide();
				auxBars[i].hide();
			}
			if (filterType.needsWindow()) {
				windowChooser.show();
				setWindow();
			} else {
				windowChooser.hide();
				setWindow();
			}
			validate();
		}
		filterType.setup();
		curFilter = null;
	}

	void setInputLabel() {
		wformInfo = getWaveformObject();
		String inText = wformInfo.getInputText();
		if (inText == null) {
			inputLabel.hide();
			inputBar.hide();
		} else {
			inputLabel.setText(inText);
			inputLabel.show();
			inputBar.show();
		}
		validate();
	}

	Waveform getWaveformObject() {
		Waveform wform;
		int ic = inputChooser.getSelectedIndex();
		switch (ic) {
		case 0:
			wform = new NoiseWaveform();
			break;
		case 1:
			wform = new SineWaveform();
			break;
		case 2:
			wform = new SawtoothWaveform();
			break;
		case 3:
			wform = new TriangleWaveform();
			break;
		case 4:
			wform = new SquareWaveform();
			break;
		case 5:
			wform = new PeriodicNoiseWaveform();
			break;
		case 6:
			wform = new SweepWaveform();
			break;
		case 7:
			wform = new ImpulseWaveform();
			break;
		default:
			wform = new NoiseWaveform();
			break;
		}
		return wform;
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
		cv.repaint(pause);
	}

	public void componentResized(ComponentEvent e) {
		handleResize();
		cv.repaint(pause);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == exitItem) {
			applet.destroyFrame();
			return;
		}
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		setupFilter();
		System.out.print(((Scrollbar) e.getSource()).getValue() + "\n");
		if (e.getSource() == inputBar) {
			setInputW();
		}
		cv.repaint(pause);
	}

	void setInputW() {
		inputW = pi * inputBar.getValue() / 1000.;
	}

	
	public boolean handleEvent(Event ev) {
		if (ev.id == Event.WINDOW_DESTROY) {
			if (playThread != null) {
				playThread.requestShutdown();
			}
			applet.destroyFrame();
			return true;
		}
		return super.handleEvent(ev);
	}

	public void mouseDragged(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();
		edit(e);
		cv.repaint(pause);
	}

	public void mouseMoved(MouseEvent e) {
		dragX = mouseX = e.getX();
		dragY = mouseY = e.getY();
		cv.repaint(pause);
		if (respView != null && respView.contains(e.getX(), e.getY())) {
			selection = SELECT_RESPONSE;
		}
		if (spectrumView != null && spectrumView.contains(e.getX(), e.getY())) {
			selection = SELECT_SPECTRUM;
		}
		if (polesView != null && polesView.contains(e.getX(), e.getY()) && !ferrisCheckItem.getState()) {
			selection = SELECT_POLES;
			selectPoleZero(e.getX(), e.getY());
		}
	}

	void selectPoleZero(int x, int y) {
		selectedPole = selectedZero = -1;
		int i;
		int ph = polesView.height / 2;
		int pw = ph;
		int cx = polesView.x + pw;
		int cy = polesView.y + ph;
		Complex c1 = new Complex();
		int polect = filterType.getPoleCount();
		int zeroct = filterType.getZeroCount();
		int bestdist = 10000;
		for (i = 0; i != polect; i++) {
			filterType.getPole(i, c1);
			int c1x = cx + (int) (pw * c1.re);
			int c1y = cy - (int) (ph * c1.im);
			int dist = distanceSq(c1x, c1y, x, y);
			if (dist <= bestdist) {
				bestdist = dist;
				selectedPole = i;
				selectedZero = -1;
			}
		}
		for (i = 0; i != zeroct; i++) {
			filterType.getZero(i, c1);
			int c1x = cx + (int) (pw * c1.re);
			int c1y = cy - (int) (ph * c1.im);
			int dist = distanceSq(c1x, c1y, x, y);
			if (dist < bestdist) {
				bestdist = dist;
				selectedPole = -1;
				selectedZero = i;
			}
		}
	}

	int distanceSq(int x1, int y1, int x2, int y2) {
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		mouseMoved(e);
		edit(e);
	}

	public void mouseReleased(MouseEvent e) {
	}

	void edit(MouseEvent e) {
		if (selection == SELECT_RESPONSE) {
			if (filterType instanceof CustomFIRFilter) {
				editCustomFIRFilter(e);
				return;
			}
			double f = getFreqFromX(e.getX(), respView);
			if (f < 0) {
				return;
			}
			filterType.setCutoff(f);
			setupFilter();
		}
		if (selection == SELECT_SPECTRUM) {
			if (!wformInfo.needsFrequency()) {
				return;
			}
			double f = getFreqFromX(e.getX(), spectrumView);
			if (f < 0) {
				return;
			}
			inputW = 2 * pi * f;
			inputBar.setValue((int) (2000 * f));
		}
		if (selection == SELECT_POLES && filterType instanceof CustomIIRFilter) {
			editCustomIIRFilter(e);
			return;
		}
	}

	void editCustomFIRFilter(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		if (dragX == x) {
			editCustomFIRFilterPoint(x, y);
			dragY = y;
		} else {
			// need to draw a line from old x,y to new x,y and
			// call editFuncPoint for each point on that line. yuck.
			int x1 = x < dragX ? x : dragX;
			int y1 = x < dragX ? y : dragY;
			int x2 = x > dragX ? x : dragX;
			int y2 = x > dragX ? y : dragY;
			dragX = x;
			dragY = y;
			for (x = x1; x <= x2; x++) {
				y = y1 + (y2 - y1) * (x - x1) / (x2 - x1);
				editCustomFIRFilterPoint(x, y);
			}
		}
		setupFilter();
	}

	void editCustomFIRFilterPoint(int x, int y) {
		double xx1 = getFreqFromX(x, respView) * 2;
		double xx2 = getFreqFromX(x + 1, respView) * 2;
		y -= respView.y;
		double ym = .069;
		double yy = Math.exp(-y * Math.log(10) / (ym * 4 * respView.height));
		if (yy >= 1) {
			yy = 1;
		}
		((CustomFIRFilter) filterType).edit(xx1, xx2, yy);
	}

	void editCustomIIRFilter(MouseEvent e) {
		if (ferrisCheckItem.getState()) {
			return;
		}
		int x = e.getX();
		int y = e.getY();
		int ph = polesView.height / 2;
		int pw = ph;
		int cx = polesView.x + pw;
		int cy = polesView.y + ph;
		Complex c1 = new Complex();
		c1.set((x - cx) / (double) pw, (y - cy) / (double) ph);
		((CustomIIRFilter) filterType).editPoleZero(c1);
		setupFilter();
	}

	public void itemStateChanged(ItemEvent e) {
		filterChanged = true;
		if (e.getSource() == displayCheck) {
			cv.repaint(pause);
			return;
		}
		if (e.getSource() == inputChooser) {
			if (playThread != null) {
				playThread.requestShutdown();
			}
			setInputLabel();
		}
		if (e.getSource() == rateChooser) {
			if (playThread != null) {
				playThread.requestShutdown();
			}
			inputW *= sampleRate;
			switch (rateChooser.getSelectedIndex()) {
			case 0:
				sampleRate = 8000;
				break;
			case 1:
				sampleRate = 11025;
				break;
			case 2:
				sampleRate = 16000;
				break;
			case 3:
				sampleRate = 22050;
				break;
			case 4:
				sampleRate = 32000;
				break;
			case 5:
				sampleRate = 44100;
				break;
			}
			inputW /= sampleRate;
		}
		if (e.getSource() == shiftSpectrumCheck) {
			if (shiftSpectrumCheck.getState()) {
				shiftFreqLabel.show();
				shiftFreqBar.show();
			} else {
				shiftFreqLabel.hide();
				shiftFreqBar.hide();
			}
			validate();
		}
		if (e.getSource() == windowChooser) {
			setWindow();
		}
		if (e.getSource() instanceof CheckboxMenuItem) {
			handleResize();
		} else {
			setupFilter();
		}
		cv.repaint(pause);
	}

	void setWindow() {
		if (windowChooser.getSelectedIndex() == WINDOW_KAISER && filterType.needsWindow()) {
			kaiserLabel.show();
			kaiserBar.show();
		} else {
			kaiserLabel.hide();
			kaiserBar.hide();
		}
		validate();
	}

	void setSampleRate(int r) {
		int x = 0;
		switch (r) {
		case 8000:
			x = 0;
			break;
		case 11025:
			x = 1;
			break;
		case 16000:
			x = 2;
			break;
		case 22050:
			x = 3;
			break;
		case 32000:
			x = 4;
			break;
		case 44100:
			x = 5;
			break;
		}
		rateChooser.select(x);
		sampleRate = r;
	}

	class FFT {
		double wtabf[];
		double wtabi[];
		int size;

		FFT(int sz) {
			size = sz;
			if ((size & size - 1) != 0) {
				System.out.println("size must be power of two!");
			}
			calcWTable();
		}

		void calcWTable() {
			// calculate table of powers of w
			wtabf = new double[size];
			wtabi = new double[size];
			int i;
			for (i = 0; i != size; i += 2) {
				double pi = 3.1415926535;
				double th = pi * i / size;
				wtabf[i] = Math.cos(th);
				wtabf[i + 1] = Math.sin(th);
				wtabi[i] = wtabf[i];
				wtabi[i + 1] = -wtabf[i + 1];
			}
		}

		void transform(double data[], boolean inv) {
			int i;
			int j = 0;
			int size2 = size * 2;

			if ((size & size - 1) != 0) {
				System.out.println("size must be power of two!");
			}

			// bit-reversal
			double q;
			int bit;
			for (i = 0; i != size2; i += 2) {
				if (i > j) {
					q = data[i];
					data[i] = data[j];
					data[j] = q;
					q = data[i + 1];
					data[i + 1] = data[j + 1];
					data[j + 1] = q;
				}
				// increment j by one, from the left side (bit-reversed)
				bit = size;
				while ((bit & j) != 0) {
					j &= ~bit;
					bit >>= 1;
				}
				j |= bit;
			}

			// amount to skip through w table
			int tabskip = size << 1;
			double wtab[] = inv ? wtabi : wtabf;

			int skip1, skip2, ix, j2;
			double wr, wi, d1r, d1i, d2r, d2i, d2wr, d2wi;

			// unroll the first iteration of the main loop
			for (i = 0; i != size2; i += 4) {
				d1r = data[i];
				d1i = data[i + 1];
				d2r = data[i + 2];
				d2i = data[i + 3];
				data[i] = d1r + d2r;
				data[i + 1] = d1i + d2i;
				data[i + 2] = d1r - d2r;
				data[i + 3] = d1i - d2i;
			}
			tabskip >>= 1;

			// unroll the second iteration of the main loop
			int imult = inv ? -1 : 1;
			for (i = 0; i != size2; i += 8) {
				d1r = data[i];
				d1i = data[i + 1];
				d2r = data[i + 4];
				d2i = data[i + 5];
				data[i] = d1r + d2r;
				data[i + 1] = d1i + d2i;
				data[i + 4] = d1r - d2r;
				data[i + 5] = d1i - d2i;
				d1r = data[i + 2];
				d1i = data[i + 3];
				d2r = data[i + 6] * imult;
				d2i = data[i + 7] * imult;
				data[i + 2] = d1r - d2i;
				data[i + 3] = d1i + d2r;
				data[i + 6] = d1r + d2i;
				data[i + 7] = d1i - d2r;
			}
			tabskip >>= 1;

			for (skip1 = 16; skip1 <= size2; skip1 <<= 1) {
				// skip2 = length of subarrays we are combining
				// skip1 = length of subarray after combination
				skip2 = skip1 >> 1;
				tabskip >>= 1;
				for (i = 0; i != 1000; i++) {
					;
				}
				// for each subarray
				for (i = 0; i < size2; i += skip1) {
					ix = 0;
					// for each pair of complex numbers (one in each subarray)
					for (j = i; j != i + skip2; j += 2, ix += tabskip) {
						wr = wtab[ix];
						wi = wtab[ix + 1];
						d1r = data[j];
						d1i = data[j + 1];
						j2 = j + skip2;
						d2r = data[j2];
						d2i = data[j2 + 1];
						d2wr = d2r * wr - d2i * wi;
						d2wi = d2r * wi + d2i * wr;
						data[j] = d1r + d2wr;
						data[j + 1] = d1i + d2wi;
						data[j2] = d1r - d2wr;
						data[j2 + 1] = d1i - d2wi;
					}
				}
			}
		}
	}

	abstract class Waveform {
		short buffer[];

		boolean start() {
			return true;
		}

		abstract int getData();

		int getChannels() {
			return 2;
		}

		void getBuffer() {
			buffer = new short[getPower2(sampleRate / 12) * getChannels()];
		}

		String getInputText() {
			return "Input Frequency";
		}

		boolean needsFrequency() {
			return true;
		}
	}

	class NoiseWaveform extends Waveform {
		
		boolean start() {
			getBuffer();
			return true;
		}

		
		int getData() {
			int i;
			for (i = 0; i != buffer.length; i++) {
				buffer[i] = (short) random.nextInt();
			}
			return buffer.length;
		}

		
		String getInputText() {
			return null;
		}

		
		boolean needsFrequency() {
			return false;
		}
	}

	class PeriodicNoiseWaveform extends Waveform {
		short smbuf[];
		int ix;

		
		int getChannels() {
			return 1;
		}

		
		boolean start() {
			getBuffer();
			smbuf = new short[1];
			ix = 0;
			return true;
		}

		
		int getData() {
			int period = (int) (2 * pi / inputW);
			if (period != smbuf.length) {
				smbuf = new short[period];
				int i;
				for (i = 0; i != period; i++) {
					smbuf[i] = (short) random.nextInt();
				}
			}
			int i;
			for (i = 0; i != buffer.length; i++, ix++) {
				if (ix >= period) {
					ix = 0;
				}
				buffer[i] = smbuf[ix];
			}
			return buffer.length;
		}
	}

	class SineWaveform extends Waveform {
		int ix;

		
		int getChannels() {
			return 1;
		}

		
		boolean start() {
			getBuffer();
			ix = 0;
			return true;
		}

		
		int getData() {
			int i;
			for (i = 0; i != buffer.length; i++) {
				ix++;
				buffer[i] = (short) (Math.sin(ix * inputW) * 32000);
			}
			return buffer.length;
		}
	}

	class TriangleWaveform extends Waveform {
		int ix;
		short smbuf[];

		
		int getChannels() {
			return 1;
		}

		
		boolean start() {
			getBuffer();
			ix = 0;
			smbuf = new short[1];
			return true;
		}

		
		int getData() {
			int i;
			int period = (int) (2 * pi / inputW);
			if (period != smbuf.length) {
				smbuf = new short[period];
				double p2 = period / 2.;
				for (i = 0; i < p2; i++) {
					smbuf[i] = (short) (i / p2 * 64000 - 32000);
				}
				for (; i != period; i++) {
					smbuf[i] = (short) ((2 - i / p2) * 64000 - 32000);
				}
			}
			for (i = 0; i != buffer.length; i++, ix++) {
				if (ix >= period) {
					ix = 0;
				}
				buffer[i] = smbuf[ix];
			}
			return buffer.length;
		}
	}

	class SawtoothWaveform extends Waveform {
		int ix;
		short smbuf[];

		
		int getChannels() {
			return 1;
		}

		
		boolean start() {
			getBuffer();
			ix = 0;
			smbuf = new short[1];
			return true;
		}

		
		int getData() {
			int i;
			int period = (int) (2 * pi / inputW);
			if (period != smbuf.length) {
				smbuf = new short[period];
				double p2 = period / 2.;
				for (i = 0; i != period; i++) {
					smbuf[i] = (short) ((i / p2 - 1) * 32000);
				}
			}
			for (i = 0; i != buffer.length; i++, ix++) {
				if (ix >= period) {
					ix = 0;
				}
				buffer[i] = smbuf[ix];
			}
			return buffer.length;
		}
	}

	class SquareWaveform extends Waveform {
		int ix;
		double omega;
		short smbuf[];

		
		int getChannels() {
			return 1;
		}

		
		boolean start() {
			getBuffer();
			ix = 0;
			smbuf = new short[1];
			return true;
		}

		
		int getData() {
			int i;
			int period = (int) (2 * pi / inputW);
			if (period != smbuf.length) {
				smbuf = new short[period];
				for (i = 0; i != period / 2; i++) {
					smbuf[i] = 32000;
				}
				if ((period & 1) > 0) {
					smbuf[i++] = 0;
				}
				for (; i != period; i++) {
					smbuf[i] = -32000;
				}
			}
			for (i = 0; i != buffer.length; i++, ix++) {
				if (ix >= period) {
					ix = 0;
				}
				buffer[i] = smbuf[ix];
			}
			return buffer.length;
		}
	}

	class SweepWaveform extends Waveform {
		int ix;
		double omega, nextOmega, t, startOmega;

		
		int getChannels() {
			return 1;
		}

		
		boolean start() {
			getBuffer();
			ix = 0;
			startOmega = nextOmega = omega = 2 * pi * 40 / sampleRate;
			t = 0;
			return true;
		}

		
		int getData() {
			int i;
			double nmul = 1;
			double nadd = 0;
			double maxspeed = 1 / (.66 * sampleRate);
			double minspeed = 1 / (sampleRate * 16);
			if (logFreqCheckItem.getState()) {
				nmul = Math.pow(2 * pi / startOmega,
						2 * (minspeed + (maxspeed - minspeed) * inputBar.getValue() / 1000.));
			} else {
				nadd = (2 * pi - startOmega)
						* (minspeed + (maxspeed - minspeed) * inputBar.getValue() / 1000.);
			}
			for (i = 0; i != buffer.length; i++) {
				ix++;
				t += omega;
				if (t > 2 * pi) {
					t -= 2 * pi;
					omega = nextOmega;
					if (nextOmega > pi) {
						omega = nextOmega = startOmega;
					}
				}
				buffer[i] = (short) (Math.sin(t) * 32000);
				nextOmega = nextOmega * nmul + nadd;
			}
			return buffer.length;
		}

		
		String getInputText() {
			return "Sweep Speed";
		}

		
		boolean needsFrequency() {
			return false;
		}
	}

	class ImpulseWaveform extends Waveform {
		int ix;

		
		int getChannels() {
			return 1;
		}

		
		boolean start() {
			getBuffer();
			ix = 0;
			return true;
		}

		
		int getData() {
			int i;
			int ww = inputBar.getValue() / 51 + 1;
			int period = 10000 / ww;
			for (i = 0; i != buffer.length; i++) {
				short q = 0;
				if (ix % period == 0) {
					q = 32767;
				}
				ix++;
				buffer[i] = q;
			}
			return buffer.length;
		}

		
		String getInputText() {
			return "Impulse Frequency";
		}

		
		boolean needsFrequency() {
			return false;
		}
	}

	class PlayThread extends Thread {
		SourceDataLine line;
		Waveform wform;
		boolean shutdownRequested;
		boolean stereo;
		Filter filt, newFilter;
		double fbufLi[];
		double fbufRi[];
		double fbufLo[];
		double fbufRo[];
		double stateL[], stateR[];
		int fbufmask, fbufsize;
		int spectrumOffset, spectrumLen;

		PlayThread() {
			shutdownRequested = false;
		}

		void requestShutdown() {
			shutdownRequested = true;
		}

		void setFilter(Filter f) {
			newFilter = f;
		}

		void openLine() {
			try {
				stereo = wform.getChannels() == 2;
				AudioFormat playFormat = new AudioFormat(sampleRate, 16, 2, true, false);
				DataLine.Info info = new DataLine.Info(SourceDataLine.class, playFormat);

				if (!AudioSystem.isLineSupported(info)) {
					throw new LineUnavailableException("sorry, the sound format cannot be played");
				}
				line = (SourceDataLine) AudioSystem.getLine(info);
				line.open(playFormat, getPower2(sampleRate / 4));
				line.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		int inbp, outbp;
		int spectCt;

		
		public void run() {
			try {
				doRun();
			} catch (Exception e) {
				e.printStackTrace();
			}
			playThread = null;
		}

		void doRun() {
			rateChooser.enable();
			wform = getWaveformObject();
			mp3Error = null;
			unstable = false;
			if (!wform.start()) {
				cv.repaint();
				try {
					Thread.sleep(1000L);
				} catch (Exception e) {
				}
				return;
			}

			fbufsize = 32768;
			fbufmask = fbufsize - 1;
			fbufLi = new double[fbufsize];
			fbufRi = new double[fbufsize];
			fbufLo = new double[fbufsize];
			fbufRo = new double[fbufsize];
			openLine();
			inbp = outbp = spectCt = 0;
			int ss = stereo ? 2 : 1;
			outputGain = 1;
			newFilter = filt = curFilter;
			spectrumLen = getPower2(sampleRate / 12);
			int gainCounter = 0;
			boolean maxGain = true;
			boolean useConvolve = false;

			ob = new byte[16384];
			int shiftCtr = 0;
			while (!shutdownRequested && soundCheck.getState() && applet.ogf != null) {
				// System.out.println("nf " + newFilter + " " +(inbp-outbp));
				if (newFilter != null) {
					gainCounter = 0;
					maxGain = true;
					if (wform instanceof SweepWaveform || wform instanceof SineWaveform) {
						maxGain = false;
					}
					outputGain = 1;
					// we avoid doing this unless necessary because it sounds
					// bad
					if (filt == null || filt.getLength() != newFilter.getLength()) {
						convBufPtr = inbp = outbp = spectCt = 0;
					}
					filt = newFilter;
					newFilter = null;
					impulseBuf = null;
					useConvolve = filt.useConvolve();
					stateL = filt.createState();
					stateR = filt.createState();
				}
				int length = wform.getData();
				if (length == 0) {
					break;
				}
				short ib[] = wform.buffer;

				int i2;
				int i = inbp;
				for (i2 = 0; i2 < length; i2 += ss) {
					fbufLi[i] = ib[i2];
					i = i + 1 & fbufmask;
				}
				i = inbp;
				if (stereo) {
					for (i2 = 0; i2 < length; i2 += 2) {
						fbufRi[i] = ib[i2 + 1];
						i = i + 1 & fbufmask;
					}
				} else {
					for (i2 = 0; i2 < length; i2++) {
						fbufRi[i] = fbufLi[i];
						i = i + 1 & fbufmask;
					}
				}
				if (shiftSpectrumCheck.getState()) {
					double shiftFreq = shiftFreqBar.getValue() * pi / 1000.;
					if (shiftFreq > pi) {
						shiftFreq = pi;
					}
					i = inbp;
					for (i2 = 0; i2 < length; i2 += ss) {
						double q = Math.cos(shiftFreq * shiftCtr++);
						fbufLi[i] *= q;
						fbufRi[i] *= q;
						i = i + 1 & fbufmask;
					}
				}

				int sampleCount = length / ss;
				if (useConvolve) {
					doConvolveFilter(sampleCount, maxGain);
				} else {
					doFilter(sampleCount);
					if (unstable) {
						break;
					}
					int outlen = sampleCount * 4;
					doOutput(outlen, maxGain);
				}

				if (unstable) {
					break;
				}

				if (spectCt >= spectrumLen) {
					spectrumOffset = outbp - spectrumLen & fbufmask;
					spectCt -= spectrumLen;
					cv.repaint();
				}
				gainCounter += sampleCount;
				if (maxGain && gainCounter >= sampleRate) {
					gainCounter = 0;
					maxGain = false;
					// System.out.println("gain ctr up " + outputGain);
				}
			}
			if (shutdownRequested || unstable || !soundCheck.getState()) {
				line.flush();
			} else {
				line.drain();
			}
			cv.repaint();
		}

		void doFilter(int sampleCount) {
			filt.run(fbufLi, fbufLo, inbp, fbufmask, sampleCount, stateL);
			filt.run(fbufRi, fbufRo, inbp, fbufmask, sampleCount, stateR);
			inbp = inbp + sampleCount & fbufmask;
			double q = fbufLo[inbp - 1 & fbufmask];
			if (Double.isNaN(q) || Double.isInfinite(q)) {
				unstable = true;
			}
		}

		double impulseBuf[], convolveBuf[];
		int convBufPtr;
		FFT convFFT;

		void doConvolveFilter(int sampleCount, boolean maxGain) {
			int i;
			int fi2 = inbp, i20;
			double filtA[] = ((DirectFilter) filt).aList;
			int cblen = getPower2(512 + filtA.length * 2);
			if (convolveBuf == null || convolveBuf.length != cblen) {
				convolveBuf = new double[cblen];
			}
			if (impulseBuf == null) {
				// take FFT of the impulse response
				impulseBuf = new double[cblen];
				for (i = 0; i != filtA.length; i++) {
					impulseBuf[i * 2] = filtA[i];
				}
				convFFT = new FFT(convolveBuf.length / 2);
				convFFT.transform(impulseBuf, false);
			}
			int cbptr = convBufPtr;
			// result = impulseLen+inputLen-1 samples long; result length
			// is fixed, so use it to get inputLen
			int cbptrmax = convolveBuf.length + 2 - 2 * filtA.length;
			// System.out.println("reading " + sampleCount);
			for (i = 0; i != sampleCount; i++, fi2++) {
				i20 = fi2 & fbufmask;
				convolveBuf[cbptr] = fbufLi[i20];
				convolveBuf[cbptr + 1] = fbufRi[i20];
				cbptr += 2;
				if (cbptr == cbptrmax) {
					// buffer is full, do the transform
					convFFT.transform(convolveBuf, false);
					double mult = 2. / cblen;
					int j;
					// multiply transforms to get convolution
					for (j = 0; j != cblen; j += 2) {
						double a = convolveBuf[j] * impulseBuf[j] - convolveBuf[j + 1] * impulseBuf[j + 1];
						double b = convolveBuf[j] * impulseBuf[j + 1] + convolveBuf[j + 1] * impulseBuf[j];
						convolveBuf[j] = a * mult;
						convolveBuf[j + 1] = b * mult;
					}
					// inverse transform to get signal
					convFFT.transform(convolveBuf, true);
					int fj2 = outbp, j20;
					int overlap = cblen - cbptrmax;
					// generate output that overlaps with old data
					for (j = 0; j != overlap; j += 2, fj2++) {
						j20 = fj2 & fbufmask;
						fbufLo[j20] += convolveBuf[j];
						fbufRo[j20] += convolveBuf[j + 1];
					}
					// generate new output
					for (; j != cblen; j += 2, fj2++) {
						j20 = fj2 & fbufmask;
						fbufLo[j20] = convolveBuf[j];
						fbufRo[j20] = convolveBuf[j + 1];
					}
					cbptr = 0;
					// output the sound
					doOutput(cbptrmax * 2, maxGain);
					// System.out.println("outputting " + cbptrmax);
					// clear transform buffer
					for (j = 0; j != cblen; j++) {
						convolveBuf[j] = 0;
					}
				}
			}
			inbp = fi2 & fbufmask;
			convBufPtr = cbptr;
		}

		byte ob[];

		void doOutput(int outlen, boolean maxGain) {
			if (ob.length < outlen) {
				ob = new byte[outlen];
			}
			int qi;
			int i, i2;
			while (true) {
				int max = 0;
				i = outbp;
				for (i2 = 0; i2 < outlen; i2 += 4) {
					qi = (int) (fbufLo[i] * outputGain);
					if (qi > max) {
						max = qi;
					}
					if (qi < -max) {
						max = -qi;
					}
					ob[i2 + 1] = (byte) (qi >> 8);
					ob[i2] = (byte) qi;
					i = i + 1 & fbufmask;
				}
				i = outbp;
				for (i2 = 2; i2 < outlen; i2 += 4) {
					qi = (int) (fbufRo[i] * outputGain);
					if (qi > max) {
						max = qi;
					}
					if (qi < -max) {
						max = -qi;
					}
					ob[i2 + 1] = (byte) (qi >> 8);
					ob[i2] = (byte) qi;
					i = i + 1 & fbufmask;
				}
				// if we're getting overflow, adjust the gain
				if (max > 32767) {
					// System.out.println("max = " + max);
					outputGain *= 30000. / max;
					if (outputGain < 1e-8 || Double.isInfinite(outputGain)) {
						unstable = true;
						break;
					}
					continue;
				} else if (maxGain && max < 24000) {
					if (max == 0) {
						if (outputGain == 1) {
							break;
						}
						outputGain = 1;
					} else {
						outputGain *= 30000. / max;
					}
					continue;
				}
				break;
			}
			if (unstable) {
				return;
			}
			int oldoutbp = outbp;
			outbp = i;

			line.write(ob, 0, outlen);
			spectCt += outlen / 4;
		}
	}

	class Complex {
		public double re, im, mag, phase;

		Complex() {
			re = im = mag = phase = 0;
		}

		Complex(double r, double i) {
			set(r, i);
		}

		Complex(Complex c) {
			set(c.re, c.im);
		}

		double magSquared() {
			return mag * mag;
		}

		void set(double aa, double bb) {
			re = aa;
			im = bb;
			setMagPhase();
		}

		void set(double aa) {
			re = aa;
			im = 0;
			setMagPhase();
		}

		void set(Complex c) {
			re = c.re;
			im = c.im;
			mag = c.mag;
			phase = c.phase;
		}

		void add(double r) {
			re += r;
			setMagPhase();
		}

		void add(double r, double i) {
			re += r;
			im += i;
			setMagPhase();
		}

		void add(Complex c) {
			re += c.re;
			im += c.im;
			setMagPhase();
		}

		void addMult(double x, Complex z) {
			re += z.re * x;
			im += z.im * x;
			setMagPhase();
		}

		void square() {
			set(re * re - im * im, 2 * re * im);
		}

		void sqrt() {
			setMagPhase(Math.sqrt(mag), phase * .5);
		}

		void mult(double c, double d) {
			set(re * c - im * d, re * d + im * c);
		}

		void mult(double c) {
			re *= c;
			im *= c;
			mag *= c;
		}

		void mult(Complex c) {
			mult(c.re, c.im);
		}

		void setMagPhase() {
			mag = Math.sqrt(re * re + im * im);
			phase = Math.atan2(im, re);
		}

		void setMagPhase(double m, double ph) {
			mag = m;
			phase = ph;
			re = m * Math.cos(ph);
			im = m * Math.sin(ph);
		}

		void recip() {
			double n = re * re + im * im;
			set(re / n, -im / n);
		}

		void div(Complex c) {
			double n = c.re * c.re + c.im * c.im;
			mult(c.re / n, -c.im / n);
		}

		void rotate(double a) {
			setMagPhase(mag, (phase + a) % (2 * pi));
		}

		void conjugate() {
			im = -im;
			phase = -phase;
		}

		void pow(double p) {
			double arg = java.lang.Math.atan2(im, re);
			phase *= p;
			double abs = java.lang.Math.pow(re * re + im * im, p * .5);
			setMagPhase(abs, phase);
		}
	};

	abstract class Filter {
		abstract void run(double inBuf[], double outBuf[], int bp, int mask, int count, double x[]);

		abstract void evalTransfer(Complex c);

		abstract int getImpulseOffset();

		abstract int getStepOffset();

		abstract int getLength();

		boolean useConvolve() {
			return false;
		}

		double[] getImpulseResponse(int offset) {
			int pts = 1000;
			double inbuf[] = new double[offset + pts];
			double outbuf[] = new double[offset + pts];
			inbuf[offset] = 1;
			double state[] = createState();
			run(inbuf, outbuf, offset, ~0, pts, state);
			return outbuf;
		}

		double[] getStepResponse(int offset) {
			int pts = 1000;
			double inbuf[] = new double[offset + pts];
			double outbuf[] = new double[offset + pts];
			int i;
			for (i = offset; i != inbuf.length; i++) {
				inbuf[i] = 1;
			}
			double state[] = createState();
			run(inbuf, outbuf, offset, ~0, pts, state);
			return outbuf;
		}

		int getImpulseLen(int offset, double buf[]) {
			return countPoints(buf, offset);
		}

		int getStepLen(int offset, double buf[]) {
			return countPoints(buf, offset);
		}

		double[] createState() {
			return null;
		}
	}

	class DirectFilter extends Filter {
		double aList[], bList[];
		int nList[];

		DirectFilter() {
			aList = new double[] { 1 };
			bList = null;
			nList = new int[] { 0 };
		}

		
		int getLength() {
			return aList.length;
		}

		
		boolean useConvolve() {
			return bList == null && aList.length > 25;
		}

		void dump() {
			System.out.print("a ");
			dump(aList);
			if (bList != null) {
				System.out.print("b ");
				dump(bList);
			}
		}

		void dump(double x[]) {
			int i;
			for (i = 0; i != x.length; i++) {
				System.out.print(x[i] + " ");
			}
			System.out.println("");
		}

		Complex czn, top, bottom;

		
		void evalTransfer(Complex c) {
			if (czn == null) {
				czn = new Complex();
				top = new Complex();
				bottom = new Complex();
			}
			int i, j;
			czn.set(1);
			top.set(0);
			bottom.set(0);
			int n = 0;
			for (i = 0; i != aList.length; i++) {
				int n1 = nList[i];
				while (n < n1) {
					if (n + 3 < n1) {
						czn.set(c);
						czn.pow(-n1);
						n = n1;
						break;
					}
					czn.div(c);
					n++;
				}
				top.addMult(aList[i], czn);
				if (bList != null) {
					bottom.addMult(bList[i], czn);
				}
			}
			if (bList != null) {
				top.div(bottom);
			}
			c.set(top);
		}

		
		void run(double inBuf[], double outBuf[], int bp, int mask, int count, double state[]) {
			int j;
			int fi2 = bp, i20;
			double q = 0;

			int i2;
			for (i2 = 0; i2 != count; i2++) {
				fi2 = bp + i2;
				i20 = fi2 & mask;

				q = inBuf[i20] * aList[0];
				if (bList == null) {
					for (j = 1; j < aList.length; j++) {
						int ji = fi2 - nList[j] & mask;
						q += inBuf[ji] * aList[j];
					}
				} else {
					for (j = 1; j < aList.length; j++) {
						int ji = fi2 - nList[j] & mask;
						q += inBuf[ji] * aList[j] - outBuf[ji] * bList[j];
					}
				}
				outBuf[i20] = q;
			}
		}

		boolean isSimpleAList() {
			if (bList != null) {
				return false;
			}
			return nList[nList.length - 1] == nList.length - 1;
		}

		
		int getImpulseOffset() {
			if (isSimpleAList()) {
				return 0;
			}
			return getStepOffset();
		}

		
		int getStepOffset() {
			int i;
			int offset = 0;
			for (i = 0; i != aList.length; i++) {
				if (nList[i] > offset) {
					offset = nList[i];
				}
			}
			return offset;
		}

		
		double[] getImpulseResponse(int offset) {
			if (isSimpleAList()) {
				return aList;
			}
			return super.getImpulseResponse(offset);
		}

		
		int getImpulseLen(int offset, double buf[]) {
			if (isSimpleAList()) {
				return aList.length;
			}
			return countPoints(buf, offset);
		}
	}

	class CascadeFilter extends Filter {
		CascadeFilter(int s) {
			size = s;
			a1 = new double[s];
			a2 = new double[s];
			b0 = new double[s];
			b1 = new double[s];
			b2 = new double[s];
			int i;
			for (i = 0; i != s; i++) {
				b0[i] = 1;
			}
		}

		double a1[], a2[], b0[], b1[], b2[];
		int size;

		
		double[] createState() {
			return new double[size * 3];
		}

		void setAStage(double x1, double x2) {
			int i;
			for (i = 0; i != size; i++) {
				if (a1[i] == 0 && a2[i] == 0) {
					a1[i] = x1;
					a2[i] = x2;
					return;
				}
				if (a2[i] == 0 && x2 == 0) {
					a2[i] = -a1[i] * x1;
					a1[i] += x1;
					// System.out.println("setastate " + i + " " + a1[i] + " " +
					// a2[i]);
					return;
				}
			}
			System.out.println("setAStage failed");
		}

		void setBStage(double x0, double x1, double x2) {
			// System.out.println("setting b " + i + " "+ x0 + " "+ x1 + " "+ x2
			// + " " + size);
			int i;
			for (i = 0; i != size; i++) {
				if (b1[i] == 0 && b2[i] == 0) {
					b0[i] = x0;
					b1[i] = x1;
					b2[i] = x2;
					// System.out.println("setbstage " + i + " " + x0 + " " + x1
					// + " " + x2);
					return;
				}
				if (b2[i] == 0 && x2 == 0) {
					// (b0 + z b1)(x0 + z x1) = (b0 x0 + (b1 x0+b0 x1) z + b1 x1
					// z^2)
					b2[i] = b1[i] * x1;
					b1[i] = b1[i] * x0 + b0[i] * x1;
					b0[i] *= x0;
					// System.out.println("setbstage " + i + " " +
					// b0[i]+" "+b1[i] + " " + b2[i]);
					return;
				}
			}
			System.out.println("setBStage failed");
		}

		
		void run(double inBuf[], double outBuf[], int bp, int mask, int count, double state[]) {
			int fi2, i20;
			int i2, j;
			double in = 0, d2, d1, d0;
			for (i2 = 0; i2 != count; i2++) {
				fi2 = bp + i2;
				i20 = fi2 & mask;
				in = inBuf[i20];
				for (j = 0; j != size; j++) {
					int j3 = j * 3;
					d2 = state[j3 + 2] = state[j3 + 1];
					d1 = state[j3 + 1] = state[j3];
					d0 = state[j3] = in + a1[j] * d1 + a2[j] * d2;
					in = b0[j] * d0 + b1[j] * d1 + b2[j] * d2;
				}
				outBuf[i20] = in;
			}
		}

		Complex cm2, cm1, top, bottom;

		
		void evalTransfer(Complex c) {
			if (cm1 == null) {
				cm1 = new Complex();
				cm2 = new Complex();
				top = new Complex();
				bottom = new Complex();
			}
			int i;
			cm1.set(c);
			cm1.recip();
			cm2.set(cm1);
			cm2.square();
			c.set(1);
			for (i = 0; i != size; i++) {
				top.set(b0[i]);
				top.addMult(b1[i], cm1);
				top.addMult(b2[i], cm2);
				bottom.set(1);
				bottom.addMult(-a1[i], cm1);
				bottom.addMult(-a2[i], cm2);
				c.mult(top);
				c.div(bottom);
			}
		}

		
		int getImpulseOffset() {
			return 0;
		}

		
		int getStepOffset() {
			return 0;
		}

		
		int getLength() {
			return 1;
		}
	}

	abstract class FilterType {
		int select() {
			return 0;
		}

		void setup() {
		}

		abstract void getResponse(double w, Complex c);

		int getPoleCount() {
			return 0;
		}

		int getZeroCount() {
			return 0;
		}

		void getPole(int i, Complex c) {
			c.set(0);
		}

		void getZero(int i, Complex c) {
			c.set(0);
		}

		abstract Filter genFilter();

		void getInfo(String x[]) {
		}

		boolean needsWindow() {
			return false;
		}

		void setCutoff(double f) {
			auxBars[0].setValue((int) (2000 * f));
		}
	}

	abstract class IIRFilterType extends FilterType {
		double response[];

		
		void getResponse(double w, Complex c) {
			if (response == null) {
				c.set(0);
				return;
			}
			int off = (int) (response.length * w / pi);
			off &= ~1;
			if (off < 0) {
				off = 0;
			}
			if (off >= response.length) {
				off = response.length - 1;
			}
			c.set(response[off], response[off + 1]);
		}

		void setResponse(DirectFilter f) {
			response = new double[8192];
			Complex czn = new Complex();
			Complex top = new Complex();
			Complex bottom = new Complex();
			int i, j;
			double maxresp = 0;
			f.bList[0] = 1;

			if (f.aList.length != f.bList.length) {
				System.out.println("length mismatch " + f.aList.length + " " + f.bList.length);
			}
			// use the coefficients to multiply out the transfer function for
			// various values of z
			for (j = 0; j != response.length; j += 2) {
				top.set(0);
				bottom.set(0);
				int czni = 0;
				for (i = 0; i != f.aList.length; i++) {
					czn.setMagPhase(1, -pi * j * f.nList[i] / response.length);
					top.addMult(f.aList[i], czn);
					bottom.addMult(f.bList[i], czn);
				}
				top.div(bottom);
				if (top.mag > maxresp) {
					maxresp = top.mag;
				}
				response[j] = top.re;
				response[j + 1] = top.im;
			}
			// normalize response
			for (j = 0; j != response.length; j++) {
				response[j] /= maxresp;
			}
			for (j = 0; j != f.aList.length; j++) {
				f.aList[j] /= maxresp;
				// System.out.println(f.aList.length + " " + f.bList.length +
				// " XX");
			}
		}

		void setResponse(CascadeFilter f) {
			// it's good to have this bigger for normalization
			response = new double[4096];
			Complex czn1 = new Complex();
			Complex czn2 = new Complex();
			Complex ch = new Complex();
			Complex ct = new Complex();
			Complex cb = new Complex();
			Complex cbot = new Complex();
			int i, j;
			double maxresp = 0;

			// use the coefficients to multiply out the transfer function for
			// various values of z
			// System.out.println("sr1");
			for (j = 0; j != response.length; j += 2) {
				ch.set(1);
				cbot.set(1);
				int czni = 0;
				czn1.setMagPhase(1, -pi * j / response.length);
				czn2.setMagPhase(1, -pi * j * 2 / response.length);
				for (i = 0; i != f.size; i++) {
					ct.set(f.b0[i]);
					cb.set(1);
					ct.addMult(f.b1[i], czn1);
					cb.addMult(-f.a1[i], czn1);
					ct.addMult(f.b2[i], czn2);
					cb.addMult(-f.a2[i], czn2);
					ch.mult(ct);
					cbot.mult(cb);
				}
				ch.div(cbot);
				if (ch.mag > maxresp) {
					maxresp = ch.mag;
				}
				response[j] = ch.re;
				response[j + 1] = ch.im;
			}
			// System.out.println("sr2");
			// normalize response
			for (j = 0; j != response.length; j++) {
				response[j] /= maxresp;
			}
			f.b0[0] /= maxresp;
			f.b1[0] /= maxresp;
			f.b2[0] /= maxresp;

			// System.out.println(f.aList.length + " " + f.bList.length +
			// " XX");
		}

		
		Filter genFilter() {
			int n = getPoleCount();
			CascadeFilter f = new CascadeFilter((n + 1) / 2);
			int i;
			Complex c1 = new Complex();
			int s;
			for (i = s = 0; i != n; i++) {
				getPole(i, c1);
				// System.out.println("pole " + i + " " + c1.re + " " + c1.im);
				if (Math.abs(c1.im) < 1e-6) {
					c1.im = 0;
				}
				if (c1.im < 0) {
					continue;
				}
				if (c1.im == 0) {
					double cc0 = -c1.re;
					f.setAStage(-cc0, 0);
					// System.out.println("real pole " + i + " " + c1.re + " " +
					// c1.im);
				} else {
					double cc0 = -2 * c1.re;
					double cd0 = c1.magSquared();
					f.setAStage(-cc0, -cd0);
				}
			}
			n = getZeroCount();
			for (i = s = 0; i != n; i++) {
				getZero(i, c1);
				// System.out.println("zero " + i + " " + c1.re + " " + c1.im);
				if (Math.abs(c1.im) < 1e-6) {
					c1.im = 0;
				}
				if (c1.im < 0) {
					continue;
				}
				if (c1.im == 0) {
					f.setBStage(-c1.re, 1, 0);
				} else {
					double cc0 = -2 * c1.re;
					double cd0 = c1.magSquared();
					f.setBStage(cd0, cc0, 1);
				}
			}
			setResponse(f);
			return f;
		}
	}

	abstract class PoleFilterType extends IIRFilterType {
		int n;
		double wc, wc2;

		abstract void getSPole(int i, Complex c1, double wc);

		
		void getPole(int i, Complex c1) {
			getSPole(i, c1, wc);
			bilinearXform(c1);
		}

		void bilinearXform(Complex c1) {
			Complex c2 = new Complex(c1);
			c1.add(1);
			c2.mult(-1);
			c2.add(1);
			c1.div(c2);
		}

		int selectLowPass() {
			auxLabels[0].setText("Cutoff Frequency");
			auxLabels[1].setText("Number of Poles");
			auxBars[1].setMaximum(40);
			auxBars[0].setValue(100);
			auxBars[1].setValue(4);
			return 2;
		}

		int selectBandPass() {
			auxLabels[0].setText("Center Frequency");
			auxLabels[1].setText("Passband Width");
			auxLabels[2].setText("Number of Poles");
			auxBars[2].setMaximum(20);
			auxBars[0].setValue(500);
			auxBars[1].setValue(200);
			auxBars[2].setValue(6);
			return 3;
		}

		void getBandPassPole(int i, Complex z) {
			getSPole(i / 2, z, pi * .5);
			bilinearXform(z);
			bandPassXform(i, z);
		}

		void bandPassXform(int i, Complex z) {
			double a = Math.cos((wc + wc2) * .5) / Math.cos((wc - wc2) * .5);
			double b = 1 / Math.tan(.5 * (wc - wc2));
			Complex c2 = new Complex();
			c2.addMult(4 * (b * b * (a * a - 1) + 1), z);
			c2.add(8 * (b * b * (a * a - 1) - 1));
			c2.mult(z);
			c2.add(4 * (b * b * (a * a - 1) + 1));
			c2.sqrt();
			if ((i & 1) == 0) {
				c2.mult(-1);
			}
			c2.addMult(2 * a * b, z);
			c2.add(2 * a * b);
			Complex c3 = new Complex();
			c3.addMult(2 * (b - 1), z);
			c3.add(2 * (1 + b));
			c2.div(c3);
			z.set(c2);
		}

		void getBandStopPole(int i, Complex z) {
			getSPole(i / 2, z, pi * .5);
			bilinearXform(z);
			bandStopXform(i, z);
		}

		void getBandStopZero(int i, Complex z) {
			z.set(-1, 0);
			bandStopXform(i, z);
		}

		void bandStopXform(int i, Complex z) {
			double a = Math.cos((wc + wc2) * .5) / Math.cos((wc - wc2) * .5);
			double b = Math.tan(.5 * (wc - wc2));
			Complex c2 = new Complex();
			c2.addMult(4 * (b * b + a * a - 1), z); // z^2 terms
			c2.add(8 * (b * b - a * a + 1)); // z terms
			c2.mult(z);
			c2.add(4 * (a * a + b * b - 1));
			c2.sqrt(); // c2 = discrim.
			c2.mult((i & 1) == 0 ? .5 : -.5);
			c2.add(a);
			c2.addMult(-a, z);
			Complex c3 = new Complex(b + 1, 0);
			c3.addMult(b - 1, z);
			c2.div(c3);
			z.set(c2);
		}

		void getBandPassZero(int i, Complex c1) {
			if (i >= n) {
				c1.set(1);
			} else {
				c1.set(-1);
			}
		}

		void setupLowPass() {
			wc = auxBars[0].getValue() * pi / 1000.;
			n = auxBars[1].getValue();
		}

		void setupBandPass() {
			double wcmid = auxBars[0].getValue() * pi / 1000.;
			double width = auxBars[1].getValue() * pi / 1000.;
			wc = wcmid + width / 2;
			wc2 = wcmid - width / 2;
			if (wc2 < 0) {
				wc2 = 1e-8;
			}
			if (wc >= pi) {
				wc = pi - 1e-8;
			}
			n = auxBars[2].getValue();
		}

		void getInfoLowPass(String x[]) {
			x[1] = "Cutoff freq: " + getOmegaText(wc);
		}

		void getInfoBandPass(String x[], boolean stop) {
			x[1] = (stop ? "Stopband: " : "Passband: ") + getOmegaText(wc2) + " - " + getOmegaText(wc);
		}
	}

	abstract class ButterFilterType extends PoleFilterType {
		
		void getSPole(int i, Complex c1, double wc) {
			double theta = pi / 2 + (2 * i + 1) * pi / (2 * n);
			c1.setMagPhase(Math.tan(wc * .5), theta);
		}
	}

	class ButterLowPass extends ButterFilterType {
		int sign;

		ButterLowPass() {
			sign = 1;
		}

		
		int select() {
			return selectLowPass();
		}

		
		void setup() {
			setupLowPass();
		}

		
		void getZero(int i, Complex c1) {
			c1.set(-sign);
		}

		
		int getPoleCount() {
			return n;
		}

		
		int getZeroCount() {
			return n;
		}

		
		void getInfo(String x[]) {
			x[0] = "Butterworth (IIR), " + getPoleCount() + "-pole";
			getInfoLowPass(x);
		}
	}

	class ButterHighPass extends ButterLowPass {
		ButterHighPass() {
			sign = -1;
		}
	}

	String getOmegaText(double wc) {
		return (int) (wc * sampleRate / (2 * pi)) + " Hz";
	}

	abstract class ChebyFilterType extends PoleFilterType {
		double epsilon;
		int sign;

		void selectCheby(int s) {
			auxLabels[s].setText("Passband Ripple");
			auxBars[s].setValue(60);
		}

		void setupCheby(int a) {
			int val = auxBars[a].getValue();
			double ripdb = 0;
			if (val < 300) {
				ripdb = 5 * val / 300.;
			} else {
				ripdb = 5 + 45 * (val - 300) / 700.;
			}
			double ripval = Math.exp(-ripdb * .1 * log10);
			epsilon = Math.sqrt(1 / ripval - 1);
		}

		
		void getSPole(int i, Complex c1, double wc) {
			Complex c2 = new Complex();
			double alpha = 1 / epsilon + Math.sqrt(1 + 1 / (epsilon * epsilon));
			double a = .5 * (Math.pow(alpha, 1. / n) - Math.pow(alpha, -1. / n));
			double b = .5 * (Math.pow(alpha, 1. / n) + Math.pow(alpha, -1. / n));
			double theta = pi / 2 + (2 * i + 1) * pi / (2 * n);
			if (sign == -1) {
				wc = pi - wc;
			}
			c1.setMagPhase(Math.tan(wc * .5), theta);
			c1.re *= a;
			c1.im *= b;
			c1.setMagPhase();
		}

		void getInfoCheby(String x[]) {
			x[2] = "Ripple: " + showFormat.format(-10 * Math.log(1 / (1 + epsilon * epsilon)) / log10)
					+ " dB";
		}
	}

	double cosh(double x) {
		return .5 * (Math.exp(x) + Math.exp(-x));
	}

	double sinh(double x) {
		return .5 * (Math.exp(x) - Math.exp(-x));
	}

	double acosh(double x) {
		return Math.log(x + Math.sqrt(x * x - 1));
	}

	abstract class InvChebyFilterType extends ChebyFilterType {
		double scale;

		
		void selectCheby(int s) {
			auxLabels[s].setText("Stopband Attenuation");
			auxBars[s].setValue(600);
		}

		
		void setupCheby(int a) {
			epsilon = Math.exp(-auxBars[a].getValue() / 120.);
			scale = cosh(acosh(1 / epsilon) / n);
		}

		
		void getSPole(int i, Complex c1, double wc) {
			wc = pi - wc;
			super.getSPole(i, c1, wc);
			c1.recip();
			c1.mult(scale);
		}

		void getChebyZero(int i, Complex c1, double wc) {
			double bk = 1 / Math.cos((2 * i + 1) * pi / (2 * n)) * scale;
			double a = Math.sin(pi / 4 - wc / 2) / Math.sin(pi / 4 + wc / 2);
			c1.set(1 + a, bk * (1 - a));
			Complex c2 = new Complex(1 + a, bk * (a - 1));
			c1.div(c2);
		}

		
		void getInfoCheby(String x[]) {
			x[2] = "Stopband attenuation: "
					+ showFormat.format(-10 * Math.log(1 + 1 / (epsilon * epsilon)) / log10) + " dB";
		}

		
		int getPoleCount() {
			return n;
		}

		
		int getZeroCount() {
			return n;
		}
	}

	class ChebyLowPass extends ChebyFilterType {
		ChebyLowPass() {
			sign = 1;
		}

		
		int select() {
			int s = selectLowPass();
			selectCheby(s++);
			return s;
		}

		
		void setup() {
			setupLowPass();
			setupCheby(2);
		}

		
		void getPole(int i, Complex c1) {
			super.getPole(i, c1);
			c1.mult(sign);
		}

		
		void getZero(int i, Complex c1) {
			c1.set(-sign);
		}

		
		int getPoleCount() {
			return n;
		}

		
		int getZeroCount() {
			return n;
		}

		
		void getInfo(String x[]) {
			x[0] = "Chebyshev (IIR), " + getPoleCount() + "-pole";
			getInfoLowPass(x);
			getInfoCheby(x);
		}
	}

	class ChebyHighPass extends ChebyLowPass {
		ChebyHighPass() {
			sign = -1;
		}
	}

	class InvChebyLowPass extends InvChebyFilterType {
		
		int select() {
			int s = selectLowPass();
			selectCheby(s++);
			return s;
		}

		
		void setup() {
			setupLowPass();
			setupCheby(2);
		}

		
		void getInfo(String x[]) {
			x[0] = "Inverse Chebyshev (IIR), " + getPoleCount() + "-pole";
			getInfoLowPass(x);
			getInfoCheby(x);
		}

		
		void getZero(int i, Complex c1) {
			getChebyZero(i, c1, wc);
		}
	}

	class InvChebyHighPass extends InvChebyLowPass {
		
		void getPole(int i, Complex c1) {
			getSPole(i, c1, pi - wc);
			bilinearXform(c1);
			c1.mult(-1);
		}

		
		void getZero(int i, Complex c1) {
			getChebyZero(i, c1, pi - wc);
			c1.mult(-1);
		}
	}

	class ButterBandPass extends ButterFilterType {
		
		int select() {
			return selectBandPass();
		}

		
		void setup() {
			setupBandPass();
		}

		
		void getPole(int i, Complex c1) {
			getBandPassPole(i, c1);
		}

		
		void getZero(int i, Complex c1) {
			getBandPassZero(i, c1);
		}

		
		int getPoleCount() {
			return n * 2;
		}

		
		int getZeroCount() {
			return n * 2;
		}

		
		void getInfo(String x[]) {
			x[0] = "Butterworth (IIR), " + getPoleCount() + "-pole";
			getInfoBandPass(x, this instanceof ButterBandStop);
		}
	}

	class ButterBandStop extends ButterBandPass {
		
		void getPole(int i, Complex c1) {
			getBandStopPole(i, c1);
		}

		
		void getZero(int i, Complex c1) {
			getBandStopZero(i, c1);
		}
	}

	class ChebyBandPass extends ChebyFilterType {
		
		int select() {
			int s = selectBandPass();
			selectCheby(s++);
			return s;
		}

		
		void setup() {
			setupBandPass();
			setupCheby(3);
		}

		
		void getPole(int i, Complex c1) {
			getBandPassPole(i, c1);
		}

		
		void getZero(int i, Complex c1) {
			getBandPassZero(i, c1);
		}

		
		int getPoleCount() {
			return n * 2;
		}

		
		int getZeroCount() {
			return n * 2;
		}

		
		void getInfo(String x[]) {
			x[0] = "Chebyshev (IIR), " + getPoleCount() + "-pole";
			getInfoBandPass(x, this instanceof ChebyBandStop);
			getInfoCheby(x);
		}
	}

	class ChebyBandStop extends ChebyBandPass {
		
		void getPole(int i, Complex c1) {
			getBandStopPole(i, c1);
		}

		
		void getZero(int i, Complex c1) {
			getBandStopZero(i, c1);
		}
	}

	class InvChebyBandPass extends InvChebyFilterType {
		
		int select() {
			int s = selectBandPass();
			selectCheby(s++);
			return s;
		}

		
		void setup() {
			setupBandPass();
			setupCheby(3);
		}

		
		void getPole(int i, Complex c1) {
			getBandPassPole(i, c1);
		}

		
		void getZero(int i, Complex c1) {
			getChebyZero(i / 2, c1, pi * .5);
			bandPassXform(i, c1);
		}

		
		int getPoleCount() {
			return n * 2;
		}

		
		int getZeroCount() {
			return n * 2;
		}

		
		void getInfo(String x[]) {
			x[0] = "Inv Cheby (IIR), " + getPoleCount() + "-pole";
			getInfoBandPass(x, this instanceof InvChebyBandStop);
			getInfoCheby(x);
		}
	}

	class InvChebyBandStop extends InvChebyBandPass {
		
		void getPole(int i, Complex c1) {
			getBandStopPole(i, c1);
		}

		
		void getZero(int i, Complex c1) {
			getChebyZero(i / 2, c1, pi * .5);
			bandStopXform(i, c1);
		}
	}

	abstract class EllipticFilterType extends PoleFilterType {
		void selectElliptic(int s) {
			auxLabels[s].setText("Passband Ripple");
			auxBars[s].setValue(60);
			auxLabels[s + 1].setText("Transition Band Width");
			auxBars[s + 1].setValue(100);
		}

		double p0, q;
		double zeros[];
		double K, Kprime;

		double c1[] = new double[100];
		double b1[] = new double[100];
		double a1[] = new double[100];
		double d1[] = new double[100];
		double q1[] = new double[100];
		double z1[] = new double[100];
		double f1[] = new double[100];
		double s1[] = new double[100];
		double p[] = new double[100];
		double zw1[] = new double[100];
		double zf1[] = new double[100];
		double zq1[] = new double[100];
		double rootR[] = new double[100];
		double rootI[] = new double[100];
		int nin;
		int m, n2, em;
		double e;

		void setupElliptic(int a) {
			double rp = auxBars[a].getValue() / 25.;
			// System.out.println("rp = " + rp);
			double e2 = Math.pow(10, rp * .1) - 1;
			// System.out.println("e2 = " + e2 + " e = " + Math.sqrt(e2));
			// xi = 1/k
			double xi = (Math.exp(auxBars[a + 1].getValue() / 1000.) - 1) * 5 + 1;
			// System.out.println("xi " + xi);
			Kprime = ellipticK(Math.sqrt(1 - 1 / (xi * xi)));
			K = ellipticK(1 / xi);
			int ni = (n & 1) == 1 ? 0 : 1;
			int i;
			double f[] = new double[n / 2 + 1];
			zeros = new double[n + 1];
			for (i = 1; i <= n / 2; i++) {
				double u = (2 * i - ni) * K / n;
				double sn = calcSn(u);
				sn *= 2 * pi / K;
				f[i] = zeros[i - 1] = 1 / sn;
				// System.out.println("zero " + i + " " + zeros[i-1]);
			}
			zeros[n / 2] = 1e30;
			double fb = 1 / (2 * pi);
			nin = n % 2;
			n2 = n / 2;
			double f1[] = new double[n2 + 1];
			for (i = 1; i <= n2; i++) {
				double x = f[n2 + 1 - i];
				z1[i] = Math.sqrt(1 - 1 / (x * x));
			}
			double ee = Math.pow(10, .1 * rp) - 1;
			// System.out.println("ee " + ee);
			e = Math.sqrt(ee);
			double fbb = fb * fb;
			m = nin + 2 * n2;
			em = 2 * (m / 2);
			double tp = 2 * pi;
			calcfz();
			calcqz();
			if (m > em) {
				c1[2 * m] = 0;
			}
			for (i = 0; i <= 2 * m; i += 2) {
				a1[m - i / 2] = c1[i] + d1[i];
			}
			double a0 = factorFinder(m);
			int r = 0;
			while (r < em / 2) {
				r++;
				p[r] /= 10;
				q1[r] /= 100;
				double d = 1 + p[r] + q1[r];
				b1[r] = (1 + p[r] / 2) * fbb / d;
				zf1[r] = fb / Math.pow(d, .25);
				zq1[r] = 1 / Math.sqrt(Math.abs(2 * (1 - b1[r] / (zf1[r] * zf1[r]))));
				zw1[r] = tp * zf1[r];
				rootR[r] = -.5 * zw1[r] / zq1[r];
				rootR[r + em / 2] = rootR[r];
				rootI[r] = .5 * Math
						.sqrt(Math.abs(zw1[r] * zw1[r] / (zq1[r] * zq1[r]) - 4 * zw1[r] * zw1[r]));
				rootI[r + em / 2] = -rootI[r];
				// System.out.println(r + " " + rootR[r] + " " + rootI[r]);
			}
			if (a0 != 0) {
				rootR[r + 1 + em / 2] = -Math.sqrt(fbb / (.1 * a0 - 1)) * tp;
				rootI[r + 1 + em / 2] = 0;
			}
		}

		void calcfz() {
			// calculate f(z)
			int i = 1;
			if (nin == 1) {
				s1[i++] = 1;
			}
			for (; i <= nin + n2; i++) {
				s1[i] = s1[i + n2] = z1[i - nin];
			}
			genProductPoly(nin + 2 * n2);
			for (i = 0; i <= em; i += 2) {
				a1[i] = e * b1[i];
			}
			for (i = 0; i <= 2 * em; i += 2) {
				calcfz2(i);
			}
		}

		// generate the product of (z+s1[i]) for i = 1 .. sn and store it in
		// b1[]
		// (i.e. f[z] = b1[0] + b1[1] z + b1[2] z^2 + ... b1[sn] z^sn)
		void genProductPoly(int sn) {
			b1[0] = s1[1];
			b1[1] = 1;
			int i, j;
			for (j = 2; j <= sn; j++) {
				a1[0] = s1[j] * b1[0];
				for (i = 1; i <= j - 1; i++) {
					a1[i] = b1[i - 1] + s1[j] * b1[i];
				}
				for (i = 0; i != j; i++) {
					b1[i] = a1[i];
				}
				b1[j] = 1;
			}
		}

		// determine f(z)^2
		void calcfz2(int i) {
			int ji = 0;
			int jf = 0;
			if (i < em + 2) {
				ji = 0;
				jf = i;
			}
			if (i > em) {
				ji = i - em;
				jf = em;
			}
			c1[i] = 0;
			int j;
			for (j = ji; j <= jf; j += 2) {
				c1[i] += a1[j] * a1[i - j] * Math.pow(10, m - i / 2);
			}
		}

		// determine q(z)
		void calcqz() {
			int i;
			for (i = 1; i <= nin; i++) {
				s1[i] = -10;
			}
			for (; i <= nin + n2; i++) {
				s1[i] = -10 * z1[i - nin] * z1[i - nin];
			}
			for (; i <= nin + 2 * n2; i++) {
				s1[i] = s1[i - n2];
			}
			genProductPoly(m);
			int dd = (nin & 1) == 1 ? -1 : 1;
			for (i = 0; i <= 2 * m; i += 2) {
				d1[i] = dd * b1[i / 2];
			}
		}

		double factorFinder(int t) {
			int i;
			double a = 0;
			for (i = 1; i <= t; i++) {
				a1[i] /= a1[0];
			}
			a1[0] = b1[0] = c1[0] = 1;
			int i1 = 0;
			while (true) {
				if (t <= 2) {
					break;
				}
				double p0 = 0, q0 = 0;
				i1++;
				while (true) {
					b1[1] = a1[1] - p0;
					c1[1] = b1[1] - p0;
					for (i = 2; i <= t; i++) {
						b1[i] = a1[i] - p0 * b1[i - 1] - q0 * b1[i - 2];
					}
					for (i = 2; i < t; i++) {
						c1[i] = b1[i] - p0 * c1[i - 1] - q0 * c1[i - 2];
					}
					int x1 = t - 1;
					int x2 = t - 2;
					int x3 = t - 3;
					double x4 = c1[x2] * c1[x2] + c1[x3] * (b1[x1] - c1[x1]);
					if (x4 == 0) {
						x4 = 1e-3;
					}
					double ddp = (b1[x1] * c1[x2] - b1[t] * c1[x3]) / x4;
					p0 += ddp;
					double dq = (b1[t] * c1[x2] - b1[x1] * (c1[x1] - b1[x1])) / x4;
					q0 += dq;
					if (Math.abs(ddp + dq) < 1e-6) {
						break;
					}
				}
				p[i1] = p0;
				q1[i1] = q0;
				a1[1] = a1[1] - p0;
				t -= 2;
				for (i = 2; i <= t; i++) {
					a1[i] -= p0 * a1[i - 1] + q0 * a1[i - 2];
				}
				if (t <= 2) {
					break;
				}
			}
			if (t == 2) {
				i1++;
				p[i1] = a1[1];
				q1[i1] = a1[2];
			}
			if (t == 1) {
				a = -a1[1];
			}
			return a;
		}

		double calcSn(double u) {
			double sn = 0;
			int j;
			// q = modular constant
			double q = Math.exp(-pi * Kprime / K);
			double v = pi * .5 * u / K;
			for (j = 0;; j++) {
				double w = Math.pow(q, j + .5);
				sn += w * Math.sin((2 * j + 1) * v) / (1 - w * w);
				if (w < 1e-7) {
					break;
				}
			}
			return sn;
		}

		double ellipticK(double k) {
			double a[] = new double[50];
			double theta[] = new double[50];
			a[0] = Math.atan(k / Math.sqrt(1 - k * k));
			theta[0] = pi * .5;
			int i = 0;
			while (true) {
				double x = 2 / (1 + Math.sin(a[i])) - 1;
				double y = Math.sin(a[i]) * Math.sin(theta[i]);
				a[i + 1] = Math.atan(Math.sqrt(1 - x * x) / x);
				theta[i + 1] = .5 * (theta[i] + Math.atan(y / Math.sqrt(1 - y * y)));
				double e = 1 - a[i + 1] * 2 / pi;
				i++;
				if (e < 1e-7) {
					break;
				}
				if (i == 49) {
					break;
				}
			}
			int j;
			double p = 1;
			for (j = 1; j <= i; j++) {
				p *= 1 + Math.cos(a[j]);
			}
			double x = pi * .25 + theta[i] / 2;
			return Math.log(Math.tan(x)) * p;
		}

		
		void getSPole(int i, Complex c1, double wc) {
			double tanwc = Math.tan(wc * .5);
			c1.set(rootR[i + 1] * tanwc, rootI[i + 1] * tanwc);
		}

		void getEllipticZero(int i, Complex c1, double wc) {
			double tanwc = Math.tan(wc * .5);
			c1.set(0, zeros[i / 2] * tanwc);
			if ((i & 1) == 1) {
				c1.im = -c1.im;
			}
			bilinearXform(c1);
		}

		void getInfoElliptic(String x[]) {
		}

		
		int getPoleCount() {
			return n;
		}

		
		int getZeroCount() {
			return n;
		}
	}

	class EllipticLowPass extends EllipticFilterType {
		
		int select() {
			int s = selectLowPass();
			selectElliptic(s);
			return s + 2;
		}

		
		void setup() {
			setupLowPass();
			setupElliptic(2);
		}

		
		void getInfo(String x[]) {
			x[0] = "Elliptic (IIR), " + getPoleCount() + "-pole";
			getInfoLowPass(x);
			getInfoElliptic(x);
		}

		
		void getZero(int i, Complex c1) {
			getEllipticZero(i, c1, wc);
		}
	}

	class EllipticHighPass extends EllipticLowPass {
		
		void getPole(int i, Complex c1) {
			getSPole(i, c1, pi - wc);
			bilinearXform(c1);
			c1.mult(-1);
		}

		
		void getZero(int i, Complex c1) {
			getEllipticZero(i, c1, pi - wc);
			c1.mult(-1);
		}
	}

	class EllipticBandPass extends EllipticFilterType {
		
		int select() {
			int s = selectBandPass();
			auxBars[2].setValue(5);
			selectElliptic(s);
			return s + 2;
		}

		
		void setup() {
			setupBandPass();
			setupElliptic(3);
		}

		
		void getPole(int i, Complex c1) {
			getBandPassPole(i, c1);
		}

		
		void getZero(int i, Complex c1) {
			getEllipticZero(i / 2, c1, pi * .5);
			bandPassXform(i, c1);
		}

		
		int getPoleCount() {
			return n * 2;
		}

		
		int getZeroCount() {
			return n * 2;
		}

		
		void getInfo(String x[]) {
			x[0] = "Elliptic (IIR), " + getPoleCount() + "-pole";
			getInfoBandPass(x, this instanceof EllipticBandStop);
			getInfoElliptic(x);
		}
	}

	class EllipticBandStop extends EllipticBandPass {
		
		void getPole(int i, Complex c1) {
			getBandStopPole(i, c1);
		}

		
		void getZero(int i, Complex c1) {
			getEllipticZero(i / 2, c1, pi * .5);
			bandStopXform(i, c1);
		}
	}

	class CombFilter extends IIRFilterType {
		int n, sign;
		double mult, peak;

		CombFilter(int s) {
			sign = s;
		}

		
		int select() {
			auxLabels[0].setText("1st Pole");
			auxBars[0].setValue(60);
			auxLabels[1].setText("Sharpness");
			auxBars[1].setValue(700);
			return 2;
		}

		
		void setup() {
			n = 2000 / auxBars[0].getValue();
			mult = auxBars[1].getValue() / 1000.;
			peak = 1 / (1 - mult);
		}

		
		void getPole(int i, Complex c1) {
			int odd = sign == 1 ? 0 : 1;
			c1.setMagPhase(Math.pow(mult, 1. / n), pi * (odd + 2 * i) / n);
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[] { 1 / peak, 0 };
			f.bList = new double[] { 0, -sign * mult };
			f.nList = new int[] { 0, n };
			setResponse(f);
			return f;
		}

		
		void getInfo(String x[]) {
			x[0] = "Comb (IIR); Resonance every " + getOmegaText(2 * pi / n);
			x[1] = "Delay: " + n + " samples, " + getUnitText(n / (double) sampleRate, "s");
			double tl = 340. * n / (sampleRate * 2);
			x[2] = "Tube length: " + getUnitText(tl, "m");
			if (sign == -1) {
				x[2] += " (closed)";
			} else {
				x[2] += " (open)";
			}
		}

		
		int getPoleCount() {
			return n;
		}

		
		int getZeroCount() {
			return n;
		}

		
		void getZero(int i, Complex c1) {
			c1.set(0);
		}
	}

	String getUnitText(double v, String u) {
		double va = Math.abs(v);
		if (va < 1e-17) {
			return "0 " + u;
		}
		if (va < 1e-12) {
			return showFormat.format(v * 1e15) + " f" + u;
		}
		if (va < 1e-9) {
			return showFormat.format(v * 1e12) + " p" + u;
		}
		if (va < 1e-6) {
			return showFormat.format(v * 1e9) + " n" + u;
		}
		if (va < 1e-3) {
			return showFormat.format(v * 1e6) + " \u03bc" + u;
		}
		if (va < 1e-2 || u.compareTo("m") != 0 && va < 1) {
			return showFormat.format(v * 1e3) + " m" + u;
		}
		if (va < 1) {
			return showFormat.format(v * 1e2) + " c" + u;
		}
		if (va < 1e3) {
			return showFormat.format(v) + " " + u;
		}
		if (va < 1e6) {
			return showFormat.format(v * 1e-3) + " k" + u;
		}
		if (va < 1e9) {
			return showFormat.format(v * 1e-6) + " M" + u;
		}
		if (va < 1e12) {
			return showFormat.format(v * 1e-9) + " G" + u;
		}
		if (va < 1e15) {
			return showFormat.format(v * 1e-12) + " T" + u;
		}
		return v + " " + u;
	}

	class InverseCombFilter extends FIRFilterType {
		int n;
		double mult, peak;

		
		int select() {
			auxLabels[0].setText("2nd Zero");
			auxBars[0].setValue(60);
			auxLabels[1].setText("Sharpness");
			auxBars[1].setValue(1000);
			return 2;
		}

		
		void setup() {
			n = 1990 / auxBars[0].getValue();
			mult = auxBars[1].getValue() / 1000.;
			peak = 1 + mult;
		}

		
		void getZero(int i, Complex c1) {
			c1.setMagPhase(Math.pow(mult, 1. / n), pi * 2 * i / n);
		}

		
		int getZeroCount() {
			return n;
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[] { 1 / peak, -mult / peak };
			f.nList = new int[] { 0, n };
			setResponse(f);
			return f;
		}

		
		void getInfo(String x[]) {
			x[0] = "Inverse Comb (FIR)";
			x[1] = "Zeros every " + getOmegaText(2 * pi / n);
		}
	}

	class DelayFilter extends CombFilter {
		DelayFilter() {
			super(1);
		}

		
		void getResponse(double w, Complex c) {
			if (n > 212) {
				c.set(1);
			} else {
				super.getResponse(w, c);
			}
		}

		
		void setCutoff(double f) {
		}

		
		int select() {
			auxLabels[0].setText("Delay");
			auxBars[0].setValue(300);
			auxLabels[1].setText("Strength");
			auxBars[1].setValue(700);
			return 2;
		}

		
		void setup() {
			n = auxBars[0].getValue() * 16384 / 1000;
			mult = auxBars[1].getValue() / 1250.;
			peak = 1 / (1 - mult);
		}

		
		void getInfo(String x[]) {
			x[0] = "Delay (IIR)";
			x[1] = "Delay: " + n + " samples, " + getUnitText(n / (double) sampleRate, "s");
			double tl = 340. * n / sampleRate / 2;
			x[2] = "Echo Distance: " + getUnitText(tl, "m");
			if (tl > 1) {
				x[2] += " (" + showFormat.format(tl * 3.28084) + " ft)";
			}
		}
	}

	class ResonatorFilter extends IIRFilterType {
		double r, wc;

		
		int select() {
			auxLabels[0].setText("Resonant Frequency");
			auxBars[0].setValue(500);
			auxLabels[1].setText("Sharpness");
			auxBars[1].setValue(900);
			return 2;
		}

		
		void setup() {
			wc = auxBars[0].getValue() * pi / 1000.;
			double rolldb = -auxBars[1].getValue() * 3 / 1000.;
			r = 1 - Math.pow(10, rolldb);
		}

		
		void getPole(int i, Complex c1) {
			c1.setMagPhase(r, i == 1 ? -wc : wc);
		}

		
		int getPoleCount() {
			return 2;
		}

		
		void getInfo(String x[]) {
			x[0] = "Reson (IIR)";
			x[1] = "Res. Frequency: " + getOmegaText(wc);
		}
	}

	class ResonatorZeroFilter extends ResonatorFilter {
		
		int getZeroCount() {
			return 2;
		}

		
		void getZero(int i, Complex c1) {
			c1.set(i == 0 ? 1 : -1);
		}
	}

	class NotchFilter extends IIRFilterType {
		double wc, a, b, bw;

		
		int select() {
			auxLabels[0].setText("Notch Frequency");
			auxBars[0].setValue(500);
			auxLabels[1].setText("Bandwidth");
			auxBars[1].setValue(900);
			return 2;
		}

		
		void setup() {
			wc = auxBars[0].getValue() * pi / 1000.;
			bw = auxBars[1].getValue() * pi / 2000.;
			a = (1 - Math.tan(bw / 2)) / (1 + Math.tan(bw / 2));
			b = Math.cos(wc);
		}

		
		void getPole(int i, Complex c1) {
			c1.set(-4 * a + (b + a * b) * (b + a * b));
			c1.sqrt();
			if (i == 1) {
				c1.mult(-1);
			}
			c1.add(b + a * b);
			c1.mult(.5);
		}

		
		int getPoleCount() {
			return 2;
		}

		
		void getInfo(String x[]) {
			x[0] = "Notch (IIR)";
			x[1] = "Notch Frequency: " + getOmegaText(wc);
			x[2] = "Bandwidth: " + getOmegaText(bw);
		}

		
		int getZeroCount() {
			return 2;
		}

		
		void getZero(int i, Complex c1) {
			c1.set(b * b - 1);
			c1.sqrt();
			if (i == 1) {
				c1.mult(-1);
			}
			c1.add(b);
		}
	}

	class AllPassFilter extends IIRFilterType {
		double a;

		
		int select() {
			auxLabels[0].setText("Phase Delay");
			auxBars[0].setValue(500);
			return 1;
		}

		
		void setup() {
			double delta = auxBars[0].getValue() / 1000.;
			a = (1 - delta) / (1 + delta);
		}

		
		void getPole(int i, Complex c1) {
			c1.set(-a);
		}

		
		int getPoleCount() {
			return 1;
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[2];
			f.bList = new double[2];
			f.nList = new int[] { 0, 1 };
			f.aList[0] = a;
			f.aList[1] = 1;
			f.bList[0] = 1;
			f.bList[1] = a;
			setResponse(f);
			return f;
		}

		
		void getInfo(String x[]) {
			x[0] = "Allpass Fractional Delay (IIR)";
		}
	}

	class PluckedStringFilter extends IIRFilterType {
		int n;
		double mult;

		
		int select() {
			auxLabels[0].setText("Fundamental");
			auxBars[0].setValue(20);
			auxLabels[1].setText("Sharpness");
			auxBars[1].setValue(970);
			return 2;
		}

		
		void setup() {
			n = 2000 / auxBars[0].getValue();
			mult = .5 * Math.exp(-.5 + auxBars[1].getValue() / 2000.);
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[] { 1, 1, 0, 0 };
			f.bList = new double[] { 1, 0, -mult, -mult };
			f.nList = new int[] { 0, 1, n, n + 1 };
			setResponse(f);
			return f;
		}

		
		void getInfo(String x[]) {
			x[0] = "Plucked String (IIR); Resonance every " + getOmegaText(2 * pi / n);
			x[1] = "Delay: " + n + " samples, " + getUnitText(n / (double) sampleRate, "s");
		}
	}

	class GaussianFilter extends FIRFilterType {
		int n;
		double cw;

		
		int select() {
			auxLabels[0].setText("Offset");
			auxBars[0].setMaximum(1000);
			auxBars[0].setValue(100);
			auxLabels[1].setText("Width");
			auxBars[1].setMaximum(1000);
			auxBars[1].setValue(100);
			auxLabels[2].setText("Order");
			auxBars[2].setMaximum(1600);
			auxBars[2].setValue(160);
			return 3;
		}

		
		void setup() {
			n = auxBars[2].getValue();
			cw = auxBars[0].getValue() * pi / 1000.;
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[n];
			int i;
			double w = auxBars[1].getValue() / 100000.;
			int n2 = n / 2;
			for (i = 0; i != n; i++) {
				int ii = i - n2;
				f.aList[i] = Math.exp(-w * ii * ii) * Math.cos(ii * cw) * getWindow(i, n);
			}
			setResponse(f);
			return f;
		}

		
		boolean needsWindow() {
			return true;
		}

		
		void getInfo(String x[]) {
			x[0] = "Gaussian (FIR)";
			x[1] = "Order: " + n;
		}
	}

	class RandomFilter extends FIRFilterType {
		int n;

		
		int select() {
			auxLabels[0].setText("Order");
			auxBars[0].setMaximum(1600);
			auxBars[0].setValue(100);
			return 1;
		}

		
		void setCutoff(double f) {
		}

		
		void setup() {
			n = auxBars[0].getValue();
			;
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[n];
			int i;
			for (i = 0; i != n; i++) {
				f.aList[i] = random.nextInt() * getWindow(i, n);
			}
			setResponse(f);
			return f;
		}

		
		boolean needsWindow() {
			return true;
		}

		
		void getInfo(String x[]) {
			x[0] = "Random (FIR)";
			x[1] = "Order: " + n;
		}
	}

	class BoxFilter extends FIRFilterType {
		double cw;
		double r, norm;
		int n;

		
		int select() {
			auxLabels[0].setText("Fundamental Freq");
			auxBars[0].setValue(500);
			auxLabels[1].setText("Position");
			auxBars[1].setValue(300);
			auxLabels[2].setText("Length/Width");
			auxBars[2].setValue(100);
			auxLabels[3].setText("Order");
			auxBars[3].setMaximum(1600);
			auxBars[3].setValue(100);
			return 4;
		}

		
		void setCutoff(double f) {
		}

		
		void setup() {
			cw = auxBars[0].getValue() * pi / 1000.;
			if (cw < .147) {
				cw = .147;
			}
			r = auxBars[1].getValue() / 1000.;
			n = auxBars[3].getValue();
			;
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			int nn = 20;
			double ws[][] = new double[nn][nn];
			double mg[][] = new double[nn][nn];
			int i, j, k;
			double px = r * pi;
			double py = pi / 2;
			double ly = auxBars[2].getValue() / 100.;
			for (i = 0; i != nn; i++) {
				for (j = 0; j != nn; j++) {
					ws[i][j] = cw * Math.sqrt(i * i + j * j / ly);
					mg[i][j] = Math.cos(i * px) * Math.cos(j * py);
				}
			}
			mg[0][0] = 0;
			f.aList = new double[n];
			double sum = 0;
			double ecoef = -2.5 / n;
			for (k = 0; k != n; k++) {
				double q = 0;
				for (i = 0; i != nn; i++) {
					for (j = 0; j != nn; j++) {
						double ph = k * ws[i][j];
						q += mg[i][j] * Math.cos(ph);
					}
				}
				f.aList[k] = q * Math.exp(ecoef * k);
				sum += q;
			}
			// normalize
			for (i = 0; i != n; i++) {
				f.aList[i] /= sum;
			}
			setResponse(f);
			return f;
		}

		
		void getInfo(String x[]) {
			x[0] = "Order: " + n;
		}
	}

	abstract class FIRFilterType extends FilterType {
		double response[];

		
		void getResponse(double w, Complex c) {
			if (response == null) {
				c.set(0);
				return;
			}
			int off = (int) (response.length * w / (2 * pi));
			off &= ~1;
			if (off < 0) {
				off = 0;
			}
			if (off >= response.length) {
				off = response.length - 2;
			}
			c.set(response[off], response[off + 1]);
		}

		double getWindow(int i, int n) {
			if (n == 1) {
				return 1;
			}
			double x = 2 * pi * i / (n - 1);
			double n2 = n / 2; // int
			switch (windowChooser.getSelectedIndex()) {
			case 0:
				return 1; // rect
			case 1:
				return .54 - .46 * Math.cos(x); // hamming
			case 2:
				return .5 - .5 * Math.cos(x); // hann
			case 3:
				return .42 - .5 * Math.cos(x) + .08 * Math.cos(2 * x); // blackman
			case 4: {
				double kaiserAlphaPi = kaiserBar.getValue() * pi / 120.;
				double q = 2 * i / (double) n - 1;
				return bessi0(kaiserAlphaPi * Math.sqrt(1 - q * q));
			}
			case 5:
				return i < n2 ? i / n2 : 2 - i / n2; // bartlett
			case 6: {
				double xt = (i - n2) / n2;
				return 1 - xt * xt;
			} // welch
			}
			return 0;
		}

		void setResponse(DirectFilter f) {
			response = new double[8192];
			int i;
			if (f.nList.length != f.aList.length) {
				f.nList = new int[f.aList.length];
				for (i = 0; i != f.aList.length; i++) {
					f.nList[i] = i;
				}
			}
			for (i = 0; i != f.aList.length; i++) {
				response[f.nList[i] * 2] = f.aList[i];
			}
			new FFT(response.length / 2).transform(response, false);
			double maxresp = 0;
			int j;
			for (j = 0; j != response.length; j += 2) {
				double r2 = response[j] * response[j] + response[j + 1] * response[j + 1];
				if (maxresp < r2) {
					maxresp = r2;
				}
			}
			// normalize response
			maxresp = Math.sqrt(maxresp);
			for (j = 0; j != response.length; j++) {
				response[j] /= maxresp;
			}
			for (j = 0; j != f.aList.length; j++) {
				f.aList[j] /= maxresp;
			}
		}
	}

	double bessi0(double x) {
		double ax, ans;
		double y;

		if ((ax = Math.abs(x)) < 3.75) {
			y = x / 3.75;
			y *= y;
			ans = 1.0
					+ y
					* (3.5156229 + y
							* (3.0899424 + y
									* (1.2067492 + y * (0.2659732 + y * (0.360768e-1 + y * 0.45813e-2)))));
		} else {
			y = 3.75 / ax;
			ans = Math.exp(ax)
					/ Math.sqrt(ax)
					* (0.39894228 + y
							* (0.1328592e-1 + y
									* (0.225319e-2 + y
											* (-0.157565e-2 + y
													* (0.916281e-2 + y
															* (-0.2057706e-1 + y
																	* (0.2635537e-1 + y
																			* (-0.1647633e-1 + y * 0.392377e-2))))))));
		}
		return ans;
	}

	class SincLowPassFilter extends FIRFilterType {
		int n;
		double wc, mult, peak;
		double resp[];
		boolean invert;

		
		int select() {
			auxLabels[0].setText("Cutoff Frequency");
			auxLabels[1].setText("Order");
			auxBars[0].setValue(invert ? 500 : 100);
			auxBars[1].setValue(120);
			auxBars[1].setMaximum(1600);
			return 2;
		}

		
		void setup() {
			wc = auxBars[0].getValue() * pi / 1000.;
			n = auxBars[1].getValue();
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[n];
			int n2 = n / 2;
			int i;
			double sum = 0;
			for (i = 0; i != n; i++) {
				int ii = i - n2;
				f.aList[i] = (ii == 0 ? wc : Math.sin(wc * ii) / ii) * getWindow(i, n);
				sum += f.aList[i];
			}
			// normalize
			for (i = 0; i != n; i++) {
				f.aList[i] /= sum;
			}
			if (invert) {
				for (i = 0; i != n; i++) {
					f.aList[i] = -f.aList[i];
				}
				f.aList[n2] += 1;
			}
			if (n == 1) {
				f.aList[0] = 1;
			}
			setResponse(f);
			return f;
		}

		
		void getInfo(String x[]) {
			x[0] = "Cutoff freq: " + getOmegaText(wc);
			x[1] = "Order: " + n;
		}

		
		boolean needsWindow() {
			return true;
		}
	}

	class SincHighPassFilter extends SincLowPassFilter {
		SincHighPassFilter() {
			invert = true;
		}
	}

	class SincBandStopFilter extends FIRFilterType {
		int n;
		double wc1, wc2, mult, peak;
		double resp[];
		boolean invert;

		
		int select() {
			auxLabels[0].setText("Center Frequency");
			auxLabels[1].setText(invert ? "Passband Width" : "Stopband Width");
			auxLabels[2].setText("Order");
			auxBars[0].setValue(500);
			auxBars[1].setValue(50);
			auxBars[2].setValue(140);
			auxBars[2].setMaximum(1600);
			return 3;
		}

		
		void setup() {
			double wcmid = auxBars[0].getValue() * pi / 1000.;
			double width = auxBars[1].getValue() * pi / 1000.;
			wc1 = wcmid - width;
			wc2 = wcmid + width;
			if (wc1 < 0) {
				wc1 = 0;
			}
			if (wc2 > pi) {
				wc2 = pi;
			}
			n = auxBars[2].getValue();
		}

		
		int getPoleCount() {
			return 0;
		}

		
		void getPole(int i, Complex c1) {
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[n + 1];
			double xlist[] = new double[n + 1];
			int n2 = n / 2;
			int i;

			// generate low-pass filter
			double sum = 0;
			for (i = 0; i != n; i++) {
				int ii = i - n2;
				f.aList[i] = (ii == 0 ? wc1 : Math.sin(wc1 * ii) / ii) * getWindow(i, n);
				sum += f.aList[i];
			}
			if (sum > 0) {
				// normalize
				for (i = 0; i != n; i++) {
					f.aList[i] /= sum;
				}
			}

			// generate high-pass filter
			sum = 0;
			for (i = 0; i != n; i++) {
				int ii = i - n2;
				xlist[i] = (ii == 0 ? wc2 : Math.sin(wc2 * ii) / ii) * getWindow(i, n);
				sum += xlist[i];
			}
			// normalize
			for (i = 0; i != n; i++) {
				xlist[i] /= sum;
			}
			// invert and combine with lopass
			for (i = 0; i != n; i++) {
				f.aList[i] -= xlist[i];
			}
			f.aList[n2] += 1;
			if (invert) {
				for (i = 0; i != n; i++) {
					f.aList[i] = -f.aList[i];
				}
				f.aList[n2] += 1;
			}
			if (n == 1) {
				f.aList[0] = 1;
			}
			setResponse(f);
			return f;
		}

		
		void getInfo(String x[]) {
			x[0] = invert ? "Passband: " : "Stopband: ";
			x[0] += getOmegaText(wc1) + " - " + getOmegaText(wc2);
			x[1] = "Order: " + n;
		}

		
		boolean needsWindow() {
			return true;
		}
	}

	class SincBandPassFilter extends SincBandStopFilter {
		SincBandPassFilter() {
			invert = true;
		}
	}

	class MovingAverageFilter extends FIRFilterType {
		double n;
		int ni;

		
		int select() {
			auxLabels[0].setText("Cutoff Frequency");
			auxBars[0].setValue(500);
			return 1;
		}

		
		void setup() {
			n = 2000. / auxBars[0].getValue();
			if (n > 1000) {
				n = 1000;
			}
			ni = (int) n;
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[ni + 1];
			int i;
			for (i = 0; i != ni; i++) {
				f.aList[i] = 1. / n;
			}
			f.aList[i] = (n - ni) / n;
			setResponse(f);
			return f;
		}

		
		void getInfo(String x[]) {
			x[0] = "Moving Average (FIR)";
			x[1] = "Cutoff: " + getOmegaText(2 * pi / n);
			x[2] = "Length: " + showFormat.format(n);
		}
	}

	class TriangleFilter extends FIRFilterType {
		int ni;
		double n;

		
		int select() {
			auxLabels[0].setText("Cutoff Frequency");
			auxBars[0].setValue(500);
			return 1;
		}

		
		void setup() {
			n = 4000. / auxBars[0].getValue();
			if (n > 1000) {
				n = 1000;
			}
			ni = (int) n;
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[ni + 1];
			int i;
			double sum = 0;
			double n2 = n / 2;
			for (i = 0; i < n; i++) {
				double q = 0;
				if (i < n2) {
					q = i / n2;
				} else {
					q = 2 - i / n2;
				}
				sum += q;
				f.aList[i] = q;
			}
			for (i = 0; i != f.aList.length; i++) {
				f.aList[i] /= sum;
			}
			setResponse(f);
			return f;
		}

		
		void getInfo(String x[]) {
			x[0] = "Triangle (FIR)";
			x[1] = "Cutoff: " + getOmegaText(4 * pi / n);
			x[2] = "Length: " + showFormat.format(n);
		}
	}

	double uresp[];

	class CustomFIRFilter extends FIRFilterType {
		CustomFIRFilter() {
			if (uresp == null) {
				uresp = new double[1024];
			}
		}

		
		int select() {
			auxLabels[0].setText("Order");
			auxBars[0].setValue(120);
			auxBars[0].setMaximum(1600);
			int i;
			for (i = 0; i != 512; i++) {
				uresp[i] = 1.;
			}
			return 1;
		}

		
		void setup() {
		}

		double getUserResponse(double w) {
			double q = uresp[(int) (w * uresp.length / pi)];
			return q * q;
		}

		void edit(double x, double x2, double y) {
			int xi1 = (int) (x * uresp.length);
			int xi2 = (int) (x2 * uresp.length);
			for (; xi1 < xi2; xi1++) {
				if (xi1 >= 0 && xi1 < uresp.length) {
					uresp[xi1] = y;
				}
			}
		}

		
		Filter genFilter() {
			int n = auxBars[0].getValue();
			int nsz = uresp.length * 4;
			double fbuf[] = new double[nsz];
			int i;
			int nsz2 = nsz / 2;
			int nsz4 = nsz2 / 2;
			for (i = 0; i != nsz4; i++) {
				double ur = uresp[i] / nsz2;
				fbuf[i * 2] = ur;
				if (i > 0) {
					fbuf[nsz - i * 2] = ur;
				}
			}
			new FFT(nsz2).transform(fbuf, true);

			DirectFilter f = new DirectFilter();
			f.aList = new double[n];
			f.nList = new int[n];
			for (i = 0; i != n; i++) {
				int i2 = (i - n / 2) * 2;
				f.aList[i] = fbuf[i2 & nsz - 1] * getWindow(i, n);
				f.nList[i] = i;
			}
			setResponse(f);
			return f;
		}

		
		void getInfo(String x[]) {
			int n = auxBars[0].getValue();
			x[0] = "Order: " + n;
		}

		
		boolean needsWindow() {
			return true;
		}
	}

	class NoFilter extends FilterType {
		
		void getResponse(double w, Complex c) {
			c.set(1);
		}

		
		Filter genFilter() {
			DirectFilter f = new DirectFilter();
			f.aList = new double[1];
			f.aList[0] = 1;
			return f;
		}
	}

	Complex customPoles[], customZeros[];

	class CustomIIRFilter extends IIRFilterType {
		int npoles, nzeros;

		
		int select() {
			auxLabels[0].setText("# of Pole Pairs");
			auxBars[0].setMaximum(10);
			auxBars[0].setValue(lastPoleCount / 2);
			return 1;
		}

		
		void setup() {
			npoles = nzeros = auxBars[0].getValue() * 2;
		}

		
		void getPole(int i, Complex c1) {
			c1.set(customPoles[i]);
		}

		
		int getPoleCount() {
			return npoles;
		}

		
		void getZero(int i, Complex c1) {
			c1.set(customZeros[i]);
		}

		
		int getZeroCount() {
			return nzeros;
		}

		
		void getInfo(String x[]) {
			x[0] = "Custom IIR";
			x[1] = npoles + " poles and zeros";
		}

		void editPoleZero(Complex c) {
			if (c.mag > 1.1) {
				return;
			}
			if (selectedPole != -1) {
				customPoles[selectedPole].set(c);
				customPoles[selectedPole ^ 1].set(c.re, -c.im);
			}
			if (selectedZero != -1) {
				customZeros[selectedZero].set(c);
				customZeros[selectedZero ^ 1].set(c.re, -c.im);
			}
		}
	}

};
