/*
 *  Copyright 2006-2007 Columbia University.
 *
 *  This file is part of MEAPsoft.
 *
 *  MEAPsoft is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  MEAPsoft is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MEAPsoft; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA
 *
 *  See the file "COPYING" for the text of the license.
 */

package be.hogent.tarsos.sampled.hpss;

import java.util.Stack;

/**
 * A class to hold a matrix of values that is a ring buffer of columns. Each
 * column will always have the same address, but the accessible columns will
 * change as the matrix grows. The last N columns will always be accessible.
 * 
 * @author Mike Mandel (mim@ee.columbia.edu)
 */

public class RingMatrix {
	private final double[][] M;

	private int writePos;

	long nextFrAddr;

	// Keep some extra double[]s around so we don't have to keep
	// allocating new ones. For thread safety, use a semaphore to keep
	// track of the number of buffers in use.
	private final Stack<double[]> doubleBuffers = new Stack<double[]>();

	private final int dbPoolSize = 4;

	public RingMatrix(int rows, int columns) {
		// System.out.println("rows: " + rows + " columns: " + columns);

		M = new double[columns][rows];

		for (int i = 0; i < dbPoolSize; i++) {
			doubleBuffers.push(new double[rows]);
		}

		for (int i = 0; i < columns; i++) {
			for (int j = 0; j < rows; j++) {
				M[i][j] = 0;
			}
		}

		writePos = 0;
		nextFrAddr = 0;
	}

	// Get a pre-allocated double[] from a pool. If one is not
	// available, block until it is. Array is not guaranteed to contain
	// anything in particular.
	public synchronized double[] checkOutColumn() {

		while (doubleBuffers.empty()) {
			try {
				wait();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		double[] column = doubleBuffers.pop();

		return column;
	}

	// Check in a previously checked out double buffer. Inserts the db
	// into the matrix, but at the same time puts the double[] it's
	// replacing back in the pool of double buffers.
	public synchronized long checkInColumn(double[] db) {
		// System.out.println("M.length: " + M.length);

		if (M.length == 0) {
			System.out.println("M.length can't be zero!");
			return -1;
		}
		// remember the replaced array
		double[] replaced = M[writePos];

		// do the replacement
		M[writePos++] = db;
		writePos %= M.length;
		long frAddr = nextFrAddr++;

		// put the replaced array back in the pool as long as it's not the
		// same array
		if (replaced != db) {
			doubleBuffers.push(replaced);
			notify();
		}

		return frAddr;
	}

	// Return a double[] to the pool of double buffers without inserting
	// it into the matrix. Does nothing if the array is the wrong
	// length.
	public synchronized void returnColumn(double[] db) {
		if (db.length != M[0].length) {
			return;
		}

		doubleBuffers.push(db);
		notify();
	}

	// Get a column from the matrix. Throws
	// ArrayIndexOutOfBoundsException if the column's address is too
	// high (hasn't been written yet) or too low (already overwritten)
	public double[] getColumn(long frAddr) {
		// if(frAddr < nextFrAddr-M.length)
		// throw new ArrayIndexOutOfBoundsException("Frame was already
		// overwritten");
		// if(frAddr >= nextFrAddr)
		// throw new ArrayIndexOutOfBoundsException("Frame has not been written
		// yet");

		// if(frAddr < nextFrAddr-M.length || frAddr >= nextFrAddr || frAddr <
		// 0)
		// the first condition has an off by 3 (actually a few off by
		// one errors from different places that have accumulated -
		// M.length is actually bigger than it should be, M.length is
		// always off by one compared to nextFrAddr...) So this fix is
		// a hack that ignore a few bugs elsewhere...
		if (frAddr < nextFrAddr - M.length - 3 || frAddr >= nextFrAddr || frAddr < 0) {

			// System.out.println("RingMatrix.getColumn bug hit!!! frAddr: " +
			// frAddr +
			// " nextFrAddr: " + nextFrAddr + " M.length: " + M.length);
			return null;
		}
		return M[(int) (frAddr % M.length)];
	}

	// Set a column of the matrix.
	public void setColumn(long frAddr, double[] dat) {
		M[(int) (frAddr % M.length)] = dat;
	}

	public int getRows() {
		return M[0].length;
	}

	public int getColumns() {
		return M.length;
	}

	public long getNextFrAddr() {
		return nextFrAddr;
	}
}
