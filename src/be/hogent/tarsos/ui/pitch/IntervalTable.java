package be.hogent.tarsos.ui.pitch;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import be.hogent.tarsos.midi.PitchSynth;
import be.hogent.tarsos.sampled.pitch.PitchConverter;
import be.hogent.tarsos.util.ScalaFile;

public final class IntervalTable extends JTable implements ScaleChangedListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -115156971997410548L;

	public IntervalTable() {
		super(new ToneScaleTableModel(ScalaFile.westernTuning()));
		setRowSelectionAllowed(false);
		setColumnSelectionAllowed(false);
		setCellSelectionEnabled(true);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		SelectionListener selectionListener = new SelectionListener(this);
		getSelectionModel().addListSelectionListener(selectionListener);
		getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);
		setDefaultRenderer(Object.class, new BackgroundColorCellRenderer());
		setMaximumSize(new Dimension(650, 160));
	}

	@Override
	public String getToolTipText(final MouseEvent e) {
		String tip = null;
		java.awt.Point p = e.getPoint();
		int rowIndex = rowAtPoint(p);
		int colIndex = columnAtPoint(p);
		int realColumnIndex = convertColumnIndexToModel(colIndex);

		if (getValueAt(rowIndex, realColumnIndex) != null) {
			tip = PitchConverter.closestRatio((Long) getValueAt(rowIndex, realColumnIndex));
		}
		return tip;
	}

	public void scaleChanged(final double[] newScale, final boolean isChanging) {
		ScalaFile newFile = new ScalaFile("hmmm", newScale);
		setModel(new ToneScaleTableModel(newFile));
		// setRowHeight(getWidth() / getColumnCount());
	}

	private static class SelectionListener implements ListSelectionListener {
		private PitchSynth synth;
		private final JTable table;
		private int previousRow;
		private int previousCol;

		public SelectionListener(final JTable theTable) {
			table = theTable;
			try {
				synth = new PitchSynth();
			} catch (MidiUnavailableException e) {

				e.printStackTrace();
			}
		}

		public void valueChanged(final ListSelectionEvent event) {
			final int row = table.getSelectionModel().getLeadSelectionIndex();
			final int col = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
			boolean inRange = col != -1 && row != -1;
			boolean isPrevious = col == previousCol && row == previousRow;
			if (inRange && !isPrevious) {
				if (row != 0 && table.getValueAt(row, col) != null) {
					System.out.println(String.format("%s %s", row, col));
					ToneScaleTableModel tableModel = (ToneScaleTableModel) table.getModel();
					int column = tableModel.getLeftIndex(row, col);
					long pitch = (Long) tableModel.getValueAt(0, column);
					synth.playRelativeCents(pitch, 100);
					column = tableModel.getRightIndex(row, col);
					pitch = (Long) tableModel.getValueAt(0, column);
					synth.playRelativeCents(pitch, 100);
				} else if (row == 0 && table.getValueAt(0, col) != null) {
					ToneScaleTableModel tableModel = (ToneScaleTableModel) table.getModel();
					long pitch = (Long) tableModel.getValueAt(0, col);
					synth.playRelativeCents(pitch, 100);
				}

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						table.repaint();
					}
				});
			}
			previousCol = col;
			previousRow = row;
		}
	}

	private static class BackgroundColorCellRenderer extends DefaultTableCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2391225695388829883L;

		public BackgroundColorCellRenderer() {
			super();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object color, boolean isSelected,
				boolean hasFocus, int row, int column) {
			int selectedRow = table.getSelectionModel().getLeadSelectionIndex();
			int selectedCol = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
			final Component comp;
			ToneScaleTableModel tableModel = (ToneScaleTableModel) table.getModel();
			boolean thereIsASelection = selectedRow != -1;
			boolean anInterval = thereIsASelection && table.getValueAt(selectedRow, selectedCol) != null;
			boolean isManuallySelected = row == selectedRow && column == selectedCol;
			boolean isOrigin = anInterval
					&& row == 0
					&& (column == tableModel.getLeftIndex(selectedRow, selectedCol) || column == tableModel
							.getRightIndex(selectedRow, selectedCol));

			if (anInterval && (isManuallySelected || isOrigin)) {
				comp = super.getTableCellRendererComponent(table, color, true, hasFocus, row, column);
			} else {
				comp = super.getTableCellRendererComponent(table, color, false, hasFocus, row, column);
			}
			JLabel label = (JLabel) comp;
			label.setFont(label.getFont().deriveFont(9));
			Dimension d = new Dimension(comp.getWidth(), comp.getWidth());
			comp.setMinimumSize(d);
			return comp;
		}
	}

	private static class ToneScaleTableModel extends AbstractTableModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = -7522469052102093072L;

		private final double[] pitches;

		public ToneScaleTableModel(final ScalaFile scalaFile) {
			pitches = scalaFile.getPitches();
			Arrays.sort(pitches);
		}

		public Object getValueAt(final int row, final int col) {
			final Long value;
			if (row == 0 && col % 2 == 0) {
				value = Math.round(pitches[col / 2]);
			} else if (row + col < getColumnCount() && col >= row && (col - row) % 2 == 0) {
				value = Math.round(pitches[(col + row) / 2] - pitches[(col - row) / 2]);
			} else if ((col - row) % 2 == 0) {
				int index = (col + row) / 2 % pitches.length;
				int otherIndex = (pitches.length + (col - row) / 2) % pitches.length;
				value = 1200 - Math.round(pitches[otherIndex] - pitches[index]);
			} else {
				value = null;
			}
			return value;
		}

		public int getLeftIndex(final int row, final int col) {
			final int leftIndex;
			if (row == 0) {
				leftIndex = col;
			} else {
				if (col - 1 >= 0) {
					leftIndex = getLeftIndex(row - 1, col - 1);
				} else {
					leftIndex = getLeftIndex(row - 1, col - 1 + getColumnCount());
				}
			}
			return leftIndex;
		}

		public int getRightIndex(final int row, final int col) {
			final int rightIndex;
			if (row == 0) {
				rightIndex = col;
			} else {
				if (col + 1 < getColumnCount()) {
					rightIndex = getRightIndex(row - 1, col + 1);
				} else {
					rightIndex = getRightIndex(row - 1, col + 1 - getColumnCount());
				}
			}
			return rightIndex;
		}

		public int getRowCount() {
			return pitches.length;
		}

		public int getColumnCount() {
			return pitches.length * 2;
		}
	}
}
