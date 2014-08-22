package be.tarsos.ui.link.coordinatessystems;

import java.util.ArrayList;
import java.util.List;

public enum Unit {
	MILISECONDS("ms", Quantity.TIME, 1),
	SECONDS("s", Quantity.TIME, 1. / 1000.),
	H_M_S("h:m:s", Quantity.TIME, 1),
	HERTZ("Hz", Quantity.FREQUENCY, 1),
	CENTS("Cents", Quantity.FREQUENCY, 1),
	NONE("", Quantity.NONE, 1./1000.);

	private final Quantity quantity;
	private final String unit;
	private double factor;

	Unit(final String unit, final Quantity quantity, final double factor) {
		this.quantity = quantity;
		this.unit = unit;
		this.factor = factor;
	}

	public final Quantity getQuantity() {
		return quantity;
	}

	public final String getUnit() {
		return unit;
	}

	public final static List<Unit> getUnitsForQuantity(Quantity quantity) {
		List<Unit> l = new ArrayList<Unit>();
		for (Unit u : Unit.values()) {
			if (u.quantity == quantity) {
				l.add(u);
			}
		}
		return l;
	}

	public double getFactor() {
		return factor;
	}

	public void setFactor(double factor) {
		this.factor = factor;
	}

	public double getValueInUnits(double value) {
		return ((double)value * (double)this.factor);
	}

	public float getValueInUnits(float value) {
		return (float)((double)value * (double)this.factor);
	}

	public int getValueInUnits(int value) {
		return (int)Math.round((double)value * (double)this.factor);
	}
}
