package be.hogent.peak;

class DerivativeAndPosition implements Comparable<DerivativeAndPosition>{
	Double derivative,position;
	DerivativeAndPosition(double derivative,double position){
		this.derivative = derivative;
		this.position=position;
	}

	@Override
	public int compareTo(DerivativeAndPosition arg0) {
		return derivative.compareTo(arg0.derivative);
	}
}
