/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import be.tarsos.midi.TarsosSynth;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.ScalaFile;

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
		
		setShowGrid(false);
		setShowVerticalLines(false);
		setShowHorizontalLines(false);
		
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		SelectionListener selectionListener = new SelectionListener(this);
		getSelectionModel().addListSelectionListener(selectionListener);
		getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);
		BackgroundColorCellRenderer renderer = new BackgroundColorCellRenderer();
		setDefaultRenderer(Object.class,renderer);
		addMouseMotionListener(new MouseOverBackgroundColor(renderer));
		setMaximumSize(new Dimension(650, 160));
	}
	
	private class MouseOverBackgroundColor extends MouseMotionAdapter {
		BackgroundColorCellRenderer renderer;
		public MouseOverBackgroundColor(BackgroundColorCellRenderer renderer){
			this.renderer = renderer;
		}
		public void mouseMoved(MouseEvent e) {
			JTable aTable = (JTable) e.getSource();
			renderer.hoveredCol = aTable.columnAtPoint(e.getPoint());
			renderer.hoveredRow = aTable.rowAtPoint(e.getPoint());
			boolean evenRowAndColumn = (renderer.hoveredRow %2 == 0 && renderer.hoveredCol %2 == 0 );
			boolean oddRowAndColumn = (renderer.hoveredRow %2 != 0 && renderer.hoveredCol %2 != 0 );
			if(evenRowAndColumn || oddRowAndColumn ){
				aTable.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
			}else {
				aTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
			aTable.repaint();
		}
	}

	@Override
	public String getToolTipText(final MouseEvent e) {
		String tip = null;
		java.awt.Point p = e.getPoint();
		int rowIndex = rowAtPoint(p);
		int colIndex = columnAtPoint(p);
		int realColumnIndex = convertColumnIndexToModel(colIndex);

		if (getValueAt(rowIndex, realColumnIndex) != null) {
			tip = PitchUnit.closestRatio((Long) getValueAt(rowIndex, realColumnIndex));
		}
		return tip;
	}

	public void scaleChanged(final double[] newScale, final boolean isChanging, boolean shiftHisto) {
		ScalaFile newFile = new ScalaFile("hmmm", newScale);
		setModel(new ToneScaleTableModel(newFile));
	}

	private static class SelectionListener implements ListSelectionListener {
		private final JTable table;
		private int previousRow;
		private int previousCol;

		public SelectionListener(final JTable theTable) {
			table = theTable;
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
					TarsosSynth.getInstance().playRelativeCents(pitch, 100);
					column = tableModel.getRightIndex(row, col);
					pitch = (Long) tableModel.getValueAt(0, column);
					TarsosSynth.getInstance().playRelativeCents(pitch, 100);
				} else if (row == 0 && table.getValueAt(0, col) != null) {
					ToneScaleTableModel tableModel = (ToneScaleTableModel) table.getModel();
					long pitch = (Long) tableModel.getValueAt(0, col);
					TarsosSynth.getInstance().playRelativeCents(pitch, 100);
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
		
		public int hoveredRow=-1;
		public int hoveredCol=-1;

		public BackgroundColorCellRenderer() {
			super();
		}
		
		void setBackground(int row, int column, int rowCount, int columnCount){
			boolean evenRowAndColumn = (hoveredRow %2 == 0 && hoveredCol %2 == 0 );
			boolean oddRowAndColumn = (hoveredRow %2 != 0 && hoveredCol %2 != 0 );
			if(evenRowAndColumn || oddRowAndColumn ){
				int columnLeft=hoveredCol;
				int columnRight=hoveredCol;
				setBackground(UIManager.getColor("Panel.background"));
				for(int r = hoveredRow; r >=0 ; r--){
					if((columnLeft == column || columnRight == column) && r == row){
						setBackground(Color.GRAY);
					}
					columnLeft = columnLeft==0 ? columnCount-1 : columnLeft-1;
					columnRight++;
					columnRight%=columnCount;
				}
			} else{
				setBackground(UIManager.getColor("Panel.background"));
			}
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object color, boolean isSelected,
				boolean hasFocus, int row, int column) {
			setBackground(row,column,table.getRowCount(),table.getColumnCount());
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
			
			if(row==0){
				label.setFont(label.getFont().deriveFont(9).deriveFont(Font.BOLD));
			}else{
				label.setFont(label.getFont().deriveFont(9));
			}
			if(row %2 == 0 && column %2 == 0){
				label.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			}
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
