package be.hogent.tarsos.util.histogram.peaks;

class DerivativeAndPosition implements Comparable<DerivativeAndPosition> {
	private final Double derivative;

	DerivativeAndPosition(final double derive) {
		this.derivative = derive;
	}

	public int compareTo(final DerivativeAndPosition arg0) {
		return derivative.compareTo(arg0.derivative);
	}

	
	public boolean equals(final Object other) {
		boolean isEqual = false;
		if (other != null && other instanceof DerivativeAndPosition) {
			final DerivativeAndPosition otherD = (DerivativeAndPosition) other;
			isEqual = derivative.equals(otherD.derivative);
		}
		return isEqual;
	}

	
	public int hashCode() {
		return derivative.hashCode();
	}
}
