package be.hogent.tarsos.util.histogram.peaks;

class DerivativeAndPosition implements Comparable<DerivativeAndPosition> {
    Double derivative;

    // , position;

    DerivativeAndPosition(double derivative, double position) {
        this.derivative = derivative;
        // this.position = position;
    }

    @Override
    public int compareTo(DerivativeAndPosition arg0) {
        return derivative.compareTo(arg0.derivative);
    }

    @Override
    public boolean equals(final Object other) {
        boolean isEqual = false;
        if (other != null && other instanceof DerivativeAndPosition) {
            DerivativeAndPosition otherD = (DerivativeAndPosition) other;
            isEqual = derivative.equals(otherD.derivative);
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        return derivative.hashCode();
    }
}

